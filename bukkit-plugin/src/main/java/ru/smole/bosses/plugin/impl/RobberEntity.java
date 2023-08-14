package ru.smole.bosses.plugin.impl;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pillager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.smole.bosses.api.entity.BukkitBoss;
import ru.smole.bosses.plugin.util.ItemStackUtil;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class RobberEntity extends BukkitBoss<Pillager> {

    private static final ItemStack CROSSBOW = ItemStackUtil.createItemStack(Material.CROSSBOW, itemMeta -> {
        itemMeta.setUnbreakable(true);
        itemMeta.addEnchant(Enchantment.PIERCING, 1, false);
        itemMeta.addEnchant(Enchantment.MULTISHOT, 1, false);
        return itemMeta;
    });
    private static final ItemStack AXE = ItemStackUtil.createItemStack(Material.IRON_AXE, itemMeta -> {
        itemMeta.setUnbreakable(true);
        return itemMeta;
    });

    private boolean angryMode;

    public RobberEntity() {
        super("robber");
        setType(EntityType.PILLAGER);

        registerAbility(Duration.ofSeconds(1), () -> {
            Optional.ofNullable(getEntity())
                    .ifPresent(pillager -> {
                        pillager.getNearbyEntities(10, 10, 10)
                                .stream()
                                .filter(entity -> entity.getType() == EntityType.PLAYER)
                                .findFirst()
                                .ifPresent(entity -> pillager.setTarget((LivingEntity) entity));
                    });
        });
    }

    @Override
    public void onSpawn() {
        super.onSpawn();

        angryMode = false;
        Optional.ofNullable(getEntity())
                .ifPresent(pillager -> {
                    Objects.requireNonNull(pillager.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)).setBaseValue(1.0);

                    Optional.ofNullable(pillager.getEquipment()).ifPresent(entityEquipment -> {
                        entityEquipment.setArmorContents(new ItemStack[]{});
                        entityEquipment.setItemInMainHand(ItemStackUtil.createItemStack(Material.CROSSBOW, itemMeta -> {
                            itemMeta.setUnbreakable(true);
                            itemMeta.addEnchant(Enchantment.PIERCING, 1, true);
                            itemMeta.addEnchant(Enchantment.MULTISHOT, 1, true);
                            return itemMeta;
                        }));
                    });
                });
    }

    @Override
    public void onDamage() {
        super.onDamage();

        if (angryMode) {
            return;
        }

        Optional.ofNullable(getEntity())
                .ifPresent(pillager -> {
                    if ((getMaxHealth() / 2) < pillager.getHealth()) {
                        return;
                    }

                    angryMode = true;

                    Optional.ofNullable(pillager.getEquipment())
                            .ifPresent(entityEquipment -> entityEquipment.setItemInMainHand(AXE));

                    registerAbility(Duration.ofMinutes(1), () -> {
                        pillager.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 15, 2));
                        Optional.ofNullable(pillager.getTarget()).ifPresent(target -> {
                            pillager.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, pillager.getLocation(), 3);
                            pillager.setVelocity(
                                    pillager.getLocation()
                                            .toVector()
                                            .subtract(target.getEyeLocation().toVector())
                                            .normalize()
                                            .multiply(-2)
                                            .setY(0.5)
                            );
                        });
                    });
                });
    }
}
