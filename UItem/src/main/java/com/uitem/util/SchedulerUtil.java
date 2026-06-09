package com.uitem.util;

import com.ultra.ucore.api.UCoreAPI;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Scheduler wrapper hỗ trợ Paper + Folia bằng API scheduler hiện đại.
 * <p>
 * Các method cơ bản delegate lên {@link com.ultra.ucore.scheduler.SchedulerService}
 * của UCore để đảm bảo đồng bộ. Các method nâng cao (runDelayedAsync, runGlobal)
 * giữ implementation riêng vì UCore chưa cung cấp tương ứng.
 */
public final class SchedulerUtil {

    private SchedulerUtil() {
    }

    public static boolean isFolia() {
        return UCoreAPI.getSchedulerService().isFolia();
    }

    public static void runAsync(Plugin plugin, Runnable runnable) {
        UCoreAPI.getSchedulerService().async(runnable);
    }

    public static void runSync(Plugin plugin, Runnable runnable) {
        UCoreAPI.getSchedulerService().sync(runnable);
    }

    public static void runDelayedAsync(Plugin plugin, Runnable runnable, long delay, TimeUnit unit) {
        plugin.getServer().getAsyncScheduler().runDelayed(plugin, task -> runnable.run(), delay, unit);
    }

    public static void runGlobal(Plugin plugin, Runnable runnable) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, runnable);
    }

    public static void runLater(Plugin plugin, Runnable runnable, long delayTicks) {
        UCoreAPI.getSchedulerService().later(runnable, delayTicks);
    }

    public static void runAtLocation(Plugin plugin, Location location, Runnable runnable) {
        UCoreAPI.getSchedulerService().runAtLocation(location, runnable);
    }

    public static void runForEntity(Plugin plugin, Entity entity, Runnable runnable) {
        UCoreAPI.getSchedulerService().runForEntity(entity, runnable);
    }
}
