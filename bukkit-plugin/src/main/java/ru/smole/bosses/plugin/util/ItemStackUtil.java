package ru.smole.bosses.plugin.util;

import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Function;

@UtilityClass
public class ItemStackUtil {

    public ItemStack createItemStack(ItemStack itemStack, Function<ItemMeta, ItemMeta> itemMeta) {
        itemStack.setItemMeta(itemMeta.apply(itemStack.getItemMeta()));

        return itemStack;
    }

    public ItemStack createItemStack(Material material, Function<ItemMeta, ItemMeta> itemMeta) {
        return createItemStack(new ItemStack(material), itemMeta);
    }
}
