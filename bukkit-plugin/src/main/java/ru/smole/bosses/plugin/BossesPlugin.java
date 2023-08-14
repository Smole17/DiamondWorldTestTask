package ru.smole.bosses.plugin;

import lombok.Getter;
import lombok.val;
import org.bukkit.plugin.java.JavaPlugin;
import ru.smole.bosses.api.Bosses;
import ru.smole.bosses.plugin.impl.RobberEntity;
import ru.smole.bosses.plugin.impl.SummonerEntity;

import java.util.Objects;

@Getter
public class BossesPlugin extends JavaPlugin {

    private static BossesPlugin INSTANCE;

    @Override
    public void onEnable() {
        INSTANCE = this;
        Bosses.setUp(this, getDataFolder() + "/db.db", "messages", "bosses");

        getCommand("reloadmessages").setExecutor((commandSender, command, s, args) -> {
            if (!commandSender.isOp()) return false;

            Bosses.reloadLocalization(this, "messages");

            commandSender.sendMessage("§aReload successfully!");
            return true;
        });

        getCommand("spawnboss").setExecutor((commandSender, command, s, args) -> {
            if (!commandSender.isOp()) return false;

            if (args.length < 1) return false;

            val bosses = Objects.requireNonNull(Bosses.getBossesService())
                    .getBosses();

            val id = args[0];
            if (!bosses.containsKey(id)) {
                commandSender.sendMessage("§c§lUnknown boss id!");
                return false;
            }

            commandSender.sendMessage("§7Attempt to spawn boss...");
            bosses.get(id).spawn();
            return true;
        });

        new RobberEntity().register();
        new SummonerEntity().register();
    }

    @Override
    public void onDisable() {
        Bosses.uninstall();
    }
}
