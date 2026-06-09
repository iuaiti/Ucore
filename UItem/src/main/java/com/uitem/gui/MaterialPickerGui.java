package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MaterialPickerGui extends AbstractGui {

    private final UItemData data;
    private final Consumer<String> callback;
    private int page;
    private String filter;
    private String category;
    private List<Material> materials;

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final Map<String, List<String>> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("swords", List.of("WOODEN_SWORD", "STONE_SWORD", "IRON_SWORD", "GOLDEN_SWORD", "DIAMOND_SWORD", "NETHERITE_SWORD"));
        CATEGORIES.put("tools", List.of("WOODEN_PICKAXE", "STONE_PICKAXE", "IRON_PICKAXE", "GOLDEN_PICKAXE", "DIAMOND_PICKAXE", "NETHERITE_PICKAXE",
                "WOODEN_AXE", "STONE_AXE", "IRON_AXE", "GOLDEN_AXE", "DIAMOND_AXE", "NETHERITE_AXE",
                "WOODEN_SHOVEL", "STONE_SHOVEL", "IRON_SHOVEL", "GOLDEN_SHOVEL", "DIAMOND_SHOVEL", "NETHERITE_SHOVEL",
                "WOODEN_HOE", "STONE_HOE", "IRON_HOE", "GOLDEN_HOE", "DIAMOND_HOE", "NETHERITE_HOE"));
        CATEGORIES.put("armor", List.of("LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS",
                "CHAINMAIL_HELMET", "CHAINMAIL_CHESTPLATE", "CHAINMAIL_LEGGINGS", "CHAINMAIL_BOOTS",
                "IRON_HELMET", "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS",
                "GOLDEN_HELMET", "GOLDEN_CHESTPLATE", "GOLDEN_LEGGINGS", "GOLDEN_BOOTS",
                "DIAMOND_HELMET", "DIAMOND_CHESTPLATE", "DIAMOND_LEGGINGS", "DIAMOND_BOOTS",
                "NETHERITE_HELMET", "NETHERITE_CHESTPLATE", "NETHERITE_LEGGINGS", "NETHERITE_BOOTS",
                "TURTLE_HELMET"));
        CATEGORIES.put("blocks", List.of()); // special: all blocks
        CATEGORIES.put("items", List.of()); // special: all items
        CATEGORIES.put("unobtainable", List.of("BARRIER", "BEDROCK", "COMMAND_BLOCK", "CHAIN_COMMAND_BLOCK", "REPEATING_COMMAND_BLOCK",
                "STRUCTURE_BLOCK", "STRUCTURE_VOID", "JIGSAW", "END_PORTAL_FRAME", "END_PORTAL",
                "PORTAL", "VOID_AIR", "CAVE_AIR", "BUBBLE_COLUMN", "KNOWLEDGE_BOOK",
                "COMMAND_BLOCK_MINECART", "SPAWNER", "END_GATEWAY", "LIGHT",
                "FARMLAND", "DIRT_PATH", "FROSTED_ICE", "PISTON_HEAD", "MOVING_PISTON",
                "FIRE", "SOUL_FIRE", "NETHER_PORTAL", "TRIPWIRE",
                "BUDDING_AMETHYST", "REINFORCED_DEEPSLATE", "PETRIFIED_OAK_SLAB",
                "PLAYER_HEAD", "PLAYER_WALL_HEAD", "DRAGON_HEAD", "DRAGON_WALL_HEAD",
                "INFESTED_STONE", "INFESTED_COBBLESTONE", "INFESTED_STONE_BRICKS",
                "INFESTED_MOSSY_STONE_BRICKS", "INFESTED_CRACKED_STONE_BRICKS",
                "INFESTED_CHISELED_STONE_BRICKS", "INFESTED_DEEPSLATE"));
    }

    public MaterialPickerGui(UItemPlugin plugin, Player player, UItemData data, Consumer<String> callback) {
        this(plugin, player, data, callback, 0, null, "all");
    }

    public MaterialPickerGui(UItemPlugin plugin, Player player, UItemData data,
                             Consumer<String> callback, int page, String filter, String category) {
        super(plugin, player, 54, "<gold><bold>Chọn Vật Liệu</bold>");
        this.data = data;
        this.callback = callback;
        this.page = page;
        this.filter = filter;
        this.category = category;
        this.materials = loadMaterials();
    }

    private List<Material> loadMaterials() {
        Set<Material> all = new HashSet<>();
        if ("all".equals(category) || "blocks".equals(category)) {
            for (Material m : Material.values()) {
                if (m.isBlock()) all.add(m);
            }
        }
        if ("all".equals(category) || "items".equals(category)) {
            for (Material m : Material.values()) {
                if (!m.isBlock()) all.add(m);
            }
        }
        if (CATEGORIES.containsKey(category)) {
            List<String> names = CATEGORIES.get(category);
            if (!names.isEmpty()) {
                for (String n : names) {
                    try { all.add(Material.valueOf(n)); } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        if ("unobtainable".equals(category)) {
            for (String n : CATEGORIES.get("unobtainable")) {
                try { all.add(Material.valueOf(n)); } catch (IllegalArgumentException ignored) {}
            }
        }

        List<Material> result = new ArrayList<>(all);
        result.removeIf(m -> !m.isItem());
        result.removeIf(m -> m.name().endsWith("_WALL_HEAD"));

        if (filter != null && !filter.isBlank()) {
            String lower = filter.toLowerCase();
            result.removeIf(m -> !m.name().toLowerCase().contains(lower));
        }

        result.sort(Comparator.comparing(Material::name));
        return result;
    }

    @Override
    protected void setup() {
        fillBorder(6);

        setItem(4, createItem(Material.COMPASS,
                "<gold><bold>Choose Material</bold>",
                List.of(
                        "<gray>Category: <white>" + category,
                        "<gray>Total: <white>" + materials.size(),
                        filter != null ? "<gray>Filter: <white>" + filter : ""
                )));

        String[] cats = {"all", "blocks", "items", "swords", "tools", "armor", "unobtainable"};
        String[] display = {"<white>All", "<green>Blocks", "<aqua>Items", "<red>Swords", "<gold>Tools", "<blue>Armor", "<dark_red>Unobtainable"};
        int[] catSlots = {1, 2, 3, 5, 6, 7, 8};

        for (int i = 0; i < cats.length; i++) {
            String cat = cats[i];
            boolean active = category.equals(cat);
            String finalCat = cat;
            setItem(0 * 9 + catSlots[i], createItem(
                    active ? Material.MAP : Material.PAPER,
                    (active ? "<bold>▶ </bold>" : "") + display[i]),
                    ev -> {
                        page = 0;
                        category = finalCat;
                        materials = loadMaterials();
                        refresh();
                    });
        }

        setItem(53, createItem(Material.OAK_SIGN,
                "<gold><bold>Search</bold>",
                List.of(filter != null ? "<gray>Đang lọc: <white>" + filter : "<gray>Click để tìm vật liệu theo tên")),
                ev -> {
                    player.closeInventory();
                    plugin.getChatInputManager().requestInput(player,
                            "<yellow>Nhập tên vật liệu để tìm (Enter để xóa lọc):",
                            input -> {
                                String f = input.trim();
                                if (f.isEmpty()) f = null;
                                MaterialPickerGui next = new MaterialPickerGui(plugin, player, data, callback, 0, f, category);
                                if (hasBackAction()) {
                                    // Preserve parent by reopening current picker context after search? Simpler: open next as child of this conceptually.
                                    // Since we closed for chat, just open the filtered one; parent back still works from the new instance if user sets it.
                                    next.open();
                                } else {
                                    next.open();
                                }
                            });
                });

        int totalPages = Math.max(1, (int) Math.ceil((double) materials.size() / ITEMS_PER_PAGE));
        int startIndex = page * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int idx = startIndex + i;
            if (idx >= materials.size()) break;
            Material mat = materials.get(idx);

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(TextUtil.parse("<white>" + mat.name()));
                meta.lore(List.of(
                        TextUtil.parse("<gray>Click để chọn vật liệu này"),
                        TextUtil.parse("<yellow>" + mat.name())
                ));
                item.setItemMeta(meta);
            }

            setItem(ITEM_SLOTS[i], item, ev -> {
                String materialName = mat.name();
                data.setMaterial(mat);
                plugin.getItemManager().registerItem(data);
                plugin.getStorageManager().saveAllDebounced(plugin.getItemManager());
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                player.sendMessage(TextUtil.success("Set material = " + materialName));
                if (callback != null) callback.accept(materialName);
            });
        }

        setItem(46, page > 0 ? createPreviousButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page > 0 ? ev -> { page--; refresh(); } : null);
        setItem(52, page < totalPages - 1 ? createNextButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page < totalPages - 1 ? ev -> { page++; refresh(); } : null);

        setItem(45, createBackButton(), ev -> goBack());
    }
}
