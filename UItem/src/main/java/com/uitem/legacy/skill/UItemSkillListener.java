package com.uitem.legacy.skill;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.api.UItemAPI;
import com.uitem.api.events.UItemEquipEvent;
import com.uitem.api.events.UItemInteractEvent;
import com.uitem.legacy.skill.SkillDataHelper;
import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.skill.Skill;
import com.ultra.ucore.skill.SkillService;
import com.ultra.ucore.skill.SkillTrigger;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Material;

import java.util.*;

/**
 * Listener chuyên biệt cho hệ thống Skill & Passive native của UItem.
 *
 * Root-cause fix (NPE): skillService được lấy từ UCoreAPI mỗi lần dùng
 * (lazy-resolve) thay vì cache tại constructor. Điều này tránh NPE khi
 * USkill chưa gọi UCoreAPI.registerSkillService() tại thời điểm UItem
 * onEnable() — dù plugin.yml đã khai báo depend: [USkill], đây là lớp
 * bảo vệ thứ 2 phòng mọi thứ tự load bất ngờ.
 */
public class UItemSkillListener implements Listener {

    private final UItemPlugin plugin;

    // KHÔNG cache skillService tại constructor — lazy-resolve mỗi lần dùng
    // để tránh NPE khi USkill chưa register SkillService vào UCoreAPI.
    // Trả về null nếu chưa sẵn sàng; tất cả callers kiểm tra null trước.
    private SkillService getSkillService() {
        try {
            SkillService svc = UCoreAPI.getSkillService();
            return svc; // có thể null nếu UCoreServices.skillService() là null
        } catch (IllegalStateException e) {
            // UCoreAPI chưa initialize (UCore chưa load) — không crash
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // Track item đang cầm để WHILE_HOLDING
    private final Map<UUID, String> lastHeldUItem = new HashMap<>();

    // Reentrancy guard: ngăn DAMAGE action gọi target.damage() → EntityDamageByEntityEvent → vòng lặp vô hạn
    private boolean inDamageProcessing = false;

    // Periodic ticker (chạy mỗi 5 tick ~0.25s)
    private ScheduledTask periodicTask;

    public UItemSkillListener(UItemPlugin plugin) {
        this.plugin = plugin;
        startPeriodicTask();
    }

    private void startPeriodicTask() {
        if (periodicTask != null) periodicTask.cancel();
        periodicTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    tickPlayerPeriodic(p);
                } catch (Exception e) {
                    plugin.getLogger().warning("[Skill] Periodic tick error for " + p.getName() + ": " + e.getMessage());
                }
            }
        }, 20L, 5L);
    }

    public void shutdown() {
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
    }

    // =========================================================
    // Periodic tick
    // =========================================================

    private void tickPlayerPeriodic(Player player) {
        SkillService svc = getSkillService();
        if (svc == null) return; // USkill chưa sẵn sàng — bỏ qua tick này

        checkAndTickHolding(svc, player, player.getInventory().getItemInMainHand(), SkillTrigger.WHILE_HOLDING);
        checkAndTickHolding(svc, player, player.getInventory().getItemInOffHand(), SkillTrigger.WHILE_HOLDING);

        checkAndTickPeriodic(svc, player, player.getInventory().getItemInMainHand());
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && !armor.getType().isAir()) {
                checkAndTickPeriodic(svc, player, armor);
            }
        }
    }

    private void checkAndTickHolding(SkillService svc, Player player, ItemStack item, SkillTrigger trigger) {
        UItemData data = getUItemData(item);
        if (data == null) return;
        List<Skill> skills = SkillDataHelper.getSkills(data);
        if (skills.isEmpty()) return;
        svc.tickPeriodicSkills(player, data.getId(), skills, trigger);
    }

    private void checkAndTickPeriodic(SkillService svc, Player player, ItemStack item) {
        UItemData data = getUItemData(item);
        if (data == null) return;
        List<Skill> skills = SkillDataHelper.getSkills(data);
        if (skills.isEmpty()) return;
        svc.tickPeriodicSkills(player, data.getId(), skills, SkillTrigger.PERIODIC);
    }

    // =========================================================
    // Interact (active skills)
    // =========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onUItemInteract(UItemInteractEvent event) {
        SkillService svc = getSkillService();
        if (svc == null) return;

        Player player = event.getPlayer();
        UItemData data = event.getItemData();
        if (data == null) return;
        List<Skill> skills = SkillDataHelper.getSkills(data);
        if (skills.isEmpty()) return;

        boolean sneaking = player.isSneaking();
        SkillTrigger trigger = switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> sneaking ? SkillTrigger.SHIFT_RIGHT_CLICK : SkillTrigger.RIGHT_CLICK;
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK   -> sneaking ? SkillTrigger.SHIFT_LEFT_CLICK  : SkillTrigger.LEFT_CLICK;
            default -> null;
        };
        if (trigger == null) return;

        boolean executed = svc.executeSkills(player, data.getId(), skills, trigger, null);
        if (executed && shouldCancel(skills, trigger)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        SkillService svc = getSkillService();
        if (svc == null) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        UItemData data = getUItemData(item);
        if (data == null) return;
        List<Skill> skills = SkillDataHelper.getSkills(data);
        if (skills.isEmpty()) return;

        boolean isThrow = event.getItemDrop().getVelocity().lengthSquared() > 0.1;
        SkillTrigger trigger = isThrow ? SkillTrigger.THROW : SkillTrigger.DROP;

        boolean executed = svc.executeSkills(player, data.getId(), skills, trigger, null);
        if (executed && shouldCancel(skills, trigger)) {
            event.setCancelled(true);
        }
    }

    // =========================================================
    // SHOOT: bow / crossbow / trident
    // =========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        SkillService svc = getSkillService();
        if (svc == null) return;
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack bow = event.getBow();
        if (bow == null || bow.getType().isAir()) return;
        UItemData data = getUItemData(bow);
        if (data == null) return;
        List<Skill> skills = SkillDataHelper.getSkills(data);
        if (skills.isEmpty()) return;

        boolean executed = svc.executeSkills(player, data.getId(), skills, SkillTrigger.SHOOT, null);
        if (executed && shouldCancel(skills, SkillTrigger.SHOOT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        SkillService svc = getSkillService();
        if (svc == null) return;
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        UItemData data = getUItemData(weapon);
        if (data == null) {
            weapon = player.getInventory().getItemInOffHand();
            data = getUItemData(weapon);
        }
        if (data == null && event.getEntity() instanceof Trident) {
            ItemStack off = player.getInventory().getItemInOffHand();
            if (off != null && off.getType() == Material.TRIDENT) data = getUItemData(off);
        }
        if (data == null) return;
        List<Skill> skills = SkillDataHelper.getSkills(data);
        if (skills.isEmpty()) return;

        boolean executed = svc.executeSkills(player, data.getId(), skills, SkillTrigger.SHOOT, null);
        if (executed && shouldCancel(skills, SkillTrigger.SHOOT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        SkillService svc = getSkillService();
        if (svc == null) return;

        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        UItemData data = getUItemData(main);
        if (data == null) return;
        List<Skill> skills = SkillDataHelper.getSkills(data);
        if (skills.isEmpty()) return;

        boolean executed = svc.executeSkills(player, data.getId(), skills, SkillTrigger.SWAP_HAND, null);
        if (executed && shouldCancel(skills, SkillTrigger.SWAP_HAND)) {
            event.setCancelled(true);
        }
    }

    // =========================================================
    // BREAK_BLOCK
    // =========================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        SkillService svc = getSkillService();
        if (svc == null) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        UItemData data = getUItemData(tool);
        if (data == null) return;
        List<Skill> skills = SkillDataHelper.getSkills(data);
        if (skills.isEmpty()) return;

        svc.executeSkills(player, data.getId(), skills, SkillTrigger.BREAK_BLOCK, null);
    }

    // =========================================================
    // Combat: HIT + TAKE_DAMAGE
    // =========================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (inDamageProcessing) return;
        SkillService svc = getSkillService();
        if (svc == null) return;

        inDamageProcessing = true;
        try {
            // HIT
            if (event.getDamager() instanceof Player damager) {
                ItemStack weapon = damager.getInventory().getItemInMainHand();
                UItemData data = getUItemData(weapon);
                if (data != null) {
                    List<Skill> skills = SkillDataHelper.getSkills(data);
                    if (!skills.isEmpty()) {
                        LivingEntity victim = (event.getEntity() instanceof LivingEntity le) ? le : null;
                        svc.executeSkills(damager, data.getId(), skills, SkillTrigger.HIT, victim);
                    }
                }
            }

            // TAKE_DAMAGE
            if (event.getEntity() instanceof Player victim) {
                ItemStack held = victim.getInventory().getItemInMainHand();
                UItemData heldData = getUItemData(held);
                if (heldData != null) {
                    List<Skill> sk = SkillDataHelper.getSkills(heldData);
                    if (!sk.isEmpty()) svc.executeSkills(victim, heldData.getId(), sk, SkillTrigger.TAKE_DAMAGE, null);
                }
                for (ItemStack armor : victim.getInventory().getArmorContents()) {
                    if (armor == null || armor.getType().isAir()) continue;
                    UItemData ad = getUItemData(armor);
                    if (ad == null) continue;
                    List<Skill> sk = SkillDataHelper.getSkills(ad);
                    if (!sk.isEmpty()) svc.executeSkills(victim, ad.getId(), sk, SkillTrigger.TAKE_DAMAGE, null);
                }
            }
        } finally {
            inDamageProcessing = false;
        }
    }

    // =========================================================
    // KILL trigger
    // =========================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDamageByEntityEvent event) {
        // Only fire KILL if the entity is about to die
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (victim.getHealth() - event.getFinalDamage() > 0) return;
        if (!(event.getDamager() instanceof Player killer)) return;

        SkillService svc = getSkillService();
        if (svc == null) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        UItemData data = getUItemData(weapon);
        if (data == null) return;
        List<Skill> skills = SkillDataHelper.getSkills(data);
        if (skills.isEmpty()) return;

        svc.executeSkills(killer, data.getId(), skills, SkillTrigger.KILL, victim);
    }

    // =========================================================
    // EQUIP / UNEQUIP
    // =========================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEquip(UItemEquipEvent event) {
        SkillService svc = getSkillService();
        if (svc == null) return;

        Player p = event.getPlayer();
        UItemData data = event.getItemData();
        if (data == null) return;
        List<Skill> skills = SkillDataHelper.getSkills(data);
        if (skills.isEmpty()) return;

        SkillTrigger trig = event.isEquipping() ? SkillTrigger.EQUIP : SkillTrigger.UNEQUIP;
        svc.executeSkills(p, data.getId(), skills, trig, null);
    }

    // =========================================================
    // Held tracking
    // =========================================================

    @EventHandler
    public void onHeldChange(PlayerItemHeldEvent event) {
        SkillService svc = getSkillService();
        Player p = event.getPlayer();
        ItemStack newItem = p.getInventory().getItem(event.getNewSlot());
        String newId = getUItemId(newItem);

        if (svc != null && newId != null) {
            String oldId = lastHeldUItem.get(p.getUniqueId());
            if (!Objects.equals(oldId, newId)) {
                UItemData data = getUItemData(newItem);
                if (data != null) {
                    List<Skill> skills = SkillDataHelper.getSkills(data);
                    if (!skills.isEmpty()) {
                        svc.executeSkills(p, newId, skills, SkillTrigger.WHILE_HOLDING, null);
                    }
                }
            }
        }

        if (newId != null) lastHeldUItem.put(p.getUniqueId(), newId);
        else lastHeldUItem.remove(p.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastHeldUItem.remove(event.getPlayer().getUniqueId());
        SkillService svc = getSkillService();
        if (svc != null) svc.onPlayerQuit(event.getPlayer());
    }

    // =========================================================
    // Helpers
    // =========================================================

    private UItemData getUItemData(ItemStack item) {
        if (!UItemAPI.isAvailable()) return null;
        return UItemAPI.get().getUItemData(item).orElse(null);
    }

    private String getUItemId(ItemStack item) {
        if (!UItemAPI.isAvailable()) return null;
        return UItemAPI.get().getUItemId(item).orElse(null);
    }

    private boolean shouldCancel(List<Skill> skills, SkillTrigger trigger) {
        return skills.stream().anyMatch(s -> s.getTrigger() == trigger && s.isCancelEvent());
    }
}
