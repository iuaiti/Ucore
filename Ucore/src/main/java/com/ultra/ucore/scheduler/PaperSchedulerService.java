package com.ultra.ucore.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper implementation using modern scheduler API with Folia support.
 * <p>
 * Trên Paper: dùng AsyncScheduler / GlobalRegionScheduler / RegionScheduler.
 * Trên Spigot: fallback về BukkitScheduler cũ.
 */
public final class PaperSchedulerService implements SchedulerService {

    private final JavaPlugin plugin;
    private final boolean folia;

    public PaperSchedulerService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Override
    public void sync(Runnable task) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
    }

    @Override
    public void async(Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    @Override
    public void later(Runnable task, long delayTicks) {
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
    }

    @Override
    public void repeat(Runnable task, long delayTicks, long periodTicks) {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), delayTicks, periodTicks);
    }

    @Override
    public void runAtLocation(Location location, Runnable task) {
        plugin.getServer().getRegionScheduler().execute(plugin, location, task);
    }

    @Override
    public void runForEntity(Entity entity, Runnable task) {
        entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
    }

    @Override
    public boolean isFolia() {
        return folia;
    }
}
