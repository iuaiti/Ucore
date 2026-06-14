package com.ultra.ucrate.manager;

import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.CrateLocation;
import com.ultra.ucrate.storage.StorageProvider;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CrateManager {

    private final StorageProvider storage;
    private final Map<String, Crate> crates = new ConcurrentHashMap<>();

    public CrateManager(StorageProvider storage) {
        this.storage = storage;
    }

    public void loadAll() {
        Collection<Crate> loaded = storage.loadAllCrates();
        crates.clear();
        for (Crate crate : loaded) {
            crates.put(crate.id().toLowerCase(), crate);
        }
    }

    public void addCrate(@NotNull Crate crate) {
        crates.put(crate.id().toLowerCase(), crate);
        storage.saveCrate(crate);
    }

    public void removeCrate(@NotNull String crateId) {
        crates.remove(crateId.toLowerCase());
        storage.deleteCrate(crateId);
    }

    @Nullable
    public Crate getCrate(@NotNull String crateId) {
        return crates.get(crateId.toLowerCase());
    }

    @NotNull
    public Optional<Crate> findCrate(@NotNull String crateId) {
        return Optional.ofNullable(crates.get(crateId.toLowerCase()));
    }

    @Nullable
    public Crate getCrateAt(@NotNull Location location) {
        return crates.values().stream()
                .filter(c -> c.location() != null && c.location().isAt(location))
                .findFirst()
                .orElse(null);
    }

    public void updateCrate(@NotNull Crate crate) {
        crates.put(crate.id().toLowerCase(), crate);
        storage.saveCrate(crate);
    }

    @NotNull
    public Collection<Crate> getAllCrates() {
        return crates.values();
    }

    public boolean crateExists(@NotNull String crateId) {
        return crates.containsKey(crateId.toLowerCase());
    }

    public int crateCount() {
        return crates.size();
    }
}
