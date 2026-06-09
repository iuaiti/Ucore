package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.recipe.UItemRecipe;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeListGui extends AbstractGui {

    private int page;
    // 4 hàng × 7 cột (bỏ cột đầu và cuối là border) = 28 slot
    private static final int PER_PAGE = 28;
    private static final int[] SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    public RecipeListGui(UItemPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public RecipeListGui(UItemPlugin plugin, Player player, int page) {
        super(plugin, player, 54, "<gradient:#ff6b6b:#ffd93d>Danh Sách Công Thức</gradient>");
        this.page = page;
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

        setItem(4, createItem(Material.KNOWLEDGE_BOOK,
                "<gradient:#ff6b6b:#ffd93d><bold>Danh Sách Công Thức</bold></gradient>",
                List.of("<gray>Click a recipe to view details")));

        List<Map.Entry<String, UItemRecipe>> list = new ArrayList<>(
                plugin.getRecipeManager().getAllRecipes().entrySet());

        int totalPages = Math.max(1, (int) Math.ceil((double) list.size() / PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int start = page * PER_PAGE;
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = start + i;
            if (idx >= list.size()) break;

            Map.Entry<String, UItemRecipe> entry = list.get(idx);
            String id = entry.getKey();
            UItemRecipe recipe = entry.getValue();
            UItemData data = plugin.getItemManager().getItem(id);

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
                new RecipeDetailGui(plugin, player, fid).open();
            });
        }

        setItem(49, createItem(Material.CRAFTING_TABLE,
                "<green><bold>Craft</bold>",
                List.of("<gray>Open crafting table")), ev -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new CraftingGui(plugin, player).open();
        });

        setItem(46, page > 0 ? createPreviousButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page > 0 ? ev -> { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new RecipeListGui(plugin, player, page - 1).open(); } : null);
        setItem(52, page < totalPages - 1 ? createNextButton() : createItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                page < totalPages - 1 ? ev -> { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    new RecipeListGui(plugin, player, page + 1).open(); } : null);
    }
}
