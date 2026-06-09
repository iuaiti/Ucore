package com.ultra.ucore.manager;

/**
 * Common lifecycle contract for UCore managers.
 */
public interface UCoreManager {

    default void enable() {
    }

    default void disable() {
    }
}
