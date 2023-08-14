package ru.smole.bosses.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.smole.bosses.api.entity.BukkitBoss;
import ru.smole.bosses.api.service.BossesService;
import ru.smole.bosses.api.sql.FightResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

@UtilityClass
public class Bosses {

    @NotNull
    public final Gson GSON = new GsonBuilder()
            .create();
    @Getter
    @Nullable
    private ConnectionSource connectionSource = null;
    @Getter
    @Nullable
    private Configuration localization = null;
    @Getter
    @Nullable
    private Configuration bosses = null;
    @Getter
    @Nullable
    private Dao<FightResult, String> fightResultDao = null;
    @Getter
    @Nullable
    private BossesService bossesService = null;

    @SneakyThrows
    public void setUp(
            @NotNull JavaPlugin plugin, @NotNull String connectionSourcePath, @NotNull String localizationFileName,
            @NotNull String bossesFileName
            ) {
        connectionSource = new JdbcPooledConnectionSource("jdbc:sqlite:" + connectionSourcePath);
        reloadLocalization(plugin, localizationFileName);
        bosses = getYamlByExists(plugin, bossesFileName);
        fightResultDao = DaoManager.createDao(connectionSource, FightResult.class);
        bossesService = new BossesService();

        TableUtils.createTableIfNotExists(connectionSource, FightResult.class);

        Bukkit.getPluginManager()
                .registerEvents(bossesService, plugin);
    }

    @SneakyThrows(value = IOException.class)
    public void uninstall() {
        Objects.requireNonNull(bossesService).getBosses()
                .values()
                .forEach(BukkitBoss::remove);
        Objects.requireNonNull(connectionSource).close();
    }

    public void reloadLocalization(@NotNull JavaPlugin plugin, @NotNull String localizationFileName) {
        localization = getYamlByExists(plugin, localizationFileName);
    }

    @NotNull
    public YamlConfiguration getYamlByExists(@NotNull JavaPlugin plugin, @NotNull String fileName) {
        val fileNameWithYml = fileName + ".yml";
        val searchedFile = new File(String.format("%s/%s", plugin.getDataFolder().getAbsolutePath(), fileNameWithYml));

        return searchedFile.exists() ? YamlConfiguration.loadConfiguration(searchedFile)
                : YamlConfiguration.loadConfiguration(
                new InputStreamReader(
                        Objects.requireNonNull(plugin.getResource(fileNameWithYml))
                )
        );
    }
}
