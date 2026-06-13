package com.ultra.uskill.listener;

import com.ultra.ucore.skill.Skill;
import com.ultra.ucore.skill.SkillTrigger;
import com.ultra.uskill.USkillPlugin;
import com.ultra.uskill.execution.SkillExecutor;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges UItem events → SkillExecutor.
 * Loaded only when UItem is present (soft-depend).
 */
public class ItemSkillListener implements Listener {

    private final USkillPlugin plugin;
    private final SkillExecutor executor;

    // Cache parsed skills per UItem ID to avoid re-parsing on every event
    private final Map<String, List<Skill>> skillCache = new ConcurrentHashMap<>();

    private BukkitTask periodicTask;

    public ItemSkillListener(USkillPlugin plugin, SkillExecutor executor) {
        this.plugin = plugin;
        this.executor = executor;
    }

    // ─── Cache ──────────────────────────────────────────────────────────────

    public void invalidateCache() {
        skillCache.clear();
    }

    @SuppressWarnings("unchecked")
    private List<Skill> getSkills(com.uitem.UItemData data) {
        return skillCache.computeIfAbsent(data.getId(), id -> {
            var raw = data.getSkillsRaw();
            if (raw == null || raw.isEmpty()) return List.of();
            return plugin.parseSkills(raw);
        });
    }

    // ─── Interact: RIGHT_CLICK / LEFT_CLICK / SHIFT variants ───────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(com.uitem.api.events.UItemInteractEvent event) {
        Player player = event.getPlayer();
        com.uitem.UItemData data = event.getItemData();
        List<Skill> skills = getSkills(data);
        if (skills.isEmpty()) return;

        boolean shift = player.isSneaking();
        SkillTrigger trigger;
        if (event.isRightClick()) {
            trigger = shift ? SkillTrigger.SHIFT_RIGHT_CLICK : SkillTrigger.RIGHT_CLICK;
        } else {
            trigger = shift ? SkillTrigger.SHIFT_LEFT_CLICK : SkillTrigger.LEFT_CLICK;
        }

        boolean executed = executor.executeSkills(player, data.getId(), skills, trigger, null);
        if (executed && hasCancelSkill(skills, trigger)) {
            event.setCancelled(true);
        }
    }

    // ─── BREAK_BLOCK ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isUItemAvailable()) return;
        Player player = event.getPlayer();
        com.uitem.UItemData data = getUItemData(player.getInventory().getItemInMainHand());
        if (data == null) return;

        List<Skill> skills = getSkills(data);
        if (skills.isEmpty()) return;

        executor.executeSkills(player, data.getId(), skills, SkillTrigger.BREAK_BLOCK, null);
    }

    // ─── Drop (Q key) ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!isUItemAvailable()) return;
        Player player = event.getPlayer();
        ItemStack stack = event.getItemDrop().getItemStack();

        com.uitem.UItemData data = getUItemData(stack);
        if (data == null) return;

        List<Skill> skills = getSkills(data);
        if (skills.isEmpty()) return;

        boolean executed = executor.executeSkills(player, data.getId(), skills, SkillTrigger.DROP, null);
        if (executed && hasCancelSkill(skills, SkillTrigger.DROP)) {
            event.setCancelled(true);
        }
    }

    // ─── Swap hand (F key) ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (!isUItemAvailable()) return;
        Player player = event.getPlayer();

        // Try both main and off-hand item
        com.uitem.UItemData data = getUItemData(event.getMainHandItem());
        if (data == null) data = getUItemData(event.getOffHandItem());
        if (data == null) return;

        List<Skill> skills = getSkills(data);
        if (skills.isEmpty()) return;

        boolean executed = executor.executeSkills(player, data.getId(), skills, SkillTrigger.SWAP_HAND, null);
        if (executed && hasCancelSkill(skills, SkillTrigger.SWAP_HAND)) {
            event.setCancelled(true);
        }
    }

    // ─── HIT (attacker) + TAKE_DAMAGE (victim) ──────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!isUItemAvailable()) return;

        // HIT: player attacks with a UItem in hand
        if (event.getDamager() instanceof Player attacker) {
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            com.uitem.UItemData data = getUItemData(weapon);
            if (data != null) {
                List<Skill> skills = getSkills(data);
                LivingEntity victim = (event.getEntity() instanceof LivingEntity le) ? le : null;
                executor.executeSkills(attacker, data.getId(), skills, SkillTrigger.HIT, victim);
            }
        }

        // TAKE_DAMAGE: player takes damage while holding/wearing a UItem
        if (event.getEntity() instanceof Player victim) {
            LivingEntity attacker = (event.getDamager() instanceof LivingEntity le) ? le : null;

            // Check main hand
            com.uitem.UItemData handData = getUItemData(victim.getInventory().getItemInMainHand());
            if (handData != null) {
                List<Skill> skills = getSkills(handData);
                executor.executeSkills(victim, handData.getId(), skills, SkillTrigger.TAKE_DAMAGE, attacker);
            }

            // Check armor pieces
            for (ItemStack armor : victim.getInventory().getArmorContents()) {
                if (armor == null || armor.getType().isAir()) continue;
                com.uitem.UItemData armorData = getUItemData(armor);
                if (armorData == null) continue;
                List<Skill> skills = getSkills(armorData);
                executor.executeSkills(victim, armorData.getId(), skills, SkillTrigger.TAKE_DAMAGE, attacker);
            }
        }
    }

    // ─── EQUIP / UNEQUIP ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEquip(com.uitem.api.events.UItemEquipEvent event) {
        Player player = event.getPlayer();
        com.uitem.UItemData data = event.getItemData();
        List<Skill> skills = getSkills(data);
        if (skills.isEmpty()) return;

        SkillTrigger trigger = event.isEquipping() ? SkillTrigger.EQUIP : SkillTrigger.UNEQUIP;
        executor.executeSkills(player, data.getId(), skills, trigger, null);
    }

    // ─── Periodic task: PERIODIC / WHILE_HOLDING / WHILE_WEARING ─────────

    public void startPeriodicTask() {
        if (periodicTask != null) periodicTask.cancel();
        periodicTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 20L, 20L);
    }

    public void stopPeriodicTask() {
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
    }

    private void tickAll() {
        if (!isUItemAvailable()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            tickPlayer(player);
        }
    }

    private void tickPlayer(Player player) {
        // WHILE_HOLDING + PERIODIC — main hand
        ItemStack hand = player.getInventory().getItemInMainHand();
        com.uitem.UItemData handData = getUItemData(hand);
        if (handData != null) {
            List<Skill> skills = getSkills(handData);
            executor.tickPeriodicSkills(player, handData.getId(), skills, SkillTrigger.WHILE_HOLDING);
            executor.tickPeriodicSkills(player, handData.getId(), skills, SkillTrigger.PERIODIC);
        }

        // WHILE_WEARING + PERIODIC — armor slots
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null || armor.getType().isAir()) continue;
            com.uitem.UItemData armorData = getUItemData(armor);
            if (armorData == null) continue;
            List<Skill> skills = getSkills(armorData);
            executor.tickPeriodicSkills(player, armorData.getId(), skills, SkillTrigger.WHILE_WEARING);
            executor.tickPeriodicSkills(player, armorData.getId(), skills, SkillTrigger.PERIODIC);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean isUItemAvailable() {
        return com.uitem.api.UItemAPI.isAvailable();
    }

    private com.uitem.UItemData getUItemData(ItemStack item) {
        if (!isUItemAvailable()) return null;
        if (item == null || item.getType().isAir()) return null;
        return com.uitem.api.UItemAPI.get().getUItemData(item).orElse(null);
    }

    private boolean hasCancelSkill(List<Skill> skills, SkillTrigger trigger) {
        for (Skill s : skills) {
            if (s.getTrigger() == trigger && s.isCancelEvent()) return true;
        }
        return false;
    }
}
