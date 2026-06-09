package com.ultra.ucore.message;

import com.ultra.ucore.config.ConfigManager;
import com.ultra.ucore.placeholder.PlaceholderManager;
import com.ultra.ucore.placeholder.PlaceholderManager.PlaceholderContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Provides MiniMessage-based messaging with placeholder support.
 */
public final class MessageManager {

    private final ConfigManager configManager;
    private final PlaceholderManager placeholderManager;
    private final MiniMessage miniMessage;

    public MessageManager(ConfigManager configManager, PlaceholderManager placeholderManager) {
        this.configManager = configManager;
        this.placeholderManager = placeholderManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void send(CommandSender sender, String path) {
        Object raw = configManager.get("messages.yml").get(path);

        if (raw == null) {
            sender.sendMessage(component(sender, path));
            return;
        }

        if (raw instanceof List<?> list) {
            for (Object line : list) {
                sender.sendMessage(component(sender, String.valueOf(line)));
            }
            return;
        }

        sender.sendMessage(component(sender, String.valueOf(raw)));
    }

    public Component get(CommandSender sender, String path) {
        String raw = configManager.get("messages.yml").getString(path, path);
        return component(sender, raw);
    }

    public Component component(CommandSender sender, String raw) {
        String prefix = configManager.get("messages.yml").getString("prefix.default", "");
        String parsed = raw.replace("<prefix>", prefix);

        Player player = sender instanceof Player p ? p : null;
        parsed = placeholderManager.apply(parsed, new PlaceholderContext(player, sender));

        return miniMessage.deserialize(parsed);
    }
}
