package com.ultra.umenu.menu;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.gui.Button;
import com.ultra.ucore.gui.Gui;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.umenu.UMenuPlugin;
import com.ultra.umenu.manager.ProfileManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ProfileMenu extends BaseMenu {

    private final ProfileManager profileManager;

    public ProfileMenu(UMenuPlugin plugin, MenuManager menuManager, ProfileManager profileManager) {
        super(plugin, menuManager, "profile-menu");
        this.profileManager = profileManager;
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
        ProfileManager.PlayerProfile profile = profileManager.getProfile(player);

        var itemLoader = menuManager.getItemLoader();
        ItemStack infoItem = ItemBuilder.create(Material.PLAYER_HEAD)
                .name("<gradient:#00FFFF:#FF00FF>" + player.getName() + "</gradient>")
                .lore(
                        "<gray>Playtime: <yellow>" + profile.playtime() + "</yellow></gray>",
                        "<gray>Balance: <yellow>$" + String.format("%,.2f", profile.balance()) + "</yellow></gray>",
                        "<gray>Rank: <light_purple>" + profile.rank() + "</light_purple></gray>",
                        "",
                        "<gray>Statistics</gray>",
                        "<dark_gray>Kills: <white>" + profile.kills() + "</white></dark_gray>",
                        "<dark_gray>Deaths: <white>" + profile.deaths() + "</white></dark_gray>",
                        "<dark_gray>Level: <white>" + profileManager.getLevel(player) + "</white></dark_gray>"
                )
                .build();

        gui.setButton(22, new Button(infoItem, null));
        addBackButton(gui, "main", 40);
    }
}
