package com.ultra.ucrate.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

public final class CrateLocation {

    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public CrateLocation(Location location) {
        this(
                Objects.requireNonNull(location.getWorld(), "World cannot be null").getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public CrateLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String worldName() {
        return worldName;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalStateException("World not loaded: " + worldName);
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public boolean isAt(Location location) {
        if (!location.getWorld().getName().equals(worldName)) return false;
        return (int) location.getX() == (int) x
                && (int) location.getY() == (int) y
                && (int) location.getZ() == (int) z;
    }

    public String serialize() {
        return worldName + ";" + x + ";" + y + ";" + z + ";" + yaw + ";" + pitch;
    }

    public static CrateLocation deserialize(String input) {
        String[] parts = input.split(";");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid location format: " + input);
        }
        return new CrateLocation(
                parts[0],
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5])
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CrateLocation that)) return false;
        return Double.compare(x, that.x) == 0
                && Double.compare(y, that.y) == 0
                && Double.compare(z, that.z) == 0
                && Float.compare(yaw, that.yaw) == 0
                && Float.compare(pitch, that.pitch) == 0
                && worldName.equals(that.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, x, y, z, yaw, pitch);
    }
}
