package com.ultra.umenu.menu;

import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.umenu.UMenuPlugin;
import com.ultra.umenu.manager.CollectionManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class CollectionMenu extends BaseMenu {

    private final CollectionManager collectionManager;

    public CollectionMenu(UMenuPlugin plugin, MenuManager menuManager, CollectionManager collectionManager) {
        super(plugin, menuManager, "collection-menu");
        this.collectionManager = collectionManager;
    }

    @Override
    public void open(Player player) {
        String title = getTitle(configKey);
        int size = getSize(configKey);
        Gui gui = createGui(title, size);
        fillBorder(gui, configKey);

        int[] contentSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int slot = 0;

        List<CollectionManager.CollectionCategory> categories = collectionManager.getCategories();
        if (categories.isEmpty()) {
            categories = List.of(
                    new CollectionManager.CollectionCategory("mob_kills", "Mob Kills", "ZOMBIE_HEAD", "kills"),
                    new CollectionManager.CollectionCategory("boss_kills", "Boss Kills", "DRAGON_HEAD", "kills"),
                    new CollectionManager.CollectionCategory("mining", "Mining", "DIAMOND_PICKAXE", "blocks"),
                    new CollectionManager.CollectionCategory("fishing", "Fishing", "FISHING_ROD", "catches"),
                    new CollectionManager.CollectionCategory("farming", "Farming", "GOLDEN_HOE", "harvests")
            );
        }

        for (var cat : categories) {
            if (slot >= contentSlots.length) break;
            Material mat = Material.getMaterial(cat.icon());
            if (mat == null) mat = Material.PAPER;

            long value = collectionManager.getValue(player, cat.id());

            ItemStack item = ItemBuilder.create(mat)
                    .name("<gradient:#FF00FF:#AA00FF>" + cat.name() + "</gradient>")
                    .lore(
                            "<gray>Total: <yellow>" + value + " " + cat.unit() + "</yellow></gray>",
                            "",
                            "<gray>Rewards available:</gray>",
                            "<dark_gray>- <white>Titles</white></dark_gray>",
                            "<dark_gray>- <white>Prefixes</white></dark_gray>",
                            "<dark_gray>- <white>Cosmetics</white></dark_gray>"
                    )
                    .build();
            gui.setButton(contentSlots[slot++], new Button(item, null));
        }

        addConfigItems(gui, configKey, player);
        gui.open(player);
    }

    @Override
    protected void build(Gui gui, Player player) {
    }
}
