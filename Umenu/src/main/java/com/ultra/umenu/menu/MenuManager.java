package com.ultra.umenu.menu;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.gui.GuiManager;
import com.ultra.umenu.UMenuPlugin;
import com.ultra.umenu.manager.*;
import com.ultra.umenu.model.MenuItemLoader;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuManager {

    private final UMenuPlugin plugin;
    private final ProfileManager profileManager;
    private final QuestManager questManager;
    private final BossManager bossManager;
    private final CollectionManager collectionManager;
    private final SettingsManager settingsManager;
    private final MenuItemLoader itemLoader;

    private final Map<String, BaseMenu> menus = new ConcurrentHashMap<>();
    private final Map<Player, String> lastMenus = new ConcurrentHashMap<>();

    public MenuManager(UMenuPlugin plugin, com.ultra.ucore.config.ConfigManager configManager) {
        this.plugin = plugin;
        this.profileManager = new ProfileManager();
        this.questManager = new QuestManager();
        this.bossManager = new BossManager();
        this.collectionManager = new CollectionManager();
        this.settingsManager = new SettingsManager();
        this.itemLoader = new MenuItemLoader(configManager);
    }

    public void load() {
        itemLoader.load();

        menus.put("main", new MainMenu(plugin, this));
        menus.put("profile", new ProfileMenu(plugin, this, profileManager));
        menus.put("quest", new QuestMenu(plugin, this, questManager));
        menus.put("boss", new BossMenu(plugin, this, bossManager));
        menus.put("crates", new CrateMenu(plugin, this));
        menus.put("shop", new ShopMenu(plugin, this));
        menus.put("collection", new CollectionMenu(plugin, this, collectionManager));
        menus.put("settings", new SettingsMenu(plugin, this, settingsManager));
        menus.put("server-info", new ServerInfoMenu(plugin, this));

        GuiManager guiManager = UCoreAPI.getGuiManager();
        guiManager.register();
    }

    public void openMain(Player player) {
        open(player, "main");
    }

    public void open(Player player, String menuKey) {
        BaseMenu menu = menus.get(menuKey.toLowerCase());
        if (menu == null) {
            player.sendMessage("§cMenu not found: " + menuKey);
            return;
        }
        lastMenus.put(player, menuKey);
        menu.open(player);
    }

    public void openPrevious(Player player) {
        String last = lastMenus.get(player);
        if (last != null) {
            open(player, last);
        } else {
            openMain(player);
        }
    }

    public void closeAll() {
        UCoreAPI.getGuiManager().closeAll();
        lastMenus.clear();
    }

    public void reload() {
        itemLoader.reload();
        for (BaseMenu menu : menus.values()) {
        }
        closeAll();
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public BossManager getBossManager() {
        return bossManager;
    }

    public CollectionManager getCollectionManager() {
        return collectionManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public MenuItemLoader getItemLoader() {
        return itemLoader;
    }

    public UMenuPlugin getPlugin() {
        return plugin;
    }
}
