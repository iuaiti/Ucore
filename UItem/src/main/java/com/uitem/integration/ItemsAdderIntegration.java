package com.uitem.integration;

import org.bukkit.inventory.ItemStack;

/**
 * Tích hợp với ItemsAdder.
 * Dùng reflection để gọi API vì ItemsAdder không có maven repo công khai ổn định.
 */
public class ItemsAdderIntegration {

    private final Class<?> customStackClass;

    public ItemsAdderIntegration() throws Exception {
        // Load class qua reflection
        this.customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
    }

    /**
     * Lấy ItemStack từ ItemsAdder bằng namespaced ID.
     *
     * @param namespacedId ID dạng "namespace:item_name"
     * @return ItemStack hoặc null nếu không tìm thấy
     */
    public ItemStack getItem(String namespacedId) {
        try {
            // CustomStack.getInstance(namespacedId)
            Object customStack = customStackClass.getMethod("getInstance", String.class)
                    .invoke(null, namespacedId);

            if (customStack == null) return null;

            // customStack.getItemStack()
            Object result = customStackClass.getMethod("getItemStack").invoke(customStack);
            if (result instanceof ItemStack item) {
                return item.clone();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
