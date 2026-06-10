package com.ultra.uskill.tree.gui;

import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.uskill.tree.PlayerSkillData;
import com.ultra.uskill.tree.SkillTreeNode;
import com.ultra.uskill.tree.SkillTreeManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SkillDetailGui {

    private final SkillTreeManager manager;
    private final SkillTreeNode node;

    public SkillDetailGui(SkillTreeManager manager, SkillTreeNode node) {
        this.manager = manager;
        this.node = node;
    }

    public void open(Player player) {
        PlayerSkillData data = manager.getPlayerData(player.getUniqueId());
        int level = data.getSkillLevel(node.getId());
        boolean unlocked = level > 0;
        boolean maxed = level >= node.getMaxLevel();
        boolean canUpgrade = data.canUpgrade(node, manager);

        Gui gui = new Gui("<gradient:#FF6B35:#FFD700>" + node.getDisplayName() + "</gradient>", 45);

        fillBorder(gui);

        gui.setButton(22, createSkillIcon(player, node, data));

        gui.setButton(31, createUpgradeButton(player, node, data, canUpgrade, maxed));

        if (unlocked && level > 1) {
            gui.setButton(33, createRefundButton(player, node, data));
        }

        int[] infoSlots = {10, 11, 12, 13, 14, 15, 16};
        int idx = 0;
        for (var entry : node.getStatsPerLevel().entrySet()) {
            if (idx >= infoSlots.length) break;
            double currentValue = entry.getValue() * Math.max(1, level);
            double nextValue = entry.getValue() * (level + 1);
            gui.setButton(infoSlots[idx++], createStatDisplay(
                entry.getKey().name(),
                entry.getKey().getDisplayName(),
                currentValue,
                maxed ? -1 : nextValue
            ));
        }

        gui.setButton(40, new Button(
            ItemBuilder.create(Material.ARROW).name("<yellow>← Back to Skill Tree</yellow>").build(),
            e -> new SkillTreeGui(manager).open(player)
        ));

        gui.open(player);
    }

    private void fillBorder(Gui gui) {
        ItemStack border = ItemBuilder.create(Material.BLACK_STAINED_GLASS_PANE)
                .name("<reset>")
                .build();
        for (int slot : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 41, 42, 43, 44}) {
            gui.setButton(slot, new Button(border, null));
        }
    }

    private Button createSkillIcon(Player player, SkillTreeNode node, PlayerSkillData data) {
        int level = data.getSkillLevel(node.getId());
        boolean unlocked = level > 0;
        boolean maxed = level >= node.getMaxLevel();

        List<String> lore = new ArrayList<>(node.getDescription());
        lore.add("");
        if (unlocked) {
            lore.add("<green>◆ Level: <yellow>" + level + "/" + node.getMaxLevel());
        } else {
            int nextLevel = level;
            int cost = node.getCost(nextLevel);
            lore.add("<gray>◆ Level: <red>LOCKED");
            lore.add("<yellow>Cost: " + cost + " SP");
        }

        if (!node.getPrerequisites().isEmpty()) {
            lore.add("");
            lore.add("<gray>Prerequisites:");
            for (String prereqId : node.getPrerequisites()) {
                SkillTreeNode prereq = manager.getNode(prereqId);
                if (prereq == null) continue;
                int prereqLevel = data.getSkillLevel(prereqId);
                boolean met = prereqLevel >= prereq.getMaxLevel();
                lore.add((met ? "<green>✓" : "<red>✗") + " " + prereq.getDisplayName());
            }
        }

        if (maxed) {
            lore.add("");
            lore.add("<gold>✦ MAX LEVEL ✦");
        }

        Material icon = unlocked ? node.getIcon() : Material.GRAY_DYE;
        String name = unlocked
            ? "<gradient:#55FF55:#AAFFAA>" + node.getDisplayName() + "</gradient>"
            : "<red>" + node.getDisplayName() + "</red>";

        ItemStack item = ItemBuilder.create(icon).name(name).lore(lore.toArray(new String[0])).build();
        return new Button(item, null);
    }

    private Button createUpgradeButton(Player player, SkillTreeNode node, PlayerSkillData data, boolean canUpgrade, boolean maxed) {
        int level = data.getSkillLevel(node.getId());
        int nextLevel = level;
        int cost = node.getCost(nextLevel);

        Material mat;
        String name;
        List<String> lore = new ArrayList<>();

        if (maxed) {
            mat = Material.NETHERITE_INGOT;
            name = "<gold>✦ MAX LEVEL ✦";
            lore.add("<gray>This skill is fully upgraded!");
        } else if (canUpgrade) {
            mat = Material.EXPERIENCE_BOTTLE;
            name = "<green>▲ Upgrade</green>";
            lore.add("<yellow>Level: " + level + " → " + (level + 1));
            lore.add("<yellow>Cost: " + cost + " SP");
            lore.add("<yellow>Points: " + data.getSkillPoints());
            lore.add("");
            lore.add("<green>Click to upgrade!");
        } else if (level <= 0 && !data.canUpgrade(node, manager)) {
            mat = Material.REDSTONE_BLOCK;
            name = "<red>Locked</red>";
            lore.add("<gray>Complete prerequisites first");
        } else {
            mat = Material.REDSTONE;
            name = "<red>Not enough points</red>";
            lore.add("<yellow>Cost: " + cost + " SP");
            lore.add("<yellow>You have: " + data.getSkillPoints() + " SP");
        }

        ItemStack item = ItemBuilder.create(mat).name(name).lore(lore.toArray(new String[0])).build();

        return new Button(item, e -> {
            if (maxed) return;
            if (canUpgrade) {
                manager.upgradeSkill(player, node.getId());
                open(player);
            } else {
                player.sendMessage(com.ultra.ucore.util.TextUtil.parse("<red>Cannot upgrade this skill yet!</red>"));
            }
        });
    }

    private Button createRefundButton(Player player, SkillTreeNode node, PlayerSkillData data) {
        int level = data.getSkillLevel(node.getId());
        int refund = 0;
        for (int i = 0; i < level; i++) {
            refund += node.getCost(i);
        }

        ItemStack item = ItemBuilder.create(Material.LAVA_BUCKET)
                .name("<red>↺ Refund</red>")
                .lore(
                        "",
                        "<gray>Refund this skill",
                        "<green>+" + refund + " SP",
                        "",
                        "<dark_red>Click to refund"
                )
                .build();

        return new Button(item, e -> {
            manager.resetSkill(player, node.getId());
            player.closeInventory();
        });
    }

    private Button createStatDisplay(String key, String name, double currentValue, double nextValue) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + name);
        lore.add("<green>Current: +" + String.format("%.1f", currentValue));
        if (nextValue >= 0) {
            lore.add("<yellow>Next: +" + String.format("%.1f", nextValue));
        }
        lore.add("");
        lore.add("<dark_gray>" + key);

        ItemStack item = ItemBuilder.create(Material.ENCHANTED_BOOK)
                .name("<gradient:#AAFFAA:#55FF55>" + name + "</gradient>")
                .lore(lore.toArray(new String[0]))
                .build();
        return new Button(item, null);
    }
}
