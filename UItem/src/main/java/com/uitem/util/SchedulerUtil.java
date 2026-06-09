package com.uitem.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Scheduler wrapper hỗ trợ Paper + Folia bằng API scheduler hiện đại.
 */
public final class SchedulerUtil {

    private SchedulerUtil() {
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static void runAsync(Plugin plugin, Runnable runnable) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> runnable.run());
    }

    public static void runSync(Plugin plugin, Runnable runnable) {
        if (isFolia()) {
            runGlobal(plugin, runnable);
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    public static void runDelayedAsync(Plugin plugin, Runnable runnable, long delay, TimeUnit unit) {
        plugin.getServer().getAsyncScheduler().runDelayed(plugin, task -> runnable.run(), delay, unit);
    }

    public static void runGlobal(Plugin plugin, Runnable runnable) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, runnable);
    }

    public static void runLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (isFolia()) {
            plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, task -> runnable.run(), delayTicks);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public static void runAtLocation(Plugin plugin, Location location, Runnable runnable) {
        plugin.getServer().getRegionScheduler().execute(plugin, location, runnable);
    }

    public static void runForEntity(Plugin plugin, Entity entity, Runnable runnable) {
        entity.getScheduler().run(plugin, task -> runnable.run(), null);
    }
}
