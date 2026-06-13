package com.ultra.uskill.execution;

import com.ultra.uskill.cooldown.CooldownManager;
import com.ultra.ucore.skill.Skill;
import com.ultra.ucore.skill.SkillAction;
import com.ultra.ucore.skill.SkillCondition;
import com.ultra.ucore.skill.SkillTrigger;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class SkillExecutor {

    private final CooldownManager cooldowns;
    private final Map<UUID, Location> recallLocations = new ConcurrentHashMap<>();

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

    public SkillExecutor(CooldownManager cooldowns) {
        this.cooldowns = cooldowns;
    }

    public CooldownManager getCooldowns() {
        return cooldowns;
    }

    public void clearPlayer(Player player) {
        recallLocations.remove(player.getUniqueId());
    }

    public boolean executeSkill(Player player, Skill skill, LivingEntity target) {
        if (skill == null) return false;
        if (!checkChance(skill)) return false;
        if (!checkConditions(player, skill, target)) return false;
        return executeSkillActions(player, skill, target);
    }

    public boolean executeSkills(Player player, String itemId, List<Skill> skills,
                                 SkillTrigger trigger, LivingEntity target) {
        if (skills == null || skills.isEmpty()) return false;

        boolean anyExecuted = false;

        for (Skill skill : skills) {
            if (skill.getTrigger() != trigger) continue;

            if (!checkChance(skill)) continue;
            if (!checkConditions(player, skill, target)) continue;
            if (!cooldowns.isReady(player, itemId, skill.getId(), skill.getCooldown())) continue;

            boolean ok = executeSkillActions(player, skill, target);
            if (ok) {
                cooldowns.markUsed(player, itemId, skill.getId());
                anyExecuted = true;
            }
        }

        return anyExecuted;
    }

    public boolean checkChance(Skill skill) {
        double ch = skill.getChance();
        if (ch >= 100) return true;
        if (ch <= 0) return false;
        return ThreadLocalRandom.current().nextDouble(100) <= ch;
    }

    public boolean checkConditions(Player player, Skill skill, LivingEntity target) {
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
                case HAS_POTION -> {
                    String effectName = cond.getString("effect", "");
                    if (effectName.isEmpty()) yield false;
                    PotionEffectType pet = Registry.EFFECT.get(NamespacedKey.minecraft(effectName.toLowerCase(Locale.ROOT)));
                    yield pet != null && player.hasPotionEffect(pet);
                }
                case LEVEL_MIN -> player.getLevel() >= cond.getInt("value", 0);
                case EXPERIENCE_MIN -> player.getTotalExperience() >= cond.getInt("value", 0);
                case IS_IN_WATER -> player.isInWater();
                case IS_IN_LAVA -> player.isInLava();
                case IS_ON_GROUND -> player.isOnGround();
                case IS_RAINING -> player.getWorld().hasStorm();
                case IS_NIGHT -> {
                    long time = player.getWorld().getTime();
                    yield time >= 13000 && time < 23000;
                }
                case IS_DAY -> {
                    long time = player.getWorld().getTime();
                    yield time >= 0 && time < 13000;
                }
                case BIOME -> {
                    String biomeName = cond.getString("biome", "");
                    yield biomeName.isEmpty() || player.getLocation().getBlock().getBiome().getKey().getKey().equalsIgnoreCase(biomeName);
                }
                case IS_SPRINTING -> player.isSprinting();
                case IS_WEARING -> {
                    String matName = cond.getString("material", "");
                    if (matName.isEmpty()) yield false;
                    try {
                        Material mat = Material.valueOf(matName.toUpperCase(Locale.ROOT));
                        for (ItemStack armor : player.getInventory().getArmorContents()) {
                            if (armor != null && armor.getType() == mat) yield true;
                        }
                    } catch (Exception ignored) {}
                    yield false;
                }
                case HAS_MONEY -> {
                    double needed = cond.getDouble("value", 0);
                    if (needed <= 0) yield true;
                    try {
                        Class<?> ecoClass = Class.forName("net.milkbowl.vault.economy.Economy");
                        Object eco = Bukkit.getServicesManager().load(ecoClass);
                        if (eco != null) {
                            double bal = (double) ecoClass.getMethod("getBalance", org.bukkit.OfflinePlayer.class).invoke(eco, player);
                            yield bal >= needed;
                        }
                    } catch (Exception ignored) {}
                    yield true;
                }
                case IS_ON_FIRE -> player.getFireTicks() > 0;
                case HEALTH_PERCENT_MIN -> {
                    var maxAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    double maxHp = maxAttr != null ? maxAttr.getValue() : 20.0;
                    double pctNeeded = cond.getDouble("value", 50);
                    yield (player.getHealth() / maxHp) * 100.0 >= pctNeeded;
                }
                case TARGET_IN_RANGE -> {
                    if (target == null || !target.isValid()) yield false;
                    double range = cond.getDouble("value", 10);
                    yield player.getLocation().distanceSquared(target.getLocation()) <= range * range;
                }
                case HAS_ITEM -> {
                    String matName = cond.getString("material", "");
                    int amt = cond.getInt("amount", 1);
                    if (matName.isEmpty()) yield false;
                    try {
                        Material mat = Material.valueOf(matName.toUpperCase(Locale.ROOT));
                        yield player.getInventory().contains(mat, amt);
                    } catch (Exception e) { yield false; }
                }
                case IS_MOUNTED -> player.isInsideVehicle();
                case LIGHT_LEVEL -> {
                    int light = player.getLocation().getBlock().getLightLevel();
                    String op = cond.getString("operator", ">=");
                    int val = cond.getInt("value", 0);
                    yield switch (op) {
                        case ">" -> light > val;
                        case "<" -> light < val;
                        case "<=" -> light <= val;
                        case "==", "=" -> light == val;
                        default -> light >= val;
                    };
                }
                default -> true;
            };
        } catch (Exception e) {
            return false;
        }
    }

    private boolean executeSkillActions(Player player, Skill skill, LivingEntity target) {
        if (skill.getActions() == null || skill.getActions().isEmpty()) return true;

        boolean anySuccess = false;

        for (SkillAction action : skill.getActions()) {
            try {
                boolean ran = runSingleAction(player, target, action, skill);
                if (ran) anySuccess = true;
            } catch (Exception e) {
                Bukkit.getLogger().warning("[USkill] Failed to execute action " + action.getType()
                        + " for skill " + skill.getId() + " on " + player.getName() + ": " + e.getMessage());
            }
        }
        return anySuccess;
    }

    private boolean runSingleAction(Player player, LivingEntity target, SkillAction action, Skill skill) {
        Location loc = player.getLocation();
        World world = player.getWorld();

        switch (action.getType()) {
            case NOTHING -> { return true; }

            case MESSAGE -> {
                String text = replacePlaceholders(action.getStringParam("text", ""), player, target);
                player.sendMessage(com.ultra.ucore.util.TextUtil.parse(text));
                return true;
            }

            case ACTION_BAR -> {
                String text = replacePlaceholders(action.getStringParam("text", ""), player, target);
                player.sendActionBar(com.ultra.ucore.util.TextUtil.parse(text));
                return true;
            }

            case TITLE -> {
                String title = replacePlaceholders(action.getStringParam("title", ""), player, target);
                String sub = replacePlaceholders(action.getStringParam("subtitle", ""), player, target);
                int fadeIn = action.getIntParam("fadeIn", 10);
                int stay = action.getIntParam("stay", 40);
                int fadeOut = action.getIntParam("fadeOut", 10);
                player.showTitle(net.kyori.adventure.title.Title.title(
                        com.ultra.ucore.util.TextUtil.parse(title),
                        com.ultra.ucore.util.TextUtil.parse(sub),
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
                    world.playSound(loc, snd, vol, pitch);
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
                    Bukkit.getLogger().warning("[USkill] Invalid particle '" + p + "' in skill " + skill.getId() + ": " + e.getMessage());
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
                LivingEntity fireTarget = (target != null && target.isValid()) ? target : player;
                fireTarget.setFireTicks(sec * 20);
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
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (!hand.getType().isAir()) {
                    hand.setAmount(hand.getAmount() - 1);
                    if (hand.getAmount() <= 0) player.getInventory().setItemInMainHand(null);
                }
                return true;
            }

            case GIVE_POTION -> {
                String effName = action.getStringParam("effect", "SPEED").toUpperCase(Locale.ROOT);
                int duration = action.getIntParam("duration", 10);
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
                    Bukkit.getLogger().warning("[USkill] Failed to give item material=" + mat + " in skill " + skill.getId() + ": " + e.getMessage());
                }
                return true;
            }

            case BLOCK_BREAK -> {
                int range = action.getIntParam("range", 5);
                int radius = action.getIntParam("radius", 0);
                Block targetBlock = player.getTargetBlockExact(range);

                if (targetBlock == null
                        || targetBlock.getType().isAir()
                        || targetBlock.isLiquid()
                        || isBlockBreakProtected(targetBlock.getType())) {
                    return true;
                }

                ItemStack tool = player.getInventory().getItemInMainHand();

                if (radius <= 0) {
                    targetBlock.breakNaturally(tool);
                } else {
                    BlockFace face = null;
                    try {
                        var ray = player.rayTraceBlocks(range);
                        if (ray != null) face = ray.getHitBlockFace();
                    } catch (Exception ignored) {}
                    if (face == null) face = BlockFace.SELF;

                    int ax1 = 0, ay1 = 0, az1 = 0, ax2 = 0, ay2 = 0, az2 = 0;
                    switch (face) {
                        case NORTH, SOUTH -> { ax1 = 1; ay1 = 0; az1 = 0; ax2 = 0; ay2 = 1; az2 = 0; }
                        case EAST, WEST   -> { ax1 = 0; ay1 = 0; az1 = 1; ax2 = 0; ay2 = 1; az2 = 0; }
                        case UP, DOWN     -> { ax1 = 1; ay1 = 0; az1 = 0; ax2 = 0; ay2 = 0; az2 = 1; }
                        default           -> { ax1 = 1; ay1 = 0; az1 = 0; ax2 = 0; ay2 = 1; az2 = 0; }
                    }

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
                LivingEntity levTarget = (target != null && target.isValid()) ? target : player;
                PotionEffectType lev = Registry.EFFECT.get(NamespacedKey.minecraft("levitation"));
                if (lev != null) {
                    levTarget.addPotionEffect(new PotionEffect(lev, dur * 20, amp, false, true, true));
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
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("[USkill] Failed to spawn mob " + entityTypeName + " in skill " + skill.getId() + ": " + e.getMessage());
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

            case BEAM -> {
                double dmg = action.getDoubleParam("damage", 5.0);
                double range = action.getDoubleParam("range", 20.0);
                double radius = action.getDoubleParam("radius", 1.5);
                var ray = world.rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), range, radius, e -> e instanceof LivingEntity && !e.equals(player));
                if (ray != null && ray.getHitEntity() instanceof LivingEntity hit) {
                    hit.damage(dmg, player);
                    hit.getWorld().spawnParticle(Particle.FLAME, hit.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.02);
                    player.getWorld().playSound(hit.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.5f, 1.2f);
                }
                return true;
            }

            case CIRCLE -> {
                double dmg = action.getDoubleParam("damage", 5.0);
                double radius = action.getDoubleParam("radius", 4.0);
                int count = 0;
                for (Entity e : world.getNearbyEntities(loc, radius, radius, radius)) {
                    if (e instanceof LivingEntity le && !le.equals(player)) {
                        le.damage(dmg, player);
                        count++;
                    }
                }
                world.spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 0.5, 0), 30, radius, 0.3, radius, 0);
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.8f);
                if (count > 0) return true;
                return false;
            }

            case CONE -> {
                double dmg = action.getDoubleParam("damage", 5.0);
                double range = action.getDoubleParam("range", 6.0);
                double angle = action.getDoubleParam("angle", 45.0);
                Vector dir = player.getLocation().getDirection().normalize();
                double angleRad = Math.toRadians(angle);
                boolean hit = false;
                for (Entity e : world.getNearbyEntities(loc, range, range, range)) {
                    if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
                    Vector toTarget = e.getLocation().toVector().subtract(loc.toVector());
                    double dist = toTarget.length();
                    if (dist > range || dist < 0.5) continue;
                    double dot = dir.dot(toTarget.normalize());
                    double entityAngle = Math.acos(Math.max(-1, Math.min(1, dot)));
                    if (entityAngle <= angleRad) {
                        le.damage(dmg * (1 - dist / range * 0.5), player);
                        hit = true;
                    }
                }
                if (hit) {
                    world.spawnParticle(Particle.CRIT, loc.clone().add(dir.clone().multiply(2)), 40, 0.5, 0.5, 0.5, 0.1);
                    world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 0.6f);
                }
                return hit;
            }

            case PULL -> {
                if (target == null || !target.isValid()) return false;
                double power = action.getDoubleParam("power", 1.5);
                Vector pullVec = player.getLocation().toVector().subtract(target.getLocation().toVector());
                if (pullVec.lengthSquared() < 1e-10) return false; // same position → NaN guard
                pullVec.normalize().multiply(power);
                pullVec.setY(Math.min(pullVec.getY() + 0.3, 1.0));
                target.setVelocity(pullVec);
                world.playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 0.5f);
                return true;
            }

            case PUSH -> {
                if (target == null || !target.isValid()) return false;
                double power = action.getDoubleParam("power", 1.5);
                Vector pushVec = target.getLocation().toVector().subtract(player.getLocation().toVector());
                if (pushVec.lengthSquared() < 1e-10) return false; // same position → NaN guard
                pushVec.normalize().multiply(power);
                pushVec.setY(Math.min(pushVec.getY() + 0.3, 1.0));
                target.setVelocity(pushVec);
                world.playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.8f, 1.5f);
                return true;
            }

            case THROW_ENTITY -> {
                if (target == null || !target.isValid()) return false;
                double power = action.getDoubleParam("power", 2.0);
                Vector throwVec = player.getLocation().getDirection().normalize().multiply(power);
                throwVec.setY(Math.max(throwVec.getY(), 0.5));
                target.setVelocity(throwVec);
                target.setFallDistance(-100);
                world.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.8f);
                return true;
            }

            case ROCKET -> {
                double power = action.getDoubleParam("power", 1.5);
                player.setVelocity(player.getVelocity().setY(power));
                player.setFallDistance(-100);
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 30, 0.3, 0, 0.3, 0.05);
                world.spawnParticle(Particle.FLAME, loc, 20, 0.3, 0, 0.3, 0.02);
                world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.7f, 0.9f);
                return true;
            }

            case LIGHTNING_STORM -> {
                int count = Math.min(action.getIntParam("count", 5), 20);
                double radius = action.getDoubleParam("radius", 8.0);
                Location center = (target != null && target.isValid()) ? target.getLocation() : loc;
                for (int i = 0; i < count; i++) {
                    double xOff = ThreadLocalRandom.current().nextDouble(-radius, radius);
                    double zOff = ThreadLocalRandom.current().nextDouble(-radius, radius);
                    Location strikeLoc = center.clone().add(xOff, 0, zOff);
                    strikeLoc.setY(world.getHighestBlockYAt(strikeLoc));
                    world.strikeLightningEffect(strikeLoc);
                    for (Entity e : world.getNearbyEntities(strikeLoc, 3, 3, 3)) {
                        if (e instanceof LivingEntity le && !le.equals(player)) {
                            le.damage(4.0, player);
                        }
                    }
                }
                world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.6f);
                return true;
            }

            case BOMB -> {
                double dmg = action.getDoubleParam("damage", 6.0);
                float power = (float) action.getDoubleParam("power", 2.0);
                boolean fire = action.getBooleanParam("fire", false);
                var fb = world.spawn(loc.clone().add(0, 1.2, 0), org.bukkit.entity.Fireball.class);
                fb.setShooter(player);
                fb.setYield(power);
                fb.setIsIncendiary(fire);
                fb.setVelocity(player.getLocation().getDirection().multiply(1.8));
                fb.setCustomName("bomb");
                Bukkit.getScheduler().runTaskLater(com.ultra.uskill.USkillPlugin.getInstance(), () -> {
                    if (fb.isValid() && !fb.isDead()) {
                        fb.getWorld().createExplosion(fb.getLocation(), power, fire, true);
                        fb.remove();
                    }
                }, 60L);
                return true;
            }

            case WARP -> {
                double distance = action.getDoubleParam("distance", 8.0);
                Vector dir = player.getLocation().getDirection().normalize();
                Location targetLoc = loc.clone().add(dir.clone().multiply(distance));
                targetLoc.setYaw(loc.getYaw());
                targetLoc.setPitch(loc.getPitch());
                Block targetBlock = targetLoc.getBlock();
                if (targetBlock.getType().isSolid()) {
                    targetLoc.setY(targetLoc.getY() + 2);
                }
                if (!targetLoc.getBlock().getType().isSolid() && targetLoc.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                    player.teleport(targetLoc);
                    world.spawnParticle(Particle.REVERSE_PORTAL, targetLoc, 30, 0.5, 0.5, 0.5, 0.1);
                    world.playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
                } else {
                    player.sendMessage(com.ultra.ucore.util.TextUtil.parse("<red>No space to warp!"));
                    return false;
                }
                return true;
            }

            case GROUND_SLAM -> {
                double dmg = action.getDoubleParam("damage", 6.0);
                double radius = action.getDoubleParam("radius", 4.0);
                world.spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.2, 0), 40, radius / 2, 0.2, radius / 2, 0.05);
                world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);
                boolean hit = false;
                for (Entity e : world.getNearbyEntities(loc, radius, 2, radius)) {
                    if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
                    le.damage(dmg, player);
                    Vector kb = le.getLocation().toVector().subtract(loc.toVector());
                    if (kb.lengthSquared() < 1e-10) continue; // same position → NaN guard
                    kb.normalize().setY(0.4);
                    le.setVelocity(kb);
                    hit = true;
                }
                return hit;
            }

            case FIRE_TRAIL -> {
                int duration = Math.min(action.getIntParam("duration", 5), 20);
                int interval = Math.max(action.getIntParam("interval", 2), 1);
                Block startBlock = loc.getBlock();
                if (startBlock.getType().isAir() || startBlock.getType() == Material.SHORT_GRASS) {
                    startBlock.setType(Material.FIRE);
                    Bukkit.getScheduler().runTaskLater(com.ultra.uskill.USkillPlugin.getInstance(), () -> {
                        if (startBlock.getType() == Material.FIRE) startBlock.setType(Material.AIR);
                    }, duration * 20L);
                }
                return true;
            }

            case ICE_TRAIL -> {
                int duration = Math.min(action.getIntParam("duration", 5), 20);
                Block startBlock = loc.getBlock();
                if (startBlock.getType().isAir() || startBlock.isLiquid() || startBlock.getType() == Material.SHORT_GRASS) {
                    startBlock.setType(Material.FROSTED_ICE);
                    Bukkit.getScheduler().runTaskLater(com.ultra.uskill.USkillPlugin.getInstance(), () -> {
                        if (startBlock.getType() == Material.FROSTED_ICE) startBlock.setType(Material.AIR);
                    }, duration * 20L);
                }
                return true;
            }

            case BLINK -> {
                double distance = action.getDoubleParam("distance", 5.0);
                Vector dir = player.getLocation().getDirection().normalize();
                Location blinkLoc = loc.clone().add(dir.clone().multiply(distance));
                blinkLoc.setYaw(loc.getYaw());
                blinkLoc.setPitch(loc.getPitch());
                if (!blinkLoc.getBlock().getType().isSolid() && !blinkLoc.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                    player.teleport(blinkLoc);
                    world.spawnParticle(Particle.PORTAL, loc, 30, 0.5, 0.5, 0.5, 0.1);
                    world.spawnParticle(Particle.PORTAL, blinkLoc, 30, 0.5, 0.5, 0.5, 0.1);
                    world.playSound(blinkLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
                } else {
                    return false;
                }
                return true;
            }

            case SHIELD -> {
                double absorption = action.getDoubleParam("absorption", 8.0);
                int duration = Math.min(action.getIntParam("duration", 10), 60);
                var maxAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                double maxHp = maxAttr != null ? maxAttr.getValue() : 20.0;
                player.setAbsorptionAmount(Math.min(absorption, maxHp));
                PotionEffectType resist = Registry.EFFECT.get(NamespacedKey.minecraft("resistance"));
                if (resist != null) {
                    player.addPotionEffect(new PotionEffect(resist, duration * 20, 0, false, false, true));
                }
                world.spawnParticle(Particle.CURRENT_DOWN, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.2f);
                return true;
            }

            case RECALL -> {
                Location prev = recallLocations.get(player.getUniqueId());
                if (prev != null) {
                    player.teleport(prev);
                    world.spawnParticle(Particle.REVERSE_PORTAL, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                    world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.8f);
                    recallLocations.remove(player.getUniqueId());
                    return true;
                } else {
                    recallLocations.put(player.getUniqueId(), player.getLocation());
                    player.sendMessage(com.ultra.ucore.util.TextUtil.parse("<green>Recall point set!"));
                    world.spawnParticle(Particle.WAX_ON, loc.clone().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0);
                    world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 1.2f);
                    return true;
                }
            }

            case SMITE -> {
                double dmg = action.getDoubleParam("damage", 6.0);
                double radius = action.getDoubleParam("radius", 8.0);
                boolean hit = false;
                for (Entity e : world.getNearbyEntities(loc, radius, radius, radius)) {
                    if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
                    world.strikeLightningEffect(le.getLocation());
                    le.damage(dmg, player);
                    hit = true;
                }
                if (hit) {
                    world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.0f);
                }
                return hit;
            }

            case FIRE_NOVA -> {
                double dmg = action.getDoubleParam("damage", 5.0);
                double radius = action.getDoubleParam("radius", 5.0);
                for (Entity e : world.getNearbyEntities(loc, radius, radius, radius)) {
                    if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
                    le.damage(dmg, player);
                    le.setFireTicks(80);
                }
                world.spawnParticle(Particle.FLAME, loc.clone().add(0, 0.5, 0), 60, radius, 0.5, radius, 0.05);
                world.spawnParticle(Particle.LAVA, loc.clone().add(0, 0.5, 0), 20, radius / 2, 0.3, radius / 2, 0.01);
                world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
                return true;
            }

            case ICE_PRISON -> {
                if (target == null || !target.isValid()) return false;
                int duration = Math.min(action.getIntParam("duration", 5), 15);
                Location tl = target.getLocation();
                Block centerBlock = tl.getBlock();
                Material originalType = centerBlock.getType();
                if (!centerBlock.getType().isAir() && centerBlock.getType() != Material.SHORT_GRASS && centerBlock.getType() != Material.SNOW) {
                    return false;
                }
                centerBlock.setType(Material.PACKED_ICE);
                if (!tl.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                    centerBlock.setType(originalType);
                    return false;
                }
                tl.clone().add(0, 1, 0).getBlock().setType(Material.PACKED_ICE);
                target.teleport(tl.clone().add(0.5, 0, 0.5));
                target.setFreezeTicks(duration * 20);
                world.spawnParticle(Particle.SNOWFLAKE, tl.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0);
                world.playSound(tl, Sound.BLOCK_GLASS_PLACE, 0.8f, 0.5f);
                Bukkit.getScheduler().runTaskLater(com.ultra.uskill.USkillPlugin.getInstance(), () -> {
                    Block b1 = tl.getBlock();
                    Block b2 = tl.clone().add(0, 1, 0).getBlock();
                    if (b1.getType() == Material.PACKED_ICE) b1.setType(Material.AIR);
                    if (b2.getType() == Material.PACKED_ICE) b2.setType(Material.AIR);
                }, duration * 20L);
                return true;
            }

            case ROOT -> {
                int dur = Math.min(action.getIntParam("duration", 3), 10);
                LivingEntity rootTarget = (target != null && target.isValid()) ? target : player;
                PotionEffectType slowness = Registry.EFFECT.get(NamespacedKey.minecraft("slowness"));
                if (slowness != null) {
                    rootTarget.addPotionEffect(new PotionEffect(slowness, dur * 20, 10, false, true, true));
                }
                rootTarget.setFreezeTicks(dur * 20);
                world.spawnParticle(Particle.SCULK_SOUL, rootTarget.getLocation().clone().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0);
                world.playSound(rootTarget.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.4f);
                return true;
            }

            case DISARM -> {
                if (target == null || !(target instanceof Player targetPlayer)) return false;
                ItemStack mainHand = targetPlayer.getInventory().getItemInMainHand().clone();
                if (mainHand.getType().isAir()) return false;
                targetPlayer.getInventory().setItemInMainHand(null);
                targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), mainHand);
                targetPlayer.sendMessage(com.ultra.ucore.util.TextUtil.parse("<red>You have been disarmed!"));
                world.playSound(targetPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 0.8f);
                return true;
            }

            case POISON_CLOUD -> {
                double dmg = action.getDoubleParam("damage", 2.0);
                double radius = action.getDoubleParam("radius", 4.0);
                int duration = Math.min(action.getIntParam("duration", 5), 15);
                Location cloudLoc = (target != null && target.isValid()) ? target.getLocation() : loc;
                var areaCloud = (org.bukkit.entity.AreaEffectCloud) world.spawnEntity(cloudLoc, EntityType.AREA_EFFECT_CLOUD);
                areaCloud.setRadius((float) radius);
                areaCloud.setDuration(duration * 20);
                areaCloud.setWaitTime(0);
                areaCloud.setReapplicationDelay(10);
                areaCloud.setCustomName(null);
                PotionEffectType poison = Registry.EFFECT.get(NamespacedKey.minecraft("poison"));
                if (poison != null) {
                    areaCloud.addCustomEffect(new PotionEffect(poison, 40, 0, false, true, true), true);
                }
                areaCloud.setParticle(Particle.SPORE_BLOSSOM_AIR);
                world.playSound(cloudLoc, Sound.ENTITY_SPIDER_AMBIENT, 0.5f, 0.5f);
                return true;
            }

            case HEALING_CIRCLE -> {
                double healAmt = action.getDoubleParam("heal", 4.0);
                double radius = action.getDoubleParam("radius", 4.0);
                world.spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 0.5, 0), 40, radius, 0.3, radius, 0);
                world.spawnParticle(Particle.HEART, loc.clone().add(0, 1.5, 0), 15, radius / 2, 0.3, radius / 2, 0);
                world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.2f);
                boolean healed = false;
                for (Entity e : world.getNearbyEntities(loc, radius, radius, radius)) {
                    if (e instanceof Player p) {
                        var maxAttr = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                        double maxHp = maxAttr != null ? maxAttr.getValue() : 20.0;
                        p.setHealth(Math.min(maxHp, p.getHealth() + healAmt));
                        healed = true;
                    }
                }
                return healed;
            }

            case SUMMON_TOTEM -> {
                String entityTypeName = action.getStringParam("entity", "IRON_GOLEM").toUpperCase(Locale.ROOT);
                int duration = Math.min(action.getIntParam("duration", 10), 60);
                Location spawnLoc = player.getLocation().add(0, 1, 0);
                try {
                    EntityType et = EntityType.valueOf(entityTypeName);
                    Entity summoned = world.spawnEntity(spawnLoc, et);
                    if (summoned instanceof Mob mob) {
                        mob.setAI(true);
                        mob.setRemoveWhenFarAway(false);
                        if (target != null && target.isValid()) mob.setTarget(target);
                    }
                    if (summoned instanceof Tameable tameable) {
                        tameable.setOwner(player);
                    }
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, spawnLoc, 30, 0.5, 0.5, 0.5, 0.5);
                    world.playSound(spawnLoc, Sound.ENTITY_EVOKER_PREPARE_WOLOLO, 0.7f, 0.9f);
                    Entity finalSummoned = summoned;
                    Bukkit.getScheduler().runTaskLater(com.ultra.uskill.USkillPlugin.getInstance(), () -> {
                        if (finalSummoned.isValid() && !finalSummoned.isDead()) {
                            finalSummoned.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, finalSummoned.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.5);
                            finalSummoned.remove();
                        }
                    }, duration * 20L);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[USkill] Failed to summon totem " + entityTypeName + " in skill " + skill.getId() + ": " + e.getMessage());
                    return false;
                }
                return true;
            }

            case DASH -> {
                double distance = action.getDoubleParam("distance", 5.0);
                Vector dir = player.getLocation().getDirection().normalize();
                Vector dashVel = dir.clone().multiply(distance / 2.0);
                dashVel.setY(Math.max(dashVel.getY(), 0.1));
                player.setVelocity(dashVel);
                player.setFallDistance(-100);
                world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.2, 0), 20, 0.3, 0.1, 0.3, 0.02);
                world.spawnParticle(Particle.POOF, loc.clone().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.01);
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.5f);
                return true;
            }

            case CONSUME_MANA -> {
                double amount = action.getDoubleParam("amount", 10.0);
                com.ultra.uskill.USkillPlugin plugin = com.ultra.uskill.USkillPlugin.getInstance();
                com.ultra.uskill.mana.ManaManager mana = plugin.getManaManager();
                if (mana == null) return false;
                if (mana.consumeMana(player, amount)) {
                    if (action.getBooleanParam("show-bar", true)) {
                        // The HUD task will auto-refresh
                    }
                    return true;
                } else {
                    if (!action.getBooleanParam("silent", false)) {
                        player.sendMessage(com.ultra.ucore.util.TextUtil.parse(
                                plugin.getConfig().getString("mana.no-mana-message", "<red>Not enough mana! Need %s MP").formatted((int) amount)
                        ));
                    }
                    return false;
                }
            }

            default -> { return false; }
        }
    }

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

    public void tickPeriodicSkills(Player player, String itemId, List<Skill> skills, SkillTrigger triggerType) {
        if (skills == null) return;
        for (Skill skill : skills) {
            if (skill == null || skill.getTrigger() != triggerType) continue;
            if (!cooldowns.isReady(player, itemId, skill.getId(), skill.getCooldown())) continue;
            if (!checkChance(skill)) continue;
            if (!checkConditions(player, skill, null)) continue;

            boolean ok = executeSkillActions(player, skill, null);
            if (ok) {
                cooldowns.markUsed(player, itemId, skill.getId());
            }
        }
    }

    private boolean isBlockBreakProtected(Material material) {
        if (material == null) return true;
        if (BLOCK_BREAK_PROTECTED.contains(material)) return true;
        String name = material.name();
        if (name.endsWith("_SHULKER_BOX") || name.equals("SHULKER_BOX")) return true;
        return false;
    }
}
