package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.PistonData;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import java.util.ArrayList;
import java.util.List;

public class PistonEvent implements Listener {
    Material SLIME_BLOCK = XMaterial.SLIME_BLOCK.parseMaterial();

    @EventHandler
    public void onPistonPushEvent(BlockPistonExtendEvent event) {
        boolean hasSlimeBlock = false;

        List<SimpleCollisionBox> boxes = new ArrayList<>();
        for (Block block : event.getBlocks()) {
            boxes.add(new SimpleCollisionBox(0, 0, 0, 1, 1, 1)
                    .offset(block.getX(),
                            block.getY(),
                            block.getZ()));
            boxes.add(new SimpleCollisionBox(0, 0, 0, 1, 1, 1)
                    .offset(block.getX() + event.getDirection().getModX(),
                            block.getY() + event.getDirection().getModY(),
                            block.getZ() + event.getDirection().getModZ()));

            if (block.getType() == SLIME_BLOCK) {
                hasSlimeBlock = true;
            }
        }

        Block piston = event.getBlock();

        // Add bounding box of the actual piston head pushing
        boxes.add(new SimpleCollisionBox(0, 0, 0, 1, 1, 1)
                .offset(piston.getX() + event.getDirection().getModX(),
                        piston.getY() + event.getDirection().getModY(),
                        piston.getZ() + event.getDirection().getModZ()));

        for (GrimPlayer player : GrimAC.playerGrimHashMap.values()) {
            if (player.compensatedWorld.isChunkLoaded(event.getBlock().getX() >> 4, event.getBlock().getZ() >> 4)) {
                player.compensatedWorld.pistonData.add(new PistonData(event.getDirection(), boxes, player.lastTransactionAtStartOfTick, true, hasSlimeBlock));
            }
        }
    }

    // For some unknown reason, bukkit handles this stupidly
    // Calls the event once without blocks
    // Calls it again with blocks -
    // This wouldn't be an issue if it didn't flip the direction of the event
    // What a stupid system, again I can stand mojang doing stupid stuff but not other mod makers
    //
    // This gives too much of a lenience when retracting
    // But as this is insanely gitchy due to bukkit I don't care.
    // The lenience is never actually given because of collisions hitting the piston base
    // Blocks outside the piston head give only as much lenience as needed
    @EventHandler
    public void onPistonRetractEvent(BlockPistonRetractEvent event) {
        boolean hasSlimeBlock = false;

        List<SimpleCollisionBox> boxes = new ArrayList<>();
        BlockFace face = event.getDirection();

        // The event was called without blocks and is therefore in the right direction
        if (event.getBlocks().isEmpty()) {
            Block piston = event.getBlock();

            // Add bounding box of the actual piston head pushing
            boxes.add(new SimpleCollisionBox(0, 0, 0, 1, 1, 1)
                    .offset(piston.getX() + face.getModX(),
                            piston.getY() + face.getModY(),
                            piston.getZ() + face.getModZ()));
        }

        for (Block block : event.getBlocks()) {
            boxes.add(new SimpleCollisionBox(0, 0, 0, 1, 1, 1)
                    .offset(block.getX(), block.getY(), block.getZ()));
            boxes.add(new SimpleCollisionBox(0, 0, 0, 1, 1, 1)
                    .offset(block.getX() + face.getModX(), block.getY() + face.getModY(), block.getZ() + face.getModZ()));

            if (block.getType() == SLIME_BLOCK) {
                hasSlimeBlock = true;
            }
        }

        for (GrimPlayer player : GrimAC.playerGrimHashMap.values()) {
            if (player.compensatedWorld.isChunkLoaded(event.getBlock().getX() >> 4, event.getBlock().getZ() >> 4)) {
                player.compensatedWorld.pistonData.add(new PistonData(event.getBlocks().isEmpty() ? event.getDirection().getOppositeFace() : event.getDirection(), boxes, player.lastTransactionAtStartOfTick, false, hasSlimeBlock));
            }
        }
    }
}
