package com.ultra.ucore.item;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

/**
 * Fluent API for building ItemStacks.
 */
public final class ItemBuilder {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final ItemStack itemStack;
    private final ItemMeta meta;

    private ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.meta = itemStack.getItemMeta();
    }

    public static ItemBuilder create(Material material) {
        return new ItemBuilder(material);
    }

    public ItemBuilder name(String name) {
        meta.displayName(MINI_MESSAGE.deserialize(name));
        return this;
    }

    public ItemBuilder lore(String... lore) {
        meta.lore(Arrays.stream(lore)
                .map(MINI_MESSAGE::deserialize)
                .toList());
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level, boolean ignoreRestriction) {
        meta.addEnchant(enchantment, level, ignoreRestriction);
        return this;
    }

    public ItemBuilder customModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder pdcString(NamespacedKey key, String value) {
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        return this;
    }

    public ItemStack build() {
        itemStack.setItemMeta(meta);
        return itemStack.clone();
    }
}
