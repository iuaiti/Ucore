package com.ultra.ucore.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic inventory GUI.
 */
public class Gui implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Button> buttons = new ConcurrentHashMap<>();

    public Gui(String title, int size) {
        Component component = MiniMessage.miniMessage().deserialize(title);
        this.inventory = Bukkit.createInventory(this, size, component);
    }

    public Gui setButton(int slot, Button button) {
        buttons.put(slot, button);
        inventory.setItem(slot, button.item());
        return this;
    }

    public Gui clearButton(int slot) {
        buttons.remove(slot);
        inventory.setItem(slot, null);
        return this;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();

        if (rawSlot < 0) {
            return;
        }

        if (rawSlot >= inventory.getSize()) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);

        Button button = buttons.get(rawSlot);

        if (button != null && button.clickAction() != null) {
            button.clickAction().accept(event);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
