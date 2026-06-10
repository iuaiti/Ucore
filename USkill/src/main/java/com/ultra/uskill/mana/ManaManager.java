package com.ultra.uskill.mana;

import com.ultra.uskill.tree.PlayerStats;
import com.ultra.uskill.tree.SkillTreeManager;
import com.ultra.uskill.tree.StatType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ManaManager {

    private final JavaPlugin plugin;
    private final SkillTreeManager skillTreeManager;
    private final Map<UUID, Double> mana = new ConcurrentHashMap<>();
    private final Map<UUID, Double> maxManaCache = new ConcurrentHashMap<>();
    private int regenTaskId = -1;
    private int hudTaskId = -1;

    private static final String HUD_CONFIG_PATH = "mana.hud";

    public ManaManager(JavaPlugin plugin, SkillTreeManager skillTreeManager) {
        this.plugin = plugin;
        this.skillTreeManager = skillTreeManager;
    }

    public void start() {
        plugin.saveResource("skill_tree.yml", false);

        long regenInterval = plugin.getConfig().getInt("mana.regen-interval-ticks", 20);
        long hudInterval = plugin.getConfig().getInt("mana.hud-interval-ticks", 10);

        this.regenTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tickRegen, regenInterval, regenInterval).getTaskId();
        this.hudTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tickHud, 0L, hudInterval).getTaskId();
    }

    public void stop() {
        if (regenTaskId != -1) Bukkit.getScheduler().cancelTask(regenTaskId);
        if (hudTaskId != -1) Bukkit.getScheduler().cancelTask(hudTaskId);
    }

    public double getMaxMana(Player player) {
        return maxManaCache.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlayerStats stats = skillTreeManager.getPlayerStats(uuid);
            double base = plugin.getConfig().getDouble("mana.base-max", 20);
            double fromStats = stats.get(StatType.MANA);
            double max = base + fromStats;
            return Math.max(0, max);
        });
    }

    public void invalidateMaxMana(Player player) {
        maxManaCache.remove(player.getUniqueId());
    }

    public double getMana(Player player) {
        return mana.getOrDefault(player.getUniqueId(), getMaxMana(player));
    }

    public void setMana(Player player, double value) {
        double max = getMaxMana(player);
        mana.put(player.getUniqueId(), Math.max(0, Math.min(max, value)));
    }

    public boolean hasMana(Player player, double amount) {
        return getMana(player) >= amount;
    }

    public boolean consumeMana(Player player, double amount) {
        double current = getMana(player);
        if (current < amount) return false;
        setMana(player, current - amount);
        return true;
    }

    public void restoreMana(Player player, double amount) {
        double current = getMana(player);
        double max = getMaxMana(player);
        setMana(player, Math.min(max, current + amount));
    }

    private void tickRegen() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isValid()) continue;
            PlayerStats stats = skillTreeManager.getPlayerStats(player.getUniqueId());
            double regenPerTick = stats.get(StatType.MANA_REGEN) / 20.0;
            if (regenPerTick > 0) {
                restoreMana(player, regenPerTick);
            }
        }
    }

    private void tickHud() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isValid()) continue;
            if (plugin.getConfig().getBoolean("mana.hud-enabled", true)) {
                sendManaHud(player);
            }
        }
    }

    private void sendManaHud(Player player) {
        double current = getMana(player);
        double max = getMaxMana(player);
        if (max <= 0) return;

        int barLength = plugin.getConfig().getInt("mana.bar-length", 10);
        int filled = (int) Math.round((current / max) * barLength);
        int empty = barLength - filled;

        String fillChar = plugin.getConfig().getString("mana.fill-char", "█");
        String emptyChar = plugin.getConfig().getString("mana.empty-char", "░");
        String manaColor = plugin.getConfig().getString("mana.color", "<aqua>");
        String emptyColor = plugin.getConfig().getString("mana.empty-color", "<dark_gray>");

        String bar = manaColor + fillChar.repeat(Math.max(0, filled))
                   + emptyColor + emptyChar.repeat(Math.max(0, empty));

        int percent = max > 0 ? (int) ((current / max) * 100) : 0;
        String manaText = plugin.getConfig().getString("mana.format", "<dark_aqua>✦ %s <gray>%d/%d <dark_gray>(%d%%)")
                .formatted(bar, (int) current, (int) max, percent);

        String hudFormat = plugin.getConfig().getString("mana.hud-format",
                "<gray>━━━━━━━━━━━━━━" + manaText);

        var component = com.ultra.ucore.util.TextUtil.parse(hudFormat);
        player.sendActionBar(component);
    }

    public void onPlayerQuit(Player player) {
        UUID pid = player.getUniqueId();
        mana.remove(pid);
        maxManaCache.remove(pid);
    }
}
