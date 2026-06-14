package com.ultra.ucrate.gui;

import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.PaginatedGui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.ucrate.UCrateProvider;
import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.Reward;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class RewardListMenu {

    private static final String TITLE = "<gradient:#FF6B35:#FFD700>Rewards</gradient>";
    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final Crate crate;

    public RewardListMenu(Crate crate) {
        this.crate = crate;
    }

    public void open(Player player) {
        PaginatedGui gui = new PaginatedGui(TITLE, SIZE, CONTENT_SLOTS);

        for (Reward reward : crate.rewards()) {
            ItemStack display = reward.displayItem();
            if (display == null) {
                display = ItemBuilder.create(Material.ENDER_PEARL)
                        .name("<gold>" + reward.displayName())
                        .lore(
                                "<gray>Chance: " + reward.chance() + "%</gray>",
                                "<gray>Commands: " + reward.commands().size() + "</gray>",
                                "<gray>Money: " + reward.money() + "</gray>",
                                "",
                                "<yellow>Click to edit</yellow>"
                        )
                        .build();
            }

            gui.addContent(new Button(display, event -> {
                new RewardEditMenu(crate, reward).open(player);
            }));
        }

        ItemStack border = ItemBuilder.create(Material.GRAY_STAINED_GLASS_PANE)
                .name("<gray>")
                .build();

        for (int slot : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53}) {
            gui.setButton(slot, new Button(border, null));
        }

        ItemStack addReward = ItemBuilder.create(Material.EMERALD)
                .name("<green>Add Reward</green>")
                .build();
        gui.setButton(51, new Button(addReward, event -> {
            Reward newReward = new Reward("reward_" + (crate.rewards().size() + 1));
            crate.addReward(newReward);
            UCrateProvider.get().getCrateManager().updateCrate(crate);
            open(player);
        }));

        if (crate.rewards().size() > CONTENT_SLOTS.length) {
            ItemStack next = ItemBuilder.create(Material.ARROW)
                    .name("<green>Next Page</green>")
                    .build();
            gui.setButton(50, new Button(next, e -> gui.nextPage(player)));

            ItemStack prev = ItemBuilder.create(Material.ARROW)
                    .name("<green>Previous Page</green>")
                    .build();
            gui.setButton(48, new Button(prev, e -> gui.previousPage(player)));
        }

        ItemStack back = ItemBuilder.create(Material.ARROW)
                .name("<red>Back to Crate</red>")
                .build();
        gui.setButton(49, new Button(back, event -> new CrateEditMenu(crate).open(player)));

        gui.open(player, 0);
    }
}
