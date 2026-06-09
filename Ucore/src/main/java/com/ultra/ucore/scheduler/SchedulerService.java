package com.ultra.ucore.scheduler;

import org.bukkit.scheduler.BukkitTask;

/**
 * Scheduler abstraction.
 */
public interface SchedulerService {

    BukkitTask sync(Runnable task);

    BukkitTask async(Runnable task);

    BukkitTask later(Runnable task, long delayTicks);

    BukkitTask repeat(Runnable task, long delayTicks, long periodTicks);
}
