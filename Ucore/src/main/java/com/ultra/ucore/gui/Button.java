package com.ultra.ucore.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * Represents a clickable GUI button.
 */
public record Button(
        ItemStack item,
        Consumer<InventoryClickEvent> clickAction
) {
}
