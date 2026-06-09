package com.ultra.umenu.menu;

import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.umenu.UMenuPlugin;
import com.ultra.umenu.manager.QuestManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class QuestMenu extends BaseMenu {

    private final QuestManager questManager;

    public QuestMenu(UMenuPlugin plugin, MenuManager menuManager, QuestManager questManager) {
        super(plugin, menuManager, "quest-menu");
        this.questManager = questManager;
    }

    @Override
    public void open(Player player) {
        String title = getTitle(configKey);
        int size = getSize(configKey);
        Gui gui = createGui(title, size);
        fillBorder(gui, configKey);

        int[] contentSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int slot = 0;

        List<QuestManager.Quest> dailies = questManager.getDailyQuests(player);
        for (QuestManager.Quest quest : dailies) {
            if (slot >= contentSlots.length) break;
            double progress = questManager.getProgress(player, quest);
            String progressBar = createProgressBar(progress, quest.target());

            ItemStack item = ItemBuilder.create(Material.PAPER)
                    .name("<aqua>" + quest.name() + "</aqua>")
                    .lore(
                            "<gray>" + quest.description() + "</gray>",
                            "",
                            "<yellow>Progress: " + progressBar + "</yellow>",
                            "<gray>" + (int) progress + " / " + (int) quest.target() + "</gray>",
                            "",
                            "<green>Reward: " + quest.reward() + "</green>"
                    )
                    .build();
            gui.setButton(contentSlots[slot++], new Button(item, null));
        }

        List<QuestManager.Quest> weeklies = questManager.getWeeklyQuests(player);
        for (QuestManager.Quest quest : weeklies) {
            if (slot >= contentSlots.length) break;
            double progress = questManager.getProgress(player, quest);
            String progressBar = createProgressBar(progress, quest.target());

            ItemStack item = ItemBuilder.create(Material.MAP)
                    .name("<light_purple>" + quest.name() + "</light_purple>")
                    .lore(
                            "<gray>" + quest.description() + "</gray>",
                            "",
                            "<yellow>Progress: " + progressBar + "</yellow>",
                            "<gray>" + (int) progress + " / " + (int) quest.target() + "</gray>",
                            "",
                            "<green>Reward: " + quest.reward() + "</green>"
                    )
                    .build();
            gui.setButton(contentSlots[slot++], new Button(item, null));
        }

        addConfigItems(gui, configKey, player);
        gui.open(player);
    }

    @Override
    protected void build(Gui gui, Player player) {
    }

    private String createProgressBar(double current, double max) {
        int filled = (int) ((current / max) * 10);
        filled = Math.min(filled, 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("<green>■</green>");
            } else {
                bar.append("<dark_gray>■</dark_gray>");
            }
        }
        return bar.toString();
    }
}
