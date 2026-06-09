package com.ultra.ucore.skill;

import java.util.*;

public class SkillAction {

    public enum ActionType {
        MESSAGE("Send message"),
        ACTION_BAR("Send action bar"),
        TITLE("Send title + subtitle"),
        PLAY_SOUND("Play sound"),
        SPAWN_PARTICLE("Spawn particle"),
        DAMAGE("Deal damage"),
        HEAL("Heal"),
        FEED("Feed"),
        SET_FIRE("Set on fire"),
        LAUNCH("Launch (velocity)"),
        TELEPORT("Teleport"),
        STRIKE_LIGHTNING("Strike lightning"),
        SHOOT_PROJECTILE("Shoot projectile"),
        PLAYER_COMMAND("Run command as player"),
        CONSOLE_COMMAND("Run console command"),
        CONSUME_DURABILITY("Consume durability"),
        CONSUME_ITEM("Consume item"),
        GIVE_POTION("Give potion effect"),
        REMOVE_POTION("Remove potion effect"),
        GIVE_ITEM("Give item"),
        NOTHING("Nothing (placeholder)"),
        BLOCK_BREAK("Break block"),
        EXPLOSION("Create explosion"),
        AREA_DAMAGE("Area damage"),
        POTION_AREA("Area potion effect"),
        BLIND("Blind target"),
        GLOW("Glow target"),
        CLEAR_EFFECTS("Clear all effects"),
        LEVITATE("Levitate"),
        SPAWN_MOB("Spawn mob"),
        FREEZE("Freeze target");

        private final String display;

        ActionType(String display) {
            this.display = display;
        }

        public String getDisplay() { return display; }
    }

    private ActionType type;
    private Map<String, Object> params;

    public SkillAction() {
        this.type = ActionType.MESSAGE;
        this.params = new LinkedHashMap<>();
    }

    public SkillAction(ActionType type) {
        this.type = type;
        this.params = new LinkedHashMap<>();
    }

    public SkillAction(ActionType type, Map<String, Object> params) {
        this.type = type;
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
    }

    public ActionType getType() { return type; }
    public void setType(ActionType type) { this.type = type; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) {
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
    }

    public void setParam(String key, Object value) {
        if (params == null) params = new LinkedHashMap<>();
        params.put(key, value);
    }

    public Object getParam(String key) { return params != null ? params.get(key) : null; }

    public String getStringParam(String key, String def) {
        Object v = getParam(key);
        return v != null ? String.valueOf(v) : def;
    }

    public double getDoubleParam(String key, double def) {
        Object v = getParam(key);
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return def; }
    }

    public int getIntParam(String key, int def) {
        Object v = getParam(key);
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }

    public boolean getBooleanParam(String key, boolean def) {
        Object v = getParam(key);
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(v));
    }

    public static SkillAction message(String text) {
        SkillAction a = new SkillAction(ActionType.MESSAGE);
        a.setParam("text", text);
        return a;
    }

    public static SkillAction actionBar(String text) {
        SkillAction a = new SkillAction(ActionType.ACTION_BAR);
        a.setParam("text", text);
        return a;
    }

    public static SkillAction playSound(String sound, float volume, float pitch) {
        SkillAction a = new SkillAction(ActionType.PLAY_SOUND);
        a.setParam("sound", sound);
        a.setParam("volume", volume);
        a.setParam("pitch", pitch);
        return a;
    }

    public static SkillAction givePotion(String effect, int durationSeconds, int amplifier) {
        SkillAction a = new SkillAction(ActionType.GIVE_POTION);
        a.setParam("effect", effect);
        a.setParam("duration", durationSeconds);
        a.setParam("amplifier", amplifier);
        return a;
    }

    public static SkillAction damage(double amount) {
        SkillAction a = new SkillAction(ActionType.DAMAGE);
        a.setParam("amount", amount);
        return a;
    }

    public static SkillAction heal(double amount) {
        SkillAction a = new SkillAction(ActionType.HEAL);
        a.setParam("amount", amount);
        return a;
    }

    public static SkillAction launch(double x, double y, double z) {
        SkillAction a = new SkillAction(ActionType.LAUNCH);
        a.setParam("x", x);
        a.setParam("y", y);
        a.setParam("z", z);
        return a;
    }

    public static SkillAction shootProjectile(String projectile, double speed, double damage) {
        SkillAction a = new SkillAction(ActionType.SHOOT_PROJECTILE);
        a.setParam("projectile", projectile);
        a.setParam("speed", speed);
        a.setParam("damage", damage);
        return a;
    }

    public static SkillAction consoleCommand(String cmd) {
        SkillAction a = new SkillAction(ActionType.CONSOLE_COMMAND);
        a.setParam("command", cmd);
        return a;
    }

    public static SkillAction playerCommand(String cmd) {
        SkillAction a = new SkillAction(ActionType.PLAYER_COMMAND);
        a.setParam("command", cmd);
        return a;
    }

    public static SkillAction strikeLightning() {
        return new SkillAction(ActionType.STRIKE_LIGHTNING);
    }

    public static SkillAction consumeDurability(int amount) {
        SkillAction a = new SkillAction(ActionType.CONSUME_DURABILITY);
        a.setParam("amount", amount);
        return a;
    }

    public static SkillAction blockBreak() {
        SkillAction a = new SkillAction(ActionType.BLOCK_BREAK);
        a.setParam("range", 5);
        a.setParam("radius", 0);
        return a;
    }

    public static SkillAction explosion(float power, boolean fire) {
        SkillAction a = new SkillAction(ActionType.EXPLOSION);
        a.setParam("power", power);
        a.setParam("fire", fire);
        return a;
    }

    public static SkillAction areaDamage(double amount, double radius) {
        SkillAction a = new SkillAction(ActionType.AREA_DAMAGE);
        a.setParam("amount", amount);
        a.setParam("radius", radius);
        return a;
    }

    public static SkillAction potionArea(String effect, int duration, int amplifier, double radius) {
        SkillAction a = new SkillAction(ActionType.POTION_AREA);
        a.setParam("effect", effect);
        a.setParam("duration", duration);
        a.setParam("amplifier", amplifier);
        a.setParam("radius", radius);
        return a;
    }

    public static SkillAction blind(int duration) {
        SkillAction a = new SkillAction(ActionType.BLIND);
        a.setParam("duration", duration);
        return a;
    }

    public static SkillAction glow(int duration) {
        SkillAction a = new SkillAction(ActionType.GLOW);
        a.setParam("duration", duration);
        return a;
    }

    public static SkillAction clearEffects() {
        return new SkillAction(ActionType.CLEAR_EFFECTS);
    }

    public static SkillAction levitate(int duration, int amplifier) {
        SkillAction a = new SkillAction(ActionType.LEVITATE);
        a.setParam("duration", duration);
        a.setParam("amplifier", amplifier);
        return a;
    }

    public static SkillAction spawnMob(String entityType, int count) {
        SkillAction a = new SkillAction(ActionType.SPAWN_MOB);
        a.setParam("entity", entityType);
        a.setParam("count", count);
        return a;
    }

    public static SkillAction freeze(int duration) {
        SkillAction a = new SkillAction(ActionType.FREEZE);
        a.setParam("duration", duration);
        return a;
    }

    public SkillAction copy() {
        return new SkillAction(this.type, new LinkedHashMap<>(this.params));
    }

    @Override
    public String toString() {
        return type + " " + params;
    }
}
