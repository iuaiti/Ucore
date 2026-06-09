package com.ultra.umenu.menu;

import com.ultra.ucore.gui.Gui;
import com.ultra.umenu.UMenuPlugin;
import org.bukkit.entity.Player;

public final class MainMenu extends BaseMenu {

    public MainMenu(UMenuPlugin plugin, MenuManager menuManager) {
        super(plugin, menuManager, "main-menu");
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
