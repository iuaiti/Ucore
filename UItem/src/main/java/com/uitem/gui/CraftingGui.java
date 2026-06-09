package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.recipe.RecipeManager;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.SchedulerUtil;
import com.uitem.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CraftingGui implements InventoryHolder {

    private static final int[] CRAFT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 23;
    private static final int GUI_SIZE = 45;

    private static final int CRAFT_1_SLOT = 36;
    private static final int CRAFT_8_SLOT = 37;
    private static final int CRAFT_64_SLOT = 38;
    private static final int CRAFT_MAX_SLOT = 39;

    private static final Set<UUID> openCraftingGuis = ConcurrentHashMap.newKeySet();

    private final UItemPlugin plugin;
    private final Player player;
    private final RecipeManager recipeManager;
    private Inventory inventory;
    private int craftUnits = 1;  // number of recipe units (each may produce >1 result items)

    public CraftingGui(UItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.recipeManager = plugin.getRecipeManager();
        this.inventory = Bukkit.createInventory(this, GUI_SIZE,
                TextUtil.parse("<gradient:#ff6b6b:#ffd93d>Crafting</gradient>"));
        initialize();
    }

    private void initialize() {
        // Nicer crafting theme: use brown / orange tones for "table" feel
        ItemStack border = new ItemStack(Material.BROWN_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(" "));
            border.setItemMeta(meta);
        }
        ItemStack accent = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta am = accent.getItemMeta();
        if (am != null) {
            am.displayName(TextUtil.parse(" "));
            accent.setItemMeta(am);
        }

        for (int i = 0; i < GUI_SIZE; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 4 || col == 0 || col == 8) {
                inventory.setItem(i, border.clone());
            } else {
                // light inner fill
                inventory.setItem(i, accent.clone());
            }
        }

        // Clear craft grid + result
        for (int slot : CRAFT_SLOTS) {
            inventory.setItem(slot, null);
        }
        inventory.setItem(RESULT_SLOT, null);

        // Add a small "craft grid" label hint
        inventory.setItem(13, createInfoLabel(Material.CRAFTING_TABLE, "<gold>Lưới Chế Tạo"));
        inventory.setItem(22, createInfoLabel(Material.ARROW, "<yellow>→"));
        inventory.setItem(24, createInfoLabel(Material.KNOWLEDGE_BOOK, "<gold>Kết Quả"));

        // Info area explaining custom crafting + FULL_SET support (visual only, not live player armor)
        inventory.setItem(0, createInfoLabel(Material.CRAFTING_TABLE, "<gold>Chế Tạo UItem"));
        inventory.setItem(1, createInfoLabel(Material.KNOWLEDGE_BOOK, "<gray>Item tùy chỉnh"));
        inventory.setItem(2, createInfoLabel(Material.ANVIL, "<gray>+ vật liệu vanilla"));
        inventory.setItem(3, createInfoLabel(Material.NETHERITE_INGOT, "<yellow>Công thức đặc biệt"));
        inventory.setItem(4, createInfoLabel(Material.NETHER_STAR, "<gold>Kết quả ở đây"));
        inventory.setItem(9, createInfoLabel(Material.ARMOR_STAND, "<gray>Mặc đủ 4 món giáp → kích hoạt hiệu ứng FULL_SET (trên UItem đó)"));

        updateCraftButtons();
    }

    private ItemStack createInfoLabel(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void updateCraftButtons() {
        int selected = craftUnits;
        ItemStack[] matrix = getMatrix();
        int maxU = maxCraftable(matrix);
        boolean hasRecipe = maxU > 0;

        inventory.setItem(CRAFT_1_SLOT, createAmountButton(Material.LIME_DYE, "1",
                List.of("<gray>Chế 1 đơn vị", selected == 1 ? "<green>✔ Đã chọn" : "<yellow>Click để chọn")));
        inventory.setItem(CRAFT_8_SLOT, createAmountButton(Material.LIGHT_BLUE_DYE, "8",
                List.of("<gray>Chế 8 đơn vị", selected == 8 ? "<green>✔ Đã chọn" : "<yellow>Click để chọn")));
        inventory.setItem(CRAFT_64_SLOT, createAmountButton(Material.CYAN_DYE, "64",
                List.of("<gray>Chế 64 đơn vị", selected == 64 ? "<green>✔ Đã chọn" : "<yellow>Click để chọn")));
        inventory.setItem(CRAFT_MAX_SLOT, createAmountButton(Material.MAGENTA_DYE, "MAX",
                List.of(
                        hasRecipe ? "<gray>Chế tối đa có thể: x" + maxU : "<gray>Không có công thức chế tạo",
                        selected == 999 ? "<green>✔ Đã chọn" : "<yellow>Click để chọn"
                )));
    }

    private ItemStack createAmountButton(Material mat, String label, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse("<gold><bold>Chế x" + label + "</bold>"));
            meta.lore(lore.stream().map(TextUtil::parse).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack[] getMatrix() {
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = inventory.getItem(CRAFT_SLOTS[i]);
        }
        return matrix;
    }

    private void updateResult() {
        ItemStack[] matrix = getMatrix();
        boolean empty = Arrays.stream(matrix).allMatch(
                item -> item == null || item.getType().isAir());
        if (empty) {
            inventory.setItem(RESULT_SLOT, null);
            return;
        }

        ItemStack baseResult = recipeManager.matchRecipe(matrix);
        if (baseResult != null) {
            UItemRecipe rec = findMatchingRecipe(matrix);
            int per = (rec != null ? rec.getResultAmount() : 1);
            int maxU = maxCraftable(matrix);
            int units = (craftUnits == 999 ? maxU : Math.min(craftUnits, maxU));
            int total = units * per;
            int showAmt = Math.min(baseResult.getMaxStackSize(), Math.min(64, total));

            // Clone for preview so we can safely add temporary "click to craft" lore without polluting the real item
            ItemStack preview = baseResult.clone();
            preview.setAmount(showAmt);

            ItemMeta pm = preview.getItemMeta();
            if (pm != null) {
                List<net.kyori.adventure.text.Component> lore = pm.lore() != null ? new ArrayList<>(pm.lore()) : new ArrayList<>();
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(TextUtil.parse("<yellow>Click để chế x" + showAmt));
                lore.add(TextUtil.parse("<gray>Max units craftable: " + maxU));
                pm.lore(lore);
                preview.setItemMeta(pm);
            }
            inventory.setItem(RESULT_SLOT, preview);
        } else {
            inventory.setItem(RESULT_SLOT, null);
        }
    }

    private int maxCraftable(ItemStack[] matrix) {
        UItemRecipe recipe = findMatchingRecipe(matrix);
        if (recipe == null) return 0;

        int max = 64;
        if (recipe.isShaped()) {
            for (Map.Entry<Character, UItemRecipe.Ingredient> e : recipe.getIngredientMap().entrySet()) {
                char ch = e.getKey();
                UItemRecipe.Ingredient ing = e.getValue();
                int needed = ing.getAmount();
                int letterCan = Integer.MAX_VALUE;
                boolean hasPos = false;
                for (int i = 0; i < 9; i++) {
                    int r = i / 3, c = i % 3;
                    if (recipe.getShape().get(r).charAt(c) == ch) {
                        hasPos = true;
                        ItemStack item = matrix[i];
                        int avail = (item != null && !item.getType().isAir()) ? item.getAmount() : 0;
                        int can = needed > 0 ? avail / needed : 64;
                        letterCan = Math.min(letterCan, can);
                    }
                }
                if (hasPos) {
                    max = Math.min(max, Math.max(0, letterCan));
                }
            }
        } else {
            List<UItemRecipe.Ingredient> ings = recipe.getIngredients();
            List<ItemStack> nonAir = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                if (matrix[i] != null && !matrix[i].getType().isAir()) {
                    nonAir.add(matrix[i]);
                }
            }
            if (nonAir.size() != ings.size()) return 0;
            List<UItemRecipe.Ingredient> remaining = new ArrayList<>(ings);
            for (ItemStack item : nonAir) {
                UItemRecipe.Ingredient matched = null;
                for (UItemRecipe.Ingredient ing : remaining) {
                    if (ing.matches(item)) {
                        matched = ing;
                        break;
                    }
                }
                if (matched != null) {
                    remaining.remove(matched);
                    int ingAmount = matched.getAmount();
                    int canCraft = ingAmount > 0 ? item.getAmount() / ingAmount : 64;
                    max = Math.min(max, canCraft);
                }
            }
            if (!remaining.isEmpty()) return 0;
        }

        return Math.max(0, max);
    }

    private UItemRecipe findMatchingRecipe(ItemStack[] matrix) {
        for (UItemRecipe recipe : plugin.getRecipeManager().getAllRecipes().values()) {
            if (recipe.matches(matrix)) {
                return recipe;
            }
        }
        return null;
    }

    private void takeResult() {
        ItemStack result = inventory.getItem(RESULT_SLOT);
        if (result == null || result.getType().isAir()) return;

        // Clear immediately to prevent double-click / race condition giving duplicate items
        inventory.setItem(RESULT_SLOT, null);

        int totalItemsWanted = result.getAmount();

        // Determine per-unit output from current recipe (may be >1)
        ItemStack[] curMatrix = getMatrix();
        UItemRecipe rec = findMatchingRecipe(curMatrix);
        int per = (rec != null ? rec.getResultAmount() : 1);
        int unitsWanted = (per > 0 ? totalItemsWanted / per : totalItemsWanted);
        String resultItemId = (rec != null ? rec.getResultItemId() : null);

        int craftedUnits = 0;

        while (craftedUnits < unitsWanted) {
            ItemStack[] matrix = getMatrix();
            UItemRecipe recipe = findMatchingRecipe(matrix);
            if (recipe == null) break;

            if (recipe.isShaped()) {
                List<String> rows = recipe.getShape();
                if (rows.size() != 3) break;
                for (int i = 0; i < 9; i++) {
                    int r = i / 3, c = i % 3;
                    if (r >= rows.size() || c >= rows.get(r).length()) continue;
                    char ch = rows.get(r).charAt(c);
                    if (ch == ' ') continue;
                    UItemRecipe.Ingredient ing = recipe.getIngredientMap().get(ch);
                    if (ing == null) continue;
                    ItemStack item = inventory.getItem(CRAFT_SLOTS[i]);
                    if (item != null && !item.getType().isAir()) {
                        int deduct = ing.getAmount();
                        item.setAmount(item.getAmount() - deduct);
                        if (item.getAmount() <= 0) {
                            inventory.setItem(CRAFT_SLOTS[i], null);
                        }
                    }
                }
            } else {
                // Shapeless: re-match ings to current slots to deduct exact amounts
                List<Integer> nonAirIdx = new ArrayList<>();
                for (int i = 0; i < 9; i++) {
                    ItemStack it = inventory.getItem(CRAFT_SLOTS[i]);
                    if (it != null && !it.getType().isAir()) nonAirIdx.add(i);
                }
                List<UItemRecipe.Ingredient> remaining = new ArrayList<>(recipe.getIngredients());
                for (int idx : nonAirIdx) {
                    ItemStack item = inventory.getItem(CRAFT_SLOTS[idx]);
                    if (item == null || item.getType().isAir()) continue;
                    UItemRecipe.Ingredient matched = null;
                    for (UItemRecipe.Ingredient ing : remaining) {
                        if (ing.matches(item)) {
                            matched = ing;
                            break;
                        }
                    }
                    if (matched != null) {
                        remaining.remove(matched);
                        int deduct = matched.getAmount();
                        item.setAmount(item.getAmount() - deduct);
                        if (item.getAmount() <= 0) {
                            inventory.setItem(CRAFT_SLOTS[idx], null);
                        }
                    }
                }
            }
            craftedUnits++;
        }

        if (craftedUnits > 0) {
            int actualItems = craftedUnits * per;
            // Re-build a clean result item (do NOT clone the preview which has extra "click to craft" lore)
            ItemStack toGive = null;
            UItemData giveData = plugin.getItemManager().getItem(resultItemId);
            if (giveData != null) {
                toGive = plugin.getItemManager().buildItemStack(giveData);
            }
            if (toGive != null) {
                toGive.setAmount(actualItems);
                for (ItemStack leftover : player.getInventory().addItem(toGive).values()) {
                    player.getWorld().dropItem(player.getLocation(), leftover);
                }
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.3f, 1.5f);
        }

        updateCraftButtons();
        updateResult();
    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == RESULT_SLOT) {
            event.setCancelled(true);
            takeResult();
            return;
        }

        if (slot == CRAFT_1_SLOT) {
            event.setCancelled(true);
            craftUnits = 1;
            updateCraftButtons();
            updateResult();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            return;
        }

        if (slot == CRAFT_8_SLOT) {
            event.setCancelled(true);
            craftUnits = 8;
            updateCraftButtons();
            updateResult();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            return;
        }

        if (slot == CRAFT_64_SLOT) {
            event.setCancelled(true);
            craftUnits = 64;
            updateCraftButtons();
            updateResult();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            return;
        }

        if (slot == CRAFT_MAX_SLOT) {
            event.setCancelled(true);
            craftUnits = 999;
            updateCraftButtons();
            updateResult();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            return;
        }

        if (slot >= 0 && slot < GUI_SIZE) {
            boolean isCraftSlot = false;
            for (int cs : CRAFT_SLOTS) {
                if (cs == slot) { isCraftSlot = true; break; }
            }
            if (isCraftSlot) {
                SchedulerUtil.runSync(plugin, () -> {
                    updateResult();
                    updateCraftButtons();
                });
                return;
            }
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // Shift-click từ bên trong GUI (slot craft/result) — cancel hết
            if (slot < GUI_SIZE) {
                event.setCancelled(true);
                return;
            }
            // Shift-click từ player inventory lên craft grid
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;

            // Điền vào TẤT CẢ craft slot còn trống (không chỉ slot đầu)
            int remaining = clicked.getAmount();
            for (int cs : CRAFT_SLOTS) {
                if (remaining <= 0) break;
                ItemStack existing = inventory.getItem(cs);
                if (existing == null || existing.getType().isAir()) {
                    ItemStack one = clicked.clone();
                    one.setAmount(1);
                    inventory.setItem(cs, one);
                    remaining--;
                }
            }

            // Cập nhật lại slot player — setAmount(0) unreliable, dùng setItem trực tiếp
            if (remaining != clicked.getAmount()) {
                int clickedSlot = event.getSlot();
                if (remaining <= 0) {
                    player.getInventory().setItem(clickedSlot, null);
                } else {
                    clicked.setAmount(remaining);
                    player.getInventory().setItem(clickedSlot, clicked);
                }
                SchedulerUtil.runSync(plugin, () -> {
                    updateResult();
                    updateCraftButtons();
                });
            }
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        for (int slot : event.getRawSlots()) {
            if (slot < GUI_SIZE) {
                if (slot == RESULT_SLOT || slot == CRAFT_1_SLOT || slot == CRAFT_8_SLOT
                        || slot == CRAFT_64_SLOT || slot == CRAFT_MAX_SLOT) {
                    event.setCancelled(true);
                    return;
                }
                boolean isCraft = false;
                for (int cs : CRAFT_SLOTS) {
                    if (cs == slot) { isCraft = true; break; }
                }
                if (!isCraft) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private void returnItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        for (ItemStack leftover : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    public void onClose() {
        // Bug fix: Only return items from craft input slots, NOT the result preview slot.
        // The result slot contains a preview of what would be crafted — it was never
        // actually crafted, so returning it would allow item duplication.
        for (int slot : CRAFT_SLOTS) {
            returnItem(inventory.getItem(slot));
            inventory.setItem(slot, null);
        }
        // Clear result slot WITHOUT returning it to prevent item duplication
        inventory.setItem(RESULT_SLOT, null);
    }

    public void open() {
        openCraftingGuis.add(player.getUniqueId());
        player.openInventory(inventory);
    }

    public static boolean isCraftingOpen(Player player) {
        return openCraftingGuis.contains(player.getUniqueId());
    }

    public static void removeCraftingPlayer(Player player) {
        if (player == null) {
            // Bulk cleanup on plugin disable — clear all tracked players
            openCraftingGuis.clear();
        } else {
            openCraftingGuis.remove(player.getUniqueId());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
