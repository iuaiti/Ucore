package com.uitem.recipe;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class UItemRecipe {

    // ═════════════════════════════════════════════════════════════════════════
    //  Recipe Categories (phân loại công thức theo chức năng)
    // ═════════════════════════════════════════════════════════════════════════

    private static final LinkedHashMap<String, CategoryInfo> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("VU_KHI",       new CategoryInfo("Vũ Khí",       "🗡", "WEAPON"));
        CATEGORIES.put("GIAP",         new CategoryInfo("Giáp",         "🛡", "CHAINMAIL_CHESTPLATE"));
        CATEGORIES.put("CONG_CU",      new CategoryInfo("Công Cụ",      "⛏", "DIAMOND_PICKAXE"));
        CATEGORIES.put("NGUYEN_LIEU",  new CategoryInfo("Nguyên Liệu",  "🧱", "IRON_INGOT"));
        CATEGORIES.put("VAT_PHAM_TIEU_HAO", new CategoryInfo("Tiêu Hao","🔥", "TIPPED_ARROW"));
        CATEGORIES.put("VAT_PHAM_DAC_BIET", new CategoryInfo("Đặc Biệt", "⭐", "NETHER_STAR"));
        CATEGORIES.put("KHOI_XAY_DUNG",     new CategoryInfo("Khối Xây Dựng", "🧱", "STONE"));
        CATEGORIES.put("MAY_MOC_TIEN_ICH",  new CategoryInfo("Máy Móc / Tiện Ích", "⚙", "PISTON"));
    }

    public static Map<String, CategoryInfo> getAllCategories() {
        return Collections.unmodifiableMap(CATEGORIES);
    }

    public static CategoryInfo getCategoryInfo(String key) {
        return CATEGORIES.get(key);
    }

    public static class CategoryInfo {
        private final String displayName;
        private final String icon;
        private final String iconMaterial;

        public CategoryInfo(String displayName, String icon, String iconMaterial) {
            this.displayName = displayName;
            this.icon = icon;
            this.iconMaterial = iconMaterial;
        }

        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
        public String getIconMaterial() { return iconMaterial; }
    }

    // ═════════════════════════════════════════════════════════════════════════

    // Cached NamespacedKey for PDC tag matching — avoids creating a new one each match()
    private static NamespacedKey CACHED_UITEM_KEY;

    private static NamespacedKey getUItemKey() {
        if (CACHED_UITEM_KEY == null) {
            var plugin = UItemPlugin.getInstance();
            if (plugin != null) {
                CACHED_UITEM_KEY = plugin.getItemManager().getUItemKey();
            } else {
                // Fallback (should not happen during normal operation)
                CACHED_UITEM_KEY = new NamespacedKey("uitem", "uitem_id");
            }
        }
        return CACHED_UITEM_KEY;
    }

    private final String resultItemId;
    private int resultAmount;
    private boolean shaped;
    private List<String> shape;
    private Map<Character, Ingredient> ingredientMap;
    private List<Ingredient> ingredients;
    private Set<String> categories = new HashSet<>();
    private String requiredSkillNode; // null = no requirement
    private int requiredSkillLevel = 1;

    public UItemRecipe(String resultItemId, int resultAmount) {
        this.resultItemId = resultItemId;
        this.resultAmount = Math.max(1, Math.min(64, resultAmount));
        this.shaped = false;
        this.shape = new ArrayList<>();
        this.ingredientMap = new HashMap<>();
        this.ingredients = new ArrayList<>();
    }

    public String getResultItemId() { return resultItemId; }
    public int getResultAmount() { return resultAmount; }
    public void setResultAmount(int amount) { this.resultAmount = Math.max(1, Math.min(64, amount)); }

    public boolean isShaped() { return shaped; }
    public void setShaped(boolean shaped) { this.shaped = shaped; }

    public List<String> getShape() { return shape; }
    public void setShape(List<String> shape) { this.shape = shape; }

    public Map<Character, Ingredient> getIngredientMap() { return ingredientMap; }
    public void setIngredientMap(Map<Character, Ingredient> ingredientMap) { this.ingredientMap = ingredientMap; }

    public List<Ingredient> getIngredients() { return ingredients; }
    public void setIngredients(List<Ingredient> ingredients) { this.ingredients = ingredients; }

    public Set<String> getCategories() { return categories; }
    public void setCategories(Set<String> categories) { this.categories = categories != null ? new HashSet<>(categories) : new HashSet<>(); }
    public void addCategory(String category) { if (CATEGORIES.containsKey(category)) categories.add(category); }
    public void removeCategory(String category) { categories.remove(category); }
    public boolean hasCategory(String category) { return categories.contains(category); }
    public void toggleCategory(String category) {
        if (CATEGORIES.containsKey(category)) {
            if (categories.contains(category)) categories.remove(category);
            else categories.add(category);
        }
    }

    public String getRequiredSkillNode() { return requiredSkillNode; }
    public void setRequiredSkillNode(String node) { this.requiredSkillNode = (node != null && !node.isBlank()) ? node : null; }
    public int getRequiredSkillLevel() { return requiredSkillLevel; }
    public void setRequiredSkillLevel(int level) { this.requiredSkillLevel = Math.max(1, level); }
    public boolean hasSkillRequirement() { return requiredSkillNode != null; }

    public static class Ingredient {
        private Material material;
        private String uitemId;
        private int amount;

        public Ingredient(Material material) {
            this(material, 1);
        }

        public Ingredient(Material material, int amount) {
            this.material = material;
            this.amount = Math.max(1, amount);
        }

        public Ingredient(String uitemId, int amount) {
            this.uitemId = uitemId;
            this.amount = Math.max(1, amount);
        }

        public Material getMaterial() { return material; }
        public String getUitemId() { return uitemId; }
        public int getAmount() { return amount; }

        public boolean matches(ItemStack item) {
            if (item == null || item.getType().isAir()) return false;
            if (uitemId != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return false;
                String id = meta.getPersistentDataContainer().get(getUItemKey(), PersistentDataType.STRING);
                return uitemId.equals(id) && item.getAmount() >= amount;
            }
            return item.getType() == material && item.getAmount() >= amount;
        }

        public boolean isAir() {
            return material == null || material.isAir();
        }

        public ItemStack toItemStack() {
            if (uitemId != null) {
                UItemPlugin plugin = UItemPlugin.getInstance();
                if (plugin != null) {
                    UItemData data = plugin.getItemManager().getItem(uitemId);
                    if (data != null) {
                        ItemStack item = plugin.getItemManager().buildItemStack(data);
                        item.setAmount(amount);
                        return item;
                    }
                }
            }
            if (material != null && !material.isAir()) {
                return new ItemStack(material, amount);
            }
            return new ItemStack(Material.AIR);
        }

        public void save(ConfigurationSection section) {
            if (uitemId != null) {
                section.set("uitem-id", uitemId);
            } else if (material != null) {
                section.set("material", material.name());
            }
            section.set("amount", amount);
        }

        public static Ingredient load(ConfigurationSection section) {
            if (section == null) return null;
            int amount = section.getInt("amount", 1);
            String uitemId = section.getString("uitem-id");
            if (uitemId != null) {
                return new Ingredient(uitemId, amount);
            }
            String materialStr = section.getString("material");
            if (materialStr != null) {
                try {
                    Material mat = Material.valueOf(materialStr.toUpperCase());
                    return new Ingredient(mat, amount);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Ingredient that)) return false;
            return amount == that.amount &&
                    material == that.material &&
                    Objects.equals(uitemId, that.uitemId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(material, uitemId, amount);
        }
    }

    public boolean matches(ItemStack[] matrix) {
        if (matrix == null || matrix.length != 9) return false;
        if (shaped) return matchesShaped(matrix);
        return matchesShapeless(matrix);
    }

    private boolean matchesShaped(ItemStack[] matrix) {
        if (shape.size() != 3) return false;
        String row0 = shape.get(0);
        String row1 = shape.get(1);
        String row2 = shape.get(2);
        // Null-check to prevent NPE from corrupt YAML data
        if (row0 == null || row1 == null || row2 == null) return false;
        if (row0.length() != 3 || row1.length() != 3 || row2.length() != 3) return false;

        List<String> rows = List.of(row0, row1, row2);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int idx = r * 3 + c;
                char ch = rows.get(r).charAt(c);
                if (ch == ' ') {
                    if (matrix[idx] != null && !matrix[idx].getType().isAir()) return false;
                } else {
                    Ingredient ing = ingredientMap.get(ch);
                    if (ing == null) return false;
                    if (!ing.matches(matrix[idx])) return false;
                }
            }
        }
        return true;
    }

    private boolean matchesShapeless(ItemStack[] matrix) {
        List<ItemStack> nonAir = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (matrix[i] != null && !matrix[i].getType().isAir()) {
                nonAir.add(matrix[i]);
            }
        }
        if (nonAir.size() != ingredients.size()) return false;

        List<Ingredient> remaining = new ArrayList<>(ingredients);
        for (ItemStack item : nonAir) {
            boolean matched = false;
            for (Iterator<Ingredient> it = remaining.iterator(); it.hasNext(); ) {
                Ingredient ing = it.next();
                if (ing.matches(item)) {
                    it.remove();
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return remaining.isEmpty();
    }

    public ItemStack[] toMatrix() {
        ItemStack[] matrix = new ItemStack[9];
        if (!shaped || shape.size() != 3) return matrix;
        String row0 = shape.get(0);
        String row1 = shape.get(1);
        String row2 = shape.get(2);
        List<String> rows = List.of(row0, row1, row2);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int idx = r * 3 + c;
                char ch = rows.get(r).charAt(c);
                if (ch != ' ') {
                    Ingredient ing = ingredientMap.get(ch);
                    if (ing != null) {
                        matrix[idx] = ing.toItemStack();
                    }
                }
            }
        }
        return matrix;
    }

    public void save(ConfigurationSection section) {
        section.set("result-item", resultItemId);
        section.set("result-amount", resultAmount);
        section.set("shaped", shaped);
        if (!categories.isEmpty()) {
            section.set("categories", new ArrayList<>(categories));
        }
        if (requiredSkillNode != null) {
            section.set("required-skill-node", requiredSkillNode);
            section.set("required-skill-level", requiredSkillLevel);
        }
        if (shaped) {
            section.set("shape", shape);
            ConfigurationSection ingMapSection = section.createSection("ingredient-map");
            for (Map.Entry<Character, Ingredient> entry : ingredientMap.entrySet()) {
                ConfigurationSection ingSection = ingMapSection.createSection(entry.getKey().toString());
                entry.getValue().save(ingSection);
            }
        } else {
            ConfigurationSection ingsSection = section.createSection("ingredients");
            for (int i = 0; i < ingredients.size(); i++) {
                ConfigurationSection ingSection = ingsSection.createSection("ingredient_" + i);
                ingredients.get(i).save(ingSection);
            }
        }
    }

    public static UItemRecipe load(ConfigurationSection section) {
        if (section == null) return null;
        try {
            String resultId = section.getString("result-item");
            if (resultId == null) return null;
            int amount = section.getInt("result-amount", 1);
            UItemRecipe recipe = new UItemRecipe(resultId, amount);
            recipe.setShaped(section.getBoolean("shaped", false));

            List<String> cats = section.getStringList("categories");
            if (cats != null && !cats.isEmpty()) {
                recipe.setCategories(new HashSet<>(cats));
            }

            String skillNode = section.getString("required-skill-node");
            if (skillNode != null && !skillNode.isBlank()) {
                recipe.setRequiredSkillNode(skillNode);
                recipe.setRequiredSkillLevel(section.getInt("required-skill-level", 1));
            }

            if (recipe.isShaped()) {
                List<String> shape = section.getStringList("shape");
                if (shape.size() != 3) return null;
                recipe.setShape(shape);

                ConfigurationSection ingMapSection = section.getConfigurationSection("ingredient-map");
                if (ingMapSection != null) {
                    Map<Character, Ingredient> map = new HashMap<>();
                    for (String key : ingMapSection.getKeys(false)) {
                        if (key.length() == 1) {
                            char ch = key.charAt(0);
                            Ingredient ing = Ingredient.load(ingMapSection.getConfigurationSection(key));
                            if (ing != null) {
                                map.put(ch, ing);
                            }
                        }
                    }
                    recipe.setIngredientMap(map);
                }
            } else {
                ConfigurationSection ingsSection = section.getConfigurationSection("ingredients");
                if (ingsSection != null) {
                    List<Ingredient> ings = new ArrayList<>();
                    for (String key : ingsSection.getKeys(false)) {
                        Ingredient ing = Ingredient.load(ingsSection.getConfigurationSection(key));
                        if (ing != null) {
                            ings.add(ing);
                        }
                    }
                    recipe.setIngredients(ings);
                }
            }

            return recipe;
        } catch (Exception e) {
            return null;
        }
    }

    public UItemRecipe copy() {
        UItemRecipe copy = new UItemRecipe(resultItemId, resultAmount);
        copy.shaped = shaped;
        copy.shape = new ArrayList<>(shape);
        copy.ingredientMap = new HashMap<>(ingredientMap);
        copy.ingredients = new ArrayList<>(ingredients);
        copy.categories = new HashSet<>(categories);
        copy.requiredSkillNode = requiredSkillNode;
        copy.requiredSkillLevel = requiredSkillLevel;
        return copy;
    }
}
