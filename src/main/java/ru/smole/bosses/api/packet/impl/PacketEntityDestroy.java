package ru.smole.bosses.api.packet.impl;

import com.comphenix.protocol.PacketType;
import ru.smole.bosses.api.packet.PacketAdapter;

import java.util.stream.IntStream;

public class PacketEntityDestroy extends PacketAdapter {

    public PacketEntityDestroy() {
        super(PacketType.Play.Server.ENTITY_DESTROY);
    }

    public void setEntityIds(int... entityIds) {
        wrapper.getIntegerArrays().write(0, IntStream.of(entityIds).toArray());
    }
}
