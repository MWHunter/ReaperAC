package ac.grim.grimac.utils.collisions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.*;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.blocks.*;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicFence;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicPane;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicWall;
import ac.grim.grimac.utils.collisions.datatypes.*;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.Boat;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static ac.grim.grimac.utils.nmsImplementations.Materials.matchLegacy;

// Warning for major game updates!
// Do not use an enum for stuff like Axis and other data types not in 1.7
// Meaning only stuff like getDirection() should have enums
//
// An enum will break support for all previous versions which is very bad
// An if statement for new data types is perfectly safe and should be used instead
public enum CollisionData {
    VINE((player, version, block, x, y, z) -> {
        ComplexCollisionBox boxes = new ComplexCollisionBox();

        Set<BlockFace> directions = ((WrappedMultipleFacing) block).getDirections();

        if (directions.contains(BlockFace.UP))
            boxes.add(new HexCollisionBox(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D));

        if (directions.contains(BlockFace.WEST))
            boxes.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D));

        if (directions.contains(BlockFace.EAST))
            boxes.add(new HexCollisionBox(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D));

        if (directions.contains(BlockFace.NORTH))
            boxes.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D));

        if (directions.contains(BlockFace.SOUTH))
            boxes.add(new SimpleCollisionBox(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D));

        return boxes;

    }, XMaterial.VINE.parseMaterial()),


    LIQUID(new SimpleCollisionBox(0, 0, 0, 1f, 0.9f, 1f),
            XMaterial.WATER.parseMaterial(), XMaterial.LAVA.parseMaterial()),

    BREWINGSTAND((player, version, block, x, y, z) -> {
        int base = 0;

        if (version.isNewerThanOrEquals(ClientVersion.v_1_13))
            base = 1;

        return new ComplexCollisionBox(
                new HexCollisionBox(base, 0, base, 16 - base, 2, 16 - base),
                new SimpleCollisionBox(0.4375, 0.0, 0.4375, 0.5625, 0.875, 0.5625));

    }, XMaterial.BREWING_STAND.parseMaterial()),

    BAMBOO((player, version, block, x, y, z) -> {
        // ViaVersion replacement block - sugarcane
        if (version.isOlderThan(ClientVersion.v_1_13_2))
            return NoCollisionBox.INSTANCE;

        // Offset taken from NMS
        long i = (x * 3129871L) ^ (long) z * 116129781L ^ (long) 0;
        i = i * i * 42317861L + i * 11L;
        i = i >> 16;

        return new HexCollisionBox(6.5D, 0.0D, 6.5D, 9.5D, 16.0D, 9.5D).offset((((i & 15L) / 15.0F) - 0.5D) * 0.5D, 0, (((i >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D);
    }, XMaterial.BAMBOO.parseMaterial()),


    BAMBOO_SAPLING((player, version, block, x, y, z) -> {
        long i = (x * 3129871L) ^ (long) z * 116129781L ^ (long) 0;
        i = i * i * 42317861L + i * 11L;
        i = i >> 16;

        return new HexCollisionBox(4.0D, 0.0D, 4.0D, 12.0D, 12.0D, 12.0D).offset((((i & 15L) / 15.0F) - 0.5D) * 0.5D, 0, (((i >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D);
    }, XMaterial.BAMBOO_SAPLING.parseMaterial()),

    COMPOSTER((player, version, block, x, y, z) -> {
        double height = 0.125;

        if (version.isOlderThanOrEquals(ClientVersion.v_1_13_2))
            height = 0.25;

        if (version.isOlderThanOrEquals(ClientVersion.v_1_12_2))
            height = 0.3125;

        return new ComplexCollisionBox(
                new SimpleCollisionBox(0, 0, 0, 1, height, 1),
                new SimpleCollisionBox(0, height, 0, 0.125, 1, 1),
                new SimpleCollisionBox(1 - 0.125, height, 0, 1, 1, 1),
                new SimpleCollisionBox(0, height, 0, 1, 1, 0.125),
                new SimpleCollisionBox(0, height, 1 - 0.125, 1, 1, 1));
    }, XMaterial.COMPOSTER.parseMaterial()),

    RAIL(new SimpleCollisionBox(0, 0, 0, 1, 0.125, 0),
            XMaterial.RAIL.parseMaterial(), XMaterial.ACTIVATOR_RAIL.parseMaterial(),
            XMaterial.DETECTOR_RAIL.parseMaterial(), XMaterial.POWERED_RAIL.parseMaterial()),

    ANVIL((player, version, data, x, y, z) -> {
        // Anvil collision box was changed in 1.13 to be more accurate
        // https://www.mcpk.wiki/wiki/Version_Differences
        // The base is 0.75×0.75, and its floor is 0.25b high.
        // The top is 1×0.625, and its ceiling is 0.375b low.
        if (version.isNewerThanOrEquals(ClientVersion.v_1_13)) {
            ComplexCollisionBox complexAnvil = new ComplexCollisionBox();
            // Base of the anvil
            complexAnvil.add(new HexCollisionBox(2, 0, 2, 14, 4, 14));

            if (((WrappedDirectional) data).getDirection() == BlockFace.NORTH) {
                complexAnvil.add(new HexCollisionBox(4.0D, 4.0D, 3.0D, 12.0D, 5.0D, 13.0D));
                complexAnvil.add(new HexCollisionBox(6.0D, 5.0D, 4.0D, 10.0D, 10.0D, 12.0D));
                complexAnvil.add(new HexCollisionBox(3.0D, 10.0D, 0.0D, 13.0D, 16.0D, 16.0D));
            } else {
                complexAnvil.add(new HexCollisionBox(3.0D, 4.0D, 4.0D, 13.0D, 5.0D, 12.0D));
                complexAnvil.add(new HexCollisionBox(4.0D, 5.0D, 6.0D, 12.0D, 10.0D, 10.0D));
                complexAnvil.add(new HexCollisionBox(0.0D, 10.0D, 3.0D, 16.0D, 16.0D, 13.0D));
            }

            return complexAnvil;
        } else {
            // Just a single solid collision box with 1.12
            if (((WrappedDirectional) data).getDirection() == BlockFace.NORTH) {
                return new SimpleCollisionBox(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F);
            } else {
                return new SimpleCollisionBox(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F);
            }
        }
    }, XMaterial.ANVIL.parseMaterial(), XMaterial.CHIPPED_ANVIL.parseMaterial(), XMaterial.DAMAGED_ANVIL.parseMaterial()),


    WALL(new DynamicWall(), Arrays.stream(Material.values()).filter(mat -> mat.name().contains("WALL")
            && !mat.name().contains("SIGN") && !mat.name().contains("HEAD") && !mat.name().contains("BANNER")
            && !mat.name().contains("FAN") && !mat.name().contains("SKULL") && !mat.name().contains("TORCH"))
            .toArray(Material[]::new)),


    SLAB((player, version, data, x, y, z) -> {
        if (((WrappedSlab) data).isDouble()) {
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
        } else if (((WrappedSlab) data).isBottom()) {
            return new SimpleCollisionBox(0, 0, 0, 1, 0.5, 1);
        }

        return new SimpleCollisionBox(0, 0.5, 0, 1, 1, 1);
        // 1.13 can handle double slabs as it's in the block data
        // 1.12 has double slabs as a separate block, no block data to differentiate it
    }, Arrays.stream(Material.values()).filter(mat -> (mat.name().contains("_SLAB") || mat.name().contains("STEP"))
            && !mat.name().contains("DOUBLE")).toArray(Material[]::new)),

    // Overwrite previous SKULL enum for legacy, where head and wall skull isn't separate
    WALL_SKULL((player, version, data, x, y, z) -> {
        switch (((WrappedDirectional) data).getDirection()) {
            case DOWN:
            default: // On the floor
                return new SimpleCollisionBox(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F);
            case NORTH:
                return new SimpleCollisionBox(0.25F, 0.25F, 0.5F, 0.75F, 0.75F, 1.0F);
            case SOUTH:
                return new SimpleCollisionBox(0.25F, 0.25F, 0.0F, 0.75F, 0.75F, 0.5F);
            case WEST:
                return new SimpleCollisionBox(0.5F, 0.25F, 0.25F, 1.0F, 0.75F, 0.75F);
            case EAST:
                return new SimpleCollisionBox(0.0F, 0.25F, 0.25F, 0.5F, 0.75F, 0.75F);
        }
    }, Arrays.stream(Material.values()).filter(mat -> (mat.name().contains("HEAD") || mat.name().contains("SKULL")) && !mat.name().contains("PISTON")).toArray(Material[]::new)),

    DOOR(new DoorHandler(), Arrays.stream(Material.values()).filter(mat -> mat.name().contains("_DOOR"))
            .toArray(Material[]::new)),

    HOPPER((player, version, data, x, y, z) -> {
        if (version.isNewerThanOrEquals(ClientVersion.v_1_13)) {
            ComplexCollisionBox hopperBox = new ComplexCollisionBox();

            switch (((WrappedDirectional) data).getDirection()) {
                case DOWN:
                    hopperBox.add(new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 4.0D, 10.0D));
                    break;
                case EAST:
                    hopperBox.add(new HexCollisionBox(12.0D, 4.0D, 6.0D, 16.0D, 8.0D, 10.0D));
                    break;
                case NORTH:
                    hopperBox.add(new HexCollisionBox(6.0D, 4.0D, 0.0D, 10.0D, 8.0D, 4.0D));
                    break;
                case SOUTH:
                    hopperBox.add(new HexCollisionBox(6.0D, 4.0D, 12.0D, 10.0D, 8.0D, 16.0D));
                    break;
                case WEST:
                    hopperBox.add(new HexCollisionBox(0.0D, 4.0D, 6.0D, 4.0D, 8.0D, 10.0D));
                    break;
            }

            hopperBox.add(new SimpleCollisionBox(0, 0.625, 0, 1.0, 0.6875, 1.0));
            hopperBox.add(new SimpleCollisionBox(0, 0.6875, 0, 0.125, 1, 1));
            hopperBox.add(new SimpleCollisionBox(0.125, 0.6875, 0, 1, 1, 0.125));
            hopperBox.add(new SimpleCollisionBox(0.125, 0.6875, 0.875, 1, 1, 1));
            hopperBox.add(new SimpleCollisionBox(0.25, 0.25, 0.25, 0.75, 0.625, 0.75));
            hopperBox.add(new SimpleCollisionBox(0.875, 0.6875, 0.125, 1, 1, 0.875));

            return hopperBox;
        } else {
            double height = 0.125 * 5;

            return new ComplexCollisionBox(
                    new SimpleCollisionBox(0, 0, 0, 1, height, 1),
                    new SimpleCollisionBox(0, height, 0, 0.125, 1, 1),
                    new SimpleCollisionBox(1 - 0.125, height, 0, 1, 1, 1),
                    new SimpleCollisionBox(0, height, 0, 1, 1, 0.125),
                    new SimpleCollisionBox(0, height, 1 - 0.125, 1, 1, 1));
        }

    }, XMaterial.HOPPER.parseMaterial()),

    CAKE((player, version, data, x, y, z) -> {
        double height = 0.5;
        if (version.isOlderThan(ClientVersion.v_1_8))
            height = 0.4375;
        double eatenPosition = (1 + ((WrappedCake) data).getSlicesEaten() * 2) / 16D;
        return new SimpleCollisionBox(eatenPosition, 0, 0.0625, 1 - 0.0625, height, 1 - 0.0625);
    }, XMaterial.CAKE.parseMaterial()),


    COCOA_BEANS((player, version, data, x, y, z) -> {
        WrappedCocoaBeans beans = (WrappedCocoaBeans) data;
        int age = beans.getAge();

        // From 1.9 - 1.10, the large cocoa block is the same as the medium one
        // https://bugs.mojang.com/browse/MC-94274
        if (version.isNewerThanOrEquals(ClientVersion.v_1_9_1) && version.isOlderThan(ClientVersion.v_1_11))
            age = Math.min(age, 1);

        switch (beans.getDirection()) {
            case EAST:
                switch (age) {
                    case 0:
                        return new HexCollisionBox(11.0D, 7.0D, 6.0D, 15.0D, 12.0D, 10.0D);
                    case 1:
                        return new HexCollisionBox(9.0D, 5.0D, 5.0D, 15.0D, 12.0D, 11.0D);
                    case 2:
                        return new HexCollisionBox(7.0D, 3.0D, 4.0D, 15.0D, 12.0D, 12.0D);
                }
            case WEST:
                switch (age) {
                    case 0:
                        return new HexCollisionBox(1.0D, 7.0D, 6.0D, 5.0D, 12.0D, 10.0D);
                    case 1:
                        return new HexCollisionBox(1.0D, 5.0D, 5.0D, 7.0D, 12.0D, 11.0D);
                    case 2:
                        return new HexCollisionBox(1.0D, 3.0D, 4.0D, 9.0D, 12.0D, 12.0D);
                }
            case NORTH:
                switch (age) {
                    case 0:
                        return new HexCollisionBox(6.0D, 7.0D, 1.0D, 10.0D, 12.0D, 5.0D);
                    case 1:
                        return new HexCollisionBox(5.0D, 5.0D, 1.0D, 11.0D, 12.0D, 7.0D);
                    case 2:
                        return new HexCollisionBox(4.0D, 3.0D, 1.0D, 12.0D, 12.0D, 9.0D);
                }
            case SOUTH:
                switch (age) {
                    case 0:
                        return new HexCollisionBox(6.0D, 7.0D, 11.0D, 10.0D, 12.0D, 15.0D);
                    case 1:
                        return new HexCollisionBox(5.0D, 5.0D, 9.0D, 11.0D, 12.0D, 15.0D);
                    case 2:
                        return new HexCollisionBox(4.0D, 3.0D, 7.0D, 12.0D, 12.0D, 15.0D);
                }
        }

        return NoCollisionBox.INSTANCE;

    }, XMaterial.COCOA.parseMaterial()),


    STONE_CUTTER((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.v_1_13_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);

        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D);
    }, XMaterial.STONECUTTER.parseMaterial()),

    BELL((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.v_1_13_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);

        Bell bell = (Bell) ((WrappedFlatBlock) data).getBlockData();
        BlockFace direction = bell.getFacing();

        if (bell.getAttachment() == Bell.Attachment.FLOOR) {
            return direction != BlockFace.NORTH && direction != BlockFace.SOUTH ?
                    new HexCollisionBox(4.0D, 0.0D, 0.0D, 12.0D, 16.0D, 16.0D) :
                    new HexCollisionBox(0.0D, 0.0D, 4.0D, 16.0D, 16.0D, 12.0D);

        }

        ComplexCollisionBox complex = new ComplexCollisionBox(
                new HexCollisionBox(5.0D, 6.0D, 5.0D, 11.0D, 13.0D, 11.0D),
                new HexCollisionBox(4.0D, 4.0D, 4.0D, 12.0D, 6.0D, 12.0D));

        if (bell.getAttachment() == Bell.Attachment.CEILING) {
            complex.add(new HexCollisionBox(7.0D, 13.0D, 7.0D, 9.0D, 16.0D, 9.0D));
        } else if (bell.getAttachment() == Bell.Attachment.DOUBLE_WALL) {
            if (direction != BlockFace.NORTH && direction != BlockFace.SOUTH) {
                complex.add(new HexCollisionBox(0.0D, 13.0D, 7.0D, 16.0D, 15.0D, 9.0D));
            } else {
                complex.add(new HexCollisionBox(7.0D, 13.0D, 0.0D, 9.0D, 15.0D, 16.0D));
            }
        } else if (direction == BlockFace.NORTH) {
            complex.add(new HexCollisionBox(7.0D, 13.0D, 0.0D, 9.0D, 15.0D, 13.0D));
        } else if (direction == BlockFace.SOUTH) {
            complex.add(new HexCollisionBox(7.0D, 13.0D, 3.0D, 9.0D, 15.0D, 16.0D));
        } else {
            if (direction == BlockFace.EAST) {
                complex.add(new HexCollisionBox(3.0D, 13.0D, 7.0D, 16.0D, 15.0D, 9.0D));
            } else {
                complex.add(new HexCollisionBox(0.0D, 13.0D, 7.0D, 13.0D, 15.0D, 9.0D));
            }
        }

        return complex;

    }, XMaterial.BELL.parseMaterial()),

    SCAFFOLDING((player, version, data, x, y, z) -> {
        // ViaVersion replacement block - hay block
        if (version.isOlderThanOrEquals(ClientVersion.v_1_13_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);

        Scaffolding scaffolding = (Scaffolding) ((WrappedFlatBlock) data).getBlockData();

        if (player.lastY > y + 1 - 1.0E-5F && !player.isSneaking) {
            return new HexCollisionBox(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        }

        return scaffolding.getDistance() != 0 && scaffolding.isBottom() && player.lastY > y + (1D / 16D) - (double) 1.0E-5F ?
                new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D) :
                NoCollisionBox.INSTANCE;
    }, XMaterial.SCAFFOLDING.parseMaterial()),

    LADDER((player, version, data, x, y, z) -> {
        int width = 3;
        if (version.isOlderThanOrEquals(ClientVersion.v_1_8))
            width = 2;

        switch (((WrappedDirectional) data).getDirection()) {
            case NORTH:
                return new HexCollisionBox(0.0D, 0.0D, 16.0D - width, 16.0D, 16.0D, 16.0D);
            case SOUTH:
                return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, width);
            case WEST:
                return new HexCollisionBox(16.0D - width, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
            default:
            case EAST:
                return new HexCollisionBox(0.0D, 0.0D, 0.0D, width, 16.0D, 16.0D);
        }
    }, XMaterial.LADDER.parseMaterial()),

    CAMPFIRE((player, version, data, x, y, z) -> {
        // ViaVersion replacement block - slab if not lit or fire if lit
        if (version.isOlderThanOrEquals(ClientVersion.v_1_13_2)) {
            WrappedFlatBlock campfire = (WrappedFlatBlock) data;

            if (((Campfire) campfire.getBlockData()).isLit()) {
                return NoCollisionBox.INSTANCE;
            }

            return new HexCollisionBox(0, 0, 0, 16, 8, 16);
        }

        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);
    }, XMaterial.CAMPFIRE.parseMaterial(), XMaterial.SOUL_CAMPFIRE.parseMaterial()),

    LANTERN((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.v_1_12_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);

        WrappedFlatBlock lantern = (WrappedFlatBlock) data;

        if (((Lantern) lantern.getBlockData()).isHanging()) {
            return new ComplexCollisionBox(new HexCollisionBox(5.0D, 1.0D, 5.0D, 11.0D, 8.0D, 11.0D),
                    new HexCollisionBox(6.0D, 8.0D, 6.0D, 10.0D, 10.0D, 10.0D));
        }

        return new ComplexCollisionBox(new HexCollisionBox(5.0D, 0.0D, 5.0D, 11.0D, 7.0D, 11.0D),
                new HexCollisionBox(6.0D, 7.0D, 6.0D, 10.0D, 9.0D, 10.0D));

    }, XMaterial.LANTERN.parseMaterial(), XMaterial.SOUL_LANTERN.parseMaterial()),


    LECTERN((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.v_1_13_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);

        return new ComplexCollisionBox(
                new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), // base
                new HexCollisionBox(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D)); // post
    }, XMaterial.LECTERN.parseMaterial()),


    HONEY_BLOCK((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.v_1_14_4))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);

        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D); // post
    }, XMaterial.HONEY_BLOCK.parseMaterial()),


    DRAGON_EGG_BLOCK(new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D), XMaterial.DRAGON_EGG.parseMaterial()),

    GRINDSTONE((player, version, data, x, y, z) -> {
        Grindstone grindstone = (Grindstone) ((WrappedFlatBlock) data).getBlockData();

        // ViaVersion replacement block - Anvil
        if (version.isOlderThanOrEquals(ClientVersion.v_1_12_2)) {
            // Just a single solid collision box with 1.12
            if (grindstone.getFacing() == BlockFace.NORTH || grindstone.getFacing() == BlockFace.SOUTH) {
                return new SimpleCollisionBox(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F);
            } else {
                return new SimpleCollisionBox(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F);
            }
        }

        if (version.isOlderThanOrEquals(ClientVersion.v_1_13_2)) {
            ComplexCollisionBox complexAnvil = new ComplexCollisionBox();
            // Base of the anvil
            complexAnvil.add(new HexCollisionBox(2, 0, 2, 14, 4, 14));

            if (grindstone.getFacing() == BlockFace.NORTH || grindstone.getFacing() == BlockFace.SOUTH) {
                complexAnvil.add(new HexCollisionBox(4.0D, 4.0D, 3.0D, 12.0D, 5.0D, 13.0D));
                complexAnvil.add(new HexCollisionBox(6.0D, 5.0D, 4.0D, 10.0D, 10.0D, 12.0D));
                complexAnvil.add(new HexCollisionBox(3.0D, 10.0D, 0.0D, 13.0D, 16.0D, 16.0D));
            } else {
                complexAnvil.add(new HexCollisionBox(3.0D, 4.0D, 4.0D, 13.0D, 5.0D, 12.0D));
                complexAnvil.add(new HexCollisionBox(4.0D, 5.0D, 6.0D, 12.0D, 10.0D, 10.0D));
                complexAnvil.add(new HexCollisionBox(0.0D, 10.0D, 3.0D, 16.0D, 16.0D, 13.0D));
            }

            return complexAnvil;
        }

        if (grindstone.getAttachedFace() == FaceAttachable.AttachedFace.FLOOR) {
            if (grindstone.getFacing() == BlockFace.NORTH || grindstone.getFacing() == BlockFace.SOUTH) {
                return new ComplexCollisionBox(new HexCollisionBox(2.0D, 0.0D, 6.0D, 4.0D, 7.0D, 10.0D),
                        new HexCollisionBox(12.0D, 0.0D, 6.0D, 14.0D, 7.0D, 10.0D),
                        new HexCollisionBox(2.0D, 7.0D, 5.0D, 4.0D, 13.0D, 11.0D),
                        new HexCollisionBox(12.0D, 7.0D, 5.0D, 14.0D, 13.0D, 11.0D),
                        new HexCollisionBox(4.0D, 4.0D, 2.0D, 12.0D, 16.0D, 14.0D));
            } else {
                return new ComplexCollisionBox(new HexCollisionBox(6.0D, 0.0D, 2.0D, 10.0D, 7.0D, 4.0D),
                        new HexCollisionBox(6.0D, 0.0D, 12.0D, 10.0D, 7.0D, 14.0D),
                        new HexCollisionBox(5.0D, 7.0D, 2.0D, 11.0D, 13.0D, 4.0D),
                        new HexCollisionBox(5.0D, 7.0D, 12.0D, 11.0D, 13.0D, 14.0D),
                        new HexCollisionBox(2.0D, 4.0D, 4.0D, 14.0D, 16.0D, 12.0D));
            }
        } else if (grindstone.getAttachedFace() == FaceAttachable.AttachedFace.WALL) {
            switch (grindstone.getFacing()) {
                case NORTH:
                    return new ComplexCollisionBox(new HexCollisionBox(2.0D, 6.0D, 7.0D, 4.0D, 10.0D, 16.0D),
                            new HexCollisionBox(12.0D, 6.0D, 7.0D, 14.0D, 10.0D, 16.0D),
                            new HexCollisionBox(2.0D, 5.0D, 3.0D, 4.0D, 11.0D, 9.0D),
                            new HexCollisionBox(12.0D, 5.0D, 3.0D, 14.0D, 11.0D, 9.0D),
                            new HexCollisionBox(4.0D, 2.0D, 0.0D, 12.0D, 14.0D, 12.0D));
                case WEST:
                    return new ComplexCollisionBox(new HexCollisionBox(7.0D, 6.0D, 2.0D, 16.0D, 10.0D, 4.0D),
                            new HexCollisionBox(7.0D, 6.0D, 12.0D, 16.0D, 10.0D, 14.0D),
                            new HexCollisionBox(3.0D, 5.0D, 2.0D, 9.0D, 11.0D, 4.0D),
                            new HexCollisionBox(3.0D, 5.0D, 12.0D, 9.0D, 11.0D, 14.0D),
                            new HexCollisionBox(0.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D));
                case SOUTH:
                    return new ComplexCollisionBox(new HexCollisionBox(2.0D, 6.0D, 0.0D, 4.0D, 10.0D, 7.0D),
                            new HexCollisionBox(12.0D, 6.0D, 0.0D, 14.0D, 10.0D, 7.0D),
                            new HexCollisionBox(2.0D, 5.0D, 7.0D, 4.0D, 11.0D, 13.0D),
                            new HexCollisionBox(12.0D, 5.0D, 7.0D, 14.0D, 11.0D, 13.0D),
                            new HexCollisionBox(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 16.0D));
                case EAST:
                    return new ComplexCollisionBox(new HexCollisionBox(0.0D, 6.0D, 2.0D, 9.0D, 10.0D, 4.0D),
                            new HexCollisionBox(0.0D, 6.0D, 12.0D, 9.0D, 10.0D, 14.0D),
                            new HexCollisionBox(7.0D, 5.0D, 2.0D, 13.0D, 11.0D, 4.0D),
                            new HexCollisionBox(7.0D, 5.0D, 12.0D, 13.0D, 11.0D, 14.0D),
                            new HexCollisionBox(4.0D, 2.0D, 4.0D, 16.0D, 14.0D, 12.0D));
            }
        } else {
            if (grindstone.getFacing() == BlockFace.NORTH || grindstone.getFacing() == BlockFace.SOUTH) {
                return new ComplexCollisionBox(new HexCollisionBox(2.0D, 9.0D, 6.0D, 4.0D, 16.0D, 10.0D),
                        new HexCollisionBox(12.0D, 9.0D, 6.0D, 14.0D, 16.0D, 10.0D),
                        new HexCollisionBox(2.0D, 3.0D, 5.0D, 4.0D, 9.0D, 11.0D),
                        new HexCollisionBox(12.0D, 3.0D, 5.0D, 14.0D, 9.0D, 11.0D),
                        new HexCollisionBox(4.0D, 0.0D, 2.0D, 12.0D, 12.0D, 14.0D));
            } else {
                return new ComplexCollisionBox(new HexCollisionBox(6.0D, 9.0D, 2.0D, 10.0D, 16.0D, 4.0D),
                        new HexCollisionBox(6.0D, 9.0D, 12.0D, 10.0D, 16.0D, 14.0D),
                        new HexCollisionBox(5.0D, 3.0D, 2.0D, 11.0D, 9.0D, 4.0D),
                        new HexCollisionBox(5.0D, 3.0D, 12.0D, 11.0D, 9.0D, 14.0D),
                        new HexCollisionBox(2.0D, 0.0D, 4.0D, 14.0D, 12.0D, 12.0D));
            }
        }

        return NoCollisionBox.INSTANCE;

    }, XMaterial.GRINDSTONE.parseMaterial()),

    CHAIN_BLOCK((player, version, data, x, y, z) -> {
        Chain chain = (Chain) ((WrappedFlatBlock) data).getBlockData();

        if (chain.getAxis() == Axis.X) {
            return new HexCollisionBox(0.0D, 6.5D, 6.5D, 16.0D, 9.5D, 9.5D);
        } else if (chain.getAxis() == Axis.Y) {
            return new HexCollisionBox(6.5D, 0.0D, 6.5D, 9.5D, 16.0D, 9.5D);
        }

        return new HexCollisionBox(6.5D, 6.5D, 0.0D, 9.5D, 9.5D, 16.0D);
    }, XMaterial.CHAIN.parseMaterial()),

    SWEET_BERRY((player, version, data, x, y, z) -> {
        Ageable berry = (Ageable) ((WrappedFlatBlock) data).getBlockData();

        if (berry.getAge() == 0) {
            return new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D);
        }

        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
    }, XMaterial.SWEET_BERRY_BUSH.parseMaterial()),

    CHORUS_PLANT(new DynamicChorusPlant(), XMaterial.CHORUS_PLANT.parseMaterial()),

    FENCE_GATE((player, version, data, x, y, z) -> {
        WrappedFenceGate gate = (WrappedFenceGate) data;

        if (gate.isOpen())
            return NoCollisionBox.INSTANCE;

        switch (gate.getDirection()) {
            case NORTH:
            case SOUTH:
                return new SimpleCollisionBox(0.0F, 0.0F, 0.375F, 1.0F, 1.5F, 0.625F);
            case WEST:
            case EAST:
                return new SimpleCollisionBox(0.375F, 0.0F, 0.0F, 0.625F, 1.5F, 1.0F);
        }

        // This code is unreachable but the compiler does not know this
        return NoCollisionBox.INSTANCE;

    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("FENCE") && mat.name().contains("GATE"))
            .toArray(Material[]::new)),


    FENCE(new DynamicFence(), Arrays.stream(Material.values()).filter(mat -> mat.name().contains("FENCE") && !mat.name().contains("GATE"))
            .toArray(Material[]::new)),


    PANE(new DynamicPane(), Arrays.stream(Material.values()).filter(mat -> mat.name().contains("GLASS_PANE") || mat.name().equals("IRON_BARS"))
            .toArray(Material[]::new)),


    SNOW((player, version, data, x, y, z) -> {
        WrappedSnow snow = (WrappedSnow) data;

        if (snow.getLayers() == 0 && version.isNewerThanOrEquals(ClientVersion.v_1_13))
            return NoCollisionBox.INSTANCE;

        return new SimpleCollisionBox(0, 0, 0, 1, snow.getLayers() * 0.125, 1);
    }, XMaterial.SNOW.parseMaterial()),


    STAIR(new DynamicStair(),
            Arrays.stream(Material.values()).filter(mat -> mat.name().contains("STAIRS"))
                    .toArray(Material[]::new)),


    CHEST(new DynamicChest(), XMaterial.CHEST.parseMaterial(), XMaterial.TRAPPED_CHEST.parseMaterial()),


    ENDER_CHEST(new SimpleCollisionBox(0.0625F, 0.0F, 0.0625F,
            0.9375F, 0.875F, 0.9375F),
            XMaterial.ENDER_CHEST.parseMaterial()),


    ENCHANTING_TABLE(new SimpleCollisionBox(0, 0, 0, 1, 1 - 0.25, 1),
            XMaterial.ENCHANTING_TABLE.parseMaterial()),


    FRAME((player, version, data, x, y, z) -> {
        WrappedFrame frame = (WrappedFrame) data;
        ComplexCollisionBox complexCollisionBox = new ComplexCollisionBox(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D));

        // 1.12 clients do not differentiate between the eye being in and not for collisions
        if (version.isNewerThanOrEquals(ClientVersion.v_1_13) && frame.hasEye()) {
            complexCollisionBox.add(new HexCollisionBox(4.0D, 13.0D, 4.0D, 12.0D, 16.0D, 12.0D));
        }

        return complexCollisionBox;

    }, XMaterial.END_PORTAL_FRAME.parseMaterial()),

    CARPET((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.v_1_7_10))
            return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F);

        return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F);
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("CARPET")).toArray(Material[]::new)),

    DAYLIGHT(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.375, 1.0F),
            Arrays.stream(Material.values()).filter(mat -> mat.name().contains("DAYLIGHT")).toArray(Material[]::new)),

    FARMLAND((player, version, data, x, y, z) -> {
        // This will be wrong if a player uses 1.10.0 or 1.10.1, not sure if I can fix this as protocol version is same
        if (version.isNewerThanOrEquals(ClientVersion.v_1_10))
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);

        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);

    }, XMaterial.FARMLAND.parseMaterial()),

    GRASS_PATH((player, version, data, x, y, z) -> {
        if (version.isNewerThanOrEquals(ClientVersion.v_1_9))
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);

        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);

    }, XMaterial.DIRT_PATH.parseMaterial()),

    LILYPAD((player, version, data, x, y, z) -> {
        // Boats break lilypads client sided on 1.12- clients.
        if (player.playerVehicle instanceof Boat && version.isOlderThanOrEquals(ClientVersion.v_1_12_2))
            return NoCollisionBox.INSTANCE;

        if (version.isOlderThan(ClientVersion.v_1_9))
            return new SimpleCollisionBox(0.0f, 0.0F, 0.0f, 1.0f, 0.015625F, 1.0f);
        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 1.5D, 15.0D);
    }, XMaterial.LILY_PAD.parseMaterial()),

    BED(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.5625, 1.0F),
            Arrays.stream(Material.values()).filter(mat -> mat.name().contains("BED") && !mat.name().contains("ROCK"))
                    .toArray(Material[]::new)),

    TRAPDOOR(new TrapDoorHandler(), Arrays.stream(Material.values())
            .filter(mat -> mat.name().contains("TRAP_DOOR") || mat.name().contains("TRAPDOOR")).toArray(Material[]::new)),


    DIODES(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F),
            matchLegacy("LEGACY_DIODE_BLOCK_OFF"), matchLegacy("LEGACY_DIODE_BLOCK_ON"),
            matchLegacy("LEGACY_REDSTONE_COMPARATOR_ON"), matchLegacy("LEGACY_REDSTONE_COMPARATOR_OFF"),
            XMaterial.REPEATER.parseMaterial(), XMaterial.COMPARATOR.parseMaterial()),

    STRUCTURE_VOID(new SimpleCollisionBox(0.375, 0.375, 0.375,
            0.625, 0.625, 0.625),
            XMaterial.STRUCTURE_VOID.parseMaterial()),

    END_ROD((player, version, data, x, y, z) -> {
        WrappedDirectional directional = (WrappedDirectional) data;

        switch (directional.getDirection()) {
            case UP:
            case DOWN:
            default:
                return new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0);
            case NORTH:
            case SOUTH:
                return new HexCollisionBox(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 16.0D);
            case EAST:
            case WEST:
                return new HexCollisionBox(0.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D);
        }

    }, XMaterial.END_ROD.parseMaterial(), XMaterial.LIGHTNING_ROD.parseMaterial()),

    CAULDRON((player, version, data, x, y, z) -> {
        double height = 0.25;

        if (version.isOlderThan(ClientVersion.v_1_13))
            height = 0.3125;

        return new ComplexCollisionBox(
                new SimpleCollisionBox(0, 0, 0, 1, height, 1),
                new SimpleCollisionBox(0, height, 0, 0.125, 1, 1),
                new SimpleCollisionBox(1 - 0.125, height, 0, 1, 1, 1),
                new SimpleCollisionBox(0, height, 0, 1, 1, 0.125),
                new SimpleCollisionBox(0, height, 1 - 0.125, 1, 1, 1));
    }, XMaterial.CAULDRON.parseMaterial()),

    CACTUS(new SimpleCollisionBox(0.0625, 0, 0.0625,
            1 - 0.0625, 1 - 0.0625, 1 - 0.0625), XMaterial.CACTUS.parseMaterial()),


    PISTON_BASE(new PistonBaseCollision(), XMaterial.PISTON.parseMaterial(), XMaterial.STICKY_PISTON.parseMaterial()),

    PISTON_HEAD(new PistonHeadCollision(), XMaterial.PISTON_HEAD.parseMaterial()),

    SOULSAND(new SimpleCollisionBox(0, 0, 0, 1, 0.875, 1),
            XMaterial.SOUL_SAND.parseMaterial()),

    PICKLE((player, version, data, x, y, z) -> {
        SeaPickle pickle = (SeaPickle) ((WrappedFlatBlock) data).getBlockData();

        // ViaVersion replacement block (West facing cocoa beans)
        if (version.isOlderThanOrEquals(ClientVersion.v_1_12_2)) {
            switch (pickle.getPickles()) {
                case 1:
                case 2:
                    return new HexCollisionBox(11.0D, 7.0D, 6.0D, 15.0D, 12.0D, 10.0D);
                case 3:
                    return new HexCollisionBox(9.0D, 5.0D, 5.0D, 15.0D, 12.0D, 11.0D);
                case 4:
                    return new HexCollisionBox(7.0D, 3.0D, 4.0D, 15.0D, 12.0D, 12.0D);
            }
        }

        switch (pickle.getPickles()) {
            case 1:
                return new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 6.0D, 10.0D);
            case 2:
                return new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 6.0D, 13.0D);
            case 3:
                return new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 6.0D, 14.0D);
            case 4:
                return new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 7.0D, 14.0D);
        }
        return NoCollisionBox.INSTANCE;

    }, XMaterial.SEA_PICKLE.parseMaterial()),

    TURTLEEGG((player, version, data, x, y, z) -> {
        TurtleEgg egg = (TurtleEgg) ((WrappedFlatBlock) data).getBlockData();

        // ViaVersion replacement block (West facing cocoa beans)
        if (version.isOlderThanOrEquals(ClientVersion.v_1_12_2)) {
            switch (egg.getEggs()) {
                case 1:
                case 2:
                    return new HexCollisionBox(1.0D, 7.0D, 6.0D, 5.0D, 12.0D, 10.0D);
                case 3:
                    return new HexCollisionBox(1.0D, 5.0D, 5.0D, 7.0D, 12.0D, 11.0D);
                case 4:
                    return new HexCollisionBox(1.0D, 3.0D, 4.0D, 9.0D, 12.0D, 12.0D);
            }
        }

        if (egg.getEggs() == 1) {
            return new HexCollisionBox(3.0D, 0.0D, 3.0D, 12.0D, 7.0D, 12.0D);
        }

        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 7.0D, 15.0D);
    }, XMaterial.TURTLE_EGG.parseMaterial()),

    CONDUIT((player, version, data, x, y, z) -> {
        // ViaVersion replacement block - Beacon
        if (version.isOlderThanOrEquals(ClientVersion.v_1_12_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);

        return new HexCollisionBox(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);
    }, XMaterial.CONDUIT.parseMaterial()),

    POT(new HexCollisionBox(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D),
            Arrays.stream(Material.values()).filter(mat -> mat.name().contains("POTTED") || mat.name().contains("FLOWER_POT")).toArray(Material[]::new)),


    WALL_SIGN((player, version, data, x, y, z) -> {
        WrappedDirectional directional = (WrappedDirectional) data;

        switch (directional.getDirection()) {
            case NORTH:
                return new HexCollisionBox(0.0D, 4.5D, 14.0D, 16.0D, 12.5D, 16.0D);
            case SOUTH:
                return new HexCollisionBox(0.0D, 4.5D, 0.0D, 16.0D, 12.5D, 2.0D);
            case WEST:
                return new HexCollisionBox(14.0D, 4.5D, 0.0D, 16.0D, 12.5D, 16.0D);
            case EAST:
                return new HexCollisionBox(0.0D, 4.5D, 0.0D, 2.0D, 12.5D, 16.0D);
            default:
                return NoCollisionBox.INSTANCE;
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("WALL_SIGN"))
            .toArray(Material[]::new)),


    // The nether signes map to sign post and other regular sign
    SIGN(new SimpleCollisionBox(0.25, 0.0, 0.25, 0.75, 1.0, 0.75),
            Arrays.stream(Material.values()).filter(mat -> mat.name().contains("SIGN") && !mat.name().contains("WALL"))
                    .toArray(Material[]::new)),


    BUTTON((player, version, data, x, y, z) -> {
        WrappedButton button = (WrappedButton) data;
        double f2 = (float) (button.isPowered() ? 1 : 2) / 16.0;

        switch (button.getDirection()) {
            case WEST:
                return new SimpleCollisionBox(0.0, 0.375, 0.3125, f2, 0.625, 0.6875);
            case EAST:
                return new SimpleCollisionBox(1.0 - f2, 0.375, 0.3125, 1.0, 0.625, 0.6875);
            case NORTH:
                return new SimpleCollisionBox(0.3125, 0.375, 0.0, 0.6875, 0.625, f2);
            case SOUTH:
                return new SimpleCollisionBox(0.3125, 0.375, 1.0 - f2, 0.6875, 0.625, 1.0);
            case DOWN:
                return new SimpleCollisionBox(0.3125, 0.0, 0.375, 0.6875, 0.0 + f2, 0.625);
            case UP:
                return new SimpleCollisionBox(0.3125, 1.0 - f2, 0.375, 0.6875, 1.0, 0.625);
        }

        return NoCollisionBox.INSTANCE;

    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("BUTTON")).toArray(Material[]::new)),

    LEVER((player, version, data, x, y, z) -> {
        double f = 0.1875;

        switch (((WrappedDirectional) data).getDirection()) {
            case WEST:
                return new SimpleCollisionBox(1.0 - f * 2.0, 0.2, 0.5 - f, 1.0, 0.8, 0.5 + f);
            case EAST:
                return new SimpleCollisionBox(0.0, 0.2, 0.5 - f, f * 2.0, 0.8, 0.5 + f);
            case NORTH:
                return new SimpleCollisionBox(0.5 - f, 0.2, 1.0 - f * 2.0, 0.5 + f, 0.8, 1.0);
            case SOUTH:
                return new SimpleCollisionBox(0.5 - f, 0.2, 0.0, 0.5 + f, 0.8, f * 2.0);
            case DOWN:
                return new SimpleCollisionBox(0.25, 0.4, 0.25, 0.75, 1.0, 0.75);
            case UP:
                return new SimpleCollisionBox(0.25, 0.0, 0.25, 0.75, 0.6, 0.75);
        }

        return NoCollisionBox.INSTANCE;

    }, XMaterial.LEVER.parseMaterial()),

    TORCH(new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 10.0D, 10.0D),
            XMaterial.TORCH.parseMaterial(), XMaterial.REDSTONE_TORCH.parseMaterial()),

    WALL_TORCH((player, version, data, x, y, z) -> {
        Directional directional = (Directional) data;

        switch (directional.getFacing()) {
            case NORTH:
                return new HexCollisionBox(5.5D, 3.0D, 11.0D, 10.5D, 13.0D, 16.0D);
            case SOUTH:
                return new HexCollisionBox(5.5D, 3.0D, 0.0D, 10.5D, 13.0D, 5.0D);
            case WEST:
                return new HexCollisionBox(11.0D, 3.0D, 5.5D, 16.0D, 13.0D, 10.5D);
            case EAST:
                return new HexCollisionBox(0.0D, 3.0D, 5.5D, 5.0D, 13.0D, 10.5D);
            default: // 1.13 separates wall and normal torches, 1.12 does not
            case UP:
                return new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 10.0D, 10.0D);
        }

    }, XMaterial.WALL_TORCH.parseMaterial(), XMaterial.REDSTONE_WALL_TORCH.parseMaterial()),

    RAILS((player, version, data, x, y, z) -> {
        WrappedRails rail = (WrappedRails) data;

        if (rail.isAscending()) {
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
        }

        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);

    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("RAIL")).toArray(Material[]::new)),

    // Known as block 36 - has no collision box
    TECHNICAL_MOVING_PISTON(NoCollisionBox.INSTANCE, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("MOVING")).toArray(Material[]::new)),

    // 1.17 blocks
    CANDLE((player, version, data, x, y, z) -> {
        Candle candle = (Candle) ((WrappedFlatBlock) data).getBlockData();

        switch (candle.getCandles()) {
            case 1:
                return new HexCollisionBox(7.0, 0.0, 7.0, 9.0, 6.0, 9.0);
            case 2:
                return new HexCollisionBox(5.0, 0.0, 6.0, 11.0, 6.0, 9.0);
            case 3:
                return new HexCollisionBox(5.0, 0.0, 6.0, 10.0, 6.0, 11.0);
            default:
            case 4:
                return new HexCollisionBox(5.0, 0.0, 5.0, 11.0, 6.0, 10.0);
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().endsWith("CANDLE")).toArray(Material[]::new)),

    CANDLE_CAKE((player, version, data, x, y, z) -> {
        ComplexCollisionBox cake = new ComplexCollisionBox(new HexCollisionBox(1.0, 0.0, 1.0, 15.0, 8.0, 15.0));
        if (version.isNewerThanOrEquals(ClientVersion.v_1_17))
            cake.add(new HexCollisionBox(7.0, 8.0, 7.0, 9.0, 14.0, 9.0));
        return cake;
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().endsWith("CANDLE_CAKE")).toArray(Material[]::new)),

    SCULK_SENSOR(new HexCollisionBox(0.0, 0.0, 0.0, 16.0, 8.0, 16.0), XMaterial.SCULK_SENSOR.parseMaterial()),

    BIG_DRIPLEAF((player, version, data, x, y, z) -> {
        BigDripleaf dripleaf = (BigDripleaf) ((WrappedFlatBlock) data).getBlockData();

        if (dripleaf.getTilt() == BigDripleaf.Tilt.NONE || dripleaf.getTilt() == BigDripleaf.Tilt.UNSTABLE) {
            return new HexCollisionBox(0.0, 11.0, 0.0, 16.0, 15.0, 16.0);
        } else if (dripleaf.getTilt() == BigDripleaf.Tilt.PARTIAL) {
            return new HexCollisionBox(0.0, 11.0, 0.0, 16.0, 13.0, 16.0);
        }

        return NoCollisionBox.INSTANCE;

    }, XMaterial.BIG_DRIPLEAF.parseMaterial()),

    DRIPSTONE((player, version, data, x, y, z) -> {
        PointedDripstone dripstone = (PointedDripstone) ((WrappedFlatBlock) data).getBlockData();

        HexCollisionBox box;

        if (dripstone.getThickness() == PointedDripstone.Thickness.TIP_MERGE) {
            box = new HexCollisionBox(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);
        } else if (dripstone.getThickness() == PointedDripstone.Thickness.TIP) {
            if (dripstone.getVerticalDirection() == BlockFace.DOWN) {
                box = new HexCollisionBox(5.0, 5.0, 5.0, 11.0, 16.0, 11.0);
            } else {
                box = new HexCollisionBox(5.0, 0.0, 5.0, 11.0, 11.0, 11.0);
            }
        } else if (dripstone.getThickness() == PointedDripstone.Thickness.FRUSTUM) {
            box = new HexCollisionBox(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
        } else if (dripstone.getThickness() == PointedDripstone.Thickness.MIDDLE) {
            box = new HexCollisionBox(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
        } else {
            box = new HexCollisionBox(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
        }

        // Copied from NMS and it works!  That's all you need to know.
        long i = (x * 3129871L) ^ (long) z * 116129781L ^ (long) 0;
        i = i * i * 42317861L + i * 11L;
        i = i >> 16;

        return box.offset((((i & 15L) / 15.0F) - 0.5D) * 0.5D, 0, (((i >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D);
    }, XMaterial.DRIPSTONE_BLOCK.parseMaterial()),

    POWDER_SNOW((player, version, data, x, y, z) -> {
        // Who makes a collision box dependent on fall distance?? If fall distance greater than 2.5, 0.899999 box
        // Until we accurately get fall distance, just let players decide what box they get
        if (Math.abs((player.y % 1.0) - 0.89999997615814) < 0.001) {
            return new SimpleCollisionBox(0.0, 0.0, 0.0, 1.0, 0.8999999761581421, 1.0);
        }

        ItemStack boots = player.bukkitPlayer.getInventory().getBoots();
        if (player.lastY > y + 1 - 9.999999747378752E-6 && boots != null && boots.getType() == Material.LEATHER_BOOTS && !player.isSneaking)
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);

        return NoCollisionBox.INSTANCE;

    }, XMaterial.POWDER_SNOW.parseMaterial()),

    AZALEA((player, version, data, x, y, z) -> {
        return new ComplexCollisionBox(new HexCollisionBox(0.0, 8.0, 0.0, 16.0, 16.0, 16.0),
                new HexCollisionBox(6.0, 0.0, 6.0, 10.0, 8.0, 10.0));
    }, XMaterial.AZALEA.parseMaterial(), XMaterial.FLOWERING_AZALEA.parseMaterial()),

    AMETHYST_CLUSTER((player, version, data, x, y, z) -> {
        Directional cluster = (Directional) ((WrappedFlatBlock) data).getBlockData();
        return getAmethystBox(cluster.getFacing(), 7, 3);
    }, XMaterial.AMETHYST_CLUSTER.parseMaterial()),

    SMALL_AMETHYST_BUD((player, version, data, x, y, z) -> {
        Directional cluster = (Directional) ((WrappedFlatBlock) data).getBlockData();
        return getAmethystBox(cluster.getFacing(), 3, 4);
    }, XMaterial.SMALL_AMETHYST_BUD.parseMaterial()),

    MEDIUM_AMETHYST_BUD((player, version, data, x, y, z) -> {
        Directional cluster = (Directional) ((WrappedFlatBlock) data).getBlockData();
        return getAmethystBox(cluster.getFacing(), 4, 3);
    }, XMaterial.MEDIUM_AMETHYST_BUD.parseMaterial()),

    LARGE_AMETHYST_BUD((player, version, data, x, y, z) -> {
        Directional cluster = (Directional) ((WrappedFlatBlock) data).getBlockData();
        return getAmethystBox(cluster.getFacing(), 5, 3);
    }, XMaterial.LARGE_AMETHYST_BUD.parseMaterial()),

    NONE(NoCollisionBox.INSTANCE, XMaterial.REDSTONE_WIRE.parseMaterial(), XMaterial.POWERED_RAIL.parseMaterial(),
            XMaterial.RAIL.parseMaterial(), XMaterial.ACTIVATOR_RAIL.parseMaterial(), XMaterial.DETECTOR_RAIL.parseMaterial(), XMaterial.AIR.parseMaterial(), XMaterial.TALL_GRASS.parseMaterial(),
            XMaterial.TRIPWIRE.parseMaterial(), XMaterial.TRIPWIRE_HOOK.parseMaterial()),

    NONE2(NoCollisionBox.INSTANCE,
            Arrays.stream(Material.values()).filter(mat -> mat.name().contains("_PLATE"))
                    .toArray(Material[]::new)),

    DEFAULT(new SimpleCollisionBox(0, 0, 0, 1, 1, 1),
            XMaterial.STONE.parseMaterial());

    private static final CollisionData[] lookup = new CollisionData[Material.values().length];

    static {
        for (CollisionData data : values()) {
            for (Material mat : data.materials) lookup[mat.ordinal()] = data;
        }
    }

    private final Material[] materials;
    private CollisionBox box;
    private CollisionFactory dynamic;

    CollisionData(CollisionBox box, Material... materials) {
        this.box = box;
        Set<Material> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new Material[0]);
    }

    CollisionData(CollisionFactory dynamic, Material... materials) {
        this.dynamic = dynamic;
        Set<Material> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new Material[0]);
    }

    private static CollisionBox getAmethystBox(BlockFace facing, int param_0, int param_1) {
        switch (facing) {
            default:
            case UP:
                return new HexCollisionBox(param_1, 0.0, param_1, 16 - param_1, param_0, 16 - param_1);
            case DOWN:
                return new HexCollisionBox(param_1, 16 - param_0, param_1, 16 - param_1, 16.0, 16 - param_1);
            case NORTH:
                return new HexCollisionBox(param_1, param_1, 16 - param_0, 16 - param_1, 16 - param_1, 16.0);
            case SOUTH:
                return new HexCollisionBox(param_1, param_1, 0.0, 16 - param_1, 16 - param_1, param_0);
            case EAST:
                return new HexCollisionBox(0.0, param_1, param_1, param_0, 16 - param_1, 16 - param_1);
            case WEST:
                return new HexCollisionBox(16 - param_0, param_1, param_1, 16.0, 16 - param_1, 16 - param_1);
        }
    }

    public static CollisionData getData(Material material) {
        // Material matched = MiscUtils.match(material.toString());
        CollisionData data = lookup[material.ordinal()];
        // _DEFAULT for second thing
        return data != null ? data : DEFAULT;
    }

    public CollisionBox getMovementCollisionBox(GrimPlayer player, ClientVersion version, BaseBlockState block, int x, int y, int z) {
        if (!Materials.checkFlag(block.getMaterial(), Materials.SOLID))
            return NoCollisionBox.INSTANCE;

        WrappedBlockDataValue blockData = WrappedBlockData.getMaterialData(block);

        if (this.box != null)
            return this.box.copy().offset(x, y, z);
        return new DynamicCollisionBox(player, version, dynamic, blockData).offset(x, y, z);
    }
}