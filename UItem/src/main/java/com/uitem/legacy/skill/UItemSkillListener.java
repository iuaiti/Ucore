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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.entity.Trident;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.*;

/**
 * Listener chuyên biệt cho hệ thống Skill & Passive native của UItem.
 * 
 * Ưu điểm so với ExecutableItems:
 * - Toàn bộ logic Java rõ ràng, không script phức tạp.
 * - Cooldown lưu PDC + memory → rất ổn định sau restart.
 * - Folia-safe qua SchedulerUtil.
 * - Dễ mở rộng action/condition.
 */
public class UItemSkillListener implements Listener {

    private final UItemPlugin plugin;
    private final SkillService skillService;

    // Track item đang cầm để WHILE_HOLDING
    private final Map<UUID, String> lastHeldUItem = new HashMap<>();

    // Reentrancy guard: ngăn DAMAGE action gọi target.damage() → EntityDamageByEntityEvent → vòng lặp vô hạn
    private boolean inDamageProcessing = false;

    // Periodic ticker (chạy mỗi 5 tick ~0.25s, đủ mượt cho hầu hết aura/regen)
    private ScheduledTask periodicTask;

    public UItemSkillListener(UItemPlugin plugin) {
        this.plugin = plugin;
        this.skillService = UCoreAPI.getSkillService();
        startPeriodicTask();
    }

    private void startPeriodicTask() {
        if (periodicTask != null) periodicTask.cancel();

        periodicTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    tickPlayerPeriodic(p);
                } catch (Exception e) {
                    // Không để 1 player làm crash toàn bộ ticker — log lỗi để debug
                    plugin.getLogger().warning("[Skill] Periodic tick error for " + p.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, 20L, 5L); // delay 1s, sau đó mỗi 5 tick
    }

    public void shutdown() {
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
    }

    private void tickPlayerPeriodic(Player player) {
        // WHILE_HOLDING (main + offhand)
        checkAndTickHolding(player, player.getInventory().getItemInMainHand(), SkillTrigger.WHILE_HOLDING);
        checkAndTickHolding(player, player.getInventory().getItemInOffHand(), SkillTrigger.WHILE_HOLDING);

        // PERIODIC trên mọi item đang cầm/mặc (để đơn giản: chỉ cầm tay chính + giáp)
        checkAndTickPeriodic(player, player.getInventory().getItemInMainHand());
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && !armor.getType().isAir()) {
                checkAndTickPeriodic(player, armor);
            }
        }
    }

    private void checkAndTickHolding(Player player, ItemStack item, SkillTrigger trigger) {
        UItemData data = getUItemData(item);
        if (data == null) return;
        List<Skill> skills = SkillDataHelper.getSkills(data);
        if (skills.isEmpty()) return;

        String itemId = data.getId();
        skillService.tickPeriodicSkills(player, itemId, skills, trigger);
    }

    private void checkAndTickPeriodic(Player player, ItemStack item) {
        UItemData data = getUItemData(item);
        if (data == null || SkillDataHelper.getSkills(data).isEmpty()) return;

        String itemId = data.getId();
        skillService.tickPeriodicSkills(player, itemId, SkillDataHelper.getSkills(data), SkillTrigger.PERIODIC);
    }

    // =========================================================
    // Interact (active skills)
    // =========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onUItemInteract(UItemInteractEvent event) {
        Player player = event.getPlayer();
        UItemData data = event.getItemData();
        if (data == null || SkillDataHelper.getSkills(data).isEmpty()) return;

        org.bukkit.event.block.Action bukkitAction = event.getAction();
        boolean sneaking = player.isSneaking();

        SkillTrigger trigger = switch (bukkitAction) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> sneaking ? SkillTrigger.SHIFT_RIGHT_CLICK : SkillTrigger.RIGHT_CLICK;
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> sneaking ? SkillTrigger.SHIFT_LEFT_CLICK : SkillTrigger.LEFT_CLICK;
            default -> null;
        };

        if (trigger == null) return;

        boolean executed = skillService.executeSkills(player, data.getId(), SkillDataHelper.getSkills(data), trigger, null);

        if (executed && shouldCancel(data, trigger)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        UItemData data = getUItemData(item);
        if (data == null || SkillDataHelper.getSkills(data).isEmpty()) return;

        // Check if this is a throw (has velocity) vs normal drop
        boolean isThrow = event.getItemDrop().getVelocity().lengthSquared() > 0.1;
        SkillTrigger trigger = isThrow ? SkillTrigger.THROW : SkillTrigger.DROP;

        boolean executed = skillService.executeSkills(player, data.getId(), SkillDataHelper.getSkills(data), trigger, null);
        if (executed && shouldCancel(data, trigger)) {
            event.setCancelled(true);
        }
    }

    // =========================================================
    // SHOOT trigger: bow/crossbow/trident (user request: ném đinh ba / bắn cung)
    // =========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null || bow.getType().isAir()) return;

        UItemData data = getUItemData(bow);
        if (data == null || SkillDataHelper.getSkills(data).isEmpty()) return;

        boolean executed = skillService.executeSkills(player, data.getId(), SkillDataHelper.getSkills(data), SkillTrigger.SHOOT, null);
        if (executed && shouldCancel(data, SkillTrigger.SHOOT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTridentOrProjectileThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        // For trident throws (and potentially other projectile weapons), look at held items.
        // Prefer main hand, then off hand. We trigger SHOOT on the UItem weapon used to launch.
        ItemStack weapon = player.getInventory().getItemInMainHand();
        UItemData data = getUItemData(weapon);

        if (data == null) {
            // Fallback to offhand (rare for trident/bow but possible)
            weapon = player.getInventory().getItemInOffHand();
            data = getUItemData(weapon);
        }

        // If the projectile is a trident, also try to match a TRIDENT in hand even if not (yet) resolved as UItem above
        if (data == null && event.getEntity() instanceof Trident) {
            if (weapon != null && weapon.getType() == Material.TRIDENT) {
                data = getUItemData(weapon);
            } else {
                ItemStack off = player.getInventory().getItemInOffHand();
                if (off != null && off.getType() == Material.TRIDENT) {
                    data = getUItemData(off);
                }
            }
        }

        if (data == null || SkillDataHelper.getSkills(data).isEmpty()) return;

        boolean executed = skillService.executeSkills(player, data.getId(), SkillDataHelper.getSkills(data), SkillTrigger.SHOOT, null);
        if (executed && shouldCancel(data, SkillTrigger.SHOOT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        UItemData data = getUItemData(main);
        if (data != null && !SkillDataHelper.getSkills(data).isEmpty()) {
            boolean executed = skillService.executeSkills(player, data.getId(), SkillDataHelper.getSkills(data), SkillTrigger.SWAP_HAND, null);
            if (executed && shouldCancel(data, SkillTrigger.SWAP_HAND)) {
                event.setCancelled(true);
            }
        }
    }

    // =========================================================
    // Combat
    // =========================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Reentrancy guard: DAMAGE action gọi target.damage() sinh ra EntityDamageByEntityEvent mới,
        // nếu không guard sẽ gây đệ quy vô hạn (HIT → DAMAGE → HIT → DAMAGE → ...)
        if (inDamageProcessing) return;
        inDamageProcessing = true;
        try {
            // HIT: damager dùng UItem
            if (event.getDamager() instanceof Player damager) {
                ItemStack weapon = damager.getInventory().getItemInMainHand();
                UItemData data = getUItemData(weapon);
                if (data != null && !SkillDataHelper.getSkills(data).isEmpty()) {
                    LivingEntity victim = (event.getEntity() instanceof LivingEntity le) ? le : null;
                    skillService.executeSkills(damager, data.getId(), SkillDataHelper.getSkills(data), SkillTrigger.HIT, victim);
                }
            }

            // TAKE_DAMAGE: victim đang cầm hoặc mặc UItem có skill
            if (event.getEntity() instanceof Player victim) {
                // held
                ItemStack held = victim.getInventory().getItemInMainHand();
                UItemData heldData = getUItemData(held);
                if (heldData != null) {
                    List<Skill> heldSk = SkillDataHelper.getSkills(heldData);
                    if (!heldSk.isEmpty()) {
                        skillService.executeSkills(victim, heldData.getId(), heldSk, SkillTrigger.TAKE_DAMAGE, null);
                    }
                }
                // armor
                for (ItemStack armor : victim.getInventory().getArmorContents()) {
                    if (armor == null || armor.getType().isAir()) continue;
                    UItemData ad = getUItemData(armor);
                    if (ad != null) {
                        List<Skill> aSkills = SkillDataHelper.getSkills(ad);
                        if (!aSkills.isEmpty()) {
                            skillService.executeSkills(victim, ad.getId(), aSkills, SkillTrigger.TAKE_DAMAGE, null);
                        }
                    }
                }
            }
        } finally {
            inDamageProcessing = false;
        }
    }

    // =========================================================
    // Equip
    // =========================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEquip(UItemEquipEvent event) {
        Player p = event.getPlayer();
        UItemData data = event.getItemData();
        if (data == null || SkillDataHelper.getSkills(data).isEmpty()) return;

        SkillTrigger trig = event.isEquipping() ? SkillTrigger.EQUIP : SkillTrigger.UNEQUIP;
        skillService.executeSkills(p, data.getId(), SkillDataHelper.getSkills(data), trig, null);
    }

    // =========================================================
    // Held tracking for WHILE_HOLDING (chuyển item)
    // =========================================================

    @EventHandler
    public void onHeldChange(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        ItemStack newItem = p.getInventory().getItem(event.getNewSlot());
        String newId = getUItemId(newItem);

        String oldId = lastHeldUItem.get(p.getUniqueId());
        if (!Objects.equals(oldId, newId) && newId != null) {
            // Trigger WHILE_HOLDING ngay khi cầm lên (nếu có skill loại này)
            UItemData data = getUItemData(newItem);
            if (data != null) {
                skillService.executeSkills(p, newId, SkillDataHelper.getSkills(data), SkillTrigger.WHILE_HOLDING, null);
            }
        }
        if (newId != null) lastHeldUItem.put(p.getUniqueId(), newId);
        else lastHeldUItem.remove(p.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastHeldUItem.remove(event.getPlayer().getUniqueId());
        skillService.onPlayerQuit(event.getPlayer());
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

    private boolean shouldCancel(UItemData data, SkillTrigger trigger) {
        List<Skill> sk = SkillDataHelper.getSkills(data);
        return sk.stream().anyMatch(s -> s.getTrigger() == trigger && s.isCancelEvent());
    }
}
