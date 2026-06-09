package com.ultra.ucore.api;

import com.ultra.ucore.config.ConfigManager;
import com.ultra.ucore.cooldown.CooldownManager;
import com.ultra.ucore.database.DatabaseManager;
import com.ultra.ucore.gui.GuiManager;
import com.ultra.ucore.message.MessageManager;
import com.ultra.ucore.placeholder.PlaceholderManager;
import com.ultra.ucore.scheduler.SchedulerService;
import com.ultra.ucore.skill.SkillService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Static public access point for UCore managers.
 */
public final class UCoreAPI {

    private static final AtomicReference<UCoreServices> SERVICES = new AtomicReference<>();

    private UCoreAPI() {
    }

    public static void initialize(UCoreServices services) {
        SERVICES.set(Objects.requireNonNull(services, "services"));
    }

    public static void shutdown() {
        SERVICES.set(null);
    }

    public static void registerSkillService(SkillService skillService) {
        UCoreServices svc = SERVICES.get();
        if (svc == null) return;
        SERVICES.set(new UCoreServices(
                svc.configManager(),
                svc.messageManager(),
                svc.guiManager(),
                svc.cooldownManager(),
                svc.databaseManager(),
                svc.schedulerService(),
                svc.placeholderManager(),
                skillService
        ));
    }

    public static UCoreServices services() {
        UCoreServices services = SERVICES.get();

        if (services == null) {
            throw new IllegalStateException("UCoreAPI has not been initialized.");
        }

        return services;
    }

    public static ConfigManager getConfigManager() {
        return services().configManager();
    }

    public static MessageManager getMessageManager() {
        return services().messageManager();
    }

    public static GuiManager getGuiManager() {
        return services().guiManager();
    }

    public static CooldownManager getCooldownManager() {
        return services().cooldownManager();
    }

    public static DatabaseManager getDatabaseManager() {
        return services().databaseManager();
    }

    public static SchedulerService getSchedulerService() {
        return services().schedulerService();
    }

    public static PlaceholderManager getPlaceholderManager() {
        return services().placeholderManager();
    }

    public static SkillService getSkillService() {
        return services().skillService();
    }
}
