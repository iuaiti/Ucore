package com.ultra.ucrate.storage;

import com.ultra.ucrate.model.Crate;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface StorageProvider {

    void saveCrate(@NotNull Crate crate);

    void deleteCrate(@NotNull String crateId);

    @NotNull
    Collection<Crate> loadAllCrates();

    int getKeys(@NotNull UUID playerId, @NotNull String crateId);

    void setKeys(@NotNull UUID playerId, @NotNull String crateId, int amount);

    void addKeys(@NotNull UUID playerId, @NotNull String crateId, int amount);

    void removeKeys(@NotNull UUID playerId, @NotNull String crateId, int amount);

    @NotNull
    Map<String, Integer> getAllKeys(@NotNull UUID playerId);

    void saveAll();

    void reload();
}
