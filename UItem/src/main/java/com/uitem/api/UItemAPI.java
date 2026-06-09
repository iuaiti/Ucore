package com.uitem.api;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.api.events.UItemCreateEvent;
import com.uitem.api.events.UItemDeleteEvent;
import com.uitem.api.events.UItemGiveEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Public API của UItem — entry point duy nhất cho external developer.
 *
 * <h2>Cách dùng trong plugin của bạn:</h2>
 *
 * <pre>{@code
 * // 1. Kiểm tra UItem có sẵn không (trong onEnable)
 * if (!UItemAPI.isAvailable()) {
 *     getLogger().warning("UItem không được cài — tắt tính năng liên quan.");
 *     return;
 * }
 * UItemAPI api = UItemAPI.get();
 *
 * // 2. Lấy item theo ID
 * Optional<ItemStack> item = api.buildItem("ruby_sword");
 * item.ifPresent(i -> player.getInventory().addItem(i));
 *
 * // 3. Kiểm tra ItemStack có phải UItem không
 * if (api.isUItem(itemInHand)) {
 *     String id = api.getUItemId(itemInHand).orElse("unknown");
 * }
 *
 * // 4. Đăng ký item mới từ code
 * UItemData data = new UItemData("my_item");
 * data.setMaterial(Material.DIAMOND);
 * data.setDisplayName("<aqua>My Item");
 * api.registerItem(data, null); // null = không có sender
 * }</pre>
 *
 * <h2>Events có thể listen:</h2>
 * <ul>
 *   <li>{@code UItemCreateEvent} — item mới được tạo</li>
 *   <li>{@code UItemDeleteEvent} — item bị xóa</li>
 *   <li>{@code UItemGiveEvent}   — item được give cho player</li>
 *   <li>{@code UItemInteractEvent} — player click với UItem trên tay</li>
 *   <li>{@code UItemPickupEvent}  — player nhặt UItem</li>
 *   <li>{@code UItemEquipEvent}   — player mặc/tháo UItem</li>
 * </ul>
 */
public final class UItemAPI {

    private static UItemAPI instance;

    private final UItemPlugin plugin;

    private UItemAPI(@NotNull UItemPlugin plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // Internal lifecycle — chỉ UItemPlugin gọi
    // =========================================================

    /**
     * Khởi tạo API instance. Chỉ gọi từ {@code UItemPlugin.onEnable()}.
     */
    public static void init(@NotNull UItemPlugin plugin) {
        instance = new UItemAPI(plugin);
    }

    /**
     * Hủy API instance. Chỉ gọi từ {@code UItemPlugin.onDisable()}.
     */
    public static void shutdown() {
        instance = null;
    }

    // =========================================================
    // Public access
    // =========================================================

    /**
     * Lấy UItemAPI instance.
     *
     * @throws IllegalStateException nếu UItem chưa bật hoặc đã tắt
     */
    @NotNull
    public static UItemAPI get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "UItem chưa được load. Kiểm tra bằng UItemAPI.isAvailable() trước.");
        }
        return instance;
    }

    /**
     * Kiểm tra UItem có đang chạy và API có sẵn không.
     * Luôn gọi cái này trước {@link #get()} trong plugin của bạn.
     */
    public static boolean isAvailable() {
        return instance != null;
    }

    // =========================================================
    // Item lookup
    // =========================================================

    /**
     * Lấy {@link UItemData} theo ID.
     *
     * @param id ID của item (case-insensitive)
     * @return Optional chứa data, empty nếu không tìm thấy
     */
    @NotNull
    public Optional<UItemData> getItem(@NotNull String id) {
        return Optional.ofNullable(plugin.getItemManager().getItem(id));
    }

    /**
     * Lấy tất cả custom items đã đăng ký.
     *
     * @return Unmodifiable map của id → data
     */
    @NotNull
    public Map<String, UItemData> getAllItems() {
        return plugin.getItemManager().getAllItems();
    }

    /**
     * Kiểm tra item ID có tồn tại không.
     */
    public boolean hasItem(@NotNull String id) {
        return plugin.getItemManager().hasItem(id);
    }

    // =========================================================
    // ItemStack building
    // =========================================================

    /**
     * Build {@link ItemStack} từ ID.
     *
     * @param id ID của item
     * @return Optional chứa ItemStack, empty nếu ID không tồn tại
     */
    @NotNull
    public Optional<ItemStack> buildItem(@NotNull String id) {
        UItemData data = plugin.getItemManager().getItem(id);
        if (data == null) return Optional.empty();
        return Optional.of(plugin.getItemManager().buildItemStack(data));
    }

    /**
     * Build {@link ItemStack} từ {@link UItemData}.
     */
    @NotNull
    public ItemStack buildItem(@NotNull UItemData data) {
        return plugin.getItemManager().buildItemStack(data);
    }

    // =========================================================
    // ItemStack identification
    // =========================================================

    /**
     * Kiểm tra ItemStack có phải UItem không (có PersistentDataContainer tag).
     *
     * @param item ItemStack cần kiểm tra (null-safe)
     * @return true nếu là UItem
     */
    public boolean isUItem(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        var meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer()
                .has(plugin.getItemManager().getUItemKey(), PersistentDataType.STRING);
    }

    /**
     * Lấy ID của UItem từ ItemStack.
     *
     * @param item ItemStack cần check
     * @return Optional chứa ID, empty nếu không phải UItem
     */
    @NotNull
    public Optional<String> getUItemId(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        var meta = item.getItemMeta();
        if (meta == null) return Optional.empty();
        String id = meta.getPersistentDataContainer()
                .get(plugin.getItemManager().getUItemKey(), PersistentDataType.STRING);
        return Optional.ofNullable(id);
    }

    /**
     * Lấy {@link UItemData} từ một ItemStack.
     * Kết hợp {@link #getUItemId} và {@link #getItem}.
     */
    @NotNull
    public Optional<UItemData> getUItemData(@Nullable ItemStack item) {
        return getUItemId(item).flatMap(this::getItem);
    }

    // =========================================================
    // Management (fires events)
    // =========================================================

    /**
     * Đăng ký (tạo hoặc update) một custom item vào UItem.
     *
     * <p>Nếu item với ID này chưa tồn tại → fire {@link UItemCreateEvent}.
     * Nếu đã tồn tại → update trực tiếp, không fire event (là update, không phải create).
     *
     * @param data    Data của item
     * @param creator Sender thực hiện (null nếu từ code)
     * @return true nếu item được đăng ký thành công (không bị cancel)
     */
    public boolean registerItem(@NotNull UItemData data, @Nullable CommandSender creator) {
        boolean isNew = !plugin.getItemManager().hasItem(data.getId());

        if (isNew) {
            UItemCreateEvent event = new UItemCreateEvent(data, creator);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
        }

        plugin.getItemManager().registerItem(data);
        plugin.getResourcePackManager().syncCmdsToLiveItems();
        plugin.getStorageManager().saveAll(plugin.getItemManager());
        return true;
    }

    /**
     * Xóa một custom item khỏi UItem.
     *
     * <p>Fire {@link UItemDeleteEvent} — cancel để ngăn xóa.
     *
     * @param id      ID của item cần xóa
     * @param deleter Sender thực hiện (null nếu từ code)
     * @return true nếu item tồn tại và được xóa thành công
     */
    public boolean removeItem(@NotNull String id, @Nullable CommandSender deleter) {
        UItemData data = plugin.getItemManager().getItem(id);
        if (data == null) return false;

        UItemDeleteEvent event = new UItemDeleteEvent(data, deleter);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        plugin.getItemManager().removeItem(id);
        // Also delete the corresponding split file(s) so the item does not reappear after reload
        // when using items/*.yml split config (addresses "edits + delete lost on reload")
        plugin.getStorageManager().deleteItemFile(id);
        plugin.getStorageManager().saveAll(plugin.getItemManager());
        return true;
    }

    // =========================================================
    // Give (fires event)
    // =========================================================

    /**
     * Give item cho player. Fire {@link UItemGiveEvent} trước khi cho.
     *
     * <p>Item dư (inventory đầy) sẽ drop tại vị trí player.
     *
     * @param player Player nhận item
     * @param id     ID của item
     * @param amount Số lượng (1-64)
     * @param giver  Sender thực hiện give (null nếu từ code)
     * @return true nếu give thành công (không bị cancel và item tồn tại)
     */
    public boolean giveItem(
            @NotNull Player player,
            @NotNull String id,
            int amount,
            @Nullable CommandSender giver) {

        UItemData data = plugin.getItemManager().getItem(id);
        if (data == null) return false;

        int clampedAmount = Math.max(1, Math.min(64, amount));
        ItemStack itemStack = plugin.getItemManager().buildItemStack(data);
        itemStack.setAmount(clampedAmount);

        UItemGiveEvent event = new UItemGiveEvent(data, player, giver, itemStack.clone(), clampedAmount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        // Dùng item và amount từ event (có thể đã bị modify bởi listener)
        ItemStack finalItem = event.getItemStack();
        finalItem.setAmount(event.getAmount());

        var leftovers = player.getInventory().addItem(finalItem);
        leftovers.values().forEach(left ->
                player.getWorld().dropItemNaturally(player.getLocation(), left));

        return true;
    }

    /**
     * Overload tiện lợi không cần giver.
     *
     * @see #giveItem(Player, String, int, CommandSender)
     */
    public boolean giveItem(@NotNull Player player, @NotNull String id, int amount) {
        return giveItem(player, id, amount, null);
    }

    // =========================================================
    // ExecutableItems Integration
    // =========================================================

    /**
     * Kiểm tra ExecutableItems có được phát hiện và sẵn sàng không.
     */
    public boolean hasExecutableItems() {
        return plugin.getIntegrationManager().hasExecutableItems();
    }

    /**
     * Lấy ExecutableItems item ID từ ItemStack (qua PDC của EI).
     *
     * @param item ItemStack cần kiểm tra
     * @return ExecutableItems ID, null nếu không phải EI item
     */
    @Nullable
    public String getExecutableItemsId(@Nullable ItemStack item) {
        if (!hasExecutableItems()) return null;
        return plugin.getIntegrationManager().getExecutableItemsIntegration().getExecutableItemId(item);
    }

    /**
     * Lấy ExecutableItems item ID từ UItem ID.
     *
     * @param uitemId ID của UItem
     * @return ExecutableItems ID đã mapping, null nếu không có
     */
    @Nullable
    public String getExecutableItemsIdByUItemId(@NotNull String uitemId) {
        return getItem(uitemId)
                .map(UItemData::getExecutableItemsId)
                .orElse(null);
    }

    /**
     * Lấy ExecutableItems ItemStack từ UItem ID.
     * ExecutableItems tự xử lý skill/activator của nó — không cần trigger thủ công.
     *
     * @param uitemId ID của UItem
     * @return ItemStack từ ExecutableItems, empty nếu không có mapping hoặc lỗi
     */
    @NotNull
    public Optional<ItemStack> buildExecutableItemsItem(@NotNull String uitemId) {
        if (!hasExecutableItems()) return Optional.empty();
        String eiId = getExecutableItemsIdByUItemId(uitemId);
        if (eiId == null) return Optional.empty();
        return Optional.ofNullable(
                plugin.getIntegrationManager().getExecutableItemsIntegration().getItem(eiId)
        );
    }

    // =========================================================
    // Utility
    // =========================================================

    /**
     * Lấy NamespacedKey UItem dùng để tag item trong PersistentDataContainer.
     * Hữu ích nếu bạn muốn tự đọc/ghi PDC.
     */
    @NotNull
    public NamespacedKey getUItemKey() {
        return plugin.getItemManager().getUItemKey();
    }

    /**
     * Lấy plugin instance. Dùng để access các manager nội bộ nếu cần.
     */
    @NotNull
    public UItemPlugin getPlugin() {
        return plugin;
    }
}
