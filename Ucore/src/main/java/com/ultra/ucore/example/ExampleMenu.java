package com.ultra.ucore.example;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ExampleMenu {

    public void open(Player player) {
        Gui gui = new Gui("<gradient:#00FFFF:#FF00FF>Example Menu</gradient>", 54);

        ItemStack item = ItemBuilder.create(Material.DIAMOND)
                .name("<gradient:#00FFFF:#FF00FF>Ultra Diamond</gradient>")
                .lore("<gray>Click to receive a message.")
                .build();

        gui.setButton(22, new Button(item, event -> {
            event.getWhoClicked().closeInventory();

            if (event.getWhoClicked() instanceof Player clicked) {
                UCoreAPI.getMessageManager().send(clicked, "prefix.server-reload");
            }
        }));

        gui.open(player);
    }
}
