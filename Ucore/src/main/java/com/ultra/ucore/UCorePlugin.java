package com.ultra.ucore;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.api.UCoreServices;
import com.ultra.ucore.command.CommandManager;
import com.ultra.ucore.command.ReloadCommand;
import com.ultra.ucore.command.UCoreCommand;
import com.ultra.ucore.config.ConfigManager;
import com.ultra.ucore.cooldown.CooldownManager;
import com.ultra.ucore.database.DatabaseManager;
import com.ultra.ucore.gui.GuiManager;
import com.ultra.ucore.listener.PlayerQuitListener;
import com.ultra.ucore.message.MessageManager;
import com.ultra.ucore.placeholder.PlaceholderManager;
import com.ultra.ucore.scheduler.PaperSchedulerService;
import com.ultra.ucore.scheduler.SchedulerService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin bootstrap class for UCore.
 */
public final class UCorePlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private PlaceholderManager placeholderManager;
    private GuiManager guiManager;
    private CooldownManager cooldownManager;
    private DatabaseManager databaseManager;
    private SchedulerService schedulerService;
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        this.schedulerService = new PaperSchedulerService(this);

        this.configManager = new ConfigManager(this);
        this.configManager.load("config.yml");
        this.configManager.load("messages.yml");

        this.placeholderManager = new PlaceholderManager();
        this.placeholderManager.register("ucore_player_name", context ->
                context.player() == null ? "Console" : context.player().getName()
        );

        this.messageManager = new MessageManager(configManager, placeholderManager);

        this.guiManager = new GuiManager(this);
        this.guiManager.register();

        this.cooldownManager = new CooldownManager();
        this.databaseManager = new DatabaseManager(this, configManager);
        this.databaseManager.initialize();

        this.commandManager = new CommandManager(this);
        this.commandManager.register(
                "ucore",
                new UCoreCommand(messageManager)
                        .addSubCommand(new ReloadCommand(configManager, messageManager))
        );

        getServer().getPluginManager().registerEvents(
                new PlayerQuitListener(cooldownManager),
                this
        );

        UCoreAPI.initialize(new UCoreServices(
                configManager,
                messageManager,
                guiManager,
                cooldownManager,
                databaseManager,
                schedulerService,
                placeholderManager,
                null // SkillService will be registered by USkill
        ));

        getLogger().info("UCore enabled.");
    }

    @Override
    public void onDisable() {
        if (guiManager != null) {
            guiManager.closeAll();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        UCoreAPI.shutdown();

        getLogger().info("UCore disabled.");
    }
}
