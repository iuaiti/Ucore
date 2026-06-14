package com.ultra.ucrate.manager;

import com.ultra.ucrate.api.events.KeyGiveEvent;
import com.ultra.ucrate.model.KeyType;
import com.ultra.ucrate.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class KeyManager {

    private final JavaPlugin plugin;
    private final StorageProvider storage;
    private final NamespacedKey crateKey;
    private final FileConfiguration config;

    public KeyManager(JavaPlugin plugin, StorageProvider storage, FileConfiguration config) {
        this.plugin = plugin;
        this.storage = storage;
        this.crateKey = new NamespacedKey(plugin, "crate_key");
        this.config = config;
    }

    @NotNull
    public NamespacedKey getCrateKey() {
        return crateKey;
    }

    public int getVirtualKeys(@NotNull Player player, @NotNull String crateId) {
        return storage.getKeys(player.getUniqueId(), crateId);
    }

    public boolean hasVirtualKeys(@NotNull Player player, @NotNull String crateId, int amount) {
        return getVirtualKeys(player, crateId) >= amount;
    }

    public void addVirtualKeys(@NotNull Player player, @NotNull String crateId, int amount) {
        storage.addKeys(player.getUniqueId(), crateId, amount);
    }

    public boolean removeVirtualKeys(@NotNull Player player, @NotNull String crateId, int amount) {
        if (!hasVirtualKeys(player, crateId, amount)) return false;
        storage.removeKeys(player.getUniqueId(), crateId, amount);
        return true;
    }

    public boolean giveVirtualKeys(
            @NotNull Player player,
            @NotNull String crateId,
            int amount,
            @Nullable CommandSender giver
    ) {
        KeyGiveEvent event = new KeyGiveEvent(player, crateId, KeyType.VIRTUAL, amount, giver);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        addVirtualKeys(player, crateId, event.getAmount());
        return true;
    }

    public boolean takeVirtualKeys(
            @NotNull Player player,
            @NotNull String crateId,
            int amount,
            @Nullable CommandSender taker
    ) {
        return removeVirtualKeys(player, crateId, amount);
    }

    public void givePhysicalKey(
            @NotNull Player player,
            @NotNull String crateId,
            int amount,
            @Nullable CommandSender giver
    ) {
        KeyGiveEvent event = new KeyGiveEvent(player, crateId, KeyType.PHYSICAL, amount, giver);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        int finalAmount = Math.max(1, Math.min(64, event.getAmount()));
        ItemStack keyItem = createPhysicalKey(crateId);
        keyItem.setAmount(finalAmount);

        var leftovers = player.getInventory().addItem(keyItem);
        for (ItemStack left : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
    }

    @Nullable
    public String getCrateIdFromItem(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(crateKey, PersistentDataType.STRING);
    }

    @NotNull
    public ItemStack createPhysicalKey(@NotNull String crateId) {
        Material material = Material.valueOf(
                config.getString("keys.physical.material", "TRIPWIRE_HOOK")
        );
        String name = config.getString("keys.physical.name", "<gold>%crate% Key</gold>")
                .replace("%crate%", crateId)
                .replace("%crate_key%", crateId);
        List<String> lore = config.getStringList("keys.physical.lore");
        lore = lore.stream()
                .map(line -> line
                        .replace("%crate%", crateId)
                        .replace("%crate_key%", crateId))
                .toList();
        boolean glow = config.getBoolean("keys.physical.glow", true);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(crateKey, PersistentDataType.STRING, crateId);
            item.setItemMeta(meta);
        }

        var builder = com.ultra.ucore.item.ItemBuilder.create(material)
                .name(name)
                .lore(lore.toArray(String[]::new));

        if (glow) {
            builder.enchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            builder.flags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        builder.pdcString(crateKey, crateId);
        return builder.build();
    }

    public boolean consumePhysicalKey(@NotNull Player player, @NotNull ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        return true;
    }

    public boolean hasKey(@NotNull Player player, @NotNull CrateManager crateManager, @NotNull String crateId) {
        CrateManager cm = crateManager;
        var crate = cm.getCrate(crateId);
        if (crate == null) return false;
        if (!crate.requireKey()) return true;

        if (hasVirtualKeys(player, crateId, 1)) return true;

        for (ItemStack item : player.getInventory()) {
            String id = getCrateIdFromItem(item);
            if (crateId.equals(id)) return true;
        }

        return false;
    }

    public boolean consumeKey(@NotNull Player player, @NotNull String crateId) {
        if (hasVirtualKeys(player, crateId, 1)) {
            return removeVirtualKeys(player, crateId, 1);
        }

        for (ItemStack item : player.getInventory().getContents()) {
            String id = getCrateIdFromItem(item);
            if (crateId.equals(id)) {
                return consumePhysicalKey(player, item);
            }
        }

        return false;
    }
}
