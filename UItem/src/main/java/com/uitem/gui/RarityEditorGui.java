package com.uitem.gui;

import com.uitem.UItemPlugin;
import com.uitem.rarity.RarityConfig;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor for a single rarity definition.
 * Allows changing color, prefix, and lore.
 */
public class RarityEditorGui extends AbstractGui {

    private final String rarityName;
    private final RarityConfig.RarityEntry entry;
    private final RarityListGui parentList;

    public RarityEditorGui(UItemPlugin plugin, Player player, String rarityName, RarityConfig.RarityEntry entry, RarityListGui parentList) {
        super(plugin, player, 54, "<gold><bold>Chỉnh Rarity: </gold><white>" + rarityName);
        this.rarityName = rarityName;
        this.entry = entry != null ? entry : new RarityConfig.RarityEntry(rarityName, "<white>", "", List.of());
        this.parentList = parentList;
    }

    @Override
    public void open() {
        if (!player.hasPermission("uitem.editor")) {
            player.sendMessage(TextUtil.error(com.uitem.util.Messages.noPermission()));
            return;
        }
        super.open();
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, createItem(Material.NETHER_STAR,
                "<gold><bold>" + rarityName + "</bold>",
                List.of(
                        "<gray>Color hiện tại: " + (entry.color() != null ? entry.color() : "<white>"),
                        "<gray>Prefix: " + (entry.prefix() != null ? entry.prefix() : ""),
                        "<gray>Số dòng lore: <white>" + (entry.lore() != null ? entry.lore().size() : 0)
                )));

        // Color
        setItem(20, createItem(Material.RED_DYE, "<gold><bold>Color</bold>",
                List.of(
                        "<gray>Hiện tại: " + (entry.color() != null ? entry.color() : "<white>"),
                        "",
                        "<yellow>Click để đổi màu (MiniMessage)",
                        "<gray>Ví dụ: <gold>, <gradient:#ff0000:#00ff00>, <red>"
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập color mới (MiniMessage) cho " + rarityName + ":",
                    input -> {
                        RarityConfig.setRarity(rarityName, input.trim(), entry.prefix(), entry.lore());
                        RarityConfig.saveRarities();
                        if (parentList != null) {
                            parentList.reopen();
                        } else {
                            refresh();
                        }
                    });
        });

        // Prefix
        setItem(22, createItem(Material.NAME_TAG, "<gold><bold>Prefix</bold>",
                List.of(
                        "<gray>Hiện tại: " + (entry.prefix() != null && !entry.prefix().isEmpty() ? entry.prefix() : "<gray>(không có)"),
                        "",
                        "<yellow>Click để đổi prefix",
                        "<gray>Có thể dùng MiniMessage gradient"
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập prefix mới cho " + rarityName + " (hoặc để trống):",
                    input -> {
                        RarityConfig.setRarity(rarityName, entry.color(), input.trim(), entry.lore());
                        RarityConfig.saveRarities();
                        if (parentList != null) {
                            parentList.reopen();
                        } else {
                            refresh();
                        }
                    });
        });

        // Lore
        setItem(24, createItem(Material.WRITABLE_BOOK, "<gold><bold>Lore</bold>",
                List.of(
                        "<gray>Số dòng: <white>" + (entry.lore() != null ? entry.lore().size() : 0),
                        "",
                        "<yellow>Click để chỉnh lore",
                        "<gray>Mỗi dòng là một dòng lore"
                )), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            // Reuse LoreEditorGui? For simplicity, use chat multi-line input
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập lore cho " + rarityName + " (phân cách bằng | ):",
                    input -> {
                        List<String> newLore = new ArrayList<>();
                        if (!input.trim().isEmpty()) {
                            for (String line : input.split("\\|")) {
                                newLore.add(line.trim());
                            }
                        }
                        RarityConfig.setRarity(rarityName, entry.color(), entry.prefix(), newLore);
                        RarityConfig.saveRarities();
                        if (parentList != null) {
                            parentList.reopen();
                        } else {
                            refresh();
                        }
                    });
        });

        // Quick presets for common colors
        setItem(38, createItem(Material.GRAY_DYE, "<gray>COMMON", List.of()), ev -> applyQuick("COMMON", "<gray>", ""));
        setItem(39, createItem(Material.GREEN_DYE, "<green>UNCOMMON", List.of()), ev -> applyQuick("UNCOMMON", "<green>", "<green>"));
        setItem(40, createItem(Material.BLUE_DYE, "<blue>RARE", List.of()), ev -> applyQuick("RARE", "<blue>", "<blue>"));
        setItem(41, createItem(Material.PURPLE_DYE, "<dark_purple>EPIC", List.of()), ev -> applyQuick("EPIC", "<dark_purple>", "<dark_purple>"));
        setItem(42, createItem(Material.GOLD_INGOT, "<gold>LEGENDARY", List.of()), ev -> applyQuick("LEGENDARY", "<gold>", "<gold>"));
        setItem(43, createItem(Material.RED_DYE, "<red>ULTRA", List.of()), ev -> applyQuick("ULTRA", "<red>", "<red>"));

        // Back
        setItem(53, createBackButton(), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            if (hasBackAction()) {
                goBack();
            } else if (parentList != null) {
                parentList.reopen();
            } else {
                new RarityListGui(plugin, player).open();
            }
        });
    }

    private void applyQuick(String name, String color, String prefix) {
        List<String> lore = List.of(
                "<dark_gray>──────────────",
                color + name
        );
        RarityConfig.setRarity(rarityName, color, prefix, lore);
        RarityConfig.saveRarities();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
        if (parentList != null) {
            parentList.reopen();
        } else {
            refresh();
        }
    }
}
