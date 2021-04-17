package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.entityaction.WrappedPacketInEntityAction;

public class PacketEntityAction extends PacketListenerDynamic {
    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.ENTITY_ACTION) {
            WrappedPacketInEntityAction action = new WrappedPacketInEntityAction(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            switch (action.getAction()) {
                case START_SPRINTING:
                    player.isPacketSprinting = true;
                    break;
                case STOP_SPRINTING:
                    player.isPacketSprinting = false;
                    break;
                case START_SNEAKING:
                    player.isPacketSneaking = true;
                    break;
                case STOP_SNEAKING:
                    player.isPacketSneaking = false;
                    break;
            }
        }
    }
}