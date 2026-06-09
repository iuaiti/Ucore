package com.ultra.umenu.menu;

import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.umenu.UMenuPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ShopMenu extends BaseMenu {

    public ShopMenu(UMenuPlugin plugin, MenuManager menuManager) {
        super(plugin, menuManager, "shop-menu");
    }

    @Override
    public void open(Player player) {
        String title = getTitle(configKey);
        int size = getSize(configKey);
        Gui gui = createGui(title, size);
        fillBorder(gui, configKey);

        int[] contentSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int slot = 0;

        record ShopCategory(String name, Material icon, String lore) {}
        var categories = java.util.List.of(
                new ShopCategory("Blocks", Material.BRICKS, "Building materials"),
                new ShopCategory("Farming", Material.WHEAT, "Farming supplies"),
                new ShopCategory("Mob Drops", Material.BONE, "Mob loot"),
                new ShopCategory("Rare Materials", Material.NETHERITE_INGOT, "End-game materials")
        );

        for (var cat : categories) {
            if (slot >= contentSlots.length) break;
            ItemStack item = ItemBuilder.create(cat.icon())
                    .name("<gradient:#00FF00:#AAFF00>" + cat.name() + "</gradient>")
                    .lore("<gray>" + cat.lore() + "</gray>", "", "<yellow>Click to browse</yellow>")
                    .build();
            gui.setButton(contentSlots[slot++], new Button(item, event -> {
                event.getWhoClicked().sendMessage("§aShop category: " + cat.name());
            }));
        }

        addConfigItems(gui, configKey, player);
        gui.open(player);
    }

    @Override
    protected void build(Gui gui, Player player) {
    }
}
