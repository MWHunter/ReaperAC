package org.abyssmc.reaperac.checks.movement;

import net.minecraft.server.v1_16_R3.*;
import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.ReaperAC;
import org.abyssmc.reaperac.events.anticheat.PlayerBaseTick;
import org.abyssmc.reaperac.utils.enums.FluidTag;
import org.abyssmc.reaperac.utils.enums.MoverType;
import org.abyssmc.reaperac.utils.math.Mth;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Wall;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.stream.Stream;

public class MovementVelocityCheck implements Listener {
    private static final double jumpingEpsilon = 0.01d;
    private static final double maxUpStep = 0.6f;
    private static final double fluidJumpThreshold = 0.04d;

    private Player bukkitPlayer;
    private GrimPlayer grimPlayer;

    // Entity line 1046
    // TODO: I could reverse this so that the vector is used to get the degrees
    private static Vector getInputVector(Vector vec3, float f, float f2) {
        // idk why this is needed, but it was fucking up input for other stuff
        double d = vec3.lengthSquared();
        if (d < 1.0E-7) {
            return new Vector();
        }
        Vector vec32 = (d > 1.0 ? vec3.normalize() : vec3).multiply(f);
        float f3 = Mth.sin(f2 * 0.017453292f);
        float f4 = Mth.cos(f2 * 0.017453292f);
        return new Vector(vec32.getX() * (double) f4 - vec32.getZ() * (double) f3,
                vec32.getY(), vec32.getZ() * (double) f4 + vec32.getX() * (double) f3);
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        this.bukkitPlayer = event.getPlayer();
        this.grimPlayer = ReaperAC.playerGrimHashMap.get(bukkitPlayer);

        Location from = event.getFrom();
        Location to = event.getTo();

        grimPlayer.lastTickPosition = from;

        // TODO: LivingEntity: 1882 (fluid adjusted movement)
        grimPlayer.actualMovement = new Vector(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());

        // We can't do everything fully async because getting entities - https://pastebin.com/s0XhgCvV
        // btw I'll move this later to another class - it's just easier to have everything in one class for now
        // Since everything is highly dependent on order

        // TODO: Remove this hack
        new PlayerBaseTick(grimPlayer).doBaseTick();

        // baseTick occurs before this
        livingEntityAIStep();

        ChatColor color;
        double diff = grimPlayer.predictedVelocity.distanceSquared(grimPlayer.actualMovement);

        if (diff < 0.01) {
            color = ChatColor.GREEN;
        } else if (diff < 0.1) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }

        Bukkit.broadcastMessage("P: " + color + grimPlayer.predictedVelocity.getX() + " " + grimPlayer.predictedVelocity.getY() + " " + grimPlayer.predictedVelocity.getZ());
        Bukkit.broadcastMessage("A: " + color + grimPlayer.actualMovement.getX() + " " + grimPlayer.actualMovement.getY() + " " + grimPlayer.actualMovement.getZ());


        // TODO: This is a check for is the player actually on the ground!
        // TODO: This check is wrong with less 1.9+ precision on movement
        if (grimPlayer.isActuallyOnGround != grimPlayer.onGround) {
            //Bukkit.broadcastMessage("Failed on ground, client believes: " + grimPlayer.onGround);
        }

        if (grimPlayer.predictedVelocity.distanceSquared(grimPlayer.actualMovement) > new Vector(0.03, 0.03, 0.03).lengthSquared()) {
            //Bukkit.broadcastMessage(ChatColor.RED + "FAILED MOVEMENT CHECK");
        } else {
            // For better accuracy trust the client's last "close enough" movement (especially important for 1.9)
            // ...this caused some bad bugs.

            // I think this is wrong because the player might have a new position?
            //grimPlayer.predictedVelocity = move(MoverType.SELF, grimPlayer.actualMovement);
        }

        grimPlayer.lastActualMovement = grimPlayer.actualMovement;
    }

    public void livingEntityAIStep() {
        // not sure if this is correct
        // Living Entity line 2153 (fuck, must have switched mappings)
        //clientVelocity.multiply(0.98f);

        // Living Entity line 2153
        // TODO: Extend this check so 1.8 clients don't trigger it
        if (Math.abs(grimPlayer.clientVelocity.getX()) < 0.003D) {
            grimPlayer.clientVelocity.setX(0D);
        }

        if (Math.abs(grimPlayer.clientVelocity.getY()) < 0.003D) {
            grimPlayer.clientVelocity.setY(0D);
        }

        if (Math.abs(grimPlayer.clientVelocity.getZ()) < 0.003D) {
            grimPlayer.clientVelocity.setZ(0D);
        }

        // Now it gets input
        // Now it does jumping and fluid movement

        // Living Entity line 2180
        // We moved this down after everything else is calculated
        //float sidewaysSpeed = 0f;
        //float forwardsSpeed = 1f;

        // random stuff about jumping in liquids
        // TODO: Jumping in liquids
        // We don't have an accurate way to know if the player is jumping, so this will do
        // This is inspired by paper's playerJumpEvent
        // LivingEntity line 2185

        /*if (grimPlayer.lastOnGround && !grimPlayer.onGround && grimPlayer.y > grimPlayer.lastY) {
        //if (this.jumping && this.isAffectedByFluids()) {
            double d7 = this.isInLava() ? this.getFluidHeight(FluidTags.LAVA) : this.getFluidHeight(FluidTags.WATER);
            boolean bl = this.isInWater() && d7 > 0.0;
            if (bl && (!this.onGround || d7 > fluidJumpThreshold)) {
                this.jumpInLiquid(FluidTags.WATER);
            } else if (this.isInLava() && (!this.onGround || d7 > fluidJumpThreshold)) {
                this.jumpInLiquid(FluidTags.LAVA);
            } else if ((this.onGround || bl && d7 <= fluidJumpThreshold) && this.noJumpDelay == 0) {
                this.jumpFromGround();
                this.noJumpDelay = 10;
            }
        } else {
            this.noJumpDelay = 0;
        }*/

        // Living Entity line 2202
        //sidewaysSpeed *= 0.98f;
        //forwardsSpeed *= 0.98f;

        //Vector inputVector = new Vector(sidewaysSpeed, 0, forwardsSpeed);

        // Living entity line 2206
        //livingEntityTravel(inputVector);
        livingEntityTravel();


        //clientVelocity.multiply(0.98f);
    }

    private Vector jumpFromGround() {
        Vector clonedClientVelocity = grimPlayer.clientVelocity.clone();
        float f = getJumpPower();

        if (bukkitPlayer.hasPotionEffect(PotionEffectType.JUMP)) {
            f += 0.1f * (float) (bukkitPlayer.getPotionEffect(PotionEffectType.JUMP).getAmplifier() + 1);
        }

        clonedClientVelocity.setY(f);

        // TODO: Use the stuff from the sprinting packet
        if (bukkitPlayer.isSprinting()) {
            // TODO: Do we use new or old rotation?  It should be new...
            float f2 = grimPlayer.lastXRot * 0.017453292f;
            clonedClientVelocity.add(new Vector(-Mth.sin(f2) * 0.2f, 0.0, Mth.cos(f2) * 0.2f));
        }

        return clonedClientVelocity;
    }

    public float getJumpPower() {
        return 0.42f * getPlayerJumpFactor();
    }

    private float getPlayerJumpFactor() {
        float f = ((CraftBlockData) bukkitPlayer.getWorld().getBlockAt
                (bukkitPlayer.getLocation().getBlockX(), bukkitPlayer.getLocation().getBlockY(), bukkitPlayer.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock().getJumpFactor();
        float f2 = ((CraftBlockData) bukkitPlayer.getWorld().getBlockAt
                (bukkitPlayer.getLocation().getBlockX(), (int) (bukkitPlayer.getBoundingBox().getMinY() - 0.5000001),
                        bukkitPlayer.getLocation().getBlockZ()).getBlockData()).getState().getBlock().getJumpFactor();

        return (double) f == 1.0 ? f2 : f;
    }


    // LivingEntity line 1741
    public void livingEntityTravel() {
        double d = 0.08;

        // TODO: Stop being lazy and rename these variables to be descriptive
        boolean bl = grimPlayer.clientVelocity.getY() <= 0.0;
        if (bl && grimPlayer.bukkitPlayer.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            d = 0.01;
            //this.fallDistance = 0.0f;
        }

        EntityPlayer entityPlayer = grimPlayer.entityPlayer;
        Fluid fluid = entityPlayer.world.getFluid(entityPlayer.getChunkCoordinates());

        double d1;
        float f;
        float f2;
        if (entityPlayer.isInWater() && !grimPlayer.isFlying) {
            d1 = entityPlayer.locY();
            // 0.8F seems hardcoded in
            f = entityPlayer.isSprinting() ? 0.9F : 0.8F;
            float f1 = 0.02F;
            f2 = (float) EnchantmentManager.e(entityPlayer);
            if (f2 > 3.0F) {
                f2 = 3.0F;
            }

            if (!grimPlayer.lastOnGround) {
                f2 *= 0.5F;
            }

            if (f2 > 0.0F) {
                f += (0.54600006F - f) * f2 / 3.0F;
                f1 += (entityPlayer.dN() - f1) * f2 / 3.0F;
            }

            if (entityPlayer.hasEffect(MobEffects.DOLPHINS_GRACE)) {
                f = 0.96F;
            }

            /*
            if (Math.abs(grimPlayer.actualMovement.getY() - grimPlayer.lastActualMovement.getY() - getJumpPower()) < jumpingEpsilon) {
                jumpFromGround();
            }*/

            guessBestMovement(f1);

            if (grimPlayer.bestJumping) {
                grimPlayer.clientVelocity = jumpFromGround();
            }

            grimPlayer.clientVelocity.add(moveRelative(f1, new Vector(grimPlayer.bestX, 0, grimPlayer.bestZ)));

            grimPlayer.clientVelocity = move(MoverType.SELF, grimPlayer.clientVelocity, false);

            if (grimPlayer.horizontalCollision && grimPlayer.entityPlayer.isClimbing()) {
                grimPlayer.clientVelocity.setY(0.2D);
            }

            grimPlayer.clientVelocity = grimPlayer.clientVelocity.multiply(new Vector(f, 0.8F, f));
            grimPlayer.clientVelocity = getFluidFallingAdjustedMovement(d, bl, grimPlayer.clientVelocity);

            if (grimPlayer.horizontalCollision && entityPlayer.e(grimPlayer.clientVelocity.getX(),
                    grimPlayer.clientVelocity.getY() + 0.6000000238418579D - grimPlayer.clientVelocity.getY() + d1,
                    grimPlayer.clientVelocity.getZ())) {
                grimPlayer.clientVelocity = grimPlayer.clientVelocity.multiply(
                        new Vector(grimPlayer.clientVelocity.getX(), 0.30000001192092896D, grimPlayer.clientVelocity.getZ()));
            }
        } else {
            if (entityPlayer.aQ() && entityPlayer.cT() && !entityPlayer.a(fluid.getType())) {
                d1 = grimPlayer.y;

                guessBestMovement(0.02F);
                grimPlayer.clientVelocity.add(moveRelative(0.02F, new Vector(grimPlayer.bestX, 0, grimPlayer.bestZ)));
                grimPlayer.clientVelocity = move(MoverType.SELF, grimPlayer.clientVelocity, false);

                if (grimPlayer.fluidHeight.getOrDefault(FluidTag.LAVA, 0) <= entityPlayer.cx()) {
                    grimPlayer.clientVelocity = grimPlayer.clientVelocity.multiply(new Vector(0.5D, 0.800000011920929D, 0.5D));
                    grimPlayer.clientVelocity = getFluidFallingAdjustedMovement(d, bl, grimPlayer.clientVelocity);
                } else {
                    grimPlayer.clientVelocity = grimPlayer.clientVelocity.multiply(0.5D);
                }

                if (grimPlayer.bukkitPlayer.hasGravity()) {
                    grimPlayer.clientVelocity = grimPlayer.clientVelocity.add(new Vector(0.0D, -d / 4.0D, 0.0D));
                }

                if (grimPlayer.horizontalCollision && entityPlayer.e(grimPlayer.clientVelocity.getX(), grimPlayer.clientVelocity.getY() + 0.6000000238418579D - grimPlayer.y + d1, grimPlayer.clientVelocity.getZ())) {
                    grimPlayer.clientVelocity = new Vector(grimPlayer.clientVelocity.getX(), 0.30000001192092896D, grimPlayer.clientVelocity.getZ());
                }
                // TODO: Do inputs even matter while gliding?  What is there to predict?
            } else if (bukkitPlayer.isGliding()) {
                /*if (grimPlayer.clientVelocity.getY() > -0.5D) {
                    //this.fallDistance = 1.0F;
                }*/

                Vector lookVector = getVectorForRotation(grimPlayer.xRot, grimPlayer.yRot);
                f = grimPlayer.yRot * 0.017453292F;
                double d2 = Math.sqrt(lookVector.getX() * lookVector.getX() + lookVector.getZ() * lookVector.getZ());
                double d3 = grimPlayer.clientVelocity.length();
                double d4 = lookVector.length();
                float f3 = MathHelper.cos(f);
                f3 = (float) ((double) f3 * (double) f3 * Math.min(1.0D, d4 / 0.4D));
                grimPlayer.clientVelocity = grimPlayer.clientVelocity.add(new Vector(0.0D, d * (-1.0D + (double) f3 * 0.75D), 0.0D));
                double d5;
                if (grimPlayer.clientVelocity.getY() < 0.0D && d2 > 0.0D) {
                    d5 = grimPlayer.clientVelocity.getY() * -0.1D * (double) f3;
                    grimPlayer.clientVelocity = grimPlayer.clientVelocity.add(new Vector(lookVector.getX() * d5 / d2, d5, lookVector.getZ() * d5 / d2));
                }

                if (f < 0.0F && d2 > 0.0D) {
                    d5 = d3 * (double) (-MathHelper.sin(f)) * 0.04D;
                    grimPlayer.clientVelocity = grimPlayer.clientVelocity.add(new Vector(-lookVector.getX() * d5 / d2, d5 * 3.2D, -lookVector.getZ() * d5 / d2));
                }

                if (d2 > 0.0D) {
                    grimPlayer.clientVelocity = grimPlayer.clientVelocity.add(new Vector((lookVector.getX() / d2 * d3 - grimPlayer.clientVelocity.getX()) * 0.1D, 0.0D, (lookVector.getZ() / d2 * d3 - grimPlayer.clientVelocity.getZ()) * 0.1D));
                }

                grimPlayer.clientVelocity = grimPlayer.clientVelocity.multiply(new Vector(0.9900000095367432D, 0.9800000190734863D, 0.9900000095367432D));
                grimPlayer.clientVelocity = move(MoverType.SELF, grimPlayer.clientVelocity, false);
                // IDK if there is a possible cheat for anti elytra damage
                /*if (grimPlayer. && !this.world.isClientSide) {
                    d5 = Math.sqrt(c((Vec3D) this.getMot()));
                    double d6 = d3 - d5;
                    float f4 = (float) (d6 * 10.0D - 3.0D);
                    if (f4 > 0.0F) {
                        this.playSound(this.getSoundFall((int) f4), 1.0F, 1.0F);
                        this.damageEntity(DamageSource.FLY_INTO_WALL, f4);
                    }
                }*/

                // Anti stop glide hack or something?  I have no clue.
                /*if (grimPlayer.onGround && !this.world.isClientSide && this.getFlag(7) && !CraftEventFactory.callToggleGlideEvent(this, false).isCancelled()) {
                    this.setFlag(7, false);
                }*/
            } else {
                float blockFriction = getBlockFriction();
                float f6 = grimPlayer.lastOnGround ? blockFriction * 0.91f : 0.91f;

                guessBestMovement(getFrictionInfluencedSpeed(blockFriction));
                //grimPlayer.bestX = 0;
                //grimPlayer.bestZ = 0.98;

                //Bukkit.broadcastMessage("Best movement: " + grimPlayer.bestX + " " + grimPlayer.bestZ);
                //grimPlayer.clientVelocity.add(moveRelative(getFrictionInfluencedSpeed(blockFriction), new Vector(grimPlayer.bestX, 0, grimPlayer.bestZ)));
                //grimPlayer.clientVelocity = move(MoverType.SELF, grimPlayer.clientVelocity);

                if (grimPlayer.bestJumping) {
                    grimPlayer.clientVelocity = jumpFromGround();
                }

                Vector vec37 = handleRelativeFrictionAndCalculateMovement(blockFriction);

                // Okay, this seems to just be gravity stuff
                double d9 = grimPlayer.clientVelocity.getY();
                if (bukkitPlayer.hasPotionEffect(PotionEffectType.LEVITATION)) {
                    d9 += (0.05 * (double) (bukkitPlayer.getPotionEffect(PotionEffectType.LEVITATION).getAmplifier() + 1) - vec37.getY()) * 0.2;
                    //this.fallDistance = 0.0f;
                } else if (bukkitPlayer.getLocation().isChunkLoaded()) {
                    if (bukkitPlayer.hasGravity()) {
                        d9 -= d;
                    }
                } else {
                    d9 = grimPlayer.clientVelocity.getY() > 0.0 ? -0.1 : 0.0;
                }

                grimPlayer.clientVelocity = new Vector(vec37.getX() * (double) f6, d9 * 0.9800000190734863, vec37.getZ() * (double) f6);
            }
        }
    }

    // TODO: this code is shit
    // Seems to work.
    public float getBlockFriction() {
        return ((CraftBlockData) bukkitPlayer.getWorld().getBlockAt
                (bukkitPlayer.getLocation().getBlockX(), (int) (bukkitPlayer.getBoundingBox().getMinY() - 0.5000001),
                        bukkitPlayer.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock().getFrictionFactor();
    }

    // TODO: Do the best guess first for optimization
    public void guessBestMovement(float f) {
        double bestMovementGuess = Integer.MAX_VALUE;

        // Iterate the isJumping values - thanks StackOverflow!
        for (boolean isJumping = false, done = false; !done; done = isJumping, isJumping = true) {
            for (int movementX = -1; movementX <= 1; movementX++) {
                for (int movementZ = -1; movementZ <= 1; movementZ++) {
                    Vector clonedClientVelocity = grimPlayer.clientVelocity.clone();
                    double movementXWithShifting = movementX;
                    double movementZWithShifting = movementZ;

                    if (bukkitPlayer.isSneaking()) {
                        movementXWithShifting *= 0.3;
                        movementZWithShifting *= 0.3;
                    }

                    if (isJumping) {
                        clonedClientVelocity = jumpFromGround();
                    }

                    Vector movementInput = getInputVector(new Vector(movementXWithShifting * 0.98, 0, movementZWithShifting * 0.98), f, bukkitPlayer.getLocation().getYaw());
                    clonedClientVelocity.add(movementInput);
                    clonedClientVelocity = move(MoverType.SELF, clonedClientVelocity, true);

                    double closeness = grimPlayer.actualMovement.clone().subtract(clonedClientVelocity).lengthSquared();

                    if (closeness < bestMovementGuess) {
                        bestMovementGuess = closeness;
                        grimPlayer.bestX = movementXWithShifting * 0.98;
                        grimPlayer.bestZ = movementZWithShifting * 0.98;

                        grimPlayer.bestJumping = isJumping;
                    }
                }
            }
        }
    }

    // Entity line 527
    // TODO: Entity piston and entity shulker (want to) call this method too.
    // I want to transform this into the actual check
    // hmmm. what if I call this method with the player's actual velocity?
    // Sounds good :D
    public Vector move(MoverType moverType, Vector vec3, boolean isPrediction) {
        Vec3D vec32;
        Vector clonedClientVelocity = grimPlayer.clientVelocity.clone();

        // Something about noClip
        // Piston movement exemption
        // What is a motion multiplier?
        clonedClientVelocity = collide(this.maybeBackOffFromEdge(vec3, moverType));
        Vector claimedYAxis = collide(this.maybeBackOffFromEdge(grimPlayer.actualMovement, moverType));

        // THIS INTRODUCES MANY EXPLOITS!
        // But it is worth it in the name of stopping false positives
        // And I can't think of GOOD exploits.
        // TODO: Is there a better way to do this?
        clonedClientVelocity.setY(claimedYAxis.getY());

        // If the Y axis is inaccurate (Most likely due to stepping upward/block collision)
        // I should really separate the X and Y axis checks entirely
        // But right now I'm liking having the code resemble Minecraft's client near 100%
        // It makes it really easy to debug.
        /*if (Math.abs(clonedClientVelocity.getY() - clonedClientVelocity.getY()) > yAxisEpsilon) {
            Vector clientControlsXAxis = collide(this.maybeBackOffFromEdge(grimPlayer.actualMovement.setY(grimPlayer.clientVelocity.getY()), moverType));

            if (clientControlsXAxis.distanceSquared(grimPlayer.clientVelocity) < clonedClientVelocity.distanceSquared(grimPlayer.clientVelocity)) {
                clonedClientVelocity.setY(clientControlsXAxis.getY());
            }
        }*/

        // Avoid overwriting predicted velocity
        // The code after this is ran AFTER the player sends their movements
        if (isPrediction) return clonedClientVelocity;

        // I'm a bit skeptical that this can always be here, but it works for now
        grimPlayer.predictedVelocity = clonedClientVelocity.clone();

        grimPlayer.horizontalCollision = !Mth.equal(vec3.getX(), clonedClientVelocity.getX()) || !Mth.equal(vec3.getZ(), clonedClientVelocity.getZ());
        grimPlayer.verticalCollision = vec3.getY() != clonedClientVelocity.getY();
        grimPlayer.isActuallyOnGround = grimPlayer.verticalCollision && clonedClientVelocity.getY() < 0.0;

        if (vec3.getX() != clonedClientVelocity.getX()) {
            clonedClientVelocity.setX(0);
        }

        if (vec3.getZ() != clonedClientVelocity.getZ()) {
            clonedClientVelocity.setZ(0);
        }

        Block onBlock = getOnBlock();
        if (vec3.getY() != clonedClientVelocity.getY()) {
            if (onBlock.getType() == org.bukkit.Material.SLIME_BLOCK) {
                // TODO: Maybe lag compensate this (idk packet order)
                if (bukkitPlayer.isSneaking()) {
                    clonedClientVelocity.setY(0);
                } else {
                    if (clonedClientVelocity.getY() < 0.0) {
                        clonedClientVelocity.setY(-clonedClientVelocity.getY());
                    }
                }
            } else if (onBlock.getBlockData() instanceof Bed) {
                if (clonedClientVelocity.getY() < 0.0) {
                    clonedClientVelocity.setY(-grimPlayer.clientVelocity.getY() * 0.6600000262260437);
                }
            } else {
                clonedClientVelocity.setY(0);
            }
        }

        // TODO: Block collision code
        // something about resetting fall state - not sure if server has functioning fall distance tracker
        // I'm being hopeful, of course the server's fall distance tracker is broken
        // TODO: Fall damage stuff
        // I need block collision code to accurately do y distance


        float f = getBlockSpeedFactor();
        clonedClientVelocity.multiply(new Vector(f, 1.0, f));

        return clonedClientVelocity;
    }

    // Entity line 686
    private Vector collide(Vector vector) {
        Vec3D vec3 = new Vec3D(vector.getX(), vector.getY(), vector.getZ());

        AxisAlignedBB aABB = grimPlayer.entityPlayer.getBoundingBox();
        VoxelShapeCollision collisionContext = VoxelShapeCollision.a(grimPlayer.entityPlayer);
        VoxelShape voxelShape = grimPlayer.entityPlayer.getWorld().getWorldBorder().c();
        Stream<VoxelShape> stream = VoxelShapes.c(voxelShape, VoxelShapes.a(aABB.shrink(1.0E-7)), OperatorBoolean.AND) ? Stream.empty() : Stream.of(voxelShape);
        Stream<VoxelShape> stream2 = grimPlayer.entityPlayer.getWorld().c(grimPlayer.entityPlayer, aABB.b(vec3), entity -> true);
        StreamAccumulator<VoxelShape> rewindableStream = new StreamAccumulator<>(Stream.concat(stream2, stream));

        Vec3D vec32 = vec3.g() == 0.0 ? vec3 : Entity.a(grimPlayer.entityPlayer, vec3, aABB, grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream);
        boolean bl2 = vec3.x != vec32.x;
        boolean bl3 = vec3.y != vec32.y;
        boolean bl4 = vec3.z != vec32.z;
        boolean bl = grimPlayer.lastOnGround || bl3 && vec3.y < 0.0;
        if (bl && (bl2 || bl4)) {
            Vec3D vec33;
            Vec3D vec34 = Entity.a(grimPlayer.entityPlayer, new Vec3D(vec3.x, maxUpStep, vec3.z), aABB, grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream);
            Vec3D vec35 = Entity.a(grimPlayer.entityPlayer, new Vec3D(0.0, maxUpStep, 0.0), aABB.b(vec3.x, 0.0, vec3.z), grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream);
            if (vec35.y < maxUpStep && Entity.c(vec33 = Entity.a(grimPlayer.entityPlayer, new Vec3D(vec3.x, 0.0, vec3.z), AxisAlignedBB.a(vec35), grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream).e(vec35)) > Entity.c(vec34)) {
                vec34 = vec33;
            }
            if (Entity.c(vec34) > Entity.c(vec32)) {
                Vec3D allowedMovement = Entity.a(grimPlayer.entityPlayer, new Vec3D(0.0, -vec34.y + vec3.y, 0.0), aABB.c(vec34), grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream);
                vec34 = vec34.e(allowedMovement);
                return new Vector(vec34.x, vec34.y, vec34.z);
            }
        }
        return new Vector(vec32.x, vec32.y, vec32.z);
    }

    // MCP mappings PlayerEntity 959
    // Mojang mappings 936
    protected Vector maybeBackOffFromEdge(Vector vec3, MoverType moverType) {
        if (!bukkitPlayer.isFlying() && (moverType == MoverType.SELF || moverType == MoverType.PLAYER) && bukkitPlayer.isSneaking() && isAboveGround()) {
            double d = vec3.getX();
            double d2 = vec3.getZ();
            while (d != 0.0 && ((CraftWorld) bukkitPlayer.getWorld()).getHandle().getCubes(((CraftPlayer) bukkitPlayer).getHandle(),
                    ((CraftPlayer) bukkitPlayer).getHandle().getBoundingBox().d(d, -maxUpStep, 0.0))) {
                if (d < 0.05 && d >= -0.05) {
                    d = 0.0;
                    continue;
                }
                if (d > 0.0) {
                    d -= 0.05;
                    continue;
                }
                d += 0.05;
            }
            while (d2 != 0.0 && ((CraftWorld) bukkitPlayer.getWorld()).getHandle().getCubes(((CraftPlayer) bukkitPlayer).getHandle(),
                    ((CraftPlayer) bukkitPlayer).getHandle().getBoundingBox().d(0.0, -maxUpStep, d2))) {
                if (d2 < 0.05 && d2 >= -0.05) {
                    d2 = 0.0;
                    continue;
                }
                if (d2 > 0.0) {
                    d2 -= 0.05;
                    continue;
                }
                d2 += 0.05;
            }
            while (d != 0.0 && d2 != 0.0 && ((CraftWorld) bukkitPlayer.getWorld()).getHandle().getCubes(((CraftPlayer) bukkitPlayer).getHandle(),
                    ((CraftPlayer) bukkitPlayer).getHandle().getBoundingBox().d(d, -maxUpStep, d2))) {
                d = d < 0.05 && d >= -0.05 ? 0.0 : (d > 0.0 ? (d -= 0.05) : (d += 0.05));
                if (d2 < 0.05 && d2 >= -0.05) {
                    d2 = 0.0;
                    continue;
                }
                if (d2 > 0.0) {
                    d2 -= 0.05;
                    continue;
                }
                d2 += 0.05;
            }
            vec3 = new Vector(d, vec3.getY(), d2);
        }
        return vec3;
    }

    // Entity line 617
    // Heavily simplified (wtf was that original code mojang)
    private Block getOnBlock() {
        Location last = grimPlayer.lastTickPosition;
        Block block1 = last.getWorld().getBlockAt(last.getBlockX(), (int) (last.getY() - 0.2F), last.getBlockZ());
        Block block2 = last.getWorld().getBlockAt(last.getBlockX(), (int) (last.getY() - 1.2F), last.getBlockZ());

        if (block2.getType().isAir()) {
            if (block2 instanceof Fence || block2 instanceof Wall || block2 instanceof Gate) {
                return block2;
            }
        }

        return block1;
    }

    // Entity line 637
    // Seems fine to me.  Haven't found issues here
    public float getBlockSpeedFactor() {
        net.minecraft.server.v1_16_R3.Block block = ((CraftBlockData) bukkitPlayer.getWorld().getBlockAt
                (bukkitPlayer.getLocation().getBlockX(), bukkitPlayer.getLocation().getBlockY(),
                        bukkitPlayer.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock();
        float f = block.getSpeedFactor();
        if (block == net.minecraft.server.v1_16_R3.Blocks.WATER || block == net.minecraft.server.v1_16_R3.Blocks.BUBBLE_COLUMN) {
            return f;
        }
        return (double) f == 1.0 ? ((CraftBlockData) bukkitPlayer.getWorld().getBlockAt
                (bukkitPlayer.getLocation().getBlockX(), (int) (bukkitPlayer.getBoundingBox().getMinY() - 0.5000001),
                        bukkitPlayer.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock().getSpeedFactor() : f;
    }

    // What the fuck is this?
    private boolean isAboveGround() {
        return grimPlayer.lastOnGround || bukkitPlayer.getFallDistance() < maxUpStep && !
                ((CraftWorld) bukkitPlayer.getWorld()).getHandle().getCubes(((CraftPlayer) bukkitPlayer).getHandle(), ((CraftPlayer) bukkitPlayer).getHandle().getBoundingBox().d(0.0, bukkitPlayer.getFallDistance() - maxUpStep, 0.0));
    }

    // Line 1871 LivingEnti
    public Vector handleRelativeFrictionAndCalculateMovement(float f) {
        f = this.getFrictionInfluencedSpeed(f);
        // TODO: Handle on climbable method

        grimPlayer.clientVelocity.add(moveRelative(f, new Vector(grimPlayer.bestX, 0, grimPlayer.bestZ)));
        grimPlayer.clientVelocity = move(MoverType.SELF, grimPlayer.clientVelocity, false);


        return grimPlayer.clientVelocity;
    }

    public Vector moveRelative(float f, Vector vec3) {
        return getInputVector(vec3, f, bukkitPlayer.getLocation().getYaw());
    }

    // Verified.  This is correct.
    private float getFrictionInfluencedSpeed(float f) {
        if (grimPlayer.lastOnGround) {
            return (float) (bukkitPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() * (0.21600002f / (f * f * f)));
        }

        if (bukkitPlayer.isSprinting()) {
            return 0.026f;
        } else {
            return 0.02f;
        }
    }

    // LivingEntity line 1882
    // I have no clue what this does, but it really doesn't matter.  It works.
    public Vector getFluidFallingAdjustedMovement(double d, boolean bl, Vector vec3) {
        if (grimPlayer.bukkitPlayer.hasGravity() && !grimPlayer.bukkitPlayer.isSprinting()) {
            double d2 = bl && Math.abs(vec3.getY() - 0.005) >= 0.003 && Math.abs(vec3.getY() - d / 16.0) < 0.003 ? -0.003 : vec3.getY() - d / 16.0;
            return new Vector(vec3.getX(), d2, vec3.getZ());
        }
        return vec3;
    }

    // Entity line 1243 - (MCP mappings)
    protected final Vector getVectorForRotation(float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180F);
        float f1 = -yaw * ((float) Math.PI / 180F);
        float f2 = MathHelper.cos(f1);
        float f3 = MathHelper.sin(f1);
        float f4 = MathHelper.cos(f);
        float f5 = MathHelper.sin(f);
        return new Vector(f3 * f4, -f5, (double) (f2 * f4));
    }
}