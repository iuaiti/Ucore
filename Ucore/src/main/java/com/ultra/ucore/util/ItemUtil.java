package com.ultra.ucore.util;

import org.bukkit.inventory.ItemStack;

/**
 * Item utility methods.
 */
public final class ItemUtil {

    private ItemUtil() {
    }

    public static boolean isValid(ItemStack item) {
        return item != null && !item.getType().isAir();
    }
}
