package com.ultra.ucrate.gui;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.PaginatedGui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.ucrate.UCrateProvider;
import com.ultra.ucrate.model.Crate;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CrateListMenu {

    private static final String TITLE = "<gradient:#FF6B35:#FFD700>UCrate Editor</gradient>";
    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public void open(Player player) {
        PaginatedGui gui = new PaginatedGui(TITLE, SIZE, CONTENT_SLOTS);

        var crates = UCrateProvider.get().getCrateManager().getAllCrates();

        for (Crate crate : crates) {
            ItemStack display = crate.displayItem();
            if (display == null) {
                display = ItemBuilder.create(Material.CHEST)
                        .name("<gold>" + crate.displayName())
                        .lore(
                                "<gray>ID: " + crate.id() + "</gray>",
                                "<gray>Type: " + crate.type().name() + "</gray>",
                                "<gray>Rewards: " + crate.rewards().size() + "</gray>",
                                "<gray>Animation: " + crate.animationId() + "</gray>",
                                "",
                                "<yellow>Left-click to edit</yellow>",
                                "<red>Shift+right-click to delete</red>"
                        )
                        .build();
            }

            gui.addContent(new Button(display, event -> {
                if (event.isShiftClick() && event.isRightClick()) {
                    UCrateProvider.get().getCrateManager().removeCrate(crate.id());
                    player.sendMessage(Component.text("Crate deleted: " + crate.id()));
                    new CrateListMenu().open(player);
                } else {
                    new CrateEditMenu(crate).open(player);
                }
            }));
        }

        ItemStack border = ItemBuilder.create(Material.GRAY_STAINED_GLASS_PANE)
                .name("<gray>")
                .build();

        for (int slot : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53}) {
            gui.setButton(slot, new Button(border, null));
        }

        if (crates.size() > CONTENT_SLOTS.length) {
            ItemStack next = ItemBuilder.create(Material.ARROW)
                    .name("<green>Next Page</green>")
                    .build();
            gui.setButton(50, new Button(next, e -> gui.nextPage(player)));

            ItemStack prev = ItemBuilder.create(Material.ARROW)
                    .name("<green>Previous Page</green>")
                    .build();
            gui.setButton(48, new Button(prev, e -> gui.previousPage(player)));
        }

        ItemStack close = ItemBuilder.create(Material.BARRIER)
                .name("<red>Close</red>")
                .build();
        gui.setButton(49, new Button(close, e -> player.closeInventory()));

        gui.open(player, 0);
    }
}
