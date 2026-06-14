package com.ultra.ucrate.gui;

import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.ucrate.UCrateProvider;
import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.Reward;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class RewardEditMenu {

    private static final String TITLE = "<gradient:#FF6B35:#FFD700>Edit Reward</gradient>";

    private final Crate crate;
    private final Reward reward;

    public RewardEditMenu(Crate crate, Reward reward) {
        this.crate = crate;
        this.reward = reward;
    }

    public void open(Player player) {
        Gui gui = new Gui(TITLE, 27);

        ItemStack info = ItemBuilder.create(Material.ENDER_PEARL)
                .name("<gold>" + reward.displayName())
                .lore(
                        "<gray>Chance: " + reward.chance() + "</gray>",
                        "<gray>Money: " + reward.money() + "</gray>",
                        "<gray>Commands: " + String.join(", ", reward.commands()) + "</gray>",
                        "<gray>Messages: " + String.join(", ", reward.messages()) + "</gray>",
                        "<gray>Broadcasts: " + String.join(", ", reward.broadcasts()) + "</gray>"
                )
                .build();
        gui.setButton(13, new Button(info, null));

        ItemStack chanceItem = ItemBuilder.create(Material.REPEATER)
                .name("<yellow>Chance: " + reward.chance() + "%</yellow>")
                .lore("<gray>Left-click +10%</gray>", "<gray>Right-click -10%</gray>")
                .build();
        gui.setButton(10, new Button(chanceItem, event -> {
            if (event.isLeftClick()) {
                reward.chance(reward.chance() + 10);
            } else {
                reward.chance(reward.chance() - 10);
            }
            UCrateProvider.get().getCrateManager().updateCrate(crate);
            open(player);
        }));

        ItemStack moneyItem = ItemBuilder.create(Material.GOLD_NUGGET)
                .name("<yellow>Money: $" + reward.money() + "</yellow>")
                .lore("<gray>Left-click +50</gray>", "<gray>Right-click -50</gray>")
                .build();
        gui.setButton(11, new Button(moneyItem, event -> {
            if (event.isLeftClick()) {
                reward.money(reward.money() + 50);
            } else {
                reward.money(reward.money() - 50);
            }
            UCrateProvider.get().getCrateManager().updateCrate(crate);
            open(player);
        }));

        ItemStack deleteItem = ItemBuilder.create(Material.RED_DYE)
                .name("<red>Delete Reward</red>")
                .lore("<gray>Click to delete this reward</gray>")
                .build();
        gui.setButton(16, new Button(deleteItem, event -> {
            crate.removeReward(reward.id());
            UCrateProvider.get().getCrateManager().updateCrate(crate);
            new RewardListMenu(crate).open(player);
        }));

        ItemStack back = ItemBuilder.create(Material.ARROW)
                .name("<red>Back</red>")
                .build();
        gui.setButton(22, new Button(back, event -> new RewardListMenu(crate).open(player)));

        gui.open(player);
    }
}
