package ru.smole.bosses.plugin.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.smole.bosses.api.entity.BukkitBoss;
import ru.smole.bosses.plugin.util.ItemStackUtil;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class SummonerEntity extends BukkitBoss<Zombie> implements Listener {

    private static final ItemStack BONE_STACK = new ItemStack(Material.BONE);
    private final List<Map.Entry<Zombie, BukkitTask>> helpers = Lists.newArrayList();

    public SummonerEntity() {
        super("summoner");
        setType(EntityType.ZOMBIE);

        registerAbility(Duration.ofSeconds(1), () -> {
            Optional.ofNullable(getEntity())
                    .ifPresent(zombie -> {
                        zombie.getNearbyEntities(10, 10, 10)
                                .stream()
                                .filter(entity -> entity.getType() == EntityType.PLAYER || (entity instanceof Tameable tameable && tameable.isTamed()))
                                .findFirst()
                                .ifPresent(entity -> zombie.setTarget((LivingEntity) entity));
                    });
        });
        val helpersAbility = registerAbility(Duration.ofMinutes(1), () -> {
            for (int i = 0; i < ThreadLocalRandom.current().nextInt(1, 4); i++) {
                val helper = (Zombie) Objects.requireNonNull(getSpawnLocation().getWorld())
                        .spawnEntity(Objects.requireNonNull(getEntity()).getLocation(), EntityType.ZOMBIE);
                Objects.requireNonNull(helper.getEquipment()).setArmorContents(new ItemStack[]{});
                Objects.requireNonNull(helper.getEquipment()).setItemInMainHand(null);

                helper.setBaby();

                helpers.add(Maps.immutableEntry(helper, Bukkit.getScheduler()
                        .runTaskTimer(JavaPlugin.getProvidingPlugin(SummonerEntity.class), () -> {
                            helper.getNearbyEntities(10, 10, 10)
                                    .stream()
                                    .filter(entity -> entity.getType() == EntityType.PLAYER || (entity instanceof Tameable tameable && tameable.isTamed()))
                                    .findFirst()
                                    .ifPresent(entity -> helper.setTarget((LivingEntity) entity));
                        }, 0, 5))
                );
            }
        });
        registerAbility(Duration.ofMinutes(10), () -> {
            helpersAbility.setCooldown(Duration.ofMinutes(3));

            Optional.ofNullable(getEntity())
                    .ifPresent(zombie -> {
                        zombie.getNearbyEntities(10, 10, 10)
                                .stream()
                                .filter(entity -> entity.getType() == EntityType.PLAYER)
                                .map(entity -> (Player) entity)
                                .forEach(player -> {
                                    zombie.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, zombie.getLocation(), 2);
                                    player.setVelocity(zombie.getLocation()
                                            .toVector()
                                            .subtract(player.getEyeLocation().toVector())
                                            .normalize()
                                            .multiply(-.5)
                                            .setY(1f));
                                });

                        Optional.ofNullable(zombie.getEquipment())
                                .ifPresent(entityEquipment -> {
                                    entityEquipment.setItemInMainHand(ItemStackUtil.createItemStack(Material.STONE_SWORD, itemMeta -> {
                                        itemMeta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
                                        itemMeta.setUnbreakable(true);
                                        return itemMeta;
                                    }));

                                    entityEquipment.setHelmet(ItemStackUtil.createItemStack(Material.LEATHER_HELMET, itemMeta -> {
                                        itemMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                                        itemMeta.setUnbreakable(true);
                                        return itemMeta;
                                    }));
                                    entityEquipment.setChestplate(ItemStackUtil.createItemStack(Material.LEATHER_CHESTPLATE, itemMeta -> {
                                        itemMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                                        itemMeta.setUnbreakable(true);
                                        return itemMeta;
                                    }));
                                    entityEquipment.setLeggings(ItemStackUtil.createItemStack(Material.LEATHER_LEGGINGS, itemMeta -> {
                                        itemMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                                        itemMeta.setUnbreakable(true);
                                        return itemMeta;
                                    }));
                                    entityEquipment.setBoots(ItemStackUtil.createItemStack(Material.LEATHER_BOOTS, itemMeta -> {
                                        itemMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                                        itemMeta.setUnbreakable(true);
                                        return itemMeta;
                                    }));

                                    Bukkit.getScheduler().runTaskLater(JavaPlugin.getProvidingPlugin(SummonerEntity.class),
                                            () -> {
                                                entityEquipment.setArmorContents(new ItemStack[]{});
                                                entityEquipment.setItemInMainHand(BONE_STACK);
                                            }, 20 * 60 * 5);
                                });
                    });
        });

        Bukkit.getPluginManager()
                .registerEvents(this, JavaPlugin.getProvidingPlugin(SummonerEntity.class));
    }

    @Override
    public void onSpawn() {
        super.onSpawn();

        Optional.ofNullable(getEntity())
                .ifPresent(zombie -> {
                    zombie.setAge(0);
                    Optional.ofNullable(zombie.getEquipment()).ifPresent(entityEquipment -> {
                        entityEquipment.setArmorContents(new ItemStack[]{});
                        entityEquipment.setItemInMainHand(BONE_STACK);
                    });
                });
    }

    @Override
    public void onDie() {
        super.onDie();

        helpers.forEach(entry -> {
            entry.getKey().remove();
            entry.getValue().cancel();
        });
        helpers.clear();
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        val entity = event.getEntity();
        if (getEntityId() != entity.getEntityId()) {
            return;
        }

        switch (event.getCause()) {
            case PROJECTILE, MAGIC, WITHER, POISON -> event.setCancelled(true);
        }
    }
}
