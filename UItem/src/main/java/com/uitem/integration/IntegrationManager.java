package com.uitem.integration;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Quản lý tích hợp với Oraxen, ItemsAdder và ExecutableItems.
 * Detect runtime và delegate build item nếu có integration ID.
 *
 * HOT-FIX changelog:
 *  - [BUG-5] Thêm "SCore-Lite" vào danh sách plugin name check cho EI.
 *  - [BUG-6] Pass logger vào ExecutableItemsIntegration để log rõ lỗi thay vì silent fail.
 *  - [BUG-7] tryBuildFromIntegration: nếu EI trả về null (item không tồn tại trong EI),
 *            log warning thay vì fall-through sang vanilla silently.
 */
public class IntegrationManager {

    private final UItemPlugin plugin;
    private OraxenIntegration oraxenIntegration;
    private ItemsAdderIntegration itemsAdderIntegration;
    private ExecutableItemsIntegration executableItemsIntegration;
    private boolean hasOraxen;
    private boolean hasItemsAdder;
    private boolean hasExecutableItems;

    private String oraxenVersion;
    private String itemsAdderVersion;
    private String executableItemsVersion;

    // BUG-5 FIX: Thêm "SCore-Lite" và "Score" vào danh sách tên plugin EI
    private static final String[] EI_PLUGIN_NAMES = {
        "ExecutableItems",
        "SCore",
        "SCore-Lite",     // BUG-5: một số server dùng SCore-Lite
        "Score"
    };

    public IntegrationManager(UItemPlugin plugin) {
        this.plugin = plugin;
        this.hasOraxen = false;
        this.hasItemsAdder = false;
        this.hasExecutableItems = false;
    }

    /**
     * Detect xem Oraxen, ItemsAdder và ExecutableItems có đang cài không.
     */
    public void detectIntegrations() {
        oraxenIntegration = null;
        itemsAdderIntegration = null;
        executableItemsIntegration = null;
        hasOraxen = false;
        hasItemsAdder = false;
        hasExecutableItems = false;

        // ── Check Oraxen ──
        Plugin oraxenPlugin = Bukkit.getPluginManager().getPlugin("Oraxen");
        if (oraxenPlugin != null && oraxenPlugin.isEnabled()) {
            oraxenVersion = oraxenPlugin.getPluginMeta().getVersion();
            try {
                Class<?> oraxenItemsClass = tryFindClass("io.th0rgal.oraxen.api.OraxenItems");
                if (oraxenItemsClass != null) {
                    oraxenIntegration = new OraxenIntegration();
                    hasOraxen = true;
                    plugin.getLogger().info("  ✔ Oraxen v" + oraxenVersion + " — hooked");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "  ○ Oraxen plugin found but API hook failed: " + e.getMessage());
                plugin.getLogger().warning("    Please report this to the UItem developer with your Oraxen version.");
            }
        } else {
            plugin.getLogger().info("  ○ Oraxen — not detected");
        }

        // ── Check ItemsAdder ──
        Plugin itemsAdderPlugin = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdderPlugin != null && itemsAdderPlugin.isEnabled()) {
            itemsAdderVersion = itemsAdderPlugin.getPluginMeta().getVersion();
            try {
                Class<?> customStackClass = tryFindClass(
                    "dev.lone.itemsadder.api.CustomStack",
                    "dev.lone.itemsadder.api.ItemsAdder"
                );
                if (customStackClass != null) {
                    itemsAdderIntegration = new ItemsAdderIntegration();
                    hasItemsAdder = true;
                    plugin.getLogger().info("  ✔ ItemsAdder v" + itemsAdderVersion + " — hooked");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "  ○ ItemsAdder plugin found but API hook failed: " + e.getMessage());
                plugin.getLogger().warning("    Please report this to the UItem developer with your ItemsAdder version.");
            }
        } else {
            plugin.getLogger().info("  ○ ItemsAdder — not detected");
        }

        // ── Check ExecutableItems (BUG-5 FIX: kiểm tra nhiều tên plugin hơn) ──
        Plugin executableItemsPlugin = findEiPlugin();

        if (executableItemsPlugin != null && executableItemsPlugin.isEnabled()) {
            executableItemsVersion = executableItemsPlugin.getPluginMeta().getVersion();
            try {
                // BUG-6 FIX: Pass logger vào để không swallow lỗi lặng lẽ
                ExecutableItemsIntegration testEi = new ExecutableItemsIntegration(plugin.getLogger());
                executableItemsIntegration = testEi;
                hasExecutableItems = true;
                plugin.getLogger().info("  ✔ ExecutableItems v" + executableItemsVersion
                        + " (via " + executableItemsPlugin.getName() + ") — hooked");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                    "  ○ ExecutableItems plugin found (" + executableItemsPlugin.getName()
                    + " v" + executableItemsVersion + ") but API hook failed!", e);
                plugin.getLogger().warning(
                    "    Root cause: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                plugin.getLogger().warning(
                    "    → Make sure ExecutableItems/SCore is fully enabled before UItem.");
                plugin.getLogger().warning(
                    "    → Please report this with your EI version to the UItem developer.");
            }
        } else {
            plugin.getLogger().info("  ○ ExecutableItems — not detected");
        }
    }

    /**
     * BUG-5 FIX: Tìm plugin EI theo nhiều tên có thể có.
     */
    private Plugin findEiPlugin() {
        for (String name : EI_PLUGIN_NAMES) {
            Plugin p = Bukkit.getPluginManager().getPlugin(name);
            if (p != null) return p;
        }
        return null;
    }

    private Class<?> tryFindClass(String... classNames) {
        for (String name : classNames) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    /**
     * Thử build ItemStack từ integration (Oraxen/ItemsAdder/ExecutableItems).
     * Trả về null nếu không có integration hoặc không có ID.
     *
     * BUG-7 FIX: Khi EI được hook nhưng trả null (EI ID không tồn tại), log cảnh báo
     * rõ ràng thay vì âm thầm fall-through về vanilla item.
     */
    public ItemStack tryBuildFromIntegration(UItemData data) {
        // Ưu tiên Oraxen
        if (hasOraxen && data.getOraxenId() != null && !data.getOraxenId().isEmpty()) {
            ItemStack item = oraxenIntegration.getItem(data.getOraxenId());
            if (item != null) return item;
            plugin.getLogger().warning("[UItem] Oraxen item '" + data.getOraxenId()
                    + "' not found (referenced by UItem '" + data.getId() + "'). Falling back to vanilla.");
        }

        // Rồi ItemsAdder
        if (hasItemsAdder && data.getItemsAdderId() != null && !data.getItemsAdderId().isEmpty()) {
            ItemStack item = itemsAdderIntegration.getItem(data.getItemsAdderId());
            if (item != null) return item;
            plugin.getLogger().warning("[UItem] ItemsAdder item '" + data.getItemsAdderId()
                    + "' not found (referenced by UItem '" + data.getId() + "'). Falling back to vanilla.");
        }

        // Sau đó ExecutableItems (BUG-7 FIX: log khi null)
        if (hasExecutableItems && data.getExecutableItemsId() != null && !data.getExecutableItemsId().isEmpty()) {
            ItemStack item = executableItemsIntegration.getItem(data.getExecutableItemsId());
            if (item != null) return item;
            plugin.getLogger().warning("[UItem] ExecutableItems item '" + data.getExecutableItemsId()
                    + "' not found or failed to build (referenced by UItem '" + data.getId() + "'). "
                    + "Check your EI item ID and ensure EI is loaded correctly. Falling back to vanilla.");
        }

        return null;
    }

    public boolean hasOraxen() {
        return hasOraxen;
    }

    public boolean hasItemsAdder() {
        return hasItemsAdder;
    }

    public boolean hasExecutableItems() {
        return hasExecutableItems;
    }

    public ExecutableItemsIntegration getExecutableItemsIntegration() {
        return executableItemsIntegration;
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        if (hasOraxen) sb.append("Oraxen v").append(oraxenVersion).append(" ✔  ");
        else sb.append("Oraxen ✘  ");
        if (hasItemsAdder) sb.append("ItemsAdder v").append(itemsAdderVersion).append(" ✔  ");
        else sb.append("ItemsAdder ✘  ");
        if (hasExecutableItems) sb.append("ExecutableItems v").append(executableItemsVersion).append(" ✔");
        else sb.append("ExecutableItems ✘");
        return sb.toString();
    }
}
