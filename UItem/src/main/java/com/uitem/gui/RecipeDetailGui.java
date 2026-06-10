package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.recipe.UItemRecipe;
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

public class RecipeDetailGui extends AbstractGui {

    private static final int[] GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int SLOT_ARROW1 = 14;
    private static final int SLOT_ARROW2 = 15;
    private static final int SLOT_RESULT = 23;
    private static final int SLOT_BACK = 36;
    private static final int SLOT_CRAFT = 44;

    private final String itemId;

    public RecipeDetailGui(UItemPlugin plugin, Player player, String itemId) {
        super(plugin, player, 45, "<gold>Recipe: <white>" + itemId);
        this.itemId = itemId;
    }

    @Override
    protected void setup() {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack inner = createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 4 || col == 0 || col == 8) {
                setItem(i, border);
            } else {
                setItem(i, inner);
            }
        }

        UItemRecipe recipe = plugin.getRecipeManager().getRecipe(itemId);
        UItemData data = plugin.getItemManager().getItem(itemId);

        if (recipe == null || data == null) {
            setItem(22, createItem(Material.BARRIER, "<red>Not found"));
            setItem(SLOT_BACK, createBackButton(), ev -> new RecipeListGui(plugin, player).open());
            return;
        }

        // Title with categories
        List<String> titleLore = new ArrayList<>();
        titleLore.add("<gray>" + (recipe.isShaped() ? "Shaped 3x3" : "Shapeless"));
        titleLore.add("<gray>Output: x" + recipe.getResultAmount());
        if (!recipe.getCategories().isEmpty()) {
            titleLore.add("");
            titleLore.add("<gold>Danh mục:</gold>");
            for (String catKey : recipe.getCategories()) {
                UItemRecipe.CategoryInfo catInfo = UItemRecipe.getCategoryInfo(catKey);
                if (catInfo != null) {
                    titleLore.add("  " + catInfo.getIcon() + " <white>" + catInfo.getDisplayName());
                }
            }
        }
        if (recipe.hasSkillRequirement()) {
            titleLore.add("");
            String reqText = SkillRequirementHelper.getRequirementText(recipe);
            if (reqText != null) titleLore.add("<gold>Yêu cầu:</gold>");
            titleLore.add("  " + (reqText != null ? reqText : "<red>Cần mở khóa node skill"));
            boolean canCraft = SkillRequirementHelper.canCraft(player, recipe);
            titleLore.add(canCraft ? "<green>✔ Bạn đã mở khóa" : "<red>✘ Bạn chưa đủ cấp độ!");
        }

        setItem(4, createItem(Material.KNOWLEDGE_BOOK,
                "<gold><bold>" + itemId + "</bold>",
                titleLore));

        if (recipe.isShaped()) {
            ItemStack[] matrix = recipe.toMatrix();
            Map<Character, UItemRecipe.Ingredient> ingMap = recipe.getIngredientMap();
            for (int i = 0; i < 9; i++) {
                ItemStack ing = matrix[i];
                if (ing != null && !ing.getType().isAir()) {
                    String name = findIngredientName(ing, ingMap);
                    setItem(GRID[i], addLore(ing.clone(), "<gray>" + name));
                }
            }
        } else {
            List<UItemRecipe.Ingredient> ings = recipe.getIngredients();
            for (int i = 0; i < Math.min(ings.size(), 9); i++) {
                ItemStack ing = ings.get(i).toItemStack();
                if (ing != null && !ing.getType().isAir()) {
                    String name = ings.get(i).getUitemId() != null ? ings.get(i).getUitemId()
                            : formatMat(ing.getType());
                    setItem(GRID[i], addLore(ing.clone(), "<gray>" + name));
                }
            }
        }

        setItem(SLOT_ARROW1, createItem(Material.ARROW, "<yellow>→"));
        setItem(SLOT_ARROW2, createItem(Material.ARROW, "<yellow>→"));

        boolean showId = player.isOp() || player.hasPermission("uitem.id");
        ItemStack result = plugin.getItemManager().buildPreviewItemStack(data, showId);
        result.setAmount(recipe.getResultAmount());
        setItem(SLOT_RESULT, result);

        setItem(SLOT_BACK, createBackButton(), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new RecipeListGui(plugin, player).open();
        });

        setItem(SLOT_CRAFT, createItem(Material.CRAFTING_TABLE,
                "<green><bold>Craft</bold>",
                List.of("<gray>Open crafting table")), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new CraftingGui(plugin, player).open();
        });

        // Skill requirement visual
        if (recipe.hasSkillRequirement()) {
            boolean canCraft = SkillRequirementHelper.canCraft(player, recipe);
            List<String> skillLore = new ArrayList<>();
            skillLore.add(SkillRequirementHelper.getRequirementText(recipe));
            skillLore.add("");
            skillLore.add(canCraft ? "<green>✔ Bạn đã mở khóa" : "<red>✘ Bạn chưa đủ cấp độ!");
            setItem(25, createItem(
                    canCraft ? Material.EXPERIENCE_BOTTLE : Material.BARRIER,
                    canCraft ? "<green><bold>✔ Yêu cầu Skill: Đạt</bold>" : "<red><bold>✘ Yêu cầu Skill: Chưa đạt</bold>",
                    skillLore
            ));
        }
    }

    private String findIngredientName(ItemStack item, Map<Character, UItemRecipe.Ingredient> map) {
        for (Map.Entry<Character, UItemRecipe.Ingredient> e : map.entrySet()) {
            if (e.getValue().matches(item)) {
                UItemRecipe.Ingredient ing = e.getValue();
                if (ing.getUitemId() != null) return ing.getUitemId();
                if (ing.getMaterial() != null) return formatMat(ing.getMaterial());
            }
        }
        return formatMat(item.getType());
    }

    private ItemStack addLore(ItemStack item, String line) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            var lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            lore.add(TextUtil.parse(line));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatMat(Material mat) {
        String s = mat.name().toLowerCase().replace("_", " ");
        String[] w = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String x : w) {
            if (x.length() > 0)
                sb.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
