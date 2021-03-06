package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.pong.WrappedPacketInPong;
import io.github.retrooper.packetevents.packetwrappers.play.in.transaction.WrappedPacketInTransaction;

public class PacketPingListener extends PacketListenerAbstract {

    // Must listen on LOWEST (or maybe low) to stop Tuinity packet limiter from kicking players for transaction/pong spam
    public PacketPingListener() {
        super(PacketListenerPriority.LOWEST);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Client.TRANSACTION) {
            WrappedPacketInTransaction transaction = new WrappedPacketInTransaction(event.getNMSPacket());
            short id = transaction.getActionNumber();

            // Vanilla always uses an ID starting from 1
            if (id <= 0) {
                GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
                if (player == null) return;

                // Check if we sent this packet before cancelling it
                if (player.addTransactionResponse(id)) {
                    event.setCancelled(true);
                }
            }
        }

        if (packetID == PacketType.Play.Client.PONG) {
            WrappedPacketInPong pong = new WrappedPacketInPong(event.getNMSPacket());

            int id = pong.getId();
            // If it wasn't below 0, it wasn't us
            // If it wasn't in short range, it wasn't us either
            if (id >= Short.MIN_VALUE && id <= 0) {
                GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
                if (player == null) return;
                if (player.addTransactionResponse((short) id)) {
                    // Not needed for vanilla as vanilla ignores this packet, needed for packet limiters
                    event.setCancelled(true);
                }
            }
        }
    }
}
