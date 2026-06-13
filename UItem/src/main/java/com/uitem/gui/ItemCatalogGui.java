package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Giao diện danh mục vật phẩm UItem — mở bằng lệnh /crafting.
 *
 * <p>Layout (54 slot):
 * <pre>
 * Row 0: [border x9]
 * Row 1: [border][item x7][border]
 * Row 2: [border][item x7][border]
 * Row 3: [border][item x7][border]
 * Row 4: [border][item x7][border]
 * Row 5: [prev][cat1][cat2][cat3][craft][cat4][cat5][cat6][next]
 * </pre>
 * Slot info tổng hợp đặt ở slot 4 (row 0 giữa).
 */
public class ItemCatalogGui extends AbstractGui {

    // 28 item slots (4 rows × 7 cols, bỏ border col 0 và col 8)
    private static final int[] ITEM_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    private static final int PER_PAGE = ITEM_SLOTS.length; // 28

    private static final int SLOT_PREV  = 45;
    private static final int SLOT_CRAFT = 49;
    private static final int SLOT_NEXT  = 53;

    // Bottom-row slots dành cho category buttons (bỏ 45=prev, 49=craft, 53=next)
    private static final int[] CAT_SLOTS = {46, 47, 48, 50, 51, 52};

    // Cache catalog.yml để tránh đọc file mỗi lần mở GUI
    private static YamlConfiguration catalogConfigCache;
    private static long catalogCacheTime = 0;
    private static final long CACHE_TTL_MS = 30_000; // 30 giây

    private int page;
    private final String filterCategory;

    public ItemCatalogGui(UItemPlugin plugin, Player player) {
        this(plugin, player, 0, null);
    }

    public ItemCatalogGui(UItemPlugin plugin, Player player, int page, String filterCategory) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d>Danh Mục Vật Phẩm</gradient>");
        this.page = page;
        this.filterCategory = filterCategory;
    }

    @Override
    public void open() {
        if (!player.hasPermission("uitem.craft")) {
            player.sendMessage(TextUtil.error(Messages.noPermission()));
            return;
        }
        super.open();
    }

    @Override
    protected void setup() {
        fillBorder(6);

        YamlConfiguration cfg = loadCatalogConfig();
        List<String> catOrder = cfg.getStringList("category-order");
        if (catOrder.isEmpty()) {
            catOrder = new ArrayList<>(UItemRecipe.getAllCategories().keySet());
        }
        ConfigurationSection customCatsSec = cfg.getConfigurationSection("custom-categories");

        // Lấy và sắp xếp tất cả items
        List<UItemData> allItems = new ArrayList<>(plugin.getItemManager().getAllItems().values());
        allItems.sort(Comparator.comparing(UItemData::getId));

        // Lọc theo category
        List<UItemData> filtered;
        if (filterCategory != null) {
            String fc = filterCategory;
            filtered = allItems.stream()
                .filter(d -> d.getCategories() != null && d.getCategories().contains(fc))
                .collect(Collectors.toList());
        } else {
            filtered = new ArrayList<>(allItems);
        }

        // Áp dụng sắp xếp tùy chỉnh nếu có
        if (filterCategory != null) {
            filtered = applyArrangement(filtered, filterCategory, cfg);
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        // Info item ở slot 4
        String catLabel = filterCategory != null ? resolveName(filterCategory, customCatsSec) : "Tất cả";
        setItem(4, createItem(Material.NETHER_STAR,
            "<gradient:#ff6b6b:#ffd93d><bold>Danh Mục Vật Phẩm</bold></gradient>",
            List.of(
                "<gray>Danh mục: <white>" + catLabel,
                "<gray>Trang: <white>" + (page + 1) + "/" + totalPages,
                "<gray>Vật phẩm: <white>" + filtered.size()
            )));

        // Hiển thị items
        int start = page * PER_PAGE;
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int idx = start + i;
            if (idx >= filtered.size()) break;

            UItemData data = filtered.get(idx);
            boolean hasRecipe = plugin.getRecipeManager().getRecipe(data.getId()) != null;
            boolean showId = player.isOp() || player.hasPermission("uitem.id");

            ItemStack item = plugin.getItemManager().buildPreviewItemStack(data, showId);
            var meta = item.getItemMeta();
            if (meta != null) {
                var lore = meta.lore();
                if (lore == null) lore = new ArrayList<>();
                lore.add(TextUtil.parse(""));
                Set<String> cats = data.getCategories();
                if (cats != null && !cats.isEmpty()) {
                    String catStr = cats.stream()
                        .map(k -> resolveName(k, customCatsSec))
                        .collect(Collectors.joining(", "));
                    lore.add(TextUtil.parse("<gray>Danh mục: <white>" + catStr));
                }
                lore.add(TextUtil.parse(hasRecipe
                    ? "<yellow>Click để xem công thức"
                    : "<dark_gray>Chưa có công thức"));
                meta.lore(lore);
                item.setItemMeta(meta);
            }

            String fId = data.getId();
            boolean fHasRecipe = hasRecipe;
            setItem(ITEM_SLOTS[i], item, ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                if (fHasRecipe) {
                    new RpgRecipeGui(plugin, player, fId).openAsChildOf(this);
                } else {
                    player.sendMessage(TextUtil.info(
                        "Vật phẩm <white>" + fId + " <gray>chưa có công thức chế tạo."));
                }
            });
        }

        // Nút trang trước
        setItem(SLOT_PREV,
            page > 0 ? createPreviousButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
            page > 0 ? ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new ItemCatalogGui(plugin, player, page - 1, filterCategory).open();
            } : null);

        // Nút trang sau
        setItem(SLOT_NEXT,
            page < totalPages - 1 ? createNextButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
            page < totalPages - 1 ? ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new ItemCatalogGui(plugin, player, page + 1, filterCategory).open();
            } : null);

        // Nút bàn chế tạo
        setItem(SLOT_CRAFT, createItem(Material.CRAFTING_TABLE,
            "<green><bold>Bàn Chế Tạo</bold>",
            List.of("<gray>Mở bàn chế tạo UItem", "<yellow>Click để craft")), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new CraftingGui(plugin, player).open();
        });

        // Các nút category
        setupCategoryButtons(allItems, catOrder, customCatsSec);
    }

    private void setupCategoryButtons(List<UItemData> allItems, List<String> catOrder,
                                       ConfigurationSection customCatsSec) {
        int slotIdx = 0;

        // Nút "Tất cả"
        if (slotIdx < CAT_SLOTS.length) {
            boolean allActive = filterCategory == null;
            setItem(CAT_SLOTS[slotIdx++], createItem(
                allActive ? Material.LIME_DYE : Material.NETHER_STAR,
                allActive ? "<green><bold>⭐ Tất Cả</bold>" : "<white>⭐ Tất Cả",
                List.of(
                    "<gray>" + allItems.size() + " vật phẩm",
                    allActive ? "<green>▸ Đang xem" : "<yellow>Click để xem tất cả"
                )
            ), allActive ? null : ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new ItemCatalogGui(plugin, player, 0, null).open();
            });
        }

        // Nút từng category
        for (String catKey : catOrder) {
            if (slotIdx >= CAT_SLOTS.length) break;

            // Lấy Material icon
            Material mat = resolveMaterial(catKey, customCatsSec);
            String displayName = resolveName(catKey, customCatsSec);

            long count = allItems.stream()
                .filter(d -> d.getCategories() != null && d.getCategories().contains(catKey))
                .count();

            boolean active = catKey.equals(filterCategory);
            String fCatKey = catKey;

            List<String> lore = new ArrayList<>();
            lore.add((count > 0 ? "<gray>" : "<dark_gray>") + count + " vật phẩm");
            if (active) lore.add("<green>▸ Đang lọc");
            if (count > 0) lore.add(active ? "<red>Click để bỏ lọc" : "<yellow>Click để lọc");

            setItem(CAT_SLOTS[slotIdx++], createItem(
                active ? Material.LIME_DYE : (count > 0 ? mat : Material.GRAY_STAINED_GLASS_PANE),
                (active ? "<green><bold>" : (count > 0 ? "<white>" : "<dark_gray>")) + displayName,
                lore
            ), count > 0 || active ? ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                String newFilter = fCatKey.equals(filterCategory) ? null : fCatKey;
                new ItemCatalogGui(plugin, player, 0, newFilter).open();
            } : null);
        }
    }

    /**
     * Áp dụng sắp xếp tùy chỉnh từ catalog.yml (category-arrangements.<category>).
     * Items được chỉ định slot cụ thể; các item còn lại điền vào slot trống theo alphabet.
     */
    private List<UItemData> applyArrangement(List<UItemData> items, String category,
                                               YamlConfiguration cfg) {
        ConfigurationSection sec = cfg.getConfigurationSection("category-arrangements." + category);
        if (sec == null) return items;

        Map<Integer, UItemData> placed = new TreeMap<>();
        Set<String> placedIds = new HashSet<>();

        for (String slotStr : sec.getKeys(false)) {
            try {
                int slot = Integer.parseInt(slotStr);
                String itemId = sec.getString(slotStr);
                if (itemId == null) continue;
                UItemData data = plugin.getItemManager().getItem(itemId.toLowerCase(Locale.ROOT));
                if (data != null && slot >= 0 && slot < PER_PAGE) {
                    placed.put(slot, data);
                    placedIds.add(itemId.toLowerCase(Locale.ROOT));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (placed.isEmpty()) return items;

        // Các items chưa được đặt vào slot cụ thể
        Iterator<UItemData> remainingIter = items.stream()
            .filter(d -> !placedIds.contains(d.getId().toLowerCase(Locale.ROOT)))
            .collect(Collectors.toList())
            .iterator();

        // Xây dựng danh sách kết quả
        int totalSlots = items.size();
        List<UItemData> result = new ArrayList<>();
        for (int i = 0; i < totalSlots; i++) {
            if (placed.containsKey(i)) {
                result.add(placed.get(i));
            } else if (remainingIter.hasNext()) {
                result.add(remainingIter.next());
            }
        }
        while (remainingIter.hasNext()) {
            result.add(remainingIter.next());
        }
        return result;
    }

    /** Lấy tên hiển thị của category (custom > mặc định > key). */
    private String resolveName(String key, ConfigurationSection customCatsSec) {
        if (customCatsSec != null && customCatsSec.isConfigurationSection(key)) {
            ConfigurationSection cs = customCatsSec.getConfigurationSection(key);
            if (cs != null) {
                String emoji = cs.getString("emoji", "");
                String name = cs.getString("display-name", key);
                return emoji.isEmpty() ? name : emoji + " " + name;
            }
        }
        UItemRecipe.CategoryInfo info = UItemRecipe.getCategoryInfo(key);
        if (info != null) return info.getIcon() + " " + info.getDisplayName();
        return key;
    }

    /** Lấy Material icon của category. */
    private Material resolveMaterial(String key, ConfigurationSection customCatsSec) {
        if (customCatsSec != null && customCatsSec.isConfigurationSection(key)) {
            ConfigurationSection cs = customCatsSec.getConfigurationSection(key);
            if (cs != null) {
                String matStr = cs.getString("icon", "PAPER");
                try {
                    return Material.valueOf(matStr.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        UItemRecipe.CategoryInfo info = UItemRecipe.getCategoryInfo(key);
        if (info != null) {
            try {
                return Material.valueOf(info.getIconMaterial().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Material.PAPER;
    }

    /** Load catalog.yml từ disk (có cache 30 giây). */
    private YamlConfiguration loadCatalogConfig() {
        long now = System.currentTimeMillis();
        if (catalogConfigCache != null && (now - catalogCacheTime) < CACHE_TTL_MS) {
            return catalogConfigCache;
        }
        File f = new File(plugin.getDataFolder(), "catalog.yml");
        if (!f.exists()) {
            catalogConfigCache = new YamlConfiguration();
            catalogCacheTime = now;
            return catalogConfigCache;
        }
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.load(f);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("[UItem] Failed to load catalog.yml: " + e.getMessage());
        }
        catalogConfigCache = cfg;
        catalogCacheTime = now;
        return catalogConfigCache;
    }

    /** Xóa cache để reload.yml được đọc ngay sau /uitem reload. */
    public static void invalidateCache() {
        catalogConfigCache = null;
        catalogCacheTime = 0;
    }
}
