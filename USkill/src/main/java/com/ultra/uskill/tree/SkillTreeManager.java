package com.ultra.uskill.tree;

import com.ultra.ucore.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkillTreeManager {

    private final JavaPlugin plugin;
    private final Map<String, SkillTreeNode> nodes = new LinkedHashMap<>();
    private final Map<UUID, PlayerSkillData> playerData = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private File configFile;
    private FileConfiguration config;

    public SkillTreeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveResource("skill_tree.yml", false);
        this.configFile = new File(plugin.getDataFolder(), "skill_tree.yml");
        reload();
    }

    @SuppressWarnings("unchecked")
    public void reload() {
        nodes.clear();
        this.config = YamlConfiguration.loadConfiguration(configFile);

        ConfigurationSection skillsSection = config.getConfigurationSection("skills");
        if (skillsSection != null) {
            for (String key : skillsSection.getKeys(false)) {
                Object raw = skillsSection.get(key);
                if (raw instanceof Map<?, ?> map) {
                    SkillTreeNode node = SkillTreeNode.fromConfig(key, (Map<String, Object>) map);
                    nodes.put(key, node);
                }
            }
        }

        plugin.getLogger().info("Loaded " + nodes.size() + " skill tree nodes. Default points: " + getDefaultPoints());

        for (Player online : Bukkit.getOnlinePlayers()) {
            recalcStats(online);
        }
    }

    public SkillTreeNode getNode(String id) {
        return nodes.get(id);
    }

    public Collection<SkillTreeNode> getAllNodes() {
        return nodes.values();
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getDefaultPoints() {
        return config.getInt("default-points", 5);
    }

    public PlayerSkillData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, pid -> {
            PlayerSkillData data = loadFromStorage(pid);
            if (data.getSkillPoints() <= 0 && data.getTotalSpent() <= 0) {
                data.addSkillPoints(getDefaultPoints());
            }
            return data;
        });
    }

    public PlayerStats getPlayerStats(UUID playerId) {
        return playerStats.computeIfAbsent(playerId, pid -> new PlayerStats());
    }

    public boolean upgradeSkill(Player player, String skillId) {
        SkillTreeNode node = getNode(skillId);
        if (node == null) {
            player.sendMessage(TextUtil.parse("<red>Skill not found!</red>"));
            return false;
        }

        PlayerSkillData data = getPlayerData(player.getUniqueId());
        int currentLevel = data.getSkillLevel(skillId);
        if (currentLevel >= node.getMaxLevel()) {
            player.sendMessage(TextUtil.parse("<red>This skill is already max level!</red>"));
            return false;
        }

        int nextLevel = currentLevel;
        int cost = node.getCost(nextLevel);

        for (String prereqId : node.getPrerequisites()) {
            SkillTreeNode prereq = getNode(prereqId);
            if (prereq == null) continue;
            int prereqLevel = data.getSkillLevel(prereqId);
            if (prereqLevel < prereq.getMaxLevel()) {
                player.sendMessage(TextUtil.parse("<red>You need to max out <gold>" + prereq.getDisplayName() + " <red>first!</red>"));
                return false;
            }
        }

        if (!data.spendPoints(cost)) {
            player.sendMessage(TextUtil.parse("<red>Not enough skill points! Need " + cost + " points.</red>"));
            return false;
        }

        data.setSkillLevel(skillId, currentLevel + 1);
        savePlayerData(player.getUniqueId(), data);
        recalcStats(player);

        player.sendMessage(TextUtil.parse("<green>✦ Upgraded <gold>" + node.getDisplayName() + " <green>to Level " + (currentLevel + 1) + "!</green>"));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        return true;
    }

    public boolean resetSkill(Player player, String skillId) {
        SkillTreeNode node = getNode(skillId);
        if (node == null) return false;

        PlayerSkillData data = getPlayerData(player.getUniqueId());
        int currentLevel = data.getSkillLevel(skillId);
        if (currentLevel <= 0) return false;

        int refund = 0;
        for (int i = 0; i < currentLevel; i++) {
            refund += node.getCost(i);
        }
        data.setSkillLevel(skillId, 0);
        data.addSkillPoints(refund);
        savePlayerData(player.getUniqueId(), data);
        recalcStats(player);

        player.sendMessage(TextUtil.parse("<yellow>↺ Reset <gold>" + node.getDisplayName() + " <yellow>refunded <green>" + refund + " <yellow>points.</yellow>"));
        return true;
    }

    public boolean resetAll(Player player) {
        PlayerSkillData data = getPlayerData(player.getUniqueId());
        int refund = 0;
        for (Map.Entry<String, Integer> entry : new ArrayList<>(data.getSkillLevels().entrySet())) {
            SkillTreeNode node = getNode(entry.getKey());
            if (node == null) continue;
            for (int i = 0; i < entry.getValue(); i++) {
                refund += node.getCost(i);
            }
            data.setSkillLevel(entry.getKey(), 0);
        }
        data.addSkillPoints(refund);
        data.setTotalSpent(0);
        savePlayerData(player.getUniqueId(), data);
        recalcStats(player);

        player.sendMessage(TextUtil.parse("<yellow>↺ Reset all skills! Refunded <green>" + refund + " <yellow>points.</yellow>"));
        return true;
    }

    public void recalcStats(Player player) {
        if (player == null) return;
        UUID pid = player.getUniqueId();
        PlayerSkillData data = getPlayerData(pid);
        PlayerStats oldStats = playerStats.get(pid);
        if (oldStats != null) oldStats.removeFromPlayer(player);

        PlayerStats newStats = PlayerStats.calculate(data, this);
        playerStats.put(pid, newStats);
        newStats.applyToPlayer(player);

        com.ultra.uskill.USkillPlugin uskill = com.ultra.uskill.USkillPlugin.getInstance();
        if (uskill.getManaManager() != null) {
            uskill.getManaManager().invalidateMaxMana(player);
        }
    }

    public void onPlayerJoin(Player player) {
        getPlayerData(player.getUniqueId());
        recalcStats(player);
    }

    public void onPlayerQuit(Player player) {
        UUID pid = player.getUniqueId();
        PlayerStats stats = playerStats.remove(pid);
        if (stats != null) stats.removeFromPlayer(player);
        PlayerSkillData data = playerData.remove(pid);
        if (data != null) savePlayerData(pid, data);
    }

    public void givePoints(Player player, int amount) {
        PlayerSkillData data = getPlayerData(player.getUniqueId());
        data.addSkillPoints(amount);
        savePlayerData(player.getUniqueId(), data);
        player.sendMessage(TextUtil.parse("<green>+ " + amount + " Skill Points!</green>"));
    }

    public void givePointsAll(int amount) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            givePoints(online, amount);
        }
    }

    private PlayerSkillData loadFromStorage(UUID playerId) {
        String path = "players." + playerId.toString();
        if (config.contains(path)) {
            Object raw = config.get(path);
            if (raw instanceof Map<?, ?> map) {
                return PlayerSkillData.deserialize(playerId, (Map<String, Object>) map);
            }
        }
        return new PlayerSkillData(playerId);
    }

    private void savePlayerData(UUID playerId, PlayerSkillData data) {
        config.set("players." + playerId.toString(), data.serialize());
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save skill tree data for " + playerId + ": " + e.getMessage());
        }
    }
}
