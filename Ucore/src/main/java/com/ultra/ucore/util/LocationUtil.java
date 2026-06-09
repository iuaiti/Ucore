package com.ultra.ucore.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Location serialization utilities.
 */
public final class LocationUtil {

    private LocationUtil() {
    }

    public static String serialize(Location location) {
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location world cannot be null.");
        }

        return location.getWorld().getName()
                + ";"
                + location.getX()
                + ";"
                + location.getY()
                + ";"
                + location.getZ()
                + ";"
                + location.getYaw()
                + ";"
                + location.getPitch();
    }

    public static Location deserialize(String input) {
        String[] parts = input.split(";");

        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid location format: " + input);
        }

        World world = Bukkit.getWorld(parts[0]);

        if (world == null) {
            throw new IllegalArgumentException("World not found: " + parts[0]);
        }

        return new Location(
                world,
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5])
        );
    }
}
