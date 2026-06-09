package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RecipeEditorGui implements InventoryHolder {

    private static final int[] CRAFT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 23;
    private static final int GUI_SIZE = 45;

    private static final int SAVE_SHAPED_SLOT = 38;
    private static final int SAVE_SHAPELESS_SLOT = 40;
    private static final int CLEAR_SLOT = 42;
    private static final int RESULT_AMOUNT_SLOT = 39;
    private static final int GIVE_CURRENT_SLOT = 41;  // Easy way to grab this custom item to use as ingredient in other recipes

    private final UItemPlugin plugin;
    private final Player player;
    private final UItemData data;
    private Inventory inventory;
    private UItemRecipe existingRecipe;
    private int resultAmount = 1;

    public RecipeEditorGui(UItemPlugin plugin, Player player, UItemData data) {
        this.plugin = plugin;
        this.player = player;
        this.data = data;
        this.existingRecipe = plugin.getRecipeManager().getRecipe(data.getId());
        if (this.existingRecipe != null) {
            this.resultAmount = Math.max(1, Math.min(64, this.existingRecipe.getResultAmount()));
        }
        this.inventory = org.bukkit.Bukkit.createInventory(this, GUI_SIZE,
                TextUtil.parse("<gold>Công thức: <white>" + data.getId()));
        initialize();
    }

    private void initialize() {
        // Prettier editor theme
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(" "));
            border.setItemMeta(meta);
        }
        ItemStack inner = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta im = inner.getItemMeta();
        if (im != null) {
            im.displayName(TextUtil.parse(" "));
            inner.setItemMeta(im);
        }

        for (int i = 0; i < GUI_SIZE; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 4 || col == 0 || col == 8) {
                inventory.setItem(i, border.clone());
            } else {
                inventory.setItem(i, inner.clone());
            }
        }

        for (int slot : CRAFT_SLOTS) {
            inventory.setItem(slot, null);
        }

        if (existingRecipe != null && existingRecipe.isShaped()) {
            ItemStack[] matrix = existingRecipe.toMatrix();
            for (int i = 0; i < 9 && i < matrix.length; i++) {
                if (matrix[i] != null && !matrix[i].getType().isAir()) {
                    inventory.setItem(CRAFT_SLOTS[i], matrix[i].clone());
                }
            }
        } else if (existingRecipe != null && !existingRecipe.isShaped()) {
            List<UItemRecipe.Ingredient> ings = existingRecipe.getIngredients();
            for (int i = 0; i < Math.min(ings.size(), 9); i++) {
                ItemStack ingItem = ings.get(i).toItemStack();
                if (ingItem != null && !ingItem.getType().isAir()) {
                    inventory.setItem(CRAFT_SLOTS[i], ingItem);
                }
            }
        }

        ItemStack preview = plugin.getItemManager().buildPreviewItemStack(data);
        preview.setAmount(resultAmount);
        inventory.setItem(RESULT_SLOT, preview);
        inventory.setItem(RESULT_SLOT + 9, createInfoItem(Material.KNOWLEDGE_BOOK,
                "<gold><bold>Hướng Dẫn</bold>",
                List.of(
                        "<gray>Đặt nguyên liệu (vanilla hoặc UItem) bên trái",
                        "<gray>Kết quả: item này x" + resultAmount,
                        "<yellow>Mẹo: /uitem give <id> rồi kéo vào đây",
                        "<yellow>Kích thước stack = số lượng nguyên liệu cần",
                        "",
                        "<green>Shaped</green> <gray>lưu theo mẫu | <aqua>Shapeless</aqua> <gray>bất kỳ thứ tự"
                )));

        inventory.setItem(SAVE_SHAPED_SLOT, createInfoItem(Material.GREEN_STAINED_GLASS_PANE,
                "<green><bold>Lưu Shaped</bold>",
                List.of("<gray>Lưu công thức shaped (3x3)")));
        inventory.setItem(SAVE_SHAPELESS_SLOT, createInfoItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                "<aqua><bold>Lưu Shapeless</bold>",
                List.of("<gray>Lưu công thức shapeless")));
        inventory.setItem(CLEAR_SLOT, createInfoItem(Material.RED_STAINED_GLASS_PANE,
                "<red><bold>Xóa Công Thức</bold>",
                List.of("<gray>Xóa công thức hiện tại")));

        // Result amount editor button
        inventory.setItem(RESULT_AMOUNT_SLOT, createInfoItem(Material.GOLD_INGOT,
                "<gold><bold>Số Lượng Kết Quả: x" + resultAmount + "</bold>",
                List.of(
                        "<gray>Click để đổi số lượng output",
                        "<gray>Hiện tại: <white>" + resultAmount,
                        "<yellow>1 - 64 item mỗi lần chế"
                )));

        // Quick "get this item" button - makes using custom UItem as ingredient for other recipes very easy
        inventory.setItem(GIVE_CURRENT_SLOT, createInfoItem(Material.ENDER_CHEST,
                "<aqua><bold>Give 1x Item Này</bold>",
                List.of(
                        "<gray>Nhận 1 bản sao của item tùy chỉnh này",
                        "<yellow>Kéo vào lưới bên trên để dùng làm nguyên liệu",
                        "<gray>Kích thước stack bạn đặt = số lượng cần trong recipe",
                        "<gray>Hoàn hảo để tạo recipe dùng item tùy chỉnh khác"
                )));

        if (existingRecipe != null) {
            inventory.setItem(44, createInfoItem(Material.LIME_DYE,
                    "<green><bold>Đã có công thức</bold>",
                    List.of(
                            "<gray>Loại: " + (existingRecipe.isShaped() ? "Shaped" : "Shapeless"),
                            "<gray>Số lượng: " + existingRecipe.getResultAmount()
                    )));
        }

        int backSlot = 36;
        inventory.setItem(backSlot, createInfoItem(Material.ARROW,
                "<yellow>← Quay Lại",
                List.of("<gray>Click để quay lại")));

    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= GUI_SIZE) return;

        boolean isCraftSlot = false;
        for (int cs : CRAFT_SLOTS) {
            if (cs == slot) { isCraftSlot = true; break; }
        }
        if (isCraftSlot) {
            return;
        }
        if (slot == RESULT_SLOT) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (slot == SAVE_SHAPED_SLOT) {
            saveRecipe(true);
            return;
        }
        if (slot == SAVE_SHAPELESS_SLOT) {
            saveRecipe(false);
            return;
        }
        if (slot == CLEAR_SLOT) {
            plugin.getRecipeManager().removeRecipe(data.getId());
            // Save this recipe as its own file (recipes/<result>.yml)
            String resultId = data.getId();
            UItemRecipe current = plugin.getRecipeManager().getRecipe(resultId);
            if (current != null) {
                plugin.getRecipeManager().saveRecipeIndividual(resultId, current);
            } else {
                plugin.getRecipeManager().saveRecipesAsync(); // fallback
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
            player.sendMessage(TextUtil.success("Đã xóa công thức cho '" + data.getId() + "'!"));
            refresh();
            return;
        }
        if (slot == RESULT_AMOUNT_SLOT) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập số lượng kết quả (1-64):",
                    input -> {
                        try {
                            int amt = Integer.parseInt(input.trim());
                            if (amt < 1 || amt > 64) {
                                player.sendMessage(TextUtil.error("Amount must be 1-64!"));
                            } else {
                                resultAmount = amt;
                                player.sendMessage(TextUtil.success("Đã đặt số lượng kết quả x" + amt));
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Số không hợp lệ!"));
                        }
                        // Update display in place to preserve placed ingredients in editor grid
                        updateResultAmountDisplay();
                    });
            return;
        }

        if (slot == GIVE_CURRENT_SLOT) {
            if (!player.hasPermission("uitem.give")) {
                player.sendMessage(com.uitem.util.TextUtil.error(com.uitem.util.Messages.noPermission()));
                return;
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            ItemStack one = plugin.getItemManager().buildItemStack(data);
            one.setAmount(1);
            var leftovers = player.getInventory().addItem(one);
            leftovers.values().forEach(l -> player.getWorld().dropItemNaturally(player.getLocation(), l));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
            player.sendMessage(TextUtil.success("Gave you 1x '" + data.getId() + "'. Drag into the crafting grid to use as ingredient for another item!"));
            return;
        }
        if (slot == 36) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            if (backAction != null) {
                backAction.run();
            } else {
                new ItemEditorGui(plugin, player, data).open();
            }
            return;
        }
    }

    private void updateResultAmountDisplay() {
        // Update preview amount
        ItemStack preview = inventory.getItem(RESULT_SLOT);
        if (preview != null) {
            preview.setAmount(resultAmount);
            inventory.setItem(RESULT_SLOT, preview);
        }
        // Update the amount button
        inventory.setItem(RESULT_AMOUNT_SLOT, createInfoItem(Material.GOLD_INGOT,
                "<gold><bold>Result Amount: x" + resultAmount + "</bold>",
                List.of(
                        "<gray>Click để đổi số lượng output",
                        "<gray>Hiện tại: <white>" + resultAmount,
                        "<yellow>1 - 64 items per craft"
                )));
        // Update guide
        inventory.setItem(RESULT_SLOT + 9, createInfoItem(Material.KNOWLEDGE_BOOK,
                "<gold><bold>Hướng Dẫn</bold>",
                List.of(
                        "<gray>Đặt nguyên liệu (vanilla hoặc UItem) bên trái",
                        "<gray>Kết quả: item này x" + resultAmount,
                        "<yellow>Mẹo: /uitem give <id> rồi kéo vào đây",
                        "<yellow>Kích thước stack = số lượng nguyên liệu cần",
                        "",
                        "<green>Shaped</green> <gray>lưu theo mẫu | <aqua>Shapeless</aqua> <gray>bất kỳ thứ tự"
                )));
    }

    public void handleDrag(InventoryDragEvent event) {
        for (int slot : event.getRawSlots()) {
            if (slot < GUI_SIZE) {
                if (slot == RESULT_SLOT) {
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

    private void saveRecipe(boolean shaped) {
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = inventory.getItem(CRAFT_SLOTS[i]);
        }

        boolean empty = Arrays.stream(matrix).allMatch(
                item -> item == null || item.getType().isAir());
        if (empty) {
            player.sendMessage(TextUtil.error("Please place ingredients in the crafting slots!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
            return;
        }

        UItemRecipe recipe = new UItemRecipe(data.getId(), resultAmount);

        if (shaped) {
            saveShaped(recipe, matrix);
        } else {
            saveShapeless(recipe, matrix);
        }

        plugin.getRecipeManager().setRecipe(data.getId(), recipe);
        plugin.getRecipeManager().saveRecipesAsync();
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        player.sendMessage(TextUtil.success("Đã lưu công thức " + (shaped ? "shaped" : "shapeless") + " cho '" + data.getId() + "'!"));
        refresh();
    }

    private void saveShaped(UItemRecipe recipe, ItemStack[] matrix) {
        recipe.setShaped(true);
        List<String> shape = new ArrayList<>();
        Map<Character, UItemRecipe.Ingredient> ingMap = new HashMap<>();
        char nextChar = 'A';

        for (int r = 0; r < 3; r++) {
            StringBuilder row = new StringBuilder();
            for (int c = 0; c < 3; c++) {
                ItemStack item = matrix[r * 3 + c];
                if (item == null || item.getType().isAir()) {
                    row.append(' ');
                } else {
                    char match = findMatchingChar(item, ingMap);
                    if (match == 0) {
                        match = nextChar++;
                        UItemRecipe.Ingredient ing;
                        if (isUItem(item)) {
                            String uitemId = getUItemId(item);
                            ing = new UItemRecipe.Ingredient(uitemId, item.getAmount());
                        } else {
                            ing = new UItemRecipe.Ingredient(item.getType(), item.getAmount());
                        }
                        ingMap.put(match, ing);
                    }
                    row.append(match);
                }
            }
            shape.add(row.toString());
        }

        recipe.setShape(shape);
        recipe.setIngredientMap(ingMap);
    }

    private void saveShapeless(UItemRecipe recipe, ItemStack[] matrix) {
        recipe.setShaped(false);
        List<UItemRecipe.Ingredient> ings = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            ItemStack item = matrix[i];
            if (item != null && !item.getType().isAir()) {
                UItemRecipe.Ingredient ing;
                if (isUItem(item)) {
                    ing = new UItemRecipe.Ingredient(getUItemId(item), item.getAmount());
                } else {
                    ing = new UItemRecipe.Ingredient(item.getType(), item.getAmount());
                }
                ings.add(ing);
            }
        }

        recipe.setIngredients(ings);
    }

    private char findMatchingChar(ItemStack item, Map<Character, UItemRecipe.Ingredient> map) {
        for (Map.Entry<Character, UItemRecipe.Ingredient> entry : map.entrySet()) {
            UItemRecipe.Ingredient ing = entry.getValue();
            // Require exact match on type/uitem AND the exact amount (stack size placed = consume amount for that position)
            // This allows same material with *different* per-position amounts (they get distinct letters)
            boolean typeMatch = false;
            if (ing.getUitemId() != null) {
                String id = getUItemId(item);
                typeMatch = ing.getUitemId().equals(id);
            } else {
                typeMatch = ing.getMaterial() == item.getType();
            }
            if (typeMatch && ing.getAmount() == item.getAmount()) {
                return entry.getKey();
            }
        }
        return 0;
    }

    private boolean isUItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(plugin.getItemManager().getUItemKey(),
                        org.bukkit.persistence.PersistentDataType.STRING);
    }

    private String getUItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(plugin.getItemManager().getUItemKey(),
                        org.bukkit.persistence.PersistentDataType.STRING);
    }

    private void refresh() {
        ItemStack[] savedItems = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            savedItems[i] = inventory.getItem(CRAFT_SLOTS[i]);
        }

        this.existingRecipe = plugin.getRecipeManager().getRecipe(data.getId());
        if (this.existingRecipe != null) {
            this.resultAmount = Math.max(1, Math.min(64, this.existingRecipe.getResultAmount()));
        }
        initialize();

        for (int i = 0; i < 9; i++) {
            if (savedItems[i] != null && !savedItems[i].getType().isAir()) {
                inventory.setItem(CRAFT_SLOTS[i], savedItems[i]);
            }
        }
    }

    private ItemStack createInfoItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            if (lore != null) {
                meta.lore(lore.stream().map(TextUtil::parse).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private Runnable backAction;

    public void open() {
        if (!player.hasPermission("uitem.editor")) {
            player.sendMessage(com.uitem.util.TextUtil.error(com.uitem.util.Messages.noPermission()));
            return;
        }
        player.openInventory(inventory);
    }

    /**
     * Open as child of an AbstractGui parent so Back returns to it.
     */
    public void openAsChildOfEditor(AbstractGui parent) {
        if (parent != null) {
            this.backAction = () -> parent.reopen();
        }
        open();
    }

    /**
     * Called when the GUI is closed — returns craft slot items to the player
     * to prevent permanent item loss.
     */
    public void onClose(Player player) {
        for (int slot : CRAFT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                for (ItemStack leftover : player.getInventory().addItem(item.clone()).values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                inventory.setItem(slot, null);
            }
        }
    }
}
