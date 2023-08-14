package ru.smole.bosses.api.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import ru.smole.bosses.api.Bosses;
import ru.smole.bosses.api.entity.ability.BossAbility;
import ru.smole.bosses.api.sql.FightResult;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Getter
public abstract class BukkitBoss<T extends LivingEntity> extends PlaceholderExpansion {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###");

    @Setter
    @NotNull
    private EntityType type;
    @Nullable
    private T entity = null;
    @NotNull
    private final String id;
    @NotNull
    private final Location spawnLocation;
    @NotNull
    private final Duration cooldown;
    @NotNull
    private final String displayName;
    private final double maxHealth;
    private final double damage;
    @NotNull
    private final Map<String, Double> playerDamages = Maps.newHashMap();
    @NotNull
    private final List<BossAbility> abilities = Lists.newArrayList();
    @NotNull
    private final List<TextLine> holograms = Lists.newArrayList();
    @Nullable
    private BossBar bossBar = null;

    private long lastKilled = -1;

    public BukkitBoss(@NotNull String id) {
        this.id = id;
        val section = Objects.requireNonNull(Objects.requireNonNull(Bosses.getBosses()).getConfigurationSection(id));

        spawnLocation = Objects.requireNonNull(section.getLocation("spawn-location"));
        cooldown = Duration.ofSeconds(section.getLong("cooldown"));
        displayName = Objects.requireNonNull(section.getString("display-name"));
        maxHealth = section.getDouble("health");
        damage = section.getDouble("damage");
        try {
            Streams.findLast(Bosses.getFightResultDao().queryBuilder()
                            .where()
                            .eq("boss_id", id)
                            .query()
                            .stream())
                    .ifPresent(fightResult -> lastKilled = fightResult.getWhenKilled());
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }


        val clonedLocation = spawnLocation.clone();
        val world = clonedLocation.getWorld();

        if (world == null) {
            return;
        }

        val hologramsText = Objects.requireNonNull(Bosses.getLocalization()).getStringList(id + "-hologram-display");

        if (hologramsText.size() < 2) {
            return;
        }

        for (int i = 0; i < 2; i++) {
            val line = new TextLine(hologramsText.get(i), clonedLocation.subtract(.0, .25, .0).clone());

            holograms.add(line);
        }
    }

    public void onSpawn() {
        bossBar = Bukkit.createBossBar(displayName, BarColor.WHITE, BarStyle.SOLID);
        bossBar.setVisible(true);
        bossBar.setProgress(1.0);

        holograms.forEach(TextLine::remove);
    }

    public void onDie() {
        lastKilled = System.currentTimeMillis();

        holograms.forEach(TextLine::spawn);

        Optional.ofNullable(bossBar)
                .ifPresent(BossBar::removeAll);

        val onBossDieMessage = Objects.requireNonNull(Bosses.getLocalization())
                .getStringList(id + "-die");

        Bukkit.broadcastMessage(PlaceholderAPI.setPlaceholders(null, onBossDieMessage.get(0)));

        for (int i = 0; i < playerDamages.size(); i++) {
            val entry = playerDamages.entrySet().stream().toList().get(i);
            val offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());

            Bukkit.broadcastMessage(PlaceholderAPI.setPlaceholders(offlinePlayer, onBossDieMessage.get(2 + i)));
        }

        playerDamages.clear();
        sendResult();
    }

    public void onDamage() {
    }


    @Override
    public final boolean register() {
        val registered = super.register();

        Objects.requireNonNull(Bosses.getBossesService())
                .getBosses()
                .put(id, this);

        Bukkit.getScheduler().runTaskTimerAsynchronously(
                JavaPlugin.getProvidingPlugin(BukkitBoss.class),
                () -> {
                    val hologramText = Objects.requireNonNull(Bosses.getLocalization()).getStringList(id + "-hologram-display");

                    if (hologramText.size() < holograms.size()) return;

                    for (int i = 0; i < hologramText.size(); i++) {
                        TextLine textLine = holograms.get(i);

                        int finalI = i;
                        Bukkit.getOnlinePlayers().forEach(player ->
                                textLine.updateText(PlaceholderAPI.setPlaceholders(player, hologramText.get(finalI)), player)
                        );
                    }
                },
                20, 5
        );

        return registered;
    }

    public final void live() {
        if (isAlive()) {
            abilities.forEach(BossAbility::use);
            return;
        }

        if (lastKilled > 0 && getRemainingTime() > 0) {
            return;
        }

        spawn();
        abilities.forEach(BossAbility::reset);
    }

    @SuppressWarnings("unchecked")
    public final void spawn() {
        if (isAlive()) {
            return;
        }
        entity = (T) Objects.requireNonNull(spawnLocation.getWorld()).spawnEntity(spawnLocation, type);

        entity.setCustomNameVisible(true);
        entity.setCustomName(displayName);

        Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(maxHealth);
        entity.setHealth(maxHealth);
        Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(damage);

        onSpawn();
    }

    public final void remove() {
        Optional.ofNullable(entity).ifPresent(safetyEntity -> {
            safetyEntity.remove();
            onDie();
        });
    }

    public final void addDamage(@Nullable Player player, double damage) {
        Optional.ofNullable(entity)
                .ifPresent(safetyEntity -> {
                    onDamage();
                    val health = entity.getHealth();

                    Optional.ofNullable(bossBar).ifPresent(safetyBar -> {
                        val progress = bossBar.getProgress() - (damage / maxHealth);
                        safetyBar.setProgress(progress <= 0 ? 0 : progress);
                        if (player == null) return;

                        safetyBar.addPlayer(player);
                        playerDamages.merge(player.getName(), Math.min(damage, health), Double::sum);
                    });
                });
    }

    @NotNull
    protected final BossAbility registerAbility(@NotNull Duration cooldown, @NotNull Runnable ability) {
        val bossAbility = new BossAbility(ability, cooldown);
        abilities.add(bossAbility);
        return bossAbility;
    }

    private void sendResult() {
        if (playerDamages.isEmpty()) {
            return;
        }

        Optional.ofNullable(Bosses.getFightResultDao())
                .ifPresent(fightResultDao -> {
                    val fightResult = new FightResult();

                    fightResult.setBossId(id);
                    fightResult.setWhenKilled(lastKilled);
                    fightResult.setResult(playerDamages);

                    try {
                        fightResultDao.create(fightResult);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Range(from = -1, to = Integer.MAX_VALUE)
    public final int getEntityId() {
        return entity == null ? -1 : entity.getEntityId();
    }

    public final boolean isAlive() {
        return entity != null && !entity.isDead();
    }

    @Range(from = Long.MIN_VALUE, to = Long.MAX_VALUE)
    public long getRemainingTime() {
        return (lastKilled + cooldown.toMillis()) - System.currentTimeMillis();
    }

    @NotNull
    public Map<String, Double> getPlayerDamages() {
        return Collections.unmodifiableMap(playerDamages);
    }

    @NotNull
    public List<BossAbility> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    @NotNull
    public List<TextLine> getHolograms() {
        return Collections.unmodifiableList(holograms);
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "smole17";
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return id;
    }

    @NotNull
    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    @Nullable
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        switch (params) {
            case "name" -> {
                return displayName;
            }
            case "time" -> {
                return lastKilled < 0 || getRemainingTime() <= 0 ? null : String.valueOf(Duration.ofMillis(getRemainingTime()).toSeconds());
            }
            case "damage" -> {
                if (offlinePlayer == null) {
                    return null;
                }

                return DECIMAL_FORMAT.format(playerDamages.getOrDefault(offlinePlayer.getName(), .0));
            }
        }

        return null;
    }
}
