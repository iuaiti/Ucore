package com.ultra.umenu.model;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucore.config.ConfigManager;
import com.ultra.ucore.item.ItemBuilder;
import com.ultra.umenu.util.HeadUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuItemLoader {

    private final ConfigManager configManager;
    private final Map<String, MenuItem> cache = new ConcurrentHashMap<>();

    public MenuItemLoader(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void load() {
        cache.clear();
        var items = configManager.get("items.yml");
        for (String key : items.getKeys(false)) {
            Material material = Material.getMaterial(items.getString(key + ".material", "STONE").toUpperCase());
            if (material == null) material = Material.STONE;

            String name = items.getString(key + ".name", "");
            List<String> lore = items.getStringList(key + ".lore");
            String texture = items.getString(key + ".texture");
            int customModelData = items.getInt(key + ".custom-model-data", 0);
            int amount = items.getInt(key + ".amount", 1);
            boolean enchanted = items.getBoolean(key + ".enchanted", false);

            cache.put(key.toLowerCase(), new MenuItem(
                    key, material, name, lore, texture, customModelData, amount, enchanted
            ));
        }
    }

    public ItemStack build(String key, Player player) {
        MenuItem menuItem = cache.get(key.toLowerCase());
        if (menuItem == null) {
            return ItemBuilder.create(Material.BARRIER)
                    .name("<red>Unknown item: " + key + "</red>")
                    .build();
        }
        return buildItem(menuItem, player);
    }

    private ItemStack buildItem(MenuItem item, Player player) {
        ItemStack stack;

        boolean hasTexture = item.texture() != null && !item.texture().isEmpty();

        if (hasTexture) {
            stack = HeadUtil.createHead(item.texture());
            if (stack == null) {
                stack = new ItemStack(item.material(), Math.max(1, item.amount()));
            }
        } else {
            stack = new ItemStack(item.material(), Math.max(1, item.amount()));
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        if (!item.name().isEmpty()) {
            String parsedName = applyPlaceholders(item.name(), player);
            meta.displayName(MiniMessage.miniMessage().deserialize(parsedName));
        }

        if (!item.lore().isEmpty()) {
            List<net.kyori.adventure.text.Component> loreComponents = item.lore().stream()
                    .map(line -> applyPlaceholders(line, player))
                    .map(line -> MiniMessage.miniMessage().deserialize(line))
                    .toList();
            meta.lore(loreComponents);
        }

        if (item.customModelData() != 0) {
            meta.setCustomModelData(item.customModelData());
        }

        if (item.enchanted()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        stack.setItemMeta(meta);

        if (item.amount() > 1) {
            stack.setAmount(item.amount());
        }

        return stack;
    }

    public String applyPlaceholders(String text, Player player) {
        return UCoreAPI.getPlaceholderManager().apply(text,
                new com.ultra.ucore.placeholder.PlaceholderManager.PlaceholderContext(player, player));
    }

    public void reload() {
        load();
    }
}
