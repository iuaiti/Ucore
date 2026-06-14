package com.ultra.ucrate;

import com.ultra.ucore.api.UCoreAPI;
import com.ultra.ucrate.animation.InstantAnimation;
import com.ultra.ucrate.animation.MysteryBoxAnimation;
import com.ultra.ucrate.animation.RouletteAnimation;
import com.ultra.ucrate.api.UCrateAPI;
import com.ultra.ucrate.api.events.CrateOpenEvent;
import com.ultra.ucrate.api.events.CrateRewardEvent;
import com.ultra.ucrate.animation.CrateAnimation;
import com.ultra.ucrate.manager.*;
import com.ultra.ucrate.model.Crate;
import com.ultra.ucrate.model.Reward;
import com.ultra.ucrate.storage.StorageProvider;
import com.ultra.ucrate.storage.YamlStorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class UCrateProvider implements UCrateAPI {

    private static UCrateProvider instance;

    private final JavaPlugin plugin;
    private final CrateManager crateManager;
    private final KeyManager keyManager;
    private final RewardManager rewardManager;
    private final AnimationManager animationManager;
    private final CacheManager cacheManager;
    private final YamlStorageProvider storageProvider;

    public UCrateProvider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storageProvider = new YamlStorageProvider(plugin);
        this.crateManager = new CrateManager(storageProvider);
        this.keyManager = new KeyManager(plugin, storageProvider, plugin.getConfig());
        this.rewardManager = new RewardManager();
        this.animationManager = new AnimationManager();
        this.cacheManager = new CacheManager();
    }

    public void init() {
        instance = this;

        storageProvider.init();

        crateManager.loadAll();

        animationManager.register(new InstantAnimation());
        animationManager.register(new RouletteAnimation());
        animationManager.register(new MysteryBoxAnimation());

        var placeholderManager = UCoreAPI.getPlaceholderManager();
        placeholderManager.register("ucrate_version", ctx -> plugin.getPluginMeta().getVersion());
        placeholderManager.register("ucrate_crates", ctx ->
                String.valueOf(crateManager.crateCount())
        );
    }

    public void shutdown() {
        storageProvider.saveAll();
        instance = null;
    }

    public void reload() {
        storageProvider.reload();
        crateManager.loadAll();
    }

    public static UCrateProvider get() {
        if (instance == null) {
            throw new IllegalStateException("UCrate has not been initialized.");
        }
        return instance;
    }

    @Nullable
    public static UCrateProvider getInstance() {
        return instance;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    @Override
    @NotNull
    public CrateManager getCrateManager() {
        return crateManager;
    }

    @Override
    @NotNull
    public KeyManager getKeyManager() {
        return keyManager;
    }

    @Override
    @NotNull
    public RewardManager getRewardManager() {
        return rewardManager;
    }

    @Override
    @NotNull
    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    @Override
    public void openCrate(@NotNull Player player, @NotNull Crate crate) {
        String permission = crate.permission();
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            UCoreAPI.getMessageManager().send(player, "crate.no-permission");
            return;
        }

        if (cacheManager.hasCooldown(player, crate.id())) {
            long seconds = cacheManager.getCooldownSeconds(player, crate.id());
            var msg = UCoreAPI.getMessageManager().component(player, "crate.on-cooldown");
            player.sendMessage(msg);
            return;
        }

        if (crate.requireKey() && !keyManager.hasKey(player, crateManager, crate.id())) {
            UCoreAPI.getMessageManager().send(player, "crate.no-key");
            return;
        }

        CrateOpenEvent openEvent = new CrateOpenEvent(crate, player);
        Bukkit.getPluginManager().callEvent(openEvent);
        if (openEvent.isCancelled()) return;

        if (crate.requireKey()) {
            keyManager.consumeKey(player, crate.id());
        }

        cacheManager.setCooldown(player, crate.id(), crate.cooldownSeconds());

        Reward reward = rewardManager.rollReward(crate);
        if (reward == null) {
            player.sendMessage("No rewards available.");
            return;
        }

        CrateAnimation animation = animationManager.getAnimation(crate.animationId());
        if (animation == null) {
            rewardManager.executeReward(player, crate, reward);
            return;
        }

        animation.animate(player, crate, reward, () -> {
            UCoreAPI.getSchedulerService().sync(() ->
                    rewardManager.executeReward(player, crate, reward)
            );
        });
    }

    @Override
    public void openCrate(@NotNull Player player, @NotNull String crateId) {
        Crate crate = crateManager.getCrate(crateId);
        if (crate == null) {
            UCoreAPI.getMessageManager().send(player, "crate.not-found");
            return;
        }
        openCrate(player, crate);
    }

    @Override
    public void registerAnimation(@NotNull CrateAnimation animation) {
        animationManager.register(animation);
    }

    @Override
    public boolean giveKey(@NotNull Player player, @NotNull String crateId, int amount) {
        return keyManager.giveVirtualKeys(player, crateId, amount, null);
    }

    @Override
    public boolean takeKey(@NotNull Player player, @NotNull String crateId, int amount) {
        return keyManager.takeVirtualKeys(player, crateId, amount, null);
    }

    @Override
    public int getKeyCount(@NotNull Player player, @NotNull String crateId) {
        return keyManager.getVirtualKeys(player, crateId);
    }

    @Override
    @NotNull
    public Optional<Crate> getCrate(@NotNull String crateId) {
        return crateManager.findCrate(crateId);
    }

    @Override
    @Nullable
    public Reward rollReward(@NotNull Crate crate) {
        return rewardManager.rollReward(crate);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }
}
