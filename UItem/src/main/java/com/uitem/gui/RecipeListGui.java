package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.Messages;
import com.uitem.util.SkillRequirementHelper;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RecipeListGui extends AbstractGui {

    private int page;
    private String filterCategory; // null = show all
    private static final int PER_PAGE = 28;
    private static final int[] SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    public RecipeListGui(UItemPlugin plugin, Player player) {
        this(plugin, player, 0, null);
    }

    public RecipeListGui(UItemPlugin plugin, Player player, int page) {
        this(plugin, player, page, null);
    }

    public RecipeListGui(UItemPlugin plugin, Player player, int page, String filterCategory) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d>Danh Sách Công Thức</gradient>");
        this.page = page;
        this.filterCategory = filterCategory;
    }

    @Override
    public void open() {
        if (!player.hasPermission("uitem.craft")) {
            player.sendMessage(TextUtil.error(com.uitem.util.Messages.noPermission()));
            return;
        }
        super.open();
    }

    @Override
    protected void setup() {
        fillBorder(6);

        boolean hasFilter = filterCategory != null;
        String title = hasFilter
            ? "<gradient:#ff6b6b:#ffd93d><bold>Danh Sách Công Thức</bold></gradient> <dark_gray>| <aqua>" + UItemRecipe.getCategoryInfo(filterCategory).getDisplayName()
            : "<gradient:#ff6b6b:#ffd93d><bold>Danh Sách Công Thức</bold></gradient>";

        setItem(4, createItem(Material.KNOWLEDGE_BOOK, title,
                List.of("<gray>Click a recipe to view details")));

        List<Map.Entry<String, UItemRecipe>> allRecipes = new ArrayList<>(
                plugin.getRecipeManager().getAllRecipes().entrySet());

        if (hasFilter) {
            allRecipes.removeIf(e -> !e.getValue().getCategories().contains(filterCategory));
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) allRecipes.size() / PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int start = page * PER_PAGE;
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = start + i;
            if (idx >= allRecipes.size()) break;

            Map.Entry<String, UItemRecipe> entry = allRecipes.get(idx);
            String id = entry.getKey();
            UItemRecipe recipe = entry.getValue();
            UItemData data = plugin.getItemManager().getItem(id);

            boolean hasSkillReq = SkillRequirementHelper.isUSkillLoaded() && recipe.hasSkillRequirement();
            boolean canCraft = !hasSkillReq || SkillRequirementHelper.canCraft(player, recipe);
            String type = recipe.isShaped() ? "Shaped" : "Shapeless";
            ItemStack item;
            if (data != null) {
                boolean showId = player.isOp() || player.hasPermission("uitem.id");
                item = plugin.getItemManager().buildPreviewItemStack(data, showId);
                var meta = item.getItemMeta();
                if (meta != null) {
                    var lore = meta.lore();
                    if (lore == null) lore = new ArrayList<>();
                    lore.add(TextUtil.parse(""));
                    lore.add(TextUtil.parse("<gold>" + type + " <dark_gray>| <gold>x" + recipe.getResultAmount()));
                    if (!recipe.getCategories().isEmpty()) {
                        lore.add(TextUtil.parse("<gray>Danh mục: " + formatCategories(recipe.getCategories())));
                    }
                    if (hasSkillReq) {
                        if (canCraft) {
                            lore.add(TextUtil.parse("<green>✔ Đã mở khóa skill"));
                        } else {
                            lore.add(TextUtil.parse("<red>🔒 Chưa đủ cấp độ!"));
                        }
                    }
                    lore.add(TextUtil.parse("<yellow>Click để xem công thức"));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
            } else {
                item = createItem(Material.BARRIER, "<red>" + id,
                        List.of("<gray>Item deleted"));
            }

            String fid = id;
            setItem(SLOTS[i], item, ev -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new RpgRecipeGui(plugin, player, fid).openAsChildOf(this);
            });
        }

        // Category filter buttons (bottom row)
        int catSlot = 45;
        for (Map.Entry<String, UItemRecipe.CategoryInfo> catEntry : UItemRecipe.getAllCategories().entrySet()) {
            String catKey = catEntry.getKey();
            UItemRecipe.CategoryInfo catInfo = catEntry.getValue();
            Material catMat;
            try {
                catMat = Material.valueOf(catInfo.getIconMaterial());
            } catch (IllegalArgumentException e) {
                catMat = Material.PAPER;
            }

            boolean active = catKey.equals(filterCategory);
            boolean hasRecipes = allRecipes.stream().anyMatch(e -> e.getValue().getCategories().contains(catKey));

            if (catSlot >= 54) break;
            int finalSlot = catSlot;
            String fCatKey = catKey;

            List<String> catLore = new ArrayList<>();
            catLore.add("<gray>" + catInfo.getDisplayName());
            if (active) {
                catLore.add("<green>▸ Đang lọc");
            } else if (!hasRecipes) {
                catLore.add("<dark_gray>Chưa có công thức");
            }
            catLore.add(active ? "<red>Click để bỏ lọc" : "<yellow>Click để lọc");

            if (active || hasRecipes) {
                setItem(finalSlot, createItem(
                    active ? Material.LIME_DYE : catMat,
                    (active ? "<green><bold>" : "<white>") + catInfo.getIcon() + " " + catInfo.getDisplayName(),
                    catLore
                ), ev -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    if (fCatKey.equals(filterCategory)) {
                        new RecipeListGui(plugin, player, 0, null).open();
                    } else {
                        new RecipeListGui(plugin, player, 0, fCatKey).open();
                    }
                });
            } else {
                setItem(finalSlot, createItem(Material.GRAY_STAINED_GLASS_PANE,
                        "<dark_gray>" + catInfo.getIcon() + " " + catInfo.getDisplayName(),
                        List.of("<dark_gray>Chưa có công thức")));
            }

            catSlot++;
        }

        setItem(49, createItem(Material.CRAFTING_TABLE,
                "<green><bold>Craft</bold>",
                List.of("<gray>Open crafting table")), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new CraftingGui(plugin, player).open();
        });

        setItem(46, page > 0 ? createPreviousButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page > 0 ? ev -> { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new RecipeListGui(plugin, player, page - 1, filterCategory).open(); } : null);
        setItem(52, page < totalPages - 1 ? createNextButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page < totalPages - 1 ? ev -> { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new RecipeListGui(plugin, player, page + 1, filterCategory).open(); } : null);
    }

    private String formatCategories(Set<String> cats) {
        return cats.stream()
                .map(k -> {
                    UItemRecipe.CategoryInfo info = UItemRecipe.getCategoryInfo(k);
                    return info != null ? info.getIcon() + " " + info.getDisplayName() : k;
                })
                .collect(Collectors.joining("<dark_gray>, </dark_gray>"));
    }
}
