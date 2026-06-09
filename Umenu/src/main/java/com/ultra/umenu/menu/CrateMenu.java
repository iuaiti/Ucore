package com.ultra.umenu.menu;

import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.umenu.UMenuPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CrateMenu extends BaseMenu {

    public CrateMenu(UMenuPlugin plugin, MenuManager menuManager) {
        super(plugin, menuManager, "crate-menu");
    }

    @Override
    public void open(Player player) {
        String title = getTitle(configKey);
        int size = getSize(configKey);
        Gui gui = createGui(title, size);
        fillBorder(gui, configKey);
        build(gui, player);
        gui.open(player);
    }

    @Override
    protected void build(Gui gui, Player player) {
        addConfigItems(gui, configKey, player);
    }
}
