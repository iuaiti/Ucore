package com.uitem.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public class GuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;

        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof AbstractGui gui) {
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                // Player clicked their own inventory → cho phép pick item
                gui.handleBottomInventoryClick(event);
            } else {
                // Player clicked top GUI → cancel + handler
                event.setCancelled(true);
                gui.handleClick(event);
            }
        } else if (holder instanceof CraftingGui gui) {
            gui.handleClick(event);
        } else if (holder instanceof RecipeEditorGui gui) {
            gui.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof AbstractGui) {
            // Allow drag only in bottom inventory
            boolean allBottom = event.getRawSlots().stream()
                    .allMatch(slot -> slot >= ((AbstractGui) holder).getInventory().getSize());
            if (!allBottom) {
                event.setCancelled(true);
            }
        } else if (holder instanceof CraftingGui gui) {
            gui.handleDrag(event);
        } else if (holder instanceof RecipeEditorGui gui) {
            gui.handleDrag(event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof CraftingGui gui) {
            gui.onClose();
            CraftingGui.removeCraftingPlayer((org.bukkit.entity.Player) event.getPlayer());
        } else if (holder instanceof RecipeEditorGui gui) {
            gui.onClose((org.bukkit.entity.Player) event.getPlayer());
        }
    }
}
