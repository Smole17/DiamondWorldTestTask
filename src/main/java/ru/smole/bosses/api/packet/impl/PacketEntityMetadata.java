package ru.smole.bosses.api.packet.impl;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.jetbrains.annotations.NotNull;
import ru.smole.bosses.api.packet.PacketAdapter;

public class PacketEntityMetadata extends PacketAdapter {

    public PacketEntityMetadata() {
        super(PacketType.Play.Server.ENTITY_METADATA);
    }

    public void setEntityId(int entityId) {
        wrapper.getIntegers().write(0, entityId);
    }

    public void setDataWatcher(@NotNull WrappedDataWatcher dataWatcher) {
        wrapper.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
    }
}
