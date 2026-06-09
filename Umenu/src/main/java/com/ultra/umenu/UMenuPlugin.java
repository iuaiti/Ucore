package com.ultra.umenu;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.config.ConfigManager;
import com.ultra.umenu.command.MenuCommand;
import com.ultra.umenu.menu.MenuManager;
import com.ultra.umenu.module.ModuleDetector;
import com.ultra.umenu.placeholder.UMenuPlaceholderExpansion;
import org.bukkit.plugin.java.JavaPlugin;

public final class UMenuPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ModuleDetector moduleDetector;
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.load("config.yml");
        this.configManager.load("menus.yml");
        this.configManager.load("items.yml");
        this.configManager.load("messages.yml");

        this.moduleDetector = new ModuleDetector(this);
        this.moduleDetector.detect();

        this.menuManager = new MenuManager(this, configManager);
        this.menuManager.load();

        var command = new MenuCommand(this, menuManager);
        var pluginCommand = getCommand("menu");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        UCoreAPI.getPlaceholderManager().register("umenu_balance",
                ctx -> String.valueOf(menuManager.getProfileManager().getBalance(ctx.player())));
        UCoreAPI.getPlaceholderManager().register("umenu_rank",
                ctx -> menuManager.getProfileManager().getRank(ctx.player()));
        UCoreAPI.getPlaceholderManager().register("umenu_playtime",
                ctx -> menuManager.getProfileManager().getPlaytime(ctx.player()));
        UCoreAPI.getPlaceholderManager().register("umenu_kills",
                ctx -> String.valueOf(menuManager.getProfileManager().getKills(ctx.player())));
        UCoreAPI.getPlaceholderManager().register("umenu_deaths",
                ctx -> String.valueOf(menuManager.getProfileManager().getDeaths(ctx.player())));

        new UMenuPlaceholderExpansion(menuManager).register();

        getLogger().info("UMenu enabled.");
    }

    @Override
    public void onDisable() {
        if (menuManager != null) {
            menuManager.closeAll();
        }
        getLogger().info("UMenu disabled.");
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public ModuleDetector getModuleDetector() {
        return moduleDetector;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
