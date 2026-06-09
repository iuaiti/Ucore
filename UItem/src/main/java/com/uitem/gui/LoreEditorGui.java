package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI chỉnh sửa lore (mô tả) của item.
 * Hiển thị từng dòng lore, cho phép thêm/sửa/xóa/di chuyển.
 */
public class LoreEditorGui extends AbstractGui {

    private final UItemData data;
    private int page;
    private int selectedLine; // -1 = không chọn dòng nào

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public LoreEditorGui(UItemPlugin plugin, Player player, UItemData data) {
        super(plugin, player, 54, "<gold><bold>Chỉnh Lore</bold> <dark_gray>- <white>" + data.getId());
        this.data = data;
        this.page = 0;
        this.selectedLine = -1;
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
        setItem(4, createItem(Material.WRITABLE_BOOK,
                "<gold><bold>Chỉnh Lore</bold>",
                List.of(
                        "<gray>Edit item description",
                        "",
                        "<yellow>Left click: <white>Edit line",
                        "<yellow>Right click: <white>Delete line",
                        "<yellow>Shift+Click: <white>Insert blank line above"
                )));

        List<String> lore = data.getLore();
        int totalPages = Math.max(1, (int) Math.ceil((double) lore.size() / ITEMS_PER_PAGE));

        int startIndex = page * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int lineIndex = startIndex + i;
            if (lineIndex >= lore.size()) break;

            String line = lore.get(lineIndex);
            int finalLineIndex = lineIndex;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(TextUtil.parse("<white>Line " + (lineIndex + 1)));
                meta.lore(List.of(
                        TextUtil.parse("<gray>Content:"),
                        TextUtil.parse(line.isEmpty() ? "<dark_gray>(empty)" : line),
                        TextUtil.parse(""),
                        TextUtil.parse("<yellow>Left click: <white>Edit"),
                        TextUtil.parse("<red>Right click: <white>Delete"),
                        TextUtil.parse("<aqua>Shift+Click: <white>Insert blank line")
                ));
                item.setItemMeta(meta);
            }

            setItem(ITEM_SLOTS[i], item, event -> {
                if (event.getClick() == ClickType.LEFT) {
                    // Sửa dòng
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    plugin.getChatInputManager().requestInput(player,
                            "<yellow>Nhập nội dung mới cho dòng " + (finalLineIndex + 1) + " <gray>(hỗ trợ MiniMessage):",
                            input -> {
                                if (finalLineIndex < data.getLore().size()) {
                                    data.getLore().set(finalLineIndex, input);
                                    plugin.getItemManager().registerItem(data);
                                    plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
                                }
                                new LoreEditorGui(plugin, player, data).open();
                            });
                } else if (event.getClick() == ClickType.RIGHT) {
                    // Xóa dòng
                    if (finalLineIndex < data.getLore().size()) {
                        data.getLore().remove(finalLineIndex);
                        plugin.getItemManager().registerItem(data);
                        plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    refresh();
                } else if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    // Chèn dòng trống phía trên
                    if (finalLineIndex <= data.getLore().size()) {
                        data.getLore().add(finalLineIndex, "");
                        plugin.getItemManager().registerItem(data);
                        plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
                    }
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    refresh();
                }
            });
        }

        // Nút THÊM DÒNG MỚI (slot 49)
        setItem(49, createItem(Material.EMERALD,
                "<green><bold>+ Add new line</bold>",
                List.of(
                        "<gray>Add a new line at the end of lore"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập nội dung dòng mới <gray>(hỗ trợ MiniMessage):",
                    input -> {
                        data.getLore().add(input);
                        plugin.getItemManager().registerItem(data);
                        plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
                        new LoreEditorGui(plugin, player, data).open();
                    });
        });

        // Nút XÓA TẤT CẢ (slot 47)
        setItem(47, createItem(Material.BARRIER,
                "<red><bold>Clear all lore</bold>",
                List.of(
                        "<gray>Xóa tất cả dòng lore"
                )), event -> {
            data.getLore().clear();
            plugin.getItemManager().registerItem(data);
            plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
            refresh();
        });

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
        setItem(51, createBackButton(), event -> goBack());
    }
}
