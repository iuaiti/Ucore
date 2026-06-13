package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.SkillRequirementHelper;
import com.uitem.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GUI chế tạo phong cách RPG/MMO — 45 slot (5 hàng, không lưới vanilla).
 *
 * <pre>
 * Layout (col 0-8, row 0-4):
 *
 *  Col   0   1   2   3   4   5   6   7   8
 *  Row0 [■] [■] [■] [■] [T] [■] [■] [■] [■]
 *  Row1 [■] [I1][■] [I2][■] [■] [■] [▲] [■]   ▲ = dec frame above result (slot 16)
 *  Row2 [■] [■] [I3][■] [→] [→] [■] [R] [▶]   → = slots 22-23, R = result slot 25, ▶ = dec right (26)
 *  Row3 [■] [I4][■] [I5][■] [■] [■] [C] [■]   C = craft button (slot 34)
 *  Row4 [B] [■] [I6][■] [■] [■] [■] [■] [X]   B = back (36), X = close (44)
 *
 *  T  = info         slot  4
 *  I1 = ingredient1  slot 10    I2 = ingredient2  slot 12
 *  I3 = ingredient3  slot 20
 *  I4 = ingredient4  slot 28    I5 = ingredient5  slot 30
 *  I6 = ingredient6  slot 38
 *  →  = arrows       slot 22, 23
 *  ▲  = dec frame    slot 16 (above result)
 *  ▶  = dec frame    slot 26 (right of result)
 *  R  = result       slot 25
 *  C  = craft        slot 34
 *  B  = back         slot 36
 *  X  = close        slot 44
 * </pre>
 */
public class RpgRecipeGui extends AbstractGui {

    // Ingredient slot positions (left-side diamond pattern, symmetric)
    private static final int[] ING_SLOTS = {10, 12, 20, 28, 30, 38};

    private static final int SLOT_INFO   = 4;
    private static final int SLOT_DEC_U  = 16;  // decoration above result
    private static final int SLOT_ARROW1 = 22;
    private static final int SLOT_ARROW2 = 23;
    private static final int SLOT_RESULT = 25;
    private static final int SLOT_DEC_R  = 26;  // decoration right of result
    private static final int SLOT_CRAFT  = 34;
    private static final int SLOT_BACK   = 36;
    private static final int SLOT_CLOSE  = 44;

    private final String itemId;

    public RpgRecipeGui(UItemPlugin plugin, Player player, String itemId) {
        super(plugin, player, 45, buildTitle(plugin, itemId));
        this.itemId = itemId;
    }

    private static String buildTitle(UItemPlugin plugin, String id) {
        UItemData d = plugin.getItemManager().getItem(id);
        String plain = (d != null && d.getDisplayName() != null)
                ? strip(d.getDisplayName()) : id;
        return "<dark_gray>⚒ <white>" + plain;
    }

    // ═════════════════════════════════════════════════════════════════════════
    @Override
    protected void setup() {
        // ── Gray glass background ────────────────────────────────────────────
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) setItem(i, bg);

        UItemRecipe recipe   = plugin.getRecipeManager().getRecipe(itemId);
        UItemData resultData = plugin.getItemManager().getItem(itemId);

        // ── No recipe / no item ──────────────────────────────────────────────
        if (recipe == null || resultData == null) {
            setItem(SLOT_INFO, createItem(Material.BARRIER,
                    "<red>Không tìm thấy công thức",
                    List.of("<gray>ID: <white>" + itemId)));
            setItem(SLOT_BACK,  createBackButton(), ev -> goBack());
            setItem(SLOT_CLOSE, buildCloseBtn(),    ev -> player.closeInventory());
            return;
        }

        // ── Consolidate ingredients (group same type, sum amounts) ───────────
        List<IngEntry> ings = consolidate(recipe);

        // Ingredient availability
        boolean allMatsOk = ings.stream().allMatch(e -> countInv(e) >= e.totalAmount);
        boolean skillOk   = !recipe.hasSkillRequirement()
                || SkillRequirementHelper.canCraft(player, recipe);
        boolean canCraft  = allMatsOk && skillOk;

        // ── Info item ────────────────────────────────────────────────────────
        setItem(SLOT_INFO, buildInfoItem(recipe, canCraft, skillOk));

        // ── Arrows ───────────────────────────────────────────────────────────
        ItemStack arrow = createItem(Material.ARROW, "<yellow>➔",
                List.of("<dark_gray>Nguyên liệu  →  Kết quả"));
        setItem(SLOT_ARROW1, arrow);
        setItem(SLOT_ARROW2, arrow);

        // ── Ingredient items ─────────────────────────────────────────────────
        for (int i = 0; i < Math.min(ings.size(), ING_SLOTS.length); i++) {
            IngEntry entry = ings.get(i);
            int has = countInv(entry);
            setItem(ING_SLOTS[i], buildIngItem(entry, has));
        }

        // ── Decorative frame (color signals craftability) ────────────────────
        Material frameMat = canCraft ? Material.LIME_STAINED_GLASS_PANE
                                     : Material.RED_STAINED_GLASS_PANE;
        ItemStack frame = createItem(frameMat, " ");
        setItem(SLOT_DEC_U, frame);
        setItem(SLOT_DEC_R, frame);

        // ── Result item ──────────────────────────────────────────────────────
        setItem(SLOT_RESULT, buildResultItem(resultData, recipe, canCraft));

        // ── Craft button ─────────────────────────────────────────────────────
        boolean fCanCraft = canCraft;
        setItem(SLOT_CRAFT, buildCraftBtn(canCraft), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
            if (!fCanCraft) {
                player.sendMessage(TextUtil.error("Không đủ nguyên liệu để chế tạo!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
                return;
            }
            if (doCraft(recipe, resultData)) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.2f);
                player.sendMessage(TextUtil.success("Đã chế tạo: "
                        + (resultData.getDisplayName() != null ? resultData.getDisplayName() : itemId)));
                reopen();   // refresh ingredient counts
            } else {
                player.sendMessage(TextUtil.error("Chế tạo thất bại — hãy thử lại!"));
            }
        });

        // ── Navigation ───────────────────────────────────────────────────────
        setItem(SLOT_BACK,  createBackButton(), ev -> goBack());
        setItem(SLOT_CLOSE, buildCloseBtn(),    ev -> player.closeInventory());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Item builders
    // ═════════════════════════════════════════════════════════════════════════

    private ItemStack buildInfoItem(UItemRecipe recipe, boolean canCraft, boolean skillOk) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Loại: <white>" + (recipe.isShaped() ? "Shaped (3×3)" : "Shapeless"));
        lore.add("<gray>Kết quả: <white>x" + recipe.getResultAmount());
        if (!recipe.getCategories().isEmpty()) {
            lore.add("<gray>Danh mục: <white>" + fmtCats(recipe.getCategories()));
        }
        if (recipe.hasSkillRequirement()) {
            String req = SkillRequirementHelper.getRequirementText(recipe);
            lore.add(skillOk
                    ? "<green>✓ Skill: Đạt"
                    : "<red>✗ Skill: " + (req != null ? req : "Chưa đủ cấp độ"));
        }
        lore.add("");
        lore.add(canCraft
                ? "<green><bold>✓ Sẵn sàng chế tạo!"
                : "<red><bold>✗ Chưa đủ nguyên liệu");
        return createItem(Material.KNOWLEDGE_BOOK,
                "<gradient:#ff6b6b:#ffd93d><bold>Thông Tin Công Thức</bold>", lore);
    }

    private ItemStack buildIngItem(IngEntry entry, int playerHas) {
        boolean ok = playerHas >= entry.totalAmount;

        // Base icon
        ItemStack base = entry.buildIcon(plugin);
        ItemMeta meta = base.getItemMeta();
        if (meta == null) return base;

        // Name with availability color
        String rawName = entry.displayName(plugin);
        meta.displayName(TextUtil.parse(ok
                ? "<green>✓ " + rawName
                : "<red>✗ " + rawName));

        // Lore
        List<Component> lore = new ArrayList<>();
        lore.add(TextUtil.parse("<dark_gray>────────────────"));
        lore.add(TextUtil.parse("<gray>Cần: <white>x" + entry.totalAmount));
        if (ok) {
            lore.add(TextUtil.parse("<gray>Bạn có: <green>x" + playerHas + "  <dark_green>✓ Đủ"));
        } else {
            int missing = entry.totalAmount - playerHas;
            lore.add(TextUtil.parse("<gray>Bạn có: <red>x" + playerHas
                    + " <dark_gray>/ <white>x" + entry.totalAmount));
            lore.add(TextUtil.parse("<red>Còn thiếu: <yellow>x" + missing));
        }
        lore.add(TextUtil.parse("<dark_gray>────────────────"));

        if (ok) meta.setEnchantmentGlintOverride(true);
        meta.lore(lore);
        base.setItemMeta(meta);
        return base;
    }

    private ItemStack buildResultItem(UItemData data, UItemRecipe recipe, boolean canCraft) {
        ItemStack item = plugin.getItemManager().buildPreviewItemStack(data, false);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(TextUtil.parse("<dark_gray>────────────────"));
        lore.add(TextUtil.parse("<gray>Sản xuất: <white>x" + recipe.getResultAmount()));
        lore.add(canCraft
                ? TextUtil.parse("<green><bold>✓ Có thể chế tạo!")
                : TextUtil.parse("<red><bold>✗ Chưa đủ nguyên liệu"));

        if (canCraft) meta.setEnchantmentGlintOverride(true);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCraftBtn(boolean canCraft) {
        if (canCraft) {
            return createItem(Material.LIME_TERRACOTTA,
                    "<green><bold>⚒ Chế Tạo</bold>",
                    List.of(
                            "<gray>Click để chế tạo ngay",
                            "<gray>Nguyên liệu tự động bị tiêu thụ",
                            "",
                            "<green>✓ Bạn đủ nguyên liệu!"
                    ));
        }
        return createItem(Material.RED_TERRACOTTA,
                "<red><bold>✗ Không Thể Chế Tạo</bold>",
                List.of(
                        "<gray>Bạn chưa đủ nguyên liệu.",
                        "<red>Hãy thu thập đủ nguyên liệu trước."
                ));
    }

    private ItemStack buildCloseBtn() {
        return createItem(Material.BARRIER,
                "<red><bold>✕ Đóng</bold>",
                List.of("<gray>Click để đóng"));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Craft logic
    // ═════════════════════════════════════════════════════════════════════════

    private boolean doCraft(UItemRecipe recipe, UItemData resultData) {
        List<IngEntry> ings = consolidate(recipe);
        // Safety re-check
        for (IngEntry entry : ings) {
            if (countInv(entry) < entry.totalAmount) return false;
        }
        if (recipe.hasSkillRequirement() && !SkillRequirementHelper.canCraft(player, recipe)) return false;

        // Consume
        for (IngEntry entry : ings) {
            removeFromInv(entry, entry.totalAmount);
        }

        // Give result
        ItemStack result = plugin.getItemManager().buildItemStack(resultData);
        result.setAmount(recipe.getResultAmount());
        player.getInventory().addItem(result).values()
                .forEach(leftover -> player.getWorld().dropItem(player.getLocation(), leftover));
        return true;
    }

    /**
     * Count total of this ingredient type across the entire player inventory.
     * Type-only match (ignores per-item amount — we sum individually).
     */
    private int countInv(IngEntry entry) {
        NamespacedKey ukey = plugin.getItemManager().getUItemKey();
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (typeMatches(entry, item, ukey)) total += item.getAmount();
        }
        return total;
    }

    private void removeFromInv(IngEntry entry, int amount) {
        NamespacedKey ukey = plugin.getItemManager().getUItemKey();
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;
            if (!typeMatches(entry, item, ukey)) continue;
            int take = Math.min(item.getAmount(), remaining);
            remaining -= take;
            if (take >= item.getAmount()) {
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - take);
            }
        }
    }

    /** Match by type only, ignoring amount (used for counting / removing). */
    private static boolean typeMatches(IngEntry entry, ItemStack item, NamespacedKey ukey) {
        UItemRecipe.Ingredient ing = entry.ingredient;
        if (ing.getUitemId() != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return false;
            String id = meta.getPersistentDataContainer().get(ukey, PersistentDataType.STRING);
            return ing.getUitemId().equals(id);
        }
        return ing.getMaterial() != null && item.getType() == ing.getMaterial();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Ingredient consolidation (group same ingredient, sum amounts)
    // ═════════════════════════════════════════════════════════════════════════

    private List<IngEntry> consolidate(UItemRecipe recipe) {
        Map<String, IngEntry> map = new LinkedHashMap<>();

        if (recipe.isShaped()) {
            // Count how many times each char appears in shape
            Map<Character, Integer> charCount = new LinkedHashMap<>();
            for (String row : recipe.getShape()) {
                for (char c : row.toCharArray()) {
                    if (c != ' ') charCount.merge(c, 1, Integer::sum);
                }
            }
            for (Map.Entry<Character, UItemRecipe.Ingredient> e : recipe.getIngredientMap().entrySet()) {
                int cnt = charCount.getOrDefault(e.getKey(), 0);
                if (cnt == 0) continue;
                UItemRecipe.Ingredient ing = e.getValue();
                int total = Math.max(1, ing.getAmount()) * cnt;
                map.computeIfAbsent(ingKey(ing), k -> new IngEntry(ing, 0)).totalAmount += total;
            }
        } else {
            for (UItemRecipe.Ingredient ing : recipe.getIngredients()) {
                map.computeIfAbsent(ingKey(ing), k -> new IngEntry(ing, 0))
                        .totalAmount += Math.max(1, ing.getAmount());
            }
        }

        return new ArrayList<>(map.values());
    }

    private static String ingKey(UItemRecipe.Ingredient ing) {
        if (ing.getUitemId() != null) return "u:" + ing.getUitemId().toLowerCase();
        if (ing.getMaterial() != null) return "m:" + ing.getMaterial().name();
        return "?";
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Format helpers
    // ═════════════════════════════════════════════════════════════════════════

    private String fmtCats(Set<String> cats) {
        return cats.stream()
                .map(k -> {
                    var info = UItemRecipe.getCategoryInfo(k);
                    return info != null ? info.getIcon() + " " + info.getDisplayName() : k;
                })
                .collect(Collectors.joining(", "));
    }

    private static String strip(String text) {
        return text == null ? "" : text.replaceAll("<[^>]*>", "");
    }

    private static String fmtMat(Material mat) {
        String s = mat.name().toLowerCase().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String w : s.split(" ")) {
            if (!w.isEmpty())
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Inner class: consolidated ingredient entry
    // ═════════════════════════════════════════════════════════════════════════

    private static final class IngEntry {
        final UItemRecipe.Ingredient ingredient;
        int totalAmount;

        IngEntry(UItemRecipe.Ingredient ingredient, int totalAmount) {
            this.ingredient  = ingredient;
            this.totalAmount = totalAmount;
        }

        /** Build icon ItemStack (1 unit, no amount override). */
        ItemStack buildIcon(UItemPlugin plugin) {
            if (ingredient.getUitemId() != null) {
                UItemData d = plugin.getItemManager().getItem(ingredient.getUitemId());
                if (d != null) {
                    ItemStack item = plugin.getItemManager().buildItemStack(d);
                    item.setAmount(1);
                    return item;
                }
            }
            if (ingredient.getMaterial() != null && !ingredient.getMaterial().isAir()) {
                return new ItemStack(ingredient.getMaterial());
            }
            return new ItemStack(Material.BARRIER);
        }

        /** Human-readable name (tags stripped). */
        String displayName(UItemPlugin plugin) {
            if (ingredient.getUitemId() != null) {
                UItemData d = plugin.getItemManager().getItem(ingredient.getUitemId());
                if (d != null && d.getDisplayName() != null) return strip(d.getDisplayName());
                return ingredient.getUitemId();
            }
            if (ingredient.getMaterial() != null) return fmtMat(ingredient.getMaterial());
            return "Unknown";
        }

        private static String strip(String t) { return t == null ? "" : t.replaceAll("<[^>]*>", ""); }
        private static String fmtMat(Material mat) {
            String s = mat.name().toLowerCase().replace("_", " ");
            StringBuilder sb = new StringBuilder();
            for (String w : s.split(" ")) {
                if (!w.isEmpty())
                    sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
            return sb.toString().trim();
        }
    }
}
