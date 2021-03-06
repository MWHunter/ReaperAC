package ac.grim.grimac.predictionengine;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.movementTick.MovementTickerHorse;
import ac.grim.grimac.predictionengine.movementTick.MovementTickerPig;
import ac.grim.grimac.predictionengine.movementTick.MovementTickerPlayer;
import ac.grim.grimac.predictionengine.movementTick.MovementTickerStrider;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.predictionengine.predictions.rideable.BoatPredictionEngine;
import ac.grim.grimac.utils.data.AlmostBoolean;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import ac.grim.grimac.utils.threads.CustomThreadPoolExecutor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// This class is how we manage to safely do everything async
// AtomicInteger allows us to make decisions safely - we can get and set values in one processor instruction
// This is the meaning of GrimPlayer.tasksNotFinished
// Stage 0 - All work is done
// Stage 1 - There is more work, number = number of jobs in the queue and running
//
// After finishing doing the predictions:
// If stage 0 - Do nothing
// If stage 1 - Subtract by 1, and add another to the queue
//
// When the player sends a packet and we have to add him to the queue:
// If stage 0 - Add one and add the data to the workers
// If stage 1 - Add the data to the queue and add one
public class MovementCheckRunner {
    public static ConcurrentHashMap<UUID, ConcurrentLinkedQueue<PredictionData>> queuedPredictions = new ConcurrentHashMap<>();
    public static CustomThreadPoolExecutor executor =
            new CustomThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setDaemon(true).build());
    public static ConcurrentLinkedQueue<PredictionData> waitingOnServerQueue = new ConcurrentLinkedQueue<>();

    public static boolean processAndCheckMovementPacket(PredictionData data) {
        // Support teleports without teleport confirmations
        // If the player is in a vehicle when teleported, they will exit their vehicle
        while (true) {
            Pair<Integer, Vector3d> teleportPos = data.player.teleports.peek();
            if (teleportPos == null) break;

            Vector3d position = teleportPos.getSecond();

            if (data.lastTransaction < teleportPos.getFirst()) {
                break;
            }

            if (position.getX() == data.playerX && position.getY() == data.playerY && position.getZ() == data.playerZ) {
                data.player.teleports.poll();
                data.isJustTeleported = true;
                data.player.timerCheck.exempt = 60; // Exempt for 3 seconds on teleport

                continue;
            } else if (data.lastTransaction > teleportPos.getFirst() + 2) {
                data.player.teleports.poll();
                continue;
            }

            break;
        }

        // Handle similar teleports for players in vehicles
        while (true) {
            Pair<Integer, Vector3d> teleportPos = data.player.vehicleTeleports.peek();
            if (teleportPos == null) break;
            if (data.lastTransaction < teleportPos.getFirst()) {
                break;
            }

            if (data.playerVehicle == null) {
                data.player.vehicleTeleports.poll();
                break;
            }

            Vector3d position = teleportPos.getSecond();
            if (position.getX() == data.playerX && position.getY() == data.playerY && position.getZ() == data.playerZ) {
                data.player.teleports.poll();
                data.isJustTeleported = true;
                data.player.timerCheck.exempt = 60; // Exempt for 3 seconds on teleport
            } else if (data.lastTransaction > teleportPos.getFirst() + 1) {
                break;
            }
        }

        // Client sends junk onGround data when they teleport
        if (data.isJustTeleported)
            data.onGround = data.player.packetStateData.packetPlayerOnGround;

        data.player.packetStateData.packetPlayerXRot = data.xRot;
        data.player.packetStateData.packetPlayerYRot = data.yRot;
        data.player.packetStateData.packetPlayerOnGround = data.onGround;

        // Filter out reminder packet for performance and consistency
        // Filter out 1.17 sending multiple identical move packets because Mojang makes great decisions!
        if (data.player.packetStateData.packetPlayerX == data.playerX &&
                data.player.packetStateData.packetPlayerY == data.playerY &&
                data.player.packetStateData.packetPlayerZ == data.playerZ
                && !data.isJustTeleported)
            return false;

        data.player.packetStateData.packetPlayerX = data.playerX;
        data.player.packetStateData.packetPlayerY = data.playerY;
        data.player.packetStateData.packetPlayerZ = data.playerZ;

        if (data.player.tasksNotFinished.getAndIncrement() == 0) {
            executor.runCheck(data);
        } else {
            queuedPredictions.get(data.player.playerUUID).add(data);
        }

        return true;
    }

    public static void check(PredictionData data) {
        GrimPlayer player = data.player;

        if (player.isCheckNotReady = (data.minimumTickRequiredToContinue > GrimAC.getCurrentTick())) {
            return;
        }

        AlmostBoolean tempUsingItem = data.isUsingItem;

        if (data.isUsingItem == AlmostBoolean.TRUE && player.packetStateData.lastSlotSelected != data.itemHeld) {
            tempUsingItem = AlmostBoolean.MAYBE;
        } else {
            // Handle the player dropping food to stop eating
            // We are sync'd to roughly the bukkit thread here
            // Although we don't have inventory lag compensation so we can't fully sync
            // Works unless the player spams their offhand button
            ItemStack mainHand = player.bukkitPlayer.getInventory().getItem(player.bukkitPlayer.getInventory().getHeldItemSlot());
            if (mainHand == null || !Materials.isUsable(mainHand.getType())) {
                tempUsingItem = AlmostBoolean.FALSE;
            }

            if (data.isUsingItem == AlmostBoolean.TRUE && XMaterial.supports(9)) {
                ItemStack offHand = player.bukkitPlayer.getInventory().getItemInOffHand();
                // I don't believe you bukkit that this cannot be null from 1.9 to 1.17
                if (Materials.isUsable(offHand.getType())) {
                    tempUsingItem = AlmostBoolean.TRUE;
                }
            }
        }

        if (data.usingHand != player.lastHand) {
            tempUsingItem = AlmostBoolean.MAYBE;
        }

        player.isUsingItem = tempUsingItem;
        player.lastHand = data.usingHand;

        player.lastVehicle = player.playerVehicle;
        player.playerVehicle = data.playerVehicle == null ? null : player.compensatedEntities.getEntity(data.playerVehicle);
        player.inVehicle = player.playerVehicle != null;

        player.tryingToRiptide = data.isTryingToRiptide;

        player.firstBreadKB = data.firstBreadKB;
        player.possibleKB = data.requiredKB;

        player.firstBreadExplosion = data.firstBreadExplosion;
        player.knownExplosion = data.possibleExplosion;

        player.lastVehicleSwitch++;
        if (player.lastVehicle != player.playerVehicle) {
            if (player.playerVehicle == null) {
                player.lastVehiclePersistent = player.lastVehicle;
            } else {
                player.lastVehiclePersistent = player.playerVehicle;
            }

            player.lastVehicleSwitch = 0;
        }

        // Stop desync where vehicle kb -> player leaves vehicle same tick
        if (player.lastVehicleSwitch < 3) {
            player.knockbackHandler.handlePlayerKb(0);
            player.explosionHandler.handlePlayerExplosion(0);
        }

        player.compensatedWorld.tickUpdates(data.lastTransaction);
        player.compensatedEntities.tickUpdates(data.lastTransaction);
        player.compensatedWorld.tickPlayerInPistonPushingArea();

        if (data.isDummy != player.lastDummy) {
            player.lastVehicleSwitch = 0;
        }
        player.lastDummy = data.isDummy;

        if (!player.inVehicle)
            player.movementSpeed = player.playerMovementSpeed;

        // Store speed for later use (handling sprinting)
        player.tempMovementSpeed = player.movementSpeed;

        // Set position now to support "dummy" riding without control
        // Warning - on pigs and striders players, can turn into dummies independent of whether they have
        // control of the vehicle or not (which could be abused to set velocity to 0 repeatedly and kind
        // of float in the air, although what's the point inside a vehicle?)
        if (data.isDummy) {
            PacketEntity entity = data.playerVehicle != null ? player.compensatedEntities.getEntity(data.playerVehicle) : null;

            // Players on horses that have saddles or players inside boats cannot be dummies
            if (entity == null || (entity instanceof PacketEntityHorse && !((PacketEntityHorse) entity).hasSaddle)
                    || entity.type != EntityType.BOAT) {
                player.lastX = player.x;
                player.lastY = player.y;
                player.lastZ = player.z;

                player.x = data.playerX;
                player.y = data.playerY;
                player.z = data.playerZ;
            }

            return;
        }

        player.uncertaintyHandler.lastTeleportTicks--;
        if (data.isJustTeleported)
            player.uncertaintyHandler.lastTeleportTicks = 0;

        player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);

        player.x = data.playerX;
        player.y = data.playerY;
        player.z = data.playerZ;
        player.xRot = data.xRot;
        player.yRot = data.yRot;

        player.onGround = data.onGround;

        player.lastSprinting = player.isSprinting;
        player.wasFlying = player.isFlying;
        player.wasGliding = player.isGliding;
        player.isSprinting = data.isSprinting;
        player.wasSneaking = player.isSneaking;
        player.isSneaking = data.isSneaking;

        player.isFlying = player.compensatedFlying.canFlyLagCompensated(data.lastTransaction);
        player.isClimbing = Collisions.onClimbable(player);
        player.isGliding = player.compensatedElytra.isGlidingLagCompensated(data.lastTransaction) && !player.isFlying;
        player.specialFlying = player.onGround && !player.isFlying && player.wasFlying || player.isFlying;

        // Stop stuff like clients using elytra in a vehicle...
        // Interesting, on a pig or strider, a player can climb a ladder
        if (player.inVehicle) {
            player.isFlying = false;
            player.isGliding = false;
            player.specialFlying = false;

            if (player.playerVehicle.type != EntityType.PIG && player.playerVehicle.type != EntityType.STRIDER) {
                player.isClimbing = false;
            }
        }

        player.playerWorld = data.playerWorld;
        player.fallDistance = data.fallDistance;

        if (data.isJustTeleported) {
            player.lastX = player.x;
            player.lastY = player.y;
            player.lastZ = player.z;
        }

        // Multiplying by 1.3 or 1.3f results in precision loss, you must multiply by 0.3
        player.movementSpeed += player.isSprinting ? player.movementSpeed * 0.3f : 0;
        player.jumpAmplifier = data.jumpAmplifier;
        player.levitationAmplifier = data.levitationAmplifier;
        player.slowFallingAmplifier = data.slowFallingAmplifier;
        player.dolphinsGraceAmplifier = data.dolphinsGraceAmplifier;
        player.flySpeed = data.flySpeed;

        player.uncertaintyHandler.wasLastOnGroundUncertain = false;

        player.uncertaintyHandler.isSteppingOnSlime = Collisions.hasSlimeBlock(player);

        // This isn't the final velocity of the player in the tick, only the one applied to the player
        player.actualMovement = new Vector(player.x - player.lastX, player.y - player.lastY, player.z - player.lastZ);

        if (data.isJustTeleported) {
            // Don't let the player move if they just teleported
            player.predictedVelocity = new VectorData(new Vector(), VectorData.VectorType.Teleport);
            player.clientVelocity = new Vector();
        } else if (player.bukkitPlayer.isDead() || (player.playerVehicle != null && player.playerVehicle.isDead)) {
            // Dead players can't cheat, if you find a way how they could, open an issue
            player.predictedVelocity = new VectorData(player.actualMovement, VectorData.VectorType.Dead);
            player.clientVelocity = new Vector();
        } else if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_7_10) && player.isFlying ||
                (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_8) && player.bukkitPlayer.getGameMode() == GameMode.SPECTATOR)) {
            // We could technically check spectator but what's the point...
            // Added complexity to analyze a gamemode used mainly by moderators
            // ViaVersion plays with 1.7 player flying speed, don't bother checking them
            // We don't know what ViaVersion is doing as their packet listener is in front of ours
            player.predictedVelocity = new VectorData(player.actualMovement, VectorData.VectorType.Spectator);
            player.clientVelocity = player.actualMovement.clone();
            player.gravity = 0;
            player.friction = 0.91f;
            PredictionEngineNormal.staticVectorEndOfTick(player, player.clientVelocity);
        } else if (player.playerVehicle == null) {
            // Depth strider was added in 1.8
            ItemStack boots = player.bukkitPlayer.getInventory().getBoots();
            if (boots != null && XMaterial.supports(8) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_8)) {
                player.depthStriderLevel = boots.getEnchantmentLevel(Enchantment.DEPTH_STRIDER);
            } else {
                player.depthStriderLevel = 0;
            }

            if (player.canGroundRiptide = (player.lastOnGround && player.tryingToRiptide)) {
                double addedY = Math.min(player.actualMovement.getY(), 1.1999999F);
                player.lastOnGround = false;
                player.lastY += addedY;

                player.boundingBox.offset(0, addedY, 0);
            }

            new PlayerBaseTick(player).doBaseTick();
            player.compensatedWorld.tickPlayerUpdates(data.lastTransaction);

            // Vehicles don't have jumping or that stupid < 0.03 thing
            // If the player isn't on the ground, a packet in between < 0.03 said they did
            // And the player is reasonably touching the ground
            //
            // And the player isn't now near the ground due to a new block placed by the player
            //
            // Give some lenience and update the onGround status
            if (data.didGroundStatusChangeWithoutPositionPacket && !player.lastOnGround
                    && (player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree
                    || !Collisions.isEmpty(player, player.boundingBox.copy().offset(0, -0.03, 0)))) {
                player.lastOnGround = true;
                player.uncertaintyHandler.wasLastOnGroundUncertain = true;
                player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree = true;
            }

            new MovementTickerPlayer(player).livingEntityAIStep();
        } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_9) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9)) {
            // The player and server are both on a version with client controlled entities
            // If either or both of the client server version has server controlled entities
            // The player can't use entities (or the server just checks the entities)
            if (player.playerVehicle.type == EntityType.BOAT) {
                new PlayerBaseTick(player).doBaseTick();
                // Speed doesn't affect anything with boat movement
                new BoatPredictionEngine(player).guessBestMovement(0, player);
            } else if (player.playerVehicle instanceof PacketEntityHorse) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerHorse(player).livingEntityAIStep();
            } else if (player.playerVehicle.type == EntityType.PIG) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerPig(player).livingEntityAIStep();
            } else if (player.playerVehicle.type == EntityType.STRIDER) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerStrider(player).livingEntityAIStep();
            }
        } // If it isn't any of these cases, the player is on a mob they can't control and therefore is exempt

        Vector offsetVector = player.predictedVelocity.vector.clone().subtract(player.actualMovement);
        double offset = offsetVector.length();

        // Exempt 1.7 players from piston checks by giving them 1 block of lenience for any piston pushing
        // ViaVersion is modifying their movement which messes us up
        //
        // This does NOT apply for 1.8 and above players
        // Anyways, 1.7 clients are more used on arena PvP servers or other gamemodes without pistons
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_7_10) && Collections.max(player.uncertaintyHandler.pistonPushing) > 0) {
            offset = Math.max(0, offset - 1);
        }

        ChatColor color;

        if (offset == 0) {
            color = ChatColor.GRAY;
        } else if (offset < 0.0001) {
            color = ChatColor.GREEN;
        } else if (offset < 0.01) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }

        if (player.lastVehicleSwitch < 3) {
            color = ChatColor.GRAY;
            offset = 0;
        }

        // Vanilla can desync with riptide status
        // This happens because of the < 0.03 thing
        // It also happens at random, especially when close to exiting water (because minecraft netcode sucks)
        //
        // We can recover from the near water desync, but we cannot recover from the rain desync and must set the player back
        if (player.tryingToRiptide != player.compensatedRiptide.getCanRiptide() && player.predictedVelocity.hasVectorType(VectorData.VectorType.Trident) && !player.compensatedWorld.containsWater(GetBoundingBox.getPlayerBoundingBox(player, player.lastX, player.lastY, player.lastZ).expand(0.3, 0.3, 0.3)))
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "DESYNC IN RIPTIDE! // todo: setback and exempt player until setback");

        player.uncertaintyHandler.wasLastGravityUncertain = player.uncertaintyHandler.gravityUncertainty != 0;
        player.uncertaintyHandler.lastLastMovementWasZeroPointZeroThree = player.uncertaintyHandler.lastMovementWasZeroPointZeroThree;
        player.uncertaintyHandler.lastMovementWasZeroPointZeroThree = (player.couldSkipTick && player.actualMovement.lengthSquared() < 0.01) || player.predictedVelocity.hasVectorType(VectorData.VectorType.ZeroPointZeroThree);
        player.uncertaintyHandler.lastLastPacketWasGroundPacket = player.uncertaintyHandler.lastPacketWasGroundPacket;
        player.uncertaintyHandler.lastPacketWasGroundPacket = player.uncertaintyHandler.wasLastOnGroundUncertain;
        player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree = !Collisions.isEmpty(player, player.boundingBox.copy().offset(0, -0.03, 0));

        player.knockbackHandler.handlePlayerKb(offset);
        player.explosionHandler.handlePlayerExplosion(offset);
        player.trigHandler.setOffset(offset);
        player.compensatedRiptide.handleRemoveRiptide();

        player.bukkitPlayer.sendMessage("P: " + color + player.predictedVelocity.vector.getX() + " " + player.predictedVelocity.vector.getY() + " " + player.predictedVelocity.vector.getZ());
        player.bukkitPlayer.sendMessage("A: " + color + player.actualMovement.getX() + " " + player.actualMovement.getY() + " " + player.actualMovement.getZ());
        player.bukkitPlayer.sendMessage("O:" + color + offset);

        GrimAC.staticGetLogger().info(player.bukkitPlayer.getName() + " P: " + color + player.predictedVelocity.vector.getX() + " " + player.predictedVelocity.vector.getY() + " " + player.predictedVelocity.vector.getZ());
        GrimAC.staticGetLogger().info(player.bukkitPlayer.getName() + " A: " + color + player.actualMovement.getX() + " " + player.actualMovement.getY() + " " + player.actualMovement.getZ());
        GrimAC.staticGetLogger().info(player.bukkitPlayer.getName() + " O: " + color + offset);
    }
}
