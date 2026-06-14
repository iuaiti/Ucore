package com.ultra.uskill.tree.gui;

import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.ucore.util.TextUtil;
import com.ultra.uskill.tree.PlayerSkillData;
import com.ultra.uskill.tree.PlayerStats;
import com.ultra.uskill.tree.SkillTreeNode;
import com.ultra.uskill.tree.SkillTreeManager;
import com.ultra.uskill.tree.StatType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SkillTreeGui {

    private static final int[] BORDER_SLOTS = {
        0, 1, 2, 3, 5, 6, 7, 8,
        9, 17,
        18, 26,
        27, 35,
        36, 44,
        45, 46, 47, 48, 49, 50, 51, 52, 53
    };

    private final SkillTreeManager manager;

    public SkillTreeGui(SkillTreeManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        PlayerSkillData data = manager.getPlayerData(player.getUniqueId());
        PlayerStats stats = manager.getPlayerStats(player.getUniqueId());

        Gui gui = new Gui("<gradient:#FF6B35:#FFD700>Skill Tree</gradient>", 54);

        fillBorder(gui);

        gui.setButton(4, createPlayerHead(player, data));

        for (SkillTreeNode node : manager.getAllNodes()) {
            gui.setButton(node.getSlot(), createSkillButton(player, node, data));
        }

        gui.setButton(48, createStatsButton(player, stats));
        gui.setButton(50, createResetButton(player, data));
        gui.setButton(53, new Button(
            ItemBuilder.create(Material.BARRIER).name("<red>Close</red>").build(),
            e -> player.closeInventory()
        ));

        gui.open(player);
    }

    private void fillBorder(Gui gui) {
        ItemStack border = ItemBuilder.create(Material.GRAY_STAINED_GLASS_PANE)
                .name("<reset>")
                .build();
        for (int slot : BORDER_SLOTS) {
            gui.setButton(slot, new Button(border, null));
        }
    }

    private Button createPlayerHead(Player player, PlayerSkillData data) {
        ItemStack head = ItemBuilder.create(Material.PLAYER_HEAD)
                .name("<gradient:#FF6B35:#FFD700>" + player.getName() + "'s Skills</gradient>")
                .lore(
                        "",
                        "<aqua>✦ Skill Points: <yellow>" + data.getSkillPoints(),
                        "<gray>Total Spent: " + data.getTotalSpent(),
                        "<gray>Skills Unlocked: " + data.getSkillLevels().size(),
                        "",
                        "<dark_gray>Click to view stats"
                )
                .build();
        if (head.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setPlayerProfile(player.getPlayerProfile());
            head.setItemMeta(skullMeta);
        }
        return new Button(head, e -> {
            new SkillStatsGui(manager).open(player);
        });
    }

    private Button createSkillButton(Player player, SkillTreeNode node, PlayerSkillData data) {
        int level = data.getSkillLevel(node.getId());
        boolean unlocked = level > 0;
        boolean maxed = level >= node.getMaxLevel();
        boolean canUnlock = data.canUpgrade(node, manager);

        List<String> lore = new ArrayList<>(node.getDescription());
        lore.add("");

        if (unlocked) {
            lore.add("<green>◆ Level: <yellow>" + level + "/" + node.getMaxLevel());
        } else {
            lore.add("<gray>◆ Level: <red>LOCKED" + "/" + node.getMaxLevel());
        }

        if (!node.getStatsPerLevel().isEmpty()) {
            lore.add("");
            lore.add("<gray>⤷ Stats per level:");
            for (var entry : node.getStatsPerLevel().entrySet()) {
                double total = entry.getValue() * Math.max(1, level);
                String color = entry.getValue() > 0 ? "<green>" : "<red>";
                lore.add("  " + color + "+" + entry.getValue() + " " + entry.getKey().getDisplayName() + " <gray>(total: " + String.format("%.1f", total) + ")");
            }
        }

        if (!node.getPrerequisites().isEmpty()) {
            lore.add("");
            lore.add("<gray>Requires:");
            for (String prereqId : node.getPrerequisites()) {
                SkillTreeNode prereq = manager.getNode(prereqId);
                if (prereq == null) continue;
                int prereqLevel = data.getSkillLevel(prereqId);
                // Chỉ cần unlocked (level >= 1) — không cần max level
                boolean met = prereqLevel >= 1;
                lore.add((met ? "<green>✓" : "<red>✗") + " " + prereq.getDisplayName()
                        + " <gray>(cần Lv.1+, đang ở Lv." + prereqLevel + ")");
            }
        }

        lore.add("");
        if (maxed) {
            lore.add("<gold>✦ MAX LEVEL ✦");
        } else if (unlocked || canUnlock) {
            int nextLevel = level;
            int cost = node.getCost(nextLevel);
            lore.add("<yellow>Click to upgrade <gray>(" + cost + " SP)");
        } else {
            lore.add("<red>Prerequisites not met");
        }

        if (unlocked && level > 1) {
            lore.add("<dark_gray>Shift+Click to refund");
        }

        Material icon = unlocked ? node.getIcon() : Material.GRAY_DYE;
        String name;
        if (maxed) {
            name = "<gradient:#FFD700:#FF6B35>✦ " + node.getDisplayName() + " ✦</gradient>";
        } else if (unlocked) {
            name = "<gradient:#55FF55:#AAFFAA>" + node.getDisplayName() + "</gradient>";
        } else if (canUnlock) {
            name = "<gold>" + node.getDisplayName() + "</gold>";
        } else {
            name = "<dark_red>" + node.getDisplayName() + "</dark_red>";
        }

        if (maxed) {
            icon = node.getIcon();
        }

        ItemStack item = ItemBuilder.create(icon)
                .name(name)
                .lore(lore.toArray(new String[0]))
                .build();

        return new Button(item, event -> {
            if (event.isShiftClick() && unlocked && level > 1) {
                manager.resetSkill(player, node.getId());
                open(player);
                return;
            }
            if (maxed) {
                player.sendMessage(TextUtil.parse("<gold>" + node.getDisplayName() + " <red>is already max level!</red>"));
                return;
            }
            new SkillDetailGui(manager, node).open(player);
        });
    }

    private Button createStatsButton(Player player, PlayerStats stats) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (StatType type : StatType.values()) {
            double val = stats.get(type);
            if (val > 0) {
                lore.add("<green>+" + String.format("%.1f", val) + " " + type.getDisplayName());
            }
        }
        if (lore.size() == 1) {
            lore.add("<gray>No stats unlocked yet");
        }
        lore.add("");
        lore.add("<yellow>Click for detailed stats");

        ItemStack item = ItemBuilder.create(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                .name("<gradient:#AA00FF:#FF00FF>Your Stats</gradient>")
                .lore(lore.toArray(new String[0]))
                .build();

        return new Button(item, e -> new SkillStatsGui(manager).open(player));
    }

    private Button createResetButton(Player player, PlayerSkillData data) {
        ItemStack item = ItemBuilder.create(Material.BARRIER)
                .name("<red>Reset All Skills</red>")
                .lore(
                        "",
                        "<gray>Refunds all spent points",
                        "<gray>You get back: <yellow>" + data.getTotalSpent() + " SP",
                        "",
                        "<dark_red>Click to reset"
                )
                .build();

        return new Button(item, e -> {
            if (data.getSkillLevels().isEmpty()) {
                player.sendMessage(TextUtil.parse("<red>No skills to reset!</red>"));
                return;
            }
            manager.resetAll(player);
            open(player);
        });
    }
}
