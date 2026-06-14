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
    // Pending action bar notifications (override mana HUD for N ticks)
    private final Map<UUID, PendingNotification> pendingNotifications = new ConcurrentHashMap<>();
    private int regenTaskId = -1;
    private int hudTaskId = -1;

    public ManaManager(JavaPlugin plugin, SkillTreeManager skillTreeManager) {
        this.plugin = plugin;
        this.skillTreeManager = skillTreeManager;
    }

    public void start() {
        long regenInterval = plugin.getConfig().getInt("mana.regen-interval-ticks", 20);
        long hudInterval = plugin.getConfig().getInt("mana.hud-interval-ticks", 20);

        this.regenTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tickRegen, regenInterval, regenInterval).getTaskId();
        this.hudTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tickHud, 40L, hudInterval).getTaskId();
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

    public boolean hasManaStats(Player player) {
        // Luôn hiển thị mana bar nếu base-max > 0 (tất cả player đều có mana pool cơ bản)
        double baseMax = plugin.getConfig().getDouble("mana.base-max", 20);
        return baseMax > 0;
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

    public void refillMana(Player player) {
        setMana(player, getMaxMana(player));
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
            if (!plugin.getConfig().getBoolean("mana.hud-enabled", true)) continue;

            UUID pid = player.getUniqueId();

            // Pending notification overrides mana HUD for its duration
            PendingNotification notif = pendingNotifications.get(pid);
            if (notif != null) {
                if (notif.expired()) {
                    pendingNotifications.remove(pid);
                } else {
                    player.sendActionBar(notif.component());
                    pendingNotifications.put(pid, notif.tick());
                    continue;
                }
            }

            if (!hasManaStats(player)) continue;
            sendManaHud(player);
        }
    }

    private void sendManaHud(Player player) {
        double current = getMana(player);
        double max = getMaxMana(player);
        if (max <= 0) return;

        int barLength = plugin.getConfig().getInt("mana.bar-length", 10);
        int filled = Math.max(0, Math.min(barLength, (int) Math.round((current / max) * barLength)));
        int empty = barLength - filled;

        String fillChar  = plugin.getConfig().getString("mana.fill-char",   "█");
        String emptyChar = plugin.getConfig().getString("mana.empty-char",  "░");
        String manaColor  = plugin.getConfig().getString("mana.color",       "<aqua>");
        String emptyColor = plugin.getConfig().getString("mana.empty-color", "<dark_gray>");

        String bar  = manaColor + fillChar.repeat(filled) + emptyColor + emptyChar.repeat(empty);
        int percent = (int) ((current / max) * 100);

        // Neo góc trái bằng cách đặt text trước rồi padding spaces rất dài ở sau.
        // sendActionBar() căn giữa chuỗi, nên nếu ta thêm trailing spaces dài hơn
        // nội dung thực, trung tâm của chuỗi dịch về bên phải → text thực xuất
        // hiện gần góc trái màn hình. Số spaces tối ưu khoảng 60-80 ký tự.
        String content = "<dark_aqua>✦ Mana " + bar + " <gray>" + (int) current + "/" + (int) max;
        // Trailing padding đẩy nội dung về bên trái (config: mana.hud-padding)
        int padding = plugin.getConfig().getInt("mana.hud-padding", 60);
        String padded = content + " ".repeat(Math.max(0, padding));

        var component = com.ultra.ucore.util.TextUtil.parse(padded);
        player.sendActionBar(component);
    }

    public void onPlayerQuit(Player player) {
        UUID pid = player.getUniqueId();
        mana.remove(pid);
        maxManaCache.remove(pid);
        pendingNotifications.remove(pid);
    }

    /**
     * Hiển thị thông báo tạm thời thay cho mana HUD (dùng cho mob XP, skill feedback...).
     * Leading spaces đẩy nội dung sang phải — đối xứng với mana HUD ở góc trái.
     * @param player  người nhận
     * @param message MiniMessage string
     * @param ticks   số tick hiển thị (20 ticks = 1 giây)
     */
    public void showNotification(Player player, String message, int ticks) {
        int padding = plugin.getConfig().getInt("mana.notification-padding", 60);
        String padded = " ".repeat(Math.max(0, padding)) + message;
        pendingNotifications.put(player.getUniqueId(),
                new PendingNotification(com.ultra.ucore.util.TextUtil.parse(padded), ticks));
    }

    private record PendingNotification(net.kyori.adventure.text.Component component, int remaining) {
        PendingNotification tick() { return new PendingNotification(component, remaining - 1); }
        boolean expired() { return remaining <= 0; }
    }
}
