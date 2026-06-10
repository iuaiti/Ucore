package com.ultra.uskill.tree.gui;

import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.uskill.tree.PlayerSkillData;
import com.ultra.uskill.tree.PlayerStats;
import com.ultra.uskill.tree.SkillTreeManager;
import com.ultra.uskill.tree.StatType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SkillStatsGui {

    private final SkillTreeManager manager;

    private static final int[] STAT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    public SkillStatsGui(SkillTreeManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        PlayerSkillData data = manager.getPlayerData(player.getUniqueId());
        PlayerStats stats = manager.getPlayerStats(player.getUniqueId());

        Gui gui = new Gui("<gradient:#AA00FF:#FF00FF>Your Stats</gradient>", 54);

        fillBorder(gui);

        gui.setButton(4, new Button(ItemBuilder.create(Material.PLAYER_HEAD)
                .name("<gradient:#AA00FF:#FF00FF>" + player.getName() + "</gradient>")
                .lore(
                        "",
                        "<aqua>✦ SP: <yellow>" + data.getSkillPoints(),
                        "<gray>Total Spent: " + data.getTotalSpent(),
                        "<gray>Skills: " + data.getSkillLevels().size() + "/" + manager.getNodeCount()
                )
                .build(), null));

        int idx = 0;
        for (StatType type : StatType.values()) {
            double val = stats.get(type);
            if (idx >= STAT_SLOTS.length) break;

            Material icon = getIconForStat(type);
            String color = val > 0 ? "<green>" : (val < 0 ? "<red>" : "<gray>");
            String sign = val > 0 ? "+" : "";

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(color + sign + String.format("%.1f", val));
            lore.add("");
            lore.add("<dark_gray>" + type.getDescription());

            ItemStack item = ItemBuilder.create(icon)
                    .name("<gradient:#AAFFAA:#55FF55>" + type.getDisplayName() + "</gradient>")
                    .lore(lore.toArray(new String[0]))
                    .build();

            gui.setButton(STAT_SLOTS[idx++], new Button(item, null));
        }

        gui.setButton(49, new Button(
            ItemBuilder.create(Material.ARROW).name("<yellow>← Back</yellow>").build(),
            e -> new SkillTreeGui(manager).open(player)
        ));

        gui.setButton(53, new Button(
            ItemBuilder.create(Material.BARRIER).name("<red>Close</red>").build(),
            e -> player.closeInventory()
        ));

        gui.open(player);
    }

    private void fillBorder(Gui gui) {
        ItemStack border = ItemBuilder.create(Material.PURPLE_STAINED_GLASS_PANE)
                .name("<reset>")
                .build();
        for (int slot : new int[]{0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 50, 51, 52}) {
            gui.setButton(slot, new Button(border, null));
        }
    }

    private Material getIconForStat(StatType type) {
        return switch (type) {
            case DAMAGE -> Material.IRON_SWORD;
            case DEFENSE -> Material.SHIELD;
            case MAX_HEALTH -> Material.GOLDEN_APPLE;
            case MOVEMENT_SPEED -> Material.FEATHER;
            case ATTACK_SPEED -> Material.BLAZE_POWDER;
            case CRIT_CHANCE -> Material.ENDER_PEARL;
            case CRIT_DAMAGE -> Material.DIAMOND_SWORD;
            case LUCK -> Material.EMERALD;
            case REGENERATION -> Material.POTION;
            case MANA -> Material.ENDER_CHEST;
            case MANA_REGEN -> Material.EXPERIENCE_BOTTLE;
            case JUMP_BOOST -> Material.SLIME_BLOCK;
            case FIRE_RESISTANCE -> Material.BLAZE_ROD;
            case ABSORPTION -> Material.TOTEM_OF_UNDYING;
            case LIFE_STEAL -> Material.REDSTONE;
            case THORNS -> Material.CACTUS;
        };
    }
}
