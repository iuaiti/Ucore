package com.ultra.uskill.listener;

import com.ultra.uskill.USkillPlugin;
import com.ultra.uskill.mana.ManaManager;
import com.ultra.uskill.tree.SkillTreeManager;
import com.ultra.uskill.xp.DailyCapTracker;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Tặng Skill Points khi player giết quái vật.
 * Config: skill_tree.yml → mob-xp (SP per mob type).
 *        config.yml     → skill-xp.daily-caps.mob (daily cap).
 */
public class MobXpListener implements Listener {

    private final USkillPlugin    plugin;
    private final DailyCapTracker caps;

    public MobXpListener(USkillPlugin plugin, DailyCapTracker caps) {
        this.plugin = plugin;
        this.caps   = caps;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player player)) return;

        SkillTreeManager stm = plugin.getSkillTreeManager();
        if (stm == null) return;

        ConfigurationSection mobXp = stm.getMobXpSection();
        if (mobXp == null || !mobXp.getBoolean("enabled", true)) return;

        String entityName = event.getEntity().getType().name();
        int sp = mobXp.getInt("values." + entityName, mobXp.getInt("default", 0));
        if (sp <= 0) return;

        // Daily cap check
        int cap    = plugin.getConfig().getInt("skill-xp.daily-caps.mob", 100);
        int actual = caps.tryEarn(player.getUniqueId(), "mob", sp, cap);
        if (actual <= 0) return;

        stm.givePointsQuiet(player, actual);

        // Action bar notification
        String color   = mobXp.getString("notify-color", "<gold>");
        int todayEarned = caps.getEarned(player.getUniqueId(), "mob");
        String capInfo  = cap > 0
                ? " <dark_gray>(" + todayEarned + "<dark_gray>/<white>" + cap + " hôm nay)"
                : "";
        String msg = color + "⚔ +" + actual + " SP <dark_gray>│ <gray>" + fmtMobName(entityName) + capInfo;

        ManaManager mm = plugin.getManaManager();
        if (mm != null) {
            mm.showNotification(player, msg, 40);
        } else {
            player.sendActionBar(com.ultra.ucore.util.TextUtil.parse(msg));
        }
    }

    private static String fmtMobName(String raw) {
        StringBuilder sb = new StringBuilder();
        for (String w : raw.toLowerCase().replace("_", " ").split(" ")) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
