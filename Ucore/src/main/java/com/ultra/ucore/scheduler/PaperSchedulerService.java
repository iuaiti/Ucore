package com.ultra.ucore.scheduler;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Paper implementation of the scheduler abstraction.
 */
public final class PaperSchedulerService implements SchedulerService {

    private final JavaPlugin plugin;

    public PaperSchedulerService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public BukkitTask sync(Runnable task) {
        return plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public BukkitTask async(Runnable task) {
        return plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public BukkitTask later(Runnable task, long delayTicks) {
        return plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public BukkitTask repeat(Runnable task, long delayTicks, long periodTicks) {
        return plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }
}
