package com.ultra.uskill.listener;

import com.ultra.uskill.USkillPlugin;
import com.ultra.uskill.xp.DailyCapTracker;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;

/**
 * Xử lý tất cả nguồn kiếm SP ngoài mob kill:
 *  - Mining (phá ore)
 *  - Farming (thu hoạch cây trồng chín)
 *  - Fishing (câu cá)
 *  - Enchanting (bàn enchant)
 *  - Level Up (tăng level Minecraft)
 *
 * Mỗi nguồn có daily cap riêng, config trong config.yml → skill-xp.
 */
public class SkillXpListener implements Listener {

    private static final Set<Material> FISH_ITEMS = Set.of(
            Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH
    );
    private static final Set<Material> TREASURE_ITEMS = Set.of(
            Material.BOW, Material.ENCHANTED_BOOK, Material.FISHING_ROD,
            Material.NAME_TAG, Material.NAUTILUS_SHELL, Material.SADDLE
    );

    private final USkillPlugin plugin;
    private final DailyCapTracker caps;

    public SkillXpListener(USkillPlugin plugin, DailyCapTracker caps) {
        this.plugin = plugin;
        this.caps   = caps;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Mining & Farming — BlockBreakEvent
    // ═════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block  block  = event.getBlock();
        String mat    = block.getType().name();

        // ── Mining ──────────────────────────────────────────────────────────
        ConfigurationSection mining = plugin.getConfig().getConfigurationSection("skill-xp.mining");
        if (mining != null && mining.getBoolean("enabled", true)
                && mining.contains("blocks." + mat)) {
            int sp = mining.getInt("blocks." + mat, 0);
            if (sp > 0) {
                int cap    = plugin.getConfig().getInt("skill-xp.daily-caps.mining", 50);
                int actual = caps.tryEarn(player.getUniqueId(), "mining", sp, cap);
                if (actual > 0) {
                    award(player, actual, mining.getString("notify-color", "<yellow>"),
                            "⛏ ", fmtName(mat), "mining", cap);
                }
            }
            return; // ore ≠ crop → no need to check farming
        }

        // ── Farming ─────────────────────────────────────────────────────────
        ConfigurationSection farming = plugin.getConfig().getConfigurationSection("skill-xp.farming");
        if (farming == null || !farming.getBoolean("enabled", true)) return;
        if (!farming.contains("crops." + mat)) return;

        // Check fully grown (Ageable blocks)
        if (farming.getBoolean("require-fully-grown", true)) {
            if (block.getBlockData() instanceof Ageable ageable) {
                if (ageable.getAge() < ageable.getMaximumAge()) return;
            }
        }

        int sp = farming.getInt("crops." + mat, 0);
        if (sp <= 0) return;

        int cap    = plugin.getConfig().getInt("skill-xp.daily-caps.farming", 30);
        int actual = caps.tryEarn(player.getUniqueId(), "farming", sp, cap);
        if (actual > 0) {
            award(player, actual, farming.getString("notify-color", "<green>"),
                    "🌾 ", fmtName(mat), "farming", cap);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Fishing
    // ═════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("skill-xp.fishing");
        if (sec == null || !sec.getBoolean("enabled", true)) return;

        if (!(event.getCaught() instanceof org.bukkit.entity.Item itemEntity)) return;
        ItemStack caught = itemEntity.getItemStack();
        Material  type   = caught.getType();

        int    sp;
        String category;
        if (FISH_ITEMS.contains(type)) {
            sp = sec.getInt("fish", 1);  category = "Cá";
        } else if (TREASURE_ITEMS.contains(type)) {
            sp = sec.getInt("treasure", 3); category = "Kho Báu";
        } else {
            sp = sec.getInt("junk", 0);  category = "Rác";
        }
        if (sp <= 0) return;

        UUID playerId = event.getPlayer().getUniqueId();
        int  cap      = plugin.getConfig().getInt("skill-xp.daily-caps.fishing", 20);
        int  actual   = caps.tryEarn(playerId, "fishing", sp, cap);
        if (actual <= 0) return;

        award(event.getPlayer(), actual, sec.getString("notify-color", "<aqua>"),
                "🎣 ", category, "fishing", cap);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Enchanting
    // ═════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("skill-xp.enchanting");
        if (sec == null || !sec.getBoolean("enabled", true)) return;

        int levelCost = event.getExpLevelCost();
        int spPer5    = Math.max(1, sec.getInt("sp-per-5-levels", 1));
        int sp        = Math.max(1, (levelCost / 5) * spPer5);

        UUID playerId = event.getEnchanter().getUniqueId();
        int  cap      = plugin.getConfig().getInt("skill-xp.daily-caps.enchanting", 30);
        int  actual   = caps.tryEarn(playerId, "enchanting", sp, cap);
        if (actual <= 0) return;

        award(event.getEnchanter(), actual, sec.getString("notify-color", "<light_purple>"),
                "✨ ", "Enchant (Lv." + levelCost + ")", "enchanting", cap);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Level Up Minecraft
    // ═════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelUp(PlayerLevelChangeEvent event) {
        if (event.getNewLevel() <= event.getOldLevel()) return; // mất level khi chết

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("skill-xp.levelup");
        if (sec == null || !sec.getBoolean("enabled", true)) return;

        int spPerLevel  = Math.max(1, sec.getInt("sp-per-level", 1));
        int levelsGained = event.getNewLevel() - event.getOldLevel();
        int sp           = spPerLevel * levelsGained;

        UUID playerId = event.getPlayer().getUniqueId();
        int  cap      = plugin.getConfig().getInt("skill-xp.daily-caps.levelup", 50);
        int  actual   = caps.tryEarn(playerId, "levelup", sp, cap);
        if (actual <= 0) return;

        award(event.getPlayer(), actual, sec.getString("notify-color", "<green>"),
                "⬆ ", "Level " + event.getNewLevel(), "levelup", cap);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Shared award helper
    // ═════════════════════════════════════════════════════════════════════════

    private void award(Player player, int sp, String color, String icon,
                       String source, String capKey, int cap) {
        plugin.getSkillTreeManager().givePointsQuiet(player, sp);

        int todayTotal = caps.getEarned(player.getUniqueId(), capKey);
        String capInfo = cap > 0
                ? " <dark_gray>(" + todayTotal + "<dark_gray>/<white>" + cap + " hôm nay)"
                : "";
        String msg = color + icon + "+" + sp + " SP <dark_gray>│ <gray>" + source + capInfo;

        var mm = plugin.getManaManager();
        if (mm != null) {
            mm.showNotification(player, msg, 40);
        } else {
            player.sendActionBar(com.ultra.ucore.util.TextUtil.parse(msg));
        }
    }

    private static String fmtName(String raw) {
        StringBuilder sb = new StringBuilder();
        for (String w : raw.toLowerCase().replace("_", " ").split(" ")) {
            if (!w.isEmpty())
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
