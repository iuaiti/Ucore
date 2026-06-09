package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemManager;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import com.uitem.search.SearchManager;
import java.util.Comparator;
import java.util.List;

/**
 * GUI hiển thị danh sách tất cả custom items.
 * Hỗ trợ phân trang, tạo mới, chỉnh sửa, xóa.
 */
public class ItemListGui extends AbstractGui {

    private int page;
    private static final int DEFAULT_ITEMS_PER_PAGE = 28; // fallback if config missing
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public ItemListGui(UItemPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public ItemListGui(UItemPlugin plugin, Player player, int page) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d><bold>Trình Chỉnh Sửa UItem</bold></gradient>");
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
        // Fill border
        fillBorder(6);

        // Title item
        setItem(4, createItem(Material.NETHER_STAR,
                "<gradient:#ff6b6b:#ffd93d><bold>UItem Editor</bold></gradient>",
                List.of(
                        "<gray>Manage all custom items",
                        "",
                        "<yellow>Total items: <white>" + plugin.getItemManager().getAllItems().size(),
                        "<yellow>Page: <white>" + (page + 1)
                )));

        // Search button (if enabled)
        if (plugin.getConfig().getBoolean("gui.search.enabled", true)
                && player.hasPermission("uitem.search")) {
            setItem(48, createItem(Material.MAGENTA_GLAZED_TERRACOTTA,
                    "<aqua><bold>Tìm Item</bold>",
                    List.of(
                            "<gray>Nhập từ khóa để lọc danh sách",
                            "<gray>Gõ 'clear' để xóa bộ lọc"
                    )), event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Nhập từ khóa tìm (<gray>hoặc 'clear' để xóa):",
                        input -> {
                            SearchManager sm = plugin.getSearchManager();
                            if (input.equalsIgnoreCase("clear")) {
                                sm.clearQuery(player);
                            } else {
                                sm.setQuery(player, input);
                            }
                            new ItemListGui(plugin, player, page).open();
                        });
            });
        }

        // Lấy danh sách items
        UItemManager manager = plugin.getItemManager();
        List<UItemData> allItems = new ArrayList<>(manager.getAllItems().values());
        allItems.sort(Comparator.comparing(UItemData::getId, String.CASE_INSENSITIVE_ORDER));

        // Determine items per page from config
        int itemsPerPage = plugin.getConfig().getInt("gui.items-per-page", DEFAULT_ITEMS_PER_PAGE);

        // Apply search filter if present
        SearchManager searchMgr = plugin.getSearchManager();
        String query = searchMgr.getQuery(player);
        if (query != null && !query.isBlank()) {
            String lower = query.toLowerCase();
            allItems.removeIf(data -> {
                if (data.getId().toLowerCase().contains(lower)) return false;
                String disp = data.getDisplayName();
                return disp == null || !disp.toLowerCase().contains(lower);
            });
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) allItems.size() / itemsPerPage));
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        if (page < 0) {
            page = 0;
        }

        // Hiển thị items cho trang hiện tại
        int startIndex = page * itemsPerPage;
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex < allItems.size()) {
                UItemData data = allItems.get(dataIndex);
                ItemStack preview = manager.buildPreviewItemStack(data);

                setItem(ITEM_SLOTS[i], preview, event -> {
                    if (event.getClick() == ClickType.LEFT) {
                        // Mở editor
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                        new ItemEditorGui(plugin, player, data).openAsChildOf(this);
                    } else if (event.getClick() == ClickType.RIGHT) {
                        // Xóa item - confirm
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.5f);
                        new ConfirmDeleteGui(plugin, player, data, this.page).openAsChildOf(this);
                    }
                });
            }
        }

        // Create new item from Template button
        setItem(49, createItem(Material.EMERALD,
                "<green><bold>+ Tạo Item Mới (Template)</bold>",
                List.of(
                        "<gray>Tạo item từ template có sẵn",
                        "<gray>Sword, Armor, Tool, Wand...",
                        "",
                        "<yellow>Click để tạo"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập ID cho item mới <gray>(chỉ chữ cái, số, gạch dưới):",
                    input -> {
                        String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                        if (id.isEmpty()) {
                            player.sendMessage(TextUtil.error("ID không hợp lệ!"));
                            new ItemListGui(plugin, player, page).open();
                            return;
                        }
                        if (manager.hasItem(id)) {
                            player.sendMessage(TextUtil.error("Item '" + id + "' already exists!"));
                            new ItemListGui(plugin, player, page).open();
                            return;
                        }
                        // Open template selector
                        new ItemTemplateGui(plugin, player, id).open();
                    });
        });

        // Create blank item button
        setItem(50, createItem(Material.PAPER,
                "<aqua><bold>+ Tạo Item Trống</bold>",
                List.of(
                        "<gray>Tạo item trống, tự cấu hình",
                        "",
                        "<yellow>Click để tạo"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập ID cho item mới <gray>(chỉ chữ cái, số, gạch dưới):",
                    input -> {
                        String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                        if (id.isEmpty()) {
                            player.sendMessage(TextUtil.error("ID không hợp lệ!"));
                            new ItemListGui(plugin, player, page).open();
                            return;
                        }
                        if (manager.hasItem(id)) {
                            player.sendMessage(TextUtil.error("Item '" + id + "' already exists!"));
                            new ItemListGui(plugin, player, page).open();
                            return;
                        }
                        UItemData newData = new UItemData(id);
                        boolean created = com.uitem.api.UItemAPI.get().registerItem(newData, player);
                        if (!created) {
                            player.sendMessage(com.uitem.util.TextUtil.error(
                                    "Cannot create item '" + id + "' (blocked by another plugin)."));
                            new ItemListGui(plugin, player, page).open();
                            return;
                        }
                        plugin.getResourcePackManager().syncCmdsToLiveItems();
                        player.sendMessage(TextUtil.success("Created item '" + id + "'!"));
                        new ItemEditorGui(plugin, player, newData).open();
                    });
        });

        // Bulk Give button
        if (player.hasPermission("uitem.give")) {
            setItem(51, createItem(Material.CHEST,
                    "<gold><bold>📦 Give Nhiều Item</bold>",
                    List.of(
                            "<gray>Give nhiều item cùng lúc",
                            "<gray>Cho bản thân hoặc người chơi khác",
                            "",
                            "<yellow>Click để mở"
                    )), event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new BulkGiveGui(plugin, player).openAsChildOf(this);
            });
        }

        // Nút trang trước
        if (page > 0) {
            setItem(46, createPreviousButton(), event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new ItemListGui(plugin, player, page - 1).open();
            });
        }

        // Nút trang sau
        if (page < totalPages - 1) {
            setItem(52, createNextButton(), event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new ItemListGui(plugin, player, page + 1).open();
            });
        }
    }
}
