package com.uitem.legacy.skill;

import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Thực thi skill/passive.
 * 
 * Thiết kế ổn định:
 * - Mọi action được bọc try/catch riêng lẻ.
 * - Placeholder đơn giản: %player%, %target%, %x%, %y%, %z%, %health%
 * - Dùng SchedulerUtil (Folia-safe).
 * - Không block main thread lâu.
 */
public final class SkillExecutor {

    private final UItemPlugin plugin;
    private final CooldownManager cooldowns;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Safe blacklist for BLOCK_BREAK area skills (3x3 pick, hoe, etc.)
    // Prevents griefing storage, spawners, bedrock, and reduces anti-xray side-effects.
    // These blocks are never destroyed by the skill even if they fall inside the radius area.
    private static final java.util.Set<Material> BLOCK_BREAK_PROTECTED = java.util.Set.of(
            Material.BEDROCK,
            Material.BARRIER,
            Material.STRUCTURE_VOID,
            Material.SPAWNER,
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.ENDER_CHEST,
            Material.HOPPER,
            Material.DISPENSER,
            Material.DROPPER,
            Material.BARREL,
            Material.BEACON,
            Material.END_PORTAL_FRAME,
            Material.DRAGON_EGG,
            Material.FURNACE,
            Material.BLAST_FURNACE,
            Material.SMOKER,
            Material.COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK,
            Material.JIGSAW,
            Material.LIGHT
    );

    public SkillExecutor(UItemPlugin plugin, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
    }

    public CooldownManager getCooldowns() {
        return cooldowns;
    }

    // =========================================================
    // Main entry: thực thi danh sách skill phù hợp trigger
    // =========================================================

    /**
     * Chạy tất cả skill của item khớp trigger.
     * Trả về true nếu có ít nhất 1 skill được thực thi (và có thể cancel event).
     */
    public boolean executeSkills(Player player, String itemId, List<Skill> skills,
                                 SkillTrigger trigger, LivingEntity target) {
        if (skills == null || skills.isEmpty()) return false;

        boolean anyExecuted = false;

        for (Skill skill : skills) {
            if (skill.getTrigger() != trigger) continue;

            if (!checkChance(skill)) continue;
            if (!checkConditions(player, skill, target)) continue;
            if (!cooldowns.isReady(player, itemId, skill.getId(), skill.getCooldown())) continue;

            boolean ok = executeSkillActions(player, itemId, skill, target);
            if (ok) {
                cooldowns.markUsed(player, itemId, skill.getId());
                anyExecuted = true;
            }
        }

        return anyExecuted;
    }

    private boolean checkChance(Skill skill) {
        double ch = skill.getChance();
        if (ch >= 100) return true;
        if (ch <= 0) return false;
        return ThreadLocalRandom.current().nextDouble(100) <= ch;
    }

    private boolean checkConditions(Player player, Skill skill, LivingEntity target) {
        if (skill.getConditions() == null || skill.getConditions().isEmpty()) return true;

        for (SkillCondition cond : skill.getConditions()) {
            if (!evaluateCondition(player, cond, target)) return false;
        }
        return true;
    }

    private boolean evaluateCondition(Player player, SkillCondition cond, LivingEntity target) {
        try {
            return switch (cond.getType()) {
                case ALWAYS -> true;
                case CHANCE -> ThreadLocalRandom.current().nextDouble(100) <= cond.getDouble("value", 100);
                case PERMISSION, HAS_PERMISSION -> {
                    String perm = cond.getString("permission", "");
                    yield perm.isEmpty() || player.hasPermission(perm);
                }
                case HEALTH_MIN -> player.getHealth() >= cond.getDouble("value", 0);
                case HEALTH_MAX -> player.getHealth() <= cond.getDouble("value", 20);
                case FOOD_MIN -> player.getFoodLevel() >= cond.getInt("value", 0);
                case SNEAKING -> player.isSneaking();
                case NOT_SNEAKING -> !player.isSneaking();
                case IN_WORLD -> {
                    String w = cond.getString("world", "");
                    yield w.isEmpty() || player.getWorld().getName().equalsIgnoreCase(w);
                }
                case HAS_OFFHAND -> {
                    ItemStack off = player.getInventory().getItemInOffHand();
                    yield off != null && !off.getType().isAir();
                }
                default -> true;
            };
        } catch (Exception e) {
            plugin.getLogger().warning("[Skill] Condition error: " + e.getMessage());
            return false;
        }
    }

    private boolean executeSkillActions(Player player, String itemId, Skill skill, LivingEntity target) {
        if (skill.getActions() == null || skill.getActions().isEmpty()) return true;

        boolean anySuccess = false;

        for (SkillAction action : skill.getActions()) {
            try {
                boolean ran = runSingleAction(player, target, action, skill);
                if (ran) anySuccess = true;
            } catch (Exception ex) {
                plugin.getLogger().warning("[Skill] Action failed for " + skill.getId() + " / " + action.getType() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        return anySuccess;
    }

    // =========================================================
    // Thực thi 1 action
    // =========================================================

    private boolean runSingleAction(Player player, LivingEntity target, SkillAction action, Skill skill) {
        Location loc = player.getLocation();
        World world = player.getWorld();

        switch (action.getType()) {
            case NOTHING -> { return true; }

            case MESSAGE -> {
                String text = replacePlaceholders(action.getStringParam("text", ""), player, target);
                player.sendMessage(mm.deserialize(text));
                return true;
            }

            case ACTION_BAR -> {
                String text = replacePlaceholders(action.getStringParam("text", ""), player, target);
                player.sendActionBar(mm.deserialize(text));
                return true;
            }

            case TITLE -> {
                String title = replacePlaceholders(action.getStringParam("title", ""), player, target);
                String sub = replacePlaceholders(action.getStringParam("subtitle", ""), player, target);
                int fadeIn = action.getIntParam("fadeIn", 10);
                int stay = action.getIntParam("stay", 40);
                int fadeOut = action.getIntParam("fadeOut", 10);
                player.showTitle(net.kyori.adventure.title.Title.title(
                        mm.deserialize(title),
                        mm.deserialize(sub),
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(fadeIn * 50L),
                                java.time.Duration.ofMillis(stay * 50L),
                                java.time.Duration.ofMillis(fadeOut * 50L)
                        )
                ));
                return true;
            }

            case PLAY_SOUND -> {
                String snd = action.getStringParam("sound", "UI_BUTTON_CLICK");
                float vol = (float) action.getDoubleParam("volume", 1.0);
                float pitch = (float) action.getDoubleParam("pitch", 1.0);
                try {
                    Sound sound = Sound.valueOf(snd.toUpperCase(Locale.ROOT));
                    player.playSound(loc, sound, vol, pitch);
                } catch (IllegalArgumentException e) {
                    world.playSound(loc, snd, vol, pitch); // custom sound support
                }
                return true;
            }

            case SPAWN_PARTICLE -> {
                String p = action.getStringParam("particle", "FLAME");
                int count = action.getIntParam("count", 10);
                double off = action.getDoubleParam("offset", 0.3);
                try {
                    Particle particle = Particle.valueOf(p.toUpperCase(Locale.ROOT));
                    world.spawnParticle(particle, loc.clone().add(0, 1, 0), count, off, off, off, 0.01);
                } catch (Exception e) {
                    plugin.getLogger().warning("[Skill] SPAWN_PARTICLE: invalid particle '" + p + "'");
                }
                return true;
            }

            case DAMAGE -> {
                double amt = action.getDoubleParam("amount", 4.0);
                if (target != null && target.isValid()) {
                    target.damage(amt, player);
                }
                return true;
            }

            case HEAL -> {
                double amt = action.getDoubleParam("amount", 4.0);
                var maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                double maxHp = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
                double newHp = Math.min(maxHp, player.getHealth() + amt);
                player.setHealth(newHp);
                return true;
            }

            case FEED -> {
                int amt = action.getIntParam("amount", 4);
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + amt));
                return true;
            }

            case SET_FIRE -> {
                int sec = Math.min(action.getIntParam("seconds", 4), 10000);
                player.setFireTicks(sec * 20);
                return true;
            }

            case LAUNCH -> {
                double x = action.getDoubleParam("x", 0);
                double y = action.getDoubleParam("y", 1.2);
                double z = action.getDoubleParam("z", 0);
                Vector v;
                if (action.getBooleanParam("relative", true)) {
                    Vector dir = player.getLocation().getDirection();
                    v = dir.clone().multiply(x > 0 ? x : 1);
                    v.setY(v.getY() + y);
                    if (z != 0) {
                        Vector lateral = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(z);
                        v.add(lateral);
                    }
                } else {
                    v = new Vector(x, y, z);
                }
                player.setVelocity(v);
                return true;
            }

            case TELEPORT -> {
                // relative or absolute
                double x = action.getDoubleParam("x", 0);
                double y = action.getDoubleParam("y", 0);
                double z = action.getDoubleParam("z", 0);
                if (action.getBooleanParam("relative", true)) {
                    player.teleport(loc.clone().add(x, y, z));
                } else {
                    World w = Bukkit.getWorld(action.getStringParam("world", loc.getWorld().getName()));
                    if (w != null) player.teleport(new Location(w, x, y, z));
                }
                return true;
            }

            case STRIKE_LIGHTNING -> {
                if (target != null && target.isValid()) {
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    target.damage(5, player);
                } else {
                    world.strikeLightningEffect(loc);
                }
                return true;
            }

            case SHOOT_PROJECTILE -> {
                String proj = action.getStringParam("projectile", "FIREBALL").toUpperCase(Locale.ROOT);
                double speed = action.getDoubleParam("speed", 1.5);
                double dmg = action.getDoubleParam("damage", 6.0);

                Projectile pr;
                switch (proj) {
                    case "ARROW" -> pr = (Projectile) world.spawnEntity(loc.clone().add(0,1.2,0), EntityType.ARROW);
                    case "SMALL_FIREBALL", "FIREBALL" -> pr = (Projectile) world.spawnEntity(loc.clone().add(0,1.2,0), EntityType.SMALL_FIREBALL);
                    case "WITHER_SKULL" -> pr = (Projectile) world.spawnEntity(loc.clone().add(0,1.2,0), EntityType.WITHER_SKULL);
                    case "TRIDENT" -> pr = (Projectile) world.spawnEntity(loc.clone().add(0,1.2,0), EntityType.TRIDENT);
                    default -> pr = (Projectile) world.spawnEntity(loc.clone().add(0,1.2,0), EntityType.SMALL_FIREBALL);
                }
                pr.setShooter(player);
                pr.setVelocity(player.getLocation().getDirection().multiply(speed));
                if (pr instanceof AbstractArrow aa) aa.setDamage(dmg);
                else if (pr instanceof Fireball fb) fb.setYield((float) Math.min(4, dmg / 2.0));
                return true;
            }

            case PLAYER_COMMAND -> {
                String cmd = replacePlaceholders(action.getStringParam("command", ""), player, target);
                if (!cmd.isEmpty()) {
                    player.performCommand(cmd.startsWith("/") ? cmd.substring(1) : cmd);
                }
                return true;
            }

            case CONSOLE_COMMAND -> {
                String cmd = replacePlaceholders(action.getStringParam("command", ""), player, target);
                if (!cmd.isEmpty()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.startsWith("/") ? cmd.substring(1) : cmd);
                }
                return true;
            }

            case CONSUME_DURABILITY -> {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable d && !hand.getType().isAir()) {
                    int amt = action.getIntParam("amount", 1);
                    int newDmg = Math.min(hand.getType().getMaxDurability(), d.getDamage() + amt);
                    d.setDamage(newDmg);
                    hand.setItemMeta(d);
                    if (newDmg >= hand.getType().getMaxDurability() && !action.getBooleanParam("keep_broken", false)) {
                        player.getInventory().setItemInMainHand(null);
                    }
                }
                return true;
            }

            case CONSUME_ITEM -> {
                // Consume 1 from main hand (simple)
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (!hand.getType().isAir()) {
                    hand.setAmount(hand.getAmount() - 1);
                    if (hand.getAmount() <= 0) player.getInventory().setItemInMainHand(null);
                }
                return true;
            }

            case GIVE_POTION -> {
                String effName = action.getStringParam("effect", "SPEED").toUpperCase(Locale.ROOT);
                int duration = action.getIntParam("duration", 10); // seconds
                int amp = action.getIntParam("amplifier", 0);
                PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(effName.toLowerCase(Locale.ROOT)));
                if (type != null) {
                    player.addPotionEffect(new PotionEffect(type, duration * 20, amp, false, true, true));
                }
                return true;
            }

            case REMOVE_POTION -> {
                String effName = action.getStringParam("effect", "").toUpperCase(Locale.ROOT);
                if (effName.isEmpty()) {
                    player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
                } else {
                    PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(effName.toLowerCase(Locale.ROOT)));
                    if (type != null) player.removePotionEffect(type);
                }
                return true;
            }

            case GIVE_ITEM -> {
                String mat = action.getStringParam("material", "DIAMOND");
                int amt = action.getIntParam("amount", 1);
                try {
                    Material m = Material.valueOf(mat.toUpperCase(Locale.ROOT));
                    ItemStack toGive = new ItemStack(m, amt);
                    var leftovers = player.getInventory().addItem(toGive);
                    for (ItemStack leftover : leftovers.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[Skill] GIVE_ITEM: invalid material '" + mat + "'");
                }
                return true;
            }

            // ==================== NEW ACTIONS ====================

            case BLOCK_BREAK -> {
                int range = action.getIntParam("range", 5);
                int radius = action.getIntParam("radius", 0);
                Block targetBlock = player.getTargetBlockExact(range);

                // Safety: abort early for air or protected blocks (storage, spawner, bedrock...)
                // This is the recommended approach for high-quality 3x3 tools instead of "fill ... destroy".
                if (targetBlock == null
                        || targetBlock.getType().isAir()
                        || targetBlock.isLiquid()
                        || isBlockBreakProtected(targetBlock.getType())) {
                    return true;
                }

                ItemStack tool = player.getInventory().getItemInMainHand();

                if (radius <= 0) {
                    // Single block (original behavior)
                    targetBlock.breakNaturally(tool);
                } else {
                    // Area mode: only the surface plane (3x3, 5x5...) perpendicular to the mined face.
                    // Never a 3x3x3 cube. Per-block breakNaturally(tool) so Fortune/Silk Touch work,
                    // and anti-xray sees only the necessary individual block updates.
                    BlockFace face = null;
                    try {
                        var ray = player.rayTraceBlocks(range);
                        if (ray != null) face = ray.getHitBlockFace();
                    } catch (Exception ignored) {}
                    if (face == null) face = BlockFace.SELF;

                    int ax1 = 0, ay1 = 0, az1 = 0, ax2 = 0, ay2 = 0, az2 = 0;
                    switch (face) {
                        case NORTH, SOUTH -> { ax1 = 1; ay1 = 0; az1 = 0; ax2 = 0; ay2 = 1; az2 = 0; } // vertical wall: expand X + Y
                        case EAST, WEST   -> { ax1 = 0; ay1 = 0; az1 = 1; ax2 = 0; ay2 = 1; az2 = 0; } // vertical wall: expand Z + Y
                        case UP, DOWN     -> { ax1 = 1; ay1 = 0; az1 = 0; ax2 = 0; ay2 = 0; az2 = 1; } // floor/ceiling: expand X + Z
                        default           -> { ax1 = 1; ay1 = 0; az1 = 0; ax2 = 0; ay2 = 1; az2 = 0; }
                    }

                    // Collect first (positions are stable even if physics like sand happens)
                    java.util.List<Block> toBreak = new java.util.ArrayList<>();
                    for (int i = -radius; i <= radius; i++) {
                        for (int j = -radius; j <= radius; j++) {
                            Block b = targetBlock.getRelative(
                                    i * ax1 + j * ax2,
                                    i * ay1 + j * ay2,
                                    i * az1 + j * az2
                            );
                            if (b != null
                                    && !b.getType().isAir()
                                    && !b.isLiquid()
                                    && !isBlockBreakProtected(b.getType())) {
                                toBreak.add(b);
                            }
                        }
                    }

                    for (Block b : toBreak) {
                        b.breakNaturally(tool);
                    }
                }

                world.playSound(targetBlock.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 1.0f);
                return true;
            }

            case EXPLOSION -> {
                float power = (float) action.getDoubleParam("power", 3.0);
                boolean fire = action.getBooleanParam("fire", false);
                Location explosionLoc = (target != null && target.isValid()) ? target.getLocation() : loc;
                world.createExplosion(explosionLoc, power, fire, true);
                return true;
            }

            case AREA_DAMAGE -> {
                double dmg = action.getDoubleParam("amount", 5.0);
                double radius = action.getDoubleParam("radius", 5.0);
                Location center = (target != null && target.isValid()) ? target.getLocation() : loc;
                for (Entity e : world.getNearbyEntities(center, radius, radius, radius)) {
                    if (e instanceof LivingEntity le && !le.equals(player)) {
                        le.damage(dmg, player);
                    }
                }
                return true;
            }

            case POTION_AREA -> {
                String effName = action.getStringParam("effect", "SPEED").toUpperCase(Locale.ROOT);
                int duration = action.getIntParam("duration", 5);
                int amp = action.getIntParam("amplifier", 0);
                double radius = action.getDoubleParam("radius", 5.0);
                PotionEffectType pType = Registry.EFFECT.get(NamespacedKey.minecraft(effName.toLowerCase(Locale.ROOT)));
                if (pType != null) {
                    Location center = (target != null && target.isValid()) ? target.getLocation() : loc;
                    for (Entity e : world.getNearbyEntities(center, radius, radius, radius)) {
                        if (e instanceof LivingEntity le) {
                            le.addPotionEffect(new PotionEffect(pType, duration * 20, amp, false, true, true));
                        }
                    }
                }
                return true;
            }

            case BLIND -> {
                int dur = Math.min(action.getIntParam("duration", 5), 60);
                if (target != null && target.isValid()) {
                    PotionEffectType blindness = Registry.EFFECT.get(NamespacedKey.minecraft("blindness"));
                    if (blindness != null) {
                        target.addPotionEffect(new PotionEffect(blindness, dur * 20, 0, false, true, true));
                    }
                }
                return true;
            }

            case GLOW -> {
                int dur = Math.min(action.getIntParam("duration", 10), 60);
                if (target != null && target.isValid()) {
                    PotionEffectType glow = Registry.EFFECT.get(NamespacedKey.minecraft("glowing"));
                    if (glow != null) {
                        target.addPotionEffect(new PotionEffect(glow, dur * 20, 0, false, true, true));
                    }
                }
                return true;
            }

            case CLEAR_EFFECTS -> {
                LivingEntity clearTarget = (target != null && target.isValid()) ? target : player;
                for (PotionEffect pe : new ArrayList<>(clearTarget.getActivePotionEffects())) {
                    clearTarget.removePotionEffect(pe.getType());
                }
                return true;
            }

            case LEVITATE -> {
                int dur = Math.min(action.getIntParam("duration", 5), 30);
                int amp = action.getIntParam("amplifier", 0);
                PotionEffectType lev = Registry.EFFECT.get(NamespacedKey.minecraft("levitation"));
                if (lev != null) {
                    player.addPotionEffect(new PotionEffect(lev, dur * 20, amp, false, true, true));
                }
                return true;
            }

            case SPAWN_MOB -> {
                String entityTypeName = action.getStringParam("entity", "ZOMBIE").toUpperCase(Locale.ROOT);
                int count = Math.min(action.getIntParam("count", 1), 10);
                Location spawnLoc = player.getLocation().add(0, 1, 0);
                for (int i = 0; i < count; i++) {
                    try {
                        EntityType et = EntityType.valueOf(entityTypeName);
                        Location offset = spawnLoc.clone().add(
                                ThreadLocalRandom.current().nextDouble(-2, 2),
                                0,
                                ThreadLocalRandom.current().nextDouble(-2, 2)
                        );
                        Entity e = world.spawnEntity(offset, et);
                        if (e instanceof Mob mob) {
                            mob.setTarget((LivingEntity) world.getNearbyEntities(offset, 10, 10, 10).stream()
                                    .filter(ex -> ex instanceof Player && !ex.equals(player))
                                    .findFirst().orElse(null));
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().warning("[Skill] SPAWN_MOB: invalid entity '" + entityTypeName + "'");
                    }
                }
                return true;
            }

            case FREEZE -> {
                int dur = Math.min(action.getIntParam("duration", 5), 30);
                LivingEntity freezeTarget = (target != null && target.isValid()) ? target : player;
                freezeTarget.setFreezeTicks(dur * 20);
                return true;
            }

            default -> { return false; }
        }
    }

    // =========================================================
    // Placeholders
    // =========================================================

    private String replacePlaceholders(String input, Player player, LivingEntity target) {
        if (input == null) return "";
        String out = input;
        out = out.replace("%player%", player.getName());
        out = out.replace("%display%", player.getDisplayName());
        out = out.replace("%health%", String.format("%.1f", player.getHealth()));
        out = out.replace("%x%", String.valueOf((int) player.getLocation().getX()));
        out = out.replace("%y%", String.valueOf((int) player.getLocation().getY()));
        out = out.replace("%z%", String.valueOf((int) player.getLocation().getZ()));
        out = out.replace("%world%", player.getWorld().getName());

        if (target != null) {
            out = out.replace("%target%", target instanceof Player p ? p.getName() : target.getType().name());
            out = out.replace("%target_health%", String.format("%.1f", target.getHealth()));
        } else {
            out = out.replace("%target%", "mục tiêu");
            out = out.replace("%target_health%", "?");
        }
        return out;
    }

    // =========================================================
    // Periodic support (gọi từ listener)
    // =========================================================

    /**
     * Chạy skill PERIODIC / WHILE_* cho player.
     * Được gọi định kỳ từ SkillListener.
     */
    public void tickPeriodicSkills(Player player, String itemId, List<Skill> skills, SkillTrigger triggerType) {
        if (skills == null) return;
        for (Skill skill : skills) {
            if (skill == null || skill.getTrigger() != triggerType) continue;
            if (!cooldowns.isReady(player, itemId, skill.getId(), skill.getCooldown())) continue;
            // For periodic we use cooldown field as "minimum interval"
            if (!checkChance(skill)) continue;
            if (!checkConditions(player, skill, null)) continue;

            boolean ok = executeSkillActions(player, itemId, skill, null);
            if (ok) {
                cooldowns.markUsed(player, itemId, skill.getId());
            }
        }
    }

    // =========================================================
    // BLOCK_BREAK helpers (3x3 / area mining support)
    // =========================================================

    /**
     * Returns true if this material should never be destroyed by the BLOCK_BREAK skill action.
     * This protects player storage, mob spawners, bedrock, admins blocks, and limits
     * anti-xray information leakage that can happen with mass block updates.
     *
     * Shulker boxes (all colors) are detected by name to avoid a huge list.
     */
    private boolean isBlockBreakProtected(Material material) {
        if (material == null) return true;
        if (BLOCK_BREAK_PROTECTED.contains(material)) return true;
        String name = material.name();
        // All shulker box variants (SHULKER_BOX + 16 colored)
        if (name.endsWith("_SHULKER_BOX") || name.equals("SHULKER_BOX")) return true;
        return false;
    }
}
