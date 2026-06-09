package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GUI chọn ItemFlags cho item.
 */
public class FlagGui extends AbstractGui {

    private final UItemData data;

    public FlagGui(UItemPlugin plugin, Player player, UItemData data) {
        super(plugin, player, 36, "<blue><bold>Cờ Item (Flags)</bold> <dark_gray>- <white>" + data.getId());
        this.data = data;
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
        // Fill border
        for (int i = 0; i < 36; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 3 || col == 0 || col == 8) {
                setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
        }

        ItemFlag[] flags = ItemFlag.values();

        // Bắt đầu từ slot 10 (hàng 1, cột 1)
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

        for (int i = 0; i < Math.min(flags.length, slots.length); i++) {
            ItemFlag flag = flags[i];
            String flagName = flag.name();
            boolean isEnabled = data.getFlags().contains(flagName);

            Material iconMat = isEnabled ? Material.LIME_DYE : Material.RED_DYE;
            String statusText = isEnabled ? "<green>✔ ON" : "<red>✘ OFF";

            ItemStack item = new ItemStack(iconMat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(TextUtil.parse(
                        (isEnabled ? "<green>" : "<red>") + flagName));
                meta.lore(List.of(
                        TextUtil.parse("<gray>Status: " + statusText),
                        TextUtil.parse(""),
                        TextUtil.parse("<gray>" + getFlagDescription(flag)),
                        TextUtil.parse(""),
                        TextUtil.parse("<yellow>Click để bật/tắt")
                ));
                if (isEnabled) {
                    meta.setEnchantmentGlintOverride(true);
                }
                item.setItemMeta(meta);
            }

            setItem(slots[i], item, event -> {
                if (isEnabled) {
                    data.getFlags().remove(flagName);
                } else {
                    data.getFlags().add(flagName);
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f,
                        !isEnabled ? 1.2f : 0.8f);
                plugin.getItemManager().registerItem(data);
                plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
                refresh();
            });
        }

        // Quay lại
        setItem(31, createBackButton(), event -> goBack());
    }

    private String getFlagDescription(ItemFlag flag) {
        return switch (flag) {
            case HIDE_ENCHANTS -> "Ẩn phù phép";
            case HIDE_ATTRIBUTES -> "Ẩn thuộc tính (sát thương, tốc độ...)";
            case HIDE_UNBREAKABLE -> "Ẩn dòng Unbreakable";
            case HIDE_DESTROYS -> "Ẩn danh sách block có thể phá";
            case HIDE_PLACED_ON -> "Ẩn danh sách block có thể đặt lên";
            case HIDE_ADDITIONAL_TOOLTIP -> "Ẩn tooltip bổ sung (potion, banner...)";
            case HIDE_DYE -> "Ẩn màu nhuộm giáp da";
            case HIDE_ARMOR_TRIM -> "Ẩn armor trim";
            default -> "Không có mô tả";
        };
    }
}
