package com.ultra.ucore.command;

import com.ultra.ucore.config.ConfigManager;
import com.ultra.ucore.message.MessageManager;
import org.bukkit.command.CommandSender;

/**
 * Reloads UCore configuration.
 */
public final class ReloadCommand implements SubCommand {

    private final ConfigManager configManager;
    private final MessageManager messageManager;

    public ReloadCommand(ConfigManager configManager, MessageManager messageManager) {
        this.configManager = configManager;
        this.messageManager = messageManager;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return "ucore.reload";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        configManager.reloadAll();
        messageManager.send(sender, "prefix.server-reload");
    }
}
