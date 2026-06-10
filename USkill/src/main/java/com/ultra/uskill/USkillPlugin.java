package com.ultra.uskill;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.uskill.api.USkillAPI;
import com.ultra.uskill.api.USkillProvider;
import com.ultra.uskill.cooldown.CooldownManager;
import com.ultra.uskill.execution.SkillExecutor;
import com.ultra.uskill.tree.SkillTreeManager;
import com.ultra.uskill.command.SkillTreeCommand;
import com.ultra.uskill.mana.ManaManager;
import com.ultra.ucore.skill.Skill;
import com.ultra.ucore.skill.SkillAction;
import com.ultra.ucore.skill.SkillCondition;
import com.ultra.ucore.skill.SkillTrigger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class USkillPlugin extends JavaPlugin implements USkillAPI, Listener {

    private static USkillPlugin instance;
    private CooldownManager cooldownManager;
    private SkillExecutor executor;
    private SkillTreeManager skillTreeManager;
    private ManaManager manaManager;

    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getPluginManager().getPlugin("UCore") == null) {
            getLogger().severe("UCore not found! Disabling USkill...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.cooldownManager = new CooldownManager(this);
        this.executor = new SkillExecutor(this.cooldownManager);

        USkillProvider.register(this);

        // Bridge: register this plugin as the SkillService via UCore
        UCoreAPI.registerSkillService(this);

        // Bridge: register USkill placeholders via UCore's PlaceholderManager
        UCoreAPI.getPlaceholderManager().register("uskill_version",
                ctx -> getPluginMeta().getVersion());

        // ─── Skill Tree ───
        this.skillTreeManager = new SkillTreeManager(this);
        this.skillTreeManager.load();

        // Register /skilltree command
        SkillTreeCommand skillTreeCmd = new SkillTreeCommand(this, this.skillTreeManager);
        var cmd = getCommand("skilltree");
        if (cmd != null) {
            cmd.setExecutor(skillTreeCmd);
            cmd.setTabCompleter(skillTreeCmd);
        }

        // ─── Mana System ───
        saveDefaultConfig();
        this.manaManager = new ManaManager(this, this.skillTreeManager);
        this.manaManager.start();

        UCoreAPI.getPlaceholderManager().register("mana",
                ctx -> ctx.player() != null ? String.valueOf((int) manaManager.getMana(ctx.player())) : "0");
        UCoreAPI.getPlaceholderManager().register("max_mana",
                ctx -> ctx.player() != null ? String.valueOf((int) manaManager.getMaxMana(ctx.player())) : "0");
        UCoreAPI.getPlaceholderManager().register("mana_percent",
                ctx -> {
                    if (ctx.player() == null) return "0";
                    double max = manaManager.getMaxMana(ctx.player());
                    if (max <= 0) return "0";
                    return String.valueOf((int) ((manaManager.getMana(ctx.player()) / max) * 100));
                });

        // Register join/quit events for skill tree stats
        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("╔══════════════════════════════════════════════╗");
        getLogger().info("║        ⚡ USkill v" + getPluginMeta().getVersion() + " — Skill Engine");
        getLogger().info("║        ◆ UCore bridge: active");
        getLogger().info("║        ◆ Skill Tree: " + skillTreeManager.getNodeCount() + " nodes loaded");
        getLogger().info("╚══════════════════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        UCoreAPI.getPlaceholderManager().unregister("uskill_version");
        if (manaManager != null) manaManager.stop();
        if (skillTreeManager != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                skillTreeManager.onPlayerQuit(p);
            }
        }
        USkillProvider.unregister();
        instance = null;
    }

    public ManaManager getManaManager() {
        return manaManager;
    }

    public SkillTreeManager getSkillTreeManager() {
        return skillTreeManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (skillTreeManager != null) {
            skillTreeManager.onPlayerJoin(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        onPlayerQuit(event.getPlayer());
    }

    @Override
    public void onPlayerQuit(Player player) {
        if (skillTreeManager != null) {
            skillTreeManager.onPlayerQuit(player);
        }
        if (manaManager != null) {
            manaManager.onPlayerQuit(player);
        }
        cooldownManager.onPlayerQuit(player);
        executor.clearPlayer(player);
    }

    public static USkillPlugin getInstance() {
        return instance;
    }

    @Override
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    @Override
    public SkillExecutor getExecutor() {
        return executor;
    }

    @Override
    public boolean executeSkill(Player player, Skill skill, LivingEntity target) {
        return executor.executeSkill(player, skill, target);
    }

    @Override
    public boolean executeSkills(Player player, String itemId, List<Skill> skills, SkillTrigger trigger, LivingEntity target) {
        return executor.executeSkills(player, itemId, skills, trigger, target);
    }

    @Override
    public void tickPeriodicSkills(Player player, String itemId, List<Skill> skills, SkillTrigger triggerType) {
        executor.tickPeriodicSkills(player, itemId, skills, triggerType);
    }

    @Override
    public boolean isReady(Player player, String itemId, String skillId, double cooldownSeconds) {
        return cooldownManager.isReady(player, itemId, skillId, cooldownSeconds);
    }

    @Override
    public double getRemainingSeconds(Player player, String itemId, String skillId, double cooldownSeconds) {
        return cooldownManager.getRemainingSeconds(player, itemId, skillId, cooldownSeconds);
    }

    @Override
    public void register(Player player, String itemId, List<Skill> skills, SkillTrigger trigger) {
    }

    @Override
    public void unregister(Player player) {
    }

    @Override
    public boolean checkConditions(Player player, Skill skill, LivingEntity target) {
        return executor.checkConditions(player, skill, target);
    }

    @Override
    public List<Skill> parseSkills(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        List<Skill> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String skillId = entry.getKey();
            Object value = entry.getValue();
            if (!(value instanceof Map<?, ?> skillMap)) continue;
            Skill skill = parseOneSkill(skillId, castMap(skillMap));
            if (skill != null) result.add(skill);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Skill parseOneSkill(String id, Map<String, Object> data) {
        Skill skill = new Skill(id);
        skill.setDisplayName(objStr(data.get("display-name"), id));
        skill.setTrigger(SkillTrigger.fromString(objStr(data.get("trigger"), "RIGHT_CLICK")));
        skill.setCooldown(objDouble(data.get("cooldown"), 0.0));
        skill.setCancelEvent(objBool(data.get("cancel-event"), true));
        skill.setChance(objDouble(data.get("chance"), 100.0));
        skill.setPeriodTicks(objInt(data.get("period-ticks"), 20));

        // conditions
        Object condsRaw = data.get("conditions");
        if (condsRaw instanceof Map<?, ?> condMap) {
            List<SkillCondition> conds = new ArrayList<>();
            for (Object condVal : condMap.values()) {
                if (!(condVal instanceof Map<?, ?> condData)) continue;
                SkillCondition c = new SkillCondition();
                String ctype = objStr(condData.get("type"), "ALWAYS");
                try {
                    c.setType(SkillCondition.ConditionType.valueOf(ctype.toUpperCase(Locale.ROOT)));
                } catch (Exception e) {
                    c.setType(SkillCondition.ConditionType.ALWAYS);
                }
                for (Map.Entry<String, Object> pe : ((Map<String, Object>) condData).entrySet()) {
                    if ("type".equals(pe.getKey())) continue;
                    c.setParam(pe.getKey(), pe.getValue());
                }
                conds.add(c);
            }
            skill.setConditions(conds);
        }

        // actions
        Object actsRaw = data.get("actions");
        if (actsRaw instanceof Map<?, ?> actMap) {
            List<SkillAction> acts = new ArrayList<>();
            for (Object actVal : actMap.values()) {
                if (!(actVal instanceof Map<?, ?> actData)) continue;
                String atype = objStr(actData.get("type"), "MESSAGE");
                SkillAction a = new SkillAction();
                try {
                    a.setType(SkillAction.ActionType.valueOf(atype.toUpperCase(Locale.ROOT)));
                } catch (Exception e) {
                    a.setType(SkillAction.ActionType.MESSAGE);
                }
                for (Map.Entry<String, Object> pe : ((Map<String, Object>) actData).entrySet()) {
                    if ("type".equals(pe.getKey())) continue;
                    a.setParam(pe.getKey(), pe.getValue());
                }
                acts.add(a);
            }
            skill.setActions(acts);
        }

        return skill;
    }

    @Override
    public Map<String, Object> serializeSkills(Collection<Skill> skills) {
        if (skills == null || skills.isEmpty()) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        for (Skill sk : skills) {
            if (sk == null) continue;
            String key = sk.getId() != null ? sk.getId() : "skill_" + result.size();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("display-name", sk.getDisplayName());
            if (sk.getTrigger() != null) data.put("trigger", sk.getTrigger().name());
            if (sk.getCooldown() > 0) data.put("cooldown", sk.getCooldown());
            data.put("cancel-event", sk.isCancelEvent());
            if (sk.getChance() < 100) data.put("chance", sk.getChance());
            if (sk.getPeriodTicks() != 20) data.put("period-ticks", sk.getPeriodTicks());

            // conditions
            if (sk.getConditions() != null && !sk.getConditions().isEmpty()) {
                Map<String, Object> conds = new LinkedHashMap<>();
                for (int ci = 0; ci < sk.getConditions().size(); ci++) {
                    SkillCondition c = sk.getConditions().get(ci);
                    Map<String, Object> cd = new LinkedHashMap<>();
                    if (c.getType() != null) cd.put("type", c.getType().name());
                    if (c.getParams() != null) cd.putAll(c.getParams());
                    conds.put("c" + ci, cd);
                }
                data.put("conditions", conds);
            }

            // actions
            if (sk.getActions() != null && !sk.getActions().isEmpty()) {
                Map<String, Object> acts = new LinkedHashMap<>();
                for (int ai = 0; ai < sk.getActions().size(); ai++) {
                    SkillAction a = sk.getActions().get(ai);
                    Map<String, Object> ad = new LinkedHashMap<>();
                    if (a.getType() != null) ad.put("type", a.getType().name());
                    if (a.getParams() != null) ad.putAll(a.getParams());
                    acts.put("a" + ai, ad);
                }
                data.put("actions", acts);
            }

            result.put(key, data);
        }
        return result;
    }

    private static String objStr(Object v, String def) {
        return v instanceof String s ? s : def;
    }

    private static double objDouble(Object v, double def) {
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return def; }
    }

    private static int objInt(Object v, int def) {
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }

    private static boolean objBool(Object v, boolean def) {
        return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            result.put(String.valueOf(e.getKey()), e.getValue());
        }
        return result;
    }
}
