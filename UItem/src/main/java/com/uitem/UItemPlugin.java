package com.uitem;

import com.uitem.api.UItemAPI;
import com.uitem.chat.ChatInputManager;
import com.uitem.command.UItemCommand;
import com.uitem.gui.GuiListener;
import com.uitem.integration.IntegrationManager;
import com.uitem.listener.UItemActionListener;
import com.uitem.rarity.RarityConfig;
import com.uitem.recipe.RecipeManager;
import com.uitem.resourcepack.ResourcePackManager;
import com.uitem.resourcepack.ResourcePackServer;
import com.uitem.storage.StorageManager;
import com.uitem.util.Messages;
import com.uitem.util.SchedulerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class UItemPlugin extends JavaPlugin {

    private static UItemPlugin instance;
    private UItemManager itemManager;
    private StorageManager storageManager;
    private IntegrationManager integrationManager;
    private ChatInputManager chatInputManager;
    private GuiListener guiListener;
    private ResourcePackManager resourcePackManager;
    private com.uitem.search.SearchManager searchManager;
    private ResourcePackServer packServer;
    private RecipeManager recipeManager;
    private Listener resourcePackJoinListener;

    // Native Skill system (delegates to USkill)
    private com.uitem.legacy.skill.UItemSkillListener skillListener;
    private com.uitem.legacy.skill.SkillActionBar skillActionBar;

    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getPluginManager().getPlugin("UCore") == null) {
            getLogger().severe("UCore not found! Disabling UItem...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Ensure split config folders exist for items and recipes (user requested)
        File itemsDir = new File(getDataFolder(), "items");
        File recipesDir = new File(getDataFolder(), "recipes");
        itemsDir.mkdirs();
        recipesDir.mkdirs();

        // Also create source folders for resource pack (textures + custom models)
        new File(getDataFolder(), "textures").mkdirs();
        new File(getDataFolder(), "models").mkdirs();

        // Seed example external config files if folders are empty (makes copy-paste easy)
        seedExampleConfigsIfNeeded(itemsDir, recipesDir);

        // Create main external config files item.yml and recipe.yml ONLY if user wants consolidated files
        boolean useConsolidated = getConfig().getBoolean("storage.use-consolidated-files", false);
        if (useConsolidated) {
            createMainExternalConfigIfNeeded(itemsDir, "item.yml", "items");
            createMainExternalConfigIfNeeded(recipesDir, "recipe.yml", "recipes");
        }

        saveDefaultConfig();

        this.storageManager = new StorageManager(this);
        this.itemManager = new UItemManager(this);
        this.integrationManager = new IntegrationManager(this);
        this.chatInputManager = new ChatInputManager(this);
        this.guiListener = new GuiListener();
        this.resourcePackManager = new ResourcePackManager(this, this.itemManager);
        this.searchManager = new com.uitem.search.SearchManager();

        storageManager.loadAll(itemManager);
        integrationManager.detectIntegrations();
        resourcePackManager.syncCmdsToLiveItems();

        this.recipeManager = new RecipeManager(this, this.itemManager);

        // === Native Skill & Passive System (delegates to USkill via UCore's SkillService) ===
        this.skillListener = new com.uitem.legacy.skill.UItemSkillListener(this);
        this.skillActionBar = new com.uitem.legacy.skill.SkillActionBar(this);
        this.skillActionBar.start();

        RarityConfig.load(this);

        // Khởi tạo Messages (language system)
        com.uitem.util.Messages.init(this);

        // Khởi tạo Public API — phải trước khi đăng ký listener
        UItemAPI.init(this);

        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(chatInputManager, this);
        getServer().getPluginManager().registerEvents(new UItemActionListener(this), this);
        getServer().getPluginManager().registerEvents(skillListener, this);

        UItemCommand command = new UItemCommand(this);
        Objects.requireNonNull(getCommand("uitem")).setExecutor(command);
        Objects.requireNonNull(getCommand("uitem")).setTabCompleter(command);

        com.uitem.command.CraftingCommand craftingCommand = new com.uitem.command.CraftingCommand(this);
        org.bukkit.command.PluginCommand craftCmd = getCommand("crafting");
        if (craftCmd != null) {
            craftCmd.setExecutor(craftingCommand);
        }

        // Khởi động resource pack server
        startPackServer();

        getLogger().info("╔══════════════════════════════════════════════╗");
        getLogger().info("║        ⚡ UItem v" + getPluginMeta().getVersion() + " — Custom Item Engine");
        getLogger().info("║        ✦ Author: iuaititi");
        getLogger().info("╠══════════════════════════════════════════════╣");
        getLogger().info("║  ■ Loaded: " + itemManager.getAllItems().size() + " custom items");
        getLogger().info("║  ■ Integrations: " + integrationManager.getStatus());
        getLogger().info("║  ■ Scheduler: " + (SchedulerUtil.isFolia() ? "🌐 Folia (Region)" : "☕ Paper/Spigot"));
        getLogger().info("╚══════════════════════════════════════════════╝");

        // Auto-export & auto-apply resource pack
        if (getConfig().getBoolean("resourcepack.auto-generate", true)) {
            SchedulerUtil.runAsync(this, () -> {
                try {
                    resourcePackManager.exportZip();
                    getLogger().info("[UItem] Resource pack exported successfully.");
                    if (getConfig().getBoolean("resourcepack.auto-apply", true) && packServer != null && packServer.isRunning()) {
                        String url = packServer.getPackUrl();
                        byte[] hash = resourcePackManager.getLastPackHash();
                        SchedulerUtil.runSync(UItemPlugin.this, () -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                resourcePackManager.applyResourcePack(p, url, hash);
                            }
                        });
                    }
                } catch (IOException e) {
                    getLogger().warning("Could not generate/export resource pack: " + e.getMessage());
                }
            });
        }

        // Đăng ký join listener sau cùng
        registerResourcePackJoinListener();

        // Bắt plugin enable để re-detect integrations nếu plugin tới sau
        registerIntegrationEnableListener();
    }

    @Override
    public void onDisable() {
        // Force-close all UItem GUIs to prevent item dupes during reload
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                var topInv = player.getOpenInventory().getTopInventory();
                if (topInv == null) continue;
                var holder = topInv.getHolder();
                if (holder instanceof com.uitem.gui.CraftingGui craftingGui) {
                    craftingGui.onClose();
                } else if (holder instanceof com.uitem.gui.RecipeEditorGui recipeEditorGui) {
                    recipeEditorGui.onClose(player);
                }
                if (holder instanceof com.uitem.gui.AbstractGui || holder instanceof com.uitem.gui.CraftingGui || holder instanceof com.uitem.gui.RecipeEditorGui) {
                    player.closeInventory();
                }
            } catch (Exception ignored) {
                // Player might be disconnecting, ignore to prevent crash on shutdown
            }
        }
        // Clear static crafting GUI tracking set
        com.uitem.gui.CraftingGui.removeCraftingPlayer(null);

        if (resourcePackJoinListener != null) {
            HandlerList.unregisterAll(resourcePackJoinListener);
            resourcePackJoinListener = null;
        }

        if (packServer != null) {
            packServer.stop();
        }
        if (storageManager != null && itemManager != null) {
            storageManager.saveAllNow(itemManager);
        }
        if (recipeManager != null) {
            recipeManager.saveRecipes();
        }
        if (skillListener != null) {
            skillListener.shutdown();
        }
        if (skillActionBar != null) {
            skillActionBar.shutdown();
        }
        UItemAPI.shutdown();
        getLogger().info("╔══════════════════════════════════════════════╗");
        getLogger().info("║   ✦ UItem disabled. All items saved.         ");
        getLogger().info("║   ✦ See you next time, iuaititi!              ");
        getLogger().info("╚══════════════════════════════════════════════╝");
        instance = null;
    }

    private void startPackServer() {
        if (!getConfig().getBoolean("resourcepack.auto-host", true)) {
            getLogger().info("[UItem] Resource pack server disabled in config.");
            return;
        }
        int port = getConfig().getInt("resourcepack.server-port", 9732);
        this.packServer = new ResourcePackServer(this);
        try {
            packServer.start(port);
            getLogger().info("[UItem] Resource pack server started on port " + port);
        } catch (IOException e) {
            getLogger().warning("[UItem] Could not start resource pack server: " + e.getMessage());
            getLogger().warning("[UItem] Auto-apply resource pack will not work. Set resourcepack.url manually.");
            this.packServer = null;
        }
    }

    private void registerResourcePackJoinListener() {
        if (resourcePackJoinListener != null) {
            HandlerList.unregisterAll(resourcePackJoinListener);
        }
        resourcePackJoinListener = new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                if (!getConfig().getBoolean("resourcepack.auto-apply", true)) {
                    return;
                }

                Player player = event.getPlayer();
                String url = null;
                byte[] hash = null;

                // Ưu tiên URL từ config (nếu người dùng tự host)
                String configUrl = getConfig().getString("resourcepack.url", "");
                if (configUrl != null && !configUrl.isBlank()) {
                    url = configUrl;
                    hash = resourcePackManager.getLastPackHash();
                } else if (packServer != null && packServer.isRunning()) {
                    // Tự động dùng server local
                    url = packServer.getPackUrl();
                    hash = resourcePackManager.getLastPackHash();
                }

                if (url == null || url.isBlank()) {
                    return;
                }

                String title = Messages.isVietnamese() ? "Đang tải resource pack, vui lòng chờ" : "Loading resource packs, please wait";
                player.showTitle(net.kyori.adventure.title.Title.title(
                        net.kyori.adventure.text.Component.text(title),
                        net.kyori.adventure.text.Component.text(title),
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(250),
                                java.time.Duration.ofMillis(3000),
                                java.time.Duration.ofMillis(500)
                        )
                ));

                byte[] finalHash = hash;
                String finalUrl = url;
                SchedulerUtil.runSync(UItemPlugin.this, () ->
                        resourcePackManager.applyResourcePack(player, finalUrl, finalHash));
            }

            @EventHandler
            public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
                if (!getConfig().getBoolean("resourcepack.auto-apply", true)) {
                    return;
                }
                if (!getConfig().getBoolean("resourcepack.kick-on-fail", true)) {
                    return;
                }

                PlayerResourcePackStatusEvent.Status status = event.getStatus();
                if (status == PlayerResourcePackStatusEvent.Status.DECLINED
                        || status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD
                        || status == PlayerResourcePackStatusEvent.Status.INVALID_URL
                        || status == PlayerResourcePackStatusEvent.Status.FAILED_RELOAD) {
                    String kickMessage = getConfig().getString(
                            "resourcepack.kick-message",
                            "Không tải được resource packs, vui lòng vào lại\nUnable to load resource packs, please rejoin"
                    );
                    event.getPlayer().kick(Component.text(kickMessage));
                }
            }
        };
        getServer().getPluginManager().registerEvents(resourcePackJoinListener, this);
    }

    private void registerIntegrationEnableListener() {
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPluginEnable(PluginEnableEvent event) {
                Plugin p = event.getPlugin();
                String name = p.getName();
                // HOT-FIX: Thêm "SCore-Lite" và "Score" vào danh sách detect
                if (name.equals("Oraxen") || name.equals("ItemsAdder")
                        || name.equals("SCore") || name.equals("SCore-Lite")
                        || name.equals("Score") || name.equals("ExecutableItems")) {
                    getLogger().info("[UItem] Detected " + name + " enabling — re-checking integrations...");
                    integrationManager.detectIntegrations();
                    getLogger().info("[UItem] Integrations: " + integrationManager.getStatus());
                }
            }
        }, this);
    }

    public static UItemPlugin getInstance() {
        return instance;
    }

    public UItemManager getItemManager() {
        return itemManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public GuiListener getGuiListener() {
        return guiListener;
    }

    public ResourcePackManager getResourcePackManager() {
    return resourcePackManager;
}

    public com.uitem.search.SearchManager getSearchManager() {
        return searchManager;
    }

    public ResourcePackServer getPackServer() {
        return packServer;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }


    /**
     * Nếu thư mục items/ hoặc recipes/ trống (không có file .yml), tạo file ví dụ.
     * Điều này giúp người dùng dễ dàng copy-paste config item/recipe bên ngoài.
     */
    private void seedExampleConfigsIfNeeded(File itemsDir, File recipesDir) {
        try {
            // Items examples
            boolean hasItemYml = false;
            File[] itemFiles = itemsDir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
            if (itemFiles != null) hasItemYml = itemFiles.length > 0;

            if (!hasItemYml) {
                File exampleItems = new File(itemsDir, "examples.yml");
                if (!exampleItems.exists()) {
                    org.bukkit.configuration.file.YamlConfiguration ex = new org.bukkit.configuration.file.YamlConfiguration();
                    ex.set("items.kiem_vi_du.material", "DIAMOND_SWORD");
                    ex.set("items.kiem_vi_du.display-name", "<gold>Kiếm Ví Dụ");
                    ex.set("items.kiem_vi_du.lore", List.of("<gray>Một item tùy chỉnh mẫu", "<yellow>Đặt config của bạn ở đây!"));
                    ex.set("items.kiem_vi_du.enchantments.sharpness", 3);
                    ex.set("items.kiem_vi_du.rarity", "RARE");
                    // Ví dụ với hiệu ứng (phản đòn / sát thương thêm)
                    ex.set("items.kiem_vi_du.effects.e0.trigger", "HIT");
                    ex.set("items.kiem_vi_du.effects.e0.type", "EXTRA_DAMAGE");
                    ex.set("items.kiem_vi_du.effects.e0.value", 3.0);
                    ex.set("items.kiem_vi_du.effects.e0.chance", 50);
                    // Ví dụ: khi bị DAMAGE (bị đánh), phản đòn một phần sát thương
                    ex.set("items.kiem_vi_du.effects.e1.trigger", "DAMAGE");
                    ex.set("items.kiem_vi_du.effects.e1.type", "REFLECT");
                    ex.set("items.kiem_vi_du.effects.e1.value", 25.0);
                    ex.set("items.kiem_vi_du.effects.e1.chance", 100);

                    // Ví dụ skill native (giống ExecutableItems nhưng dễ hơn)
                    ex.set("items.kiem_vi_du.skills.fireball.trigger", "RIGHT_CLICK");
                    ex.set("items.kiem_vi_du.skills.fireball.cooldown", 5.0);
                    ex.set("items.kiem_vi_du.skills.fireball.cancel-event", true);
                    ex.set("items.kiem_vi_du.skills.fireball.display-name", "<red>Fireball");
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a0.type", "PLAY_SOUND");
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a0.sound", "ENTITY_BLAZE_SHOOT");
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a0.volume", 1.0);
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a0.pitch", 1.0);
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a1.type", "SHOOT_PROJECTILE");
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a1.projectile", "SMALL_FIREBALL");
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a1.speed", 1.6);
                    ex.set("items.kiem_vi_du.skills.fireball.actions.a1.damage", 7.0);
                    // Ví dụ mapping model linh hoạt (thả bất kỳ .json nào vào thư mục models/)
                    // ex.set("items.kiem_vi_du.model", "my_custom_model");  // tìm models/my_custom_model.json

                    ex.save(exampleItems);
                    getLogger().info("[UItem] Đã tạo config item ví dụ trong items/examples.yml (hỗ trợ kiểu phẳng dễ copy-paste).");
                }
            }

            // Recipes examples
            boolean hasRecipeYml = false;
            File[] recipeFiles = recipesDir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
            if (recipeFiles != null) hasRecipeYml = recipeFiles.length > 0;

            if (!hasRecipeYml) {
                File exampleRecipes = new File(recipesDir, "examples.yml");
                if (!exampleRecipes.exists()) {
                    org.bukkit.configuration.file.YamlConfiguration ex = new org.bukkit.configuration.file.YamlConfiguration();
                    // Hỗ trợ kiểu phẳng
                    // Kiểu phẳng (rất dễ copy-paste recipe đơn lẻ)
                    ex.set("cong_thuc_kiem_vi_du.result-item", "kiem_vi_du");
                    ex.set("cong_thuc_kiem_vi_du.result-amount", 1);
                    ex.set("cong_thuc_kiem_vi_du.shaped", true);
                    ex.set("cong_thuc_kiem_vi_du.shape", List.of(" I ", " S ", " S "));
                    ex.set("cong_thuc_kiem_vi_du.ingredient-map.I.uitem-id", "kiem_vi_du"); // dùng UItem tùy chỉnh khác làm nguyên liệu
                    ex.set("cong_thuc_kiem_vi_du.ingredient-map.I.amount", 1);
                    ex.set("cong_thuc_kiem_vi_du.ingredient-map.S.material", "STICK");
                    ex.set("cong_thuc_kiem_vi_du.ingredient-map.S.amount", 1);

                    ex.save(exampleRecipes);
                    getLogger().info("[UItem] Đã tạo config recipe ví dụ trong recipes/examples.yml (hỗ trợ nguyên liệu UItem tùy chỉnh + kiểu phẳng dễ copy-paste).");
                }
            }
        } catch (Exception e) {
            getLogger().warning("[UItem] Could not seed example configs: " + e.getMessage());
        }
    }

    private void createMainExternalConfigIfNeeded(File dir, String fileName, String section) {
        File mainFile = new File(dir, fileName);
        if (mainFile.exists()) return;
        try {
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.options().setHeader(java.util.Arrays.asList(
                    "File config chính bên ngoài cho " + section + " - có thể chỉnh sửa qua GUI trong game.",
                    "Plugin tải tất cả .yml trong thư mục này (kiểu phẳng hoặc gói).",
                    "GUI chỉnh sửa sẽ lưu vào file " + fileName + " này (tổng hợp trạng thái hiện tại).",
                    "Demo: recipe cho kiếm_cơ_bản + recipe cho kiếm_nâng_cao dùng kiếm_cơ_bản làm nguyên liệu uitem."
            ));
            // Nội dung mẫu minh họa:
            // - Item tùy chỉnh có recipe
            // - Một item (kiếm_cơ_bản) được dùng làm nguyên liệu (uitem-id) trong recipe cho item khác (kiếm_nâng_cao)
            // - Cả hai item đều có recipe riêng
            if ("items".equals(section)) {
                // kiếm_cơ_bản - item đơn giản có recipe
                cfg.set("items.kiem_co_ban.material", "IRON_SWORD");
                cfg.set("items.kiem_co_ban.display-name", "<gray>Kiếm Cơ Bản");
                cfg.set("items.kiem_co_ban.lore", java.util.List.of("<gray>Kiếm sắt đơn giản", "<yellow>Dùng làm nguyên liệu cho item nâng cao"));
                cfg.set("items.kiem_co_ban.enchantments.sharpness", 1);

                // kiếm_nâng_cao - dùng kiếm_cơ_bản trong recipe (chuỗi)
                cfg.set("items.kiem_nang_cao.material", "DIAMOND_SWORD");
                cfg.set("items.kiem_nang_cao.display-name", "<aqua>Kiếm Nâng Cao");
                cfg.set("items.kiem_nang_cao.lore", java.util.List.of("<gray>Được rèn từ Kiếm Cơ Bản", "<yellow>Sắc bén cao!"));
                cfg.set("items.kiem_nang_cao.enchantments.sharpness", 5);
            } else {
                // recipe cho kiếm_cơ_bản dùng vật liệu vanilla
                cfg.set("recipes.cong_thuc_kiem_co_ban.result-item", "kiem_co_ban");
                cfg.set("recipes.cong_thuc_kiem_co_ban.result-amount", 1);
                cfg.set("recipes.cong_thuc_kiem_co_ban.shaped", true);
                cfg.set("recipes.cong_thuc_kiem_co_ban.shape", java.util.List.of(" I ", " S ", " S "));
                cfg.set("recipes.cong_thuc_kiem_co_ban.ingredient-map.I.material", "IRON_INGOT");
                cfg.set("recipes.cong_thuc_kiem_co_ban.ingredient-map.I.amount", 1);
                cfg.set("recipes.cong_thuc_kiem_co_ban.ingredient-map.S.material", "STICK");
                cfg.set("recipes.cong_thuc_kiem_co_ban.ingredient-map.S.amount", 1);

                // recipe cho kiếm_nâng_cao dùng kiếm_cơ_bản tùy chỉnh làm nguyên liệu (uitem-id)
                cfg.set("recipes.cong_thuc_kiem_nang_cao.result-item", "kiem_nang_cao");
                cfg.set("recipes.cong_thuc_kiem_nang_cao.result-amount", 1);
                cfg.set("recipes.cong_thuc_kiem_nang_cao.shaped", true);
                cfg.set("recipes.cong_thuc_kiem_nang_cao.shape", java.util.List.of(" B ", " S ", " S "));
                cfg.set("recipes.cong_thuc_kiem_nang_cao.ingredient-map.B.uitem-id", "kiem_co_ban");
                cfg.set("recipes.cong_thuc_kiem_nang_cao.ingredient-map.B.amount", 1);
                cfg.set("recipes.cong_thuc_kiem_nang_cao.ingredient-map.S.material", "STICK");
                cfg.set("recipes.cong_thuc_kiem_nang_cao.ingredient-map.S.amount", 1);
            }
            cfg.save(mainFile);
            getLogger().info("[UItem] Đã tạo file config chính bên ngoài " + fileName + " trong " + dir.getName() + "/ (có thể chỉnh qua GUI + lưu, hỗ trợ nguyên liệu uitem trong recipe)");
        } catch (Exception e) {
            getLogger().warning("[UItem] Could not create main " + fileName + ": " + e.getMessage());
        }
    }
}
