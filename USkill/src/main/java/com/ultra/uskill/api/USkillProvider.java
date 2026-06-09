package com.ultra.uskill.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Objects;

/**
 * Static accessor cho USkillAPI.
 */
public final class USkillProvider {

    private static USkillAPI api;

    private USkillProvider() {
    }

    public static void register(USkillAPI instance) {
        api = Objects.requireNonNull(instance, "USkillAPI instance cannot be null");
    }

    public static void unregister() {
        api = null;
    }

    public static USkillAPI get() {
        USkillAPI instance = api;
        if (instance == null) {
            throw new IllegalStateException("USkillAPI has not been initialized.");
        }
        return instance;
    }
}
