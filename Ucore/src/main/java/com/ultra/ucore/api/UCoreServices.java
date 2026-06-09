package com.ultra.ucore.api;

import com.ultra.ucore.config.ConfigManager;
import com.ultra.ucore.cooldown.CooldownManager;
import com.ultra.ucore.database.DatabaseManager;
import com.ultra.ucore.gui.GuiManager;
import com.ultra.ucore.message.MessageManager;
import com.ultra.ucore.placeholder.PlaceholderManager;
import com.ultra.ucore.scheduler.SchedulerService;

/**
 * Immutable service container used by the public UCore API.
 */
public record UCoreServices(
        ConfigManager configManager,
        MessageManager messageManager,
        GuiManager guiManager,
        CooldownManager cooldownManager,
        DatabaseManager databaseManager,
        SchedulerService schedulerService,
        PlaceholderManager placeholderManager
) {
}
