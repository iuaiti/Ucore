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
import java.util.Map;

/**
 * GUI to list and manage rarities.
 * Allows creating, editing, deleting rarities (saved to rarity.yml).
 */
public class RarityListGui extends AbstractGui {

    private final int returnPage; // for going back to item list if opened from there

    public RarityListGui(UItemPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public RarityListGui(UItemPlugin plugin, Player player, int returnPage) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>Rarity Manager</bold></gradient>");
        this.returnPage = returnPage;
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
                "<gold><bold>Quản lý Rarity</bold>",
                List.of(
                        "<gray>Rarities được lưu trong <white>rarity.yml</white>",
                        "<yellow>Click để chỉnh sửa | Click phải để xóa",
                        "<gray>Hỗ trợ copy-paste dễ dàng trong file"
                )));

        Map<String, RarityConfig.RarityEntry> all = RarityConfig.getAll();
        List<String> keys = new ArrayList<>(all.keySet());

        int[] slots = {10,11,12,13,14,15,16,17,19,20,21,22,23,24,25,26};
        for (int i = 0; i < Math.min(keys.size(), slots.length); i++) {
            final String key = keys.get(i);
            RarityConfig.RarityEntry entry = all.get(key);
            ItemStack icon = buildRarityIcon(key, entry);
            setItem(slots[i], icon, ev -> {
                if (ev.isRightClick()) {
                    // Delete
                    RarityConfig.removeRarity(key);
                    RarityConfig.saveRarities();
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 1f);
                    refresh();
                } else {
                    // Edit
                    new RarityEditorGui(plugin, player, key, entry, this).openAsChildOf(this);
                }
            });
        }

        if (keys.isEmpty()) {
            setItem(10, createItem(Material.BARRIER, "<gray><italic>No rarities yet</italic>",
                    List.of("<yellow>Click the button below to create a new rarity")));
        }

        // Create new rarity
        setItem(40, createItem(Material.EMERALD_BLOCK, "<green><bold>+ CREATE NEW RARITY</bold>",
                List.of("<gray>Create new rarity (color, prefix, lore)")), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            plugin.getChatInputManager().requestInput(player, "<yellow>Nhập tên rarity mới (ví dụ: MYTHIC):", input -> {
                String name = input.trim().toUpperCase();
                if (name.isEmpty() || RarityConfig.getAll().containsKey(name)) {
                    player.sendMessage(TextUtil.error("Tên không hợp lệ hoặc đã tồn tại!"));
                    new RarityListGui(plugin, player, returnPage).open();
                    return;
                }
                RarityConfig.setRarity(name, "<white>", "", List.of());
                RarityConfig.saveRarities();
                new RarityEditorGui(plugin, player, name, RarityConfig.get(name), this).openAsChildOf(this);
            });
        });

        // Back button
        setItem(53, createBackButton(), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            if (returnPage >= 0) {
                new ItemListGui(plugin, player, returnPage).open();
            } else {
                player.closeInventory();
            }
        });
    }

    private ItemStack buildRarityIcon(String key, RarityConfig.RarityEntry entry) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Color: <white>" + (entry.color() != null ? entry.color() : "<white>"));
        lore.add("<gray>Prefix: " + (entry.prefix() != null && !entry.prefix().isEmpty() ? entry.prefix() : "<gray>(không có)"));
        if (entry.lore() != null && !entry.lore().isEmpty()) {
            lore.add("<gray>Lore:");
            for (String line : entry.lore()) {
                lore.add("  " + line);
            }
        }
        lore.add("");
        lore.add("<yellow>Click trái: Chỉnh sửa");
        lore.add("<red>Click phải: Xóa");
        return createItem(Material.NETHER_STAR, entry.prefix() + " <white>" + key, lore);
    }
}
