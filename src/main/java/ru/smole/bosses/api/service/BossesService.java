package ru.smole.bosses.api.service;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.smole.bosses.api.entity.BukkitBoss;

import java.util.Map;

@Getter
public class BossesService implements Listener {

    private final Map<String, BukkitBoss<? extends LivingEntity>> bosses = Maps.newHashMap();
    private final BukkitTask timer;

    public BossesService() {
        this.timer = Bukkit.getScheduler().runTaskTimer(
                JavaPlugin.getProvidingPlugin(BossesService.class),
                () -> bosses.values().forEach(BukkitBoss::live),
                0, 1
        );
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        bosses.values()
                .stream()
                .filter(bukkitBoss -> !bukkitBoss.isAlive())
                .forEach(bukkitBoss -> bukkitBoss.getHolograms()
                        .forEach(textLine -> textLine.spawn(event.getPlayer())));
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        bosses.values()
                .stream()
                .filter(bukkitBoss -> !bukkitBoss.isAlive())
                .forEach(bukkitBoss -> bukkitBoss.getHolograms()
                        .forEach(textLine -> textLine.remove(event.getPlayer())));
    }

    @EventHandler
    public void onBossDie(EntityTransformEvent event) {
        bosses.values()
                .stream()
                .filter(boss -> boss.getEntityId() == event.getEntity().getEntityId())
                .forEach(bukkitBoss -> event.setCancelled(true));
    }

    @EventHandler
    public void onBossDie(EntityDeathEvent event) {
        bosses.values()
                .stream()
                .filter(boss -> boss.getEntityId() == event.getEntity().getEntityId())
                .forEach(BukkitBoss::onDie);
    }

    @EventHandler
    public void onBossDamage(EntityDamageByEntityEvent event) {
        bosses.values()
                .stream()
                .filter(boss -> boss.getEntityId() == event.getEntity().getEntityId())
                .forEach(boss -> {
                    val damager = event.getDamager();

                    boss.addDamage(damager.getType() != EntityType.PLAYER ? null : (Player) damager, event.getDamage());
                });

    }

    @EventHandler
    public void onBossDamage(EntityDamageEvent event) {
        bosses.values()
                .stream()
                .filter(boss -> boss.getEntityId() == event.getEntity().getEntityId())
                .forEach(boss -> {
                    val cause = event.getCause();

                    switch (cause) {
                        case FALL, FIRE_TICK, HOT_FLOOR, SUFFOCATION -> event.setCancelled(true);
                    }
                });

    }
}
