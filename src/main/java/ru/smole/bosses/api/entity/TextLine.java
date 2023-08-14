package ru.smole.bosses.api.entity;

import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.Data;
import lombok.Getter;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.smole.bosses.api.packet.impl.PacketEntityDestroy;
import ru.smole.bosses.api.packet.impl.PacketEntityMetadata;
import ru.smole.bosses.api.packet.impl.PacketEntitySpawn;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Data
public class TextLine {

    @NotNull
    private static final AtomicInteger ID_FIELD = new AtomicInteger(Integer.MAX_VALUE);

    private final int id = ID_FIELD.decrementAndGet();
    @NotNull
    private final WrappedDataWatcher dataWatcher = new WrappedDataWatcher();
    @NotNull
    private final Location location;
    @NotNull
    private final String text;

    public TextLine(@NotNull String text, @NotNull Location location) {
        this.text = text;
        this.location = location;
    }


    public void spawn(@NotNull Player... players) {
        val livingEntityPacket = new PacketEntitySpawn();

        livingEntityPacket.setEntityId(id);
        livingEntityPacket.setLocation(getLocation());
        livingEntityPacket.setType(EntityType.ARMOR_STAND);

        dataWatcher.setObject(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x20);
        dataWatcher.setObject(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true), Optional.of(WrappedChatComponent.fromText(text).getHandle()));
        dataWatcher.setObject(3, WrappedDataWatcher.Registry.get(Boolean.class), true, true);

        if (players.length == 0) livingEntityPacket.broadcast();
        else livingEntityPacket.send(players);
        sendMetadata(players);
    }

    public void remove(@NotNull Player... players) {
        val destroy = new PacketEntityDestroy();

        destroy.setEntityIds(id);

        if (players.length == 0) destroy.broadcast();
        else destroy.send(players);
    }

    public void updateText(@NotNull String text, @NotNull Player... players) {
        try {
            dataWatcher.setObject(2,
                    WrappedDataWatcher.Registry.getChatComponentSerializer(true),
                    Optional.of(WrappedChatComponent.fromText(text).getHandle())
            );
        } catch (IllegalArgumentException ignored) {}
        sendMetadata(players);
    }

    private void sendMetadata(@NotNull Player... players) {
        val metadata = new PacketEntityMetadata();

        metadata.setEntityId(id);
        metadata.setDataWatcher(dataWatcher);

        if (players.length == 0) metadata.broadcast();
        else metadata.send(players);
    }
}
