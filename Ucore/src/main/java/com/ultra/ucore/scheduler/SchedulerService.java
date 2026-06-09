package com.ultra.ucore.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Scheduler abstraction.
 * <p>
 * Các method chạy global/main-thread sử dụng GlobalRegionScheduler (Folia-safe).
 * Async method sử dụng AsyncScheduler.
 * runAtLocation / runForEntity delegate tới đúng region scheduler.
 */
public interface SchedulerService {

    void sync(Runnable task);

    void async(Runnable task);

    void later(Runnable task, long delayTicks);

    void repeat(Runnable task, long delayTicks, long periodTicks);

    void runAtLocation(Location location, Runnable task);

    void runForEntity(Entity entity, Runnable task);

    boolean isFolia();
}
