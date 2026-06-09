package com.uitem.legacy.skill;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.api.UItemAPI;
import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.skill.Skill;
import com.ultra.ucore.skill.SkillService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SkillActionBar {

    private final UItemPlugin plugin;
    private final SkillService skillService;
    private ScheduledTask task;

    public SkillActionBar(UItemPlugin plugin) {
        this.plugin = plugin;
        this.skillService = UCoreAPI.getSkillService();
    }

    public void start() {
        if (task != null) task.cancel();
        task = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> tick(), 40L, 20L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                List<CooldownEntry> entries = new ArrayList<>();

                checkItem(player, player.getInventory().getItemInMainHand(), entries);
                checkItem(player, player.getInventory().getItemInOffHand(), entries);

                if (entries.isEmpty()) continue;

                entries.sort(Comparator.comparingDouble(e -> e.remaining));

                int maxShow = Math.min(entries.size(), 3);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < maxShow; i++) {
                    CooldownEntry e = entries.get(i);
                    if (sb.length() > 0) sb.append("  ");
                    String color = e.remaining > 5 ? "<green>" : e.remaining > 2 ? "<yellow>" : "<red>";
                    sb.append(color).append(e.displayName).append(" <white>")
                            .append(String.format("%.1f", e.remaining)).append("s");
                }

                player.sendActionBar(com.uitem.util.TextUtil.parse(sb.toString()));
            } catch (Exception ignored) {
            }
        }
    }

    private void checkItem(Player player, ItemStack item, List<CooldownEntry> entries) {
        if (!UItemAPI.isAvailable()) return;
        UItemData data = UItemAPI.get().getUItemData(item).orElse(null);
        if (data == null) return;
        List<Skill> skills = SkillDataHelper.getSkills(data);
        if (skills.isEmpty()) return;
        String itemId = data.getId();

        for (Skill skill : skills) {
            if (skill.getCooldown() <= 0) continue;
            double remaining = skillService.getRemainingSeconds(player, itemId, skill.getId(), skill.getCooldown());
            if (remaining <= 0) continue;

            double cooldown = skill.getCooldown();
            String display = skill.getDisplayName() != null ? skill.getDisplayName() : skill.getId();

            entries.add(new CooldownEntry(display, remaining, cooldown));
        }
    }

    private record CooldownEntry(String displayName, double remaining, double cooldown) {
    }
}
