package ru.smole.bosses.api.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public abstract class PacketAdapter {

    @NotNull
    protected final PacketContainer wrapper;

    protected PacketAdapter(@NotNull PacketType type) {
        this.wrapper = new PacketContainer(type);
        wrapper.getModifier().writeDefaults();
    }

    public void send(@NotNull Player... players) {
        Arrays.stream(players).forEach(player -> {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, wrapper);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void broadcast() {
        ProtocolLibrary.getProtocolManager().broadcastServerPacket(wrapper);
    }
}
