package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.enums.Pose;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.entity.Entity;

import java.util.Locale;

public class PacketEntity {
    public EntityType type;
    public org.bukkit.entity.EntityType bukkitEntityType;
    public Pose pose = Pose.STANDING;
    public Vector3d lastTickPosition;
    public Vector3d position;
    public PacketEntity riding;
    public int[] passengers = new int[0];
    public boolean isDead = false;
    public boolean isBaby = false;

    public PacketEntity(org.bukkit.entity.EntityType type, Vector3d position) {
        this.position = position;
        this.lastTickPosition = position;
        this.bukkitEntityType = type;
        this.type = EntityType.valueOf(type.toString().toUpperCase(Locale.ROOT));
    }
}
