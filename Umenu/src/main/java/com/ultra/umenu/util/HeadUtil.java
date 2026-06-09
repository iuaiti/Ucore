package com.ultra.umenu.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.UUID;

public final class HeadUtil {

    private static Field PROFILE_FIELD;
    private static Class<?> GAME_PROFILE_CLASS;
    private static Class<?> PROPERTY_CLASS;
    private static Class<?> PROPERTY_MAP_CLASS;

    static {
        try {
            String packageName = BukkitReflection.getCraftBukkitPackage();
            PROFILE_FIELD = Class.forName(packageName + ".inventory.CraftMetaSkull")
                    .getDeclaredField("profile");
            PROFILE_FIELD.setAccessible(true);
            GAME_PROFILE_CLASS = Class.forName("com.mojang.authlib.GameProfile");
            PROPERTY_CLASS = Class.forName("com.mojang.authlib.properties.Property");
            PROPERTY_MAP_CLASS = Class.forName("com.mojang.authlib.properties.PropertyMap");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HeadUtil reflection", e);
        }
    }

    private HeadUtil() {
    }

    public static ItemStack createHead(String textureBase64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        if (textureBase64 == null || textureBase64.isEmpty()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        try {
            Object profile = GAME_PROFILE_CLASS
                    .getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), null);

            Object propertyMap = GAME_PROFILE_CLASS.getMethod("getProperties").invoke(profile);
            Object property = PROPERTY_CLASS.getConstructor(String.class, String.class)
                    .newInstance("textures", textureBase64);
            PROPERTY_MAP_CLASS.getMethod("put", Object.class, Object.class)
                    .invoke(propertyMap, "textures", property);

            PROFILE_FIELD.set(meta, profile);
        } catch (Exception e) {
            head.setItemMeta(meta);
            return head;
        }

        head.setItemMeta(meta);
        return head;
    }

    private static final class BukkitReflection {
        static String getCraftBukkitPackage() {
            try {
                Class<?> serverClass = Class.forName("org.bukkit.Bukkit");
                String name = serverClass.getMethod("getServer").invoke(null).getClass().getName();
                return name.substring(0, name.lastIndexOf('.'));
            } catch (Exception e) {
                return "org.bukkit.craftbukkit.v1_21_R3";
            }
        }
    }
}
