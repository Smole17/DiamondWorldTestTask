package ru.smole.bosses.api.packet.impl;

import com.comphenix.protocol.PacketType;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import ru.smole.bosses.api.packet.PacketAdapter;

import java.util.UUID;

public class PacketEntitySpawn extends PacketAdapter {

    public PacketEntitySpawn() {
        super(PacketType.Play.Server.SPAWN_ENTITY);
    }

    public void setEntityId(int entityId) {
        wrapper.getIntegers().write(0, entityId);
        wrapper.getUUIDs().write(0, UUID.randomUUID());
    }

    public void setType(@NotNull EntityType entityType) {
        wrapper.getEntityTypeModifier().write(0, entityType);
    }

    public void setLocation(@NotNull Location location) {
        val doubles = wrapper.getDoubles();

        doubles.write(0, location.getX());
        doubles.write(1, location.getY());
        doubles.write(2, location.getZ());
    }
}
