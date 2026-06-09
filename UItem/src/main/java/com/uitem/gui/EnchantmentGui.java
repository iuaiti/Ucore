package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI chọn và chỉnh sửa enchantments cho item.
 */
public class EnchantmentGui extends AbstractGui {

    private final UItemData data;
    private int page;

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public EnchantmentGui(UItemPlugin plugin, Player player, UItemData data) {
        this(plugin, player, data, 0);
    }

    public EnchantmentGui(UItemPlugin plugin, Player player, UItemData data, int page) {
        super(plugin, player, 54, "<light_purple><bold>Phù Phép</bold> <dark_gray>- <white>" + data.getId());
        this.data = data;
        this.page = page;
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

        // Title
        setItem(4, createItem(Material.ENCHANTING_TABLE,
                "<light_purple><bold>Trình Chỉnh Phù Phép</bold>",
                List.of(
                        "<gray>Edit enchantments for this item",
                        "",
                        "<yellow>Left click: <white>Increase level",
                        "<yellow>Right click: <white>Decrease level",
                        "<yellow>Shift+Click: <white>Remove entirely"
                )));

        List<Enchantment> enchants = plugin.getItemManager().getCommonEnchantments();
        int totalPages = Math.max(1, (int) Math.ceil((double) enchants.size() / ITEMS_PER_PAGE));

        int startIndex = page * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex >= enchants.size()) break;

            Enchantment ench = enchants.get(dataIndex);
            String enchKey = ench.getKey().getKey();
            String displayName = plugin.getItemManager().getEnchantmentDisplayName(ench);
            int currentLevel = data.getEnchantments().getOrDefault(enchKey, 0);
            boolean hasEnchant = currentLevel > 0;

            ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (hasEnchant) {
                    meta.displayName(TextUtil.parse("<green><bold>" + displayName + " " + toRoman(currentLevel)));
                    meta.setEnchantmentGlintOverride(true);
                } else {
                    meta.displayName(TextUtil.parse("<gray>" + displayName));
                    meta.setEnchantmentGlintOverride(false);
                }

                List<String> lore = new ArrayList<>();
                lore.add("<dark_gray>Key: " + enchKey);
                lore.add("");
                if (hasEnchant) {
                    lore.add("<green>✔ Added - Level " + currentLevel);
                } else {
                    lore.add("<red>✘ Not added");
                }
                lore.add("");
                lore.add("<yellow>Left click: <white>Increase level");
                lore.add("<yellow>Right click: <white>Decrease level");
                lore.add("<yellow>Shift+Click: <white>Remove");

                meta.lore(lore.stream().map(TextUtil::parse).toList());
                item.setItemMeta(meta);
            }

            setItem(ITEM_SLOTS[i], item, event -> {
                if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    // Xóa hoàn toàn
                    data.getEnchantments().remove(enchKey);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                } else if (event.getClick() == ClickType.LEFT) {
                    // Tăng level
                    int newLevel = currentLevel + 1;
                    if (newLevel > 255) newLevel = 255;
                    data.getEnchantments().put(enchKey, newLevel);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.2f);
                } else if (event.getClick() == ClickType.RIGHT) {
                    // Giảm level
                    int newLevel = currentLevel - 1;
                    if (newLevel <= 0) {
                        data.getEnchantments().remove(enchKey);
                    } else {
                        data.getEnchantments().put(enchKey, newLevel);
                    }
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
                }
                plugin.getItemManager().registerItem(data);
                plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
                refresh();
            });
        }

        // Trang trước
        if (page > 0) {
            setItem(46, createPreviousButton(), event -> {
                page--;
                refresh();
            });
        }

        // Trang sau
        if (page < totalPages - 1) {
            setItem(52, createNextButton(), event -> {
                page++;
                refresh();
            });
        }

        // Quay lại
        setItem(49, createBackButton(), event -> goBack());
    }

    private String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }
}
