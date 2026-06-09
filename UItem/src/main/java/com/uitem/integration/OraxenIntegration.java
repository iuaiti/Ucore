package com.uitem.integration;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Tích hợp với Oraxen bằng reflection để plugin vẫn build được khi không có Oraxen.
 */
public class OraxenIntegration {

    private final Class<?> oraxenItemsClass;

    public OraxenIntegration() throws ClassNotFoundException {
        this.oraxenItemsClass = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
    }

    /**
     * Lấy ItemStack từ Oraxen bằng item ID.
     */
    public ItemStack getItem(String oraxenId) {
        if (oraxenId == null || oraxenId.isBlank()) {
            return null;
        }
        try {
            Method existsMethod = oraxenItemsClass.getMethod("exists", String.class);
            boolean exists = Boolean.TRUE.equals(existsMethod.invoke(null, oraxenId));
            if (!exists) {
                return null;
            }

            Method getItemByIdMethod = oraxenItemsClass.getMethod("getItemById", String.class);
            Object itemBuilder = getItemByIdMethod.invoke(null, oraxenId);
            if (itemBuilder == null) {
                return null;
            }

            Method buildMethod = itemBuilder.getClass().getMethod("build");
            Object result = buildMethod.invoke(itemBuilder);
            if (result instanceof ItemStack itemStack) {
                return itemStack.clone();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
