package com.ultra.ucrate;

import com.ultra.ucrate.command.UCrateCommand;
import com.ultra.ucrate.command.sub.*;
import com.ultra.ucrate.listener.CrateInteractListener;
import com.ultra.ucrate.listener.PlayerConnectionListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class UCratePlugin extends JavaPlugin {

    private UCrateProvider provider;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("UCore") == null) {
            getLogger().severe("UCore not found! Disabling UCrate...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        this.provider = new UCrateProvider(this);
        provider.init();

        registerCommands();
        registerListeners();

        getLogger().info("╔══════════════════════════════════════════════╗");
        getLogger().info("║        ✦ UCrate v" + getPluginMeta().getVersion() + " — Crate Engine");
        getLogger().info("║        ✦ Author: UltraNetwork");
        getLogger().info("╠══════════════════════════════════════════════╣");
        getLogger().info("║  ■ Loaded: " + provider.getCrateManager().crateCount() + " crates");
        getLogger().info("║  ■ Animations: " + provider.getAnimationManager().getAnimations().size());
        getLogger().info("╚══════════════════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        if (provider != null) {
            provider.shutdown();
        }
        getLogger().info("UCrate disabled.");
    }

    private void registerCommands() {
        UCrateCommand command = new UCrateCommand()
                .addSubCommand(new CreateCommand())
                .addSubCommand(new DeleteCommand())
                .addSubCommand(new KeyCommand())
                .addSubCommand(new OpenCommand())
                .addSubCommand(new ListCommand())
                .addSubCommand(new EditorCommand())
                .addSubCommand(new ReloadCommand());

        Objects.requireNonNull(getCommand("ucrate")).setExecutor(command);
        Objects.requireNonNull(getCommand("ucrate")).setTabCompleter(command);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new CrateInteractListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(), this);
    }
}
