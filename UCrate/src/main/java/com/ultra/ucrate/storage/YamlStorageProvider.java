package com.ultra.ucrate.storage;

import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.CrateLocation;
import com.ultra.ucrate.model.CrateType;
import com.ultra.ucrate.model.Reward;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class YamlStorageProvider implements StorageProvider {

    private final JavaPlugin plugin;
    private final File cratesFile;
    private final File keysDir;
    private FileConfiguration cratesConfig;
    private final Map<UUID, Map<String, Integer>> playerKeysCache;

    public YamlStorageProvider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cratesFile = new File(plugin.getDataFolder(), "crates.yml");
        this.keysDir = new File(plugin.getDataFolder(), "keydata");
        this.playerKeysCache = new ConcurrentHashMap<>();
    }

    public void init() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!cratesFile.exists()) {
            plugin.saveResource("crates.yml", false);
        }
        if (!keysDir.exists()) {
            keysDir.mkdirs();
        }
        this.cratesConfig = YamlConfiguration.loadConfiguration(cratesFile);
    }

    @Override
    public void saveCrate(@NotNull Crate crate) {
        String path = "crates." + crate.id();
        cratesConfig.set(path + ".display-name", crate.displayName());
        cratesConfig.set(path + ".display-item", crate.displayItem());
        cratesConfig.set(path + ".type", crate.type().name());
        cratesConfig.set(path + ".animation", crate.animationId());
        cratesConfig.set(path + ".key-id", crate.keyId());
        cratesConfig.set(path + ".permission", crate.permission());
        cratesConfig.set(path + ".require-key", crate.requireKey());
        cratesConfig.set(path + ".cooldown-seconds", crate.cooldownSeconds());

        CrateLocation loc = crate.location();
        if (loc != null) {
            cratesConfig.set(path + ".location", loc.serialize());
        }

        cratesConfig.set(path + ".rewards", null);
        for (int i = 0; i < crate.rewards().size(); i++) {
            Reward reward = crate.rewards().get(i);
            String rp = path + ".rewards.r" + i;
            cratesConfig.set(rp + ".id", reward.id());
            cratesConfig.set(rp + ".chance", reward.chance());
            cratesConfig.set(rp + ".display-name", reward.displayName());
            cratesConfig.set(rp + ".display-item", reward.displayItem());
            cratesConfig.set(rp + ".commands", reward.commands().isEmpty() ? null : reward.commands());
            cratesConfig.set(rp + ".item", reward.item());
            cratesConfig.set(rp + ".money", reward.money() > 0 ? reward.money() : null);
            cratesConfig.set(rp + ".permissions", reward.permissions().isEmpty() ? null : reward.permissions());
            cratesConfig.set(rp + ".messages", reward.messages().isEmpty() ? null : reward.messages());
            cratesConfig.set(rp + ".broadcasts", reward.broadcasts().isEmpty() ? null : reward.broadcasts());
        }

        saveCratesFile();
    }

    @Override
    public void deleteCrate(@NotNull String crateId) {
        cratesConfig.set("crates." + crateId, null);
        saveCratesFile();
    }

    @Override
    @NotNull
    public Collection<Crate> loadAllCrates() {
        List<Crate> crates = new ArrayList<>();
        ConfigurationSection section = cratesConfig.getConfigurationSection("crates");
        if (section == null) return crates;

        for (String id : section.getKeys(false)) {
            try {
                Crate crate = new Crate(id);
                crate.displayName(section.getString(id + ".display-name", id));

                if (section.contains(id + ".display-item")) {
                    crate.displayItem((ItemStack) section.get(id + ".display-item"));
                }

                String typeStr = section.getString(id + ".type", "DEFAULT");
                try {
                    crate.type(CrateType.valueOf(typeStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    crate.type(CrateType.DEFAULT);
                }

                crate.animationId(section.getString(id + ".animation", "instant"));
                crate.keyId(section.getString(id + ".key-id", id));
                crate.permission(section.getString(id + ".permission"));
                crate.requireKey(section.getBoolean(id + ".require-key", true));
                crate.cooldownSeconds(section.getInt(id + ".cooldown-seconds", 0));

                String locStr = section.getString(id + ".location");
                if (locStr != null && !locStr.isEmpty()) {
                    try {
                        crate.location(CrateLocation.deserialize(locStr));
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                ConfigurationSection rewardsSection = section.getConfigurationSection(id + ".rewards");
                if (rewardsSection != null) {
                    for (String key : rewardsSection.getKeys(false)) {
                        try {
                            Reward reward = new Reward(rewardsSection.getString(key + ".id", key));
                            reward.chance(rewardsSection.getDouble(key + ".chance", 10.0));
                            reward.displayName(rewardsSection.getString(key + ".display-name", reward.id()));

                            if (rewardsSection.contains(key + ".display-item")) {
                                reward.displayItem((ItemStack) rewardsSection.get(key + ".display-item"));
                            }

                            reward.commands(rewardsSection.getStringList(key + ".commands"));

                            if (rewardsSection.contains(key + ".item")) {
                                reward.item((ItemStack) rewardsSection.get(key + ".item"));
                            }

                            reward.money(rewardsSection.getDouble(key + ".money", 0));
                            reward.permissions(rewardsSection.getStringList(key + ".permissions"));
                            reward.messages(rewardsSection.getStringList(key + ".messages"));
                            reward.broadcasts(rewardsSection.getStringList(key + ".broadcasts"));

                            crate.addReward(reward);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load reward " + key + " in crate " + id);
                        }
                    }
                }

                crates.add(crate);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load crate " + id + ": " + e.getMessage());
            }
        }

        return crates;
    }

    @Override
    public int getKeys(@NotNull UUID playerId, @NotNull String crateId) {
        return loadPlayerKeys(playerId).getOrDefault(crateId.toLowerCase(), 0);
    }

    @Override
    public void setKeys(@NotNull UUID playerId, @NotNull String crateId, int amount) {
        Map<String, Integer> keys = loadPlayerKeys(playerId);
        keys.put(crateId.toLowerCase(), Math.max(0, amount));
        savePlayerKeys(playerId, keys);
    }

    @Override
    public void addKeys(@NotNull UUID playerId, @NotNull String crateId, int amount) {
        Map<String, Integer> keys = loadPlayerKeys(playerId);
        keys.merge(crateId.toLowerCase(), Math.max(0, amount), Integer::sum);
        savePlayerKeys(playerId, keys);
    }

    @Override
    public void removeKeys(@NotNull UUID playerId, @NotNull String crateId, int amount) {
        Map<String, Integer> keys = loadPlayerKeys(playerId);
        String key = crateId.toLowerCase();
        int current = keys.getOrDefault(key, 0);
        int newAmount = Math.max(0, current - amount);
        if (newAmount == 0) {
            keys.remove(key);
        } else {
            keys.put(key, newAmount);
        }
        savePlayerKeys(playerId, keys);
    }

    @Override
    @NotNull
    public Map<String, Integer> getAllKeys(@NotNull UUID playerId) {
        return new HashMap<>(loadPlayerKeys(playerId));
    }

    @Override
    public void saveAll() {
        saveCratesFile();
    }

    @Override
    public void reload() {
        this.cratesConfig = YamlConfiguration.loadConfiguration(cratesFile);
    }

    private void saveCratesFile() {
        try {
            cratesConfig.save(cratesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crates.yml: " + e.getMessage());
        }
    }

    private Map<String, Integer> loadPlayerKeys(UUID playerId) {
        Map<String, Integer> cached = playerKeysCache.get(playerId);
        if (cached != null) return cached;

        File file = new File(keysDir, playerId + ".yml");
        if (!file.exists()) {
            Map<String, Integer> empty = new HashMap<>();
            playerKeysCache.put(playerId, empty);
            return empty;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, Integer> keys = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("keys");
        if (section != null) {
            for (String crateId : section.getKeys(false)) {
                keys.put(crateId, section.getInt(crateId, 0));
            }
        }

        playerKeysCache.put(playerId, keys);
        return keys;
    }

    private void savePlayerKeys(UUID playerId, Map<String, Integer> keys) {
        File file = new File(keysDir, playerId + ".yml");
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, Integer> entry : keys.entrySet()) {
            if (entry.getValue() > 0) {
                config.set("keys." + entry.getKey(), entry.getValue());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save keys for " + playerId + ": " + e.getMessage());
        }
    }

    public void unloadPlayer(UUID playerId) {
        playerKeysCache.remove(playerId);
    }
}
