package com.uitem.util;

import com.uitem.UItemPlugin;
import com.ultra.ucore.skill.SkillAction;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/**
 * Centralized message system with EN/VI language support.
 * Language controlled by config.yml → "language: en" or "language: vi"
 */
public final class Messages {

    private static UItemPlugin plugin;
    private static boolean isVietnamese;

    private Messages() {}

    public static void init(UItemPlugin pluginInstance) {
        plugin = pluginInstance;
        reload();
    }

    public static void reload() {
        if (plugin == null) { isVietnamese = false; return; }
        String lang = plugin.getConfig().getString("language", "en");
        isVietnamese = "vi".equalsIgnoreCase(lang) || "vn".equalsIgnoreCase(lang) || "vietnamese".equalsIgnoreCase(lang);
    }

    public static boolean isVietnamese() { return isVietnamese; }

    private static String get(String key, String en, String vi) {
        if (plugin != null) {
            FileConfiguration cfg = plugin.getConfig();
            // Check per-language override first
            String langKey = "messages." + (isVietnamese ? "vi." : "en.") + key;
            if (cfg.contains(langKey)) return cfg.getString(langKey);
            // Check generic key
            String path = "messages." + key;
            if (cfg.contains(path)) return cfg.getString(path);
        }
        return isVietnamese ? vi : en;
    }

    // ==================== Generic ====================
    public static String noPermission() { return get("no-permission", "You don't have permission to use this command!", "Bạn không có quyền sử dụng lệnh này!"); }
    public static String playersOnly() { return get("players-only", "This command can only be used by players!", "Lệnh này chỉ dành cho người chơi!"); }
    public static String playerNotFound(String name) { return get("player-not-found", "Player '" + name + "' is not online!", "Người chơi '" + name + "' không online!").replace("{player}", name); }
    public static String invalidCommand(String label) { return get("invalid-command", "Invalid command. Use /" + label + " help", "Lệnh không hợp lệ. Dùng /" + label + " help").replace("{label}", label); }

    // ==================== Give ====================
    public static String giveUsage() { return get("give-usage", "Usage: /uitem give <id> [player] [amount]", "Cách dùng: /uitem give <id> [người chơi] [số lượng]"); }
    public static String itemNotFound(String id) { return get("item-not-found", "Item '" + id + "' does not exist!", "Item '" + id + "' không tồn tại!").replace("{id}", id); }
    public static String gaveItem(String player, int amount, String itemId) { return get("gave-item", "Gave " + player + " x" + amount + " " + itemId, "Đã đưa cho " + player + " x" + amount + " " + itemId).replace("{player}", player).replace("{amount}", String.valueOf(amount)).replace("{item}", itemId); }
    public static String receivedItem(int amount, String itemId) { return get("received-item", "You received x" + amount + " " + itemId, "Bạn nhận được x" + amount + " " + itemId).replace("{amount}", String.valueOf(amount)).replace("{item}", itemId); }
    public static String cannotGive() { return get("cannot-give", "Cannot give item (may have been blocked by another plugin).", "Không thể đưa item (có thể bị chặn bởi plugin khác)."); }
    public static String consoleNeedPlayer() { return get("console-need-player", "Console must specify a player: /uitem give <id> <player> [amount]", "Console phải chỉ định người chơi: /uitem give <id> <người chơi> [số lượng]"); }
    public static String invalidAmount() { return get("invalid-amount", "Invalid amount! Must be an integer from 1 to 64.", "Số lượng không hợp lệ! Phải là số nguyên từ 1 đến 64."); }

    // ==================== Reload ====================
    public static String reloaded(int count) { return get("reloaded", "Reloaded UItem! Loaded " + count + " items.", "Đã reload UItem! Đã load " + count + " items.").replace("{count}", String.valueOf(count)); }
    public static String rpRegenerated() { return get("rp-regenerated", "Resource pack has been re-generated and exported.", "Resource pack đã được tạo lại và xuất ra."); }
    public static String rpGenerateFailed(String msg) { return get("rp-generate-failed", "Cannot generate/export resource pack: " + msg, "Không thể tạo/xuất resource pack: " + msg).replace("{error}", msg); }

    // ==================== Check ====================
    public static String notHoldingItem() { return get("not-holding-item", "You are not holding any item!", "Bạn không cầm item nào!"); }
    public static String validUItem() { return get("valid-uitem", "✔ Item is a valid UItem!", "✔ Item là UItem hợp lệ!"); }
    public static String notUItem() { return get("not-uitem", "✗ Item is NOT a valid UItem!", "✗ Item KHÔNG phải UItem hợp lệ!"); }
    public static String missingPDC() { return get("missing-pdc", "Missing PDC tag. This item was not obtained through /uitem give.", "Thiếu tag PDC. Item này không được lấy qua /uitem give."); }
    public static String useGiveToGet() { return get("use-give-to-get", "Use /uitem give <id> <player> to get properly tagged items.", "Dùng /uitem give <id> <người chơi> để lấy item có tag đúng."); }

    // ==================== Editor ====================
    public static String noItemsYet() { return get("no-items-yet", "No custom items yet. Use /uitem editor to create one.", "Chưa có custom item nào. Dùng /uitem editor để tạo."); }

    // ==================== Resource Pack ====================
    public static String rpHelpGenerate() { return get("rp-help-gen", "<gold>Generate resource pack: <white>/uitem rp generate", "<gold>Tạo resource pack: <white>/uitem rp generate"); }
    public static String rpHelpExport() { return get("rp-help-export", "<gold>Export zip: <white>/uitem rp export", "<gold>Xuất zip: <white>/uitem rp export"); }
    public static String rpHelpApply() { return get("rp-help-apply", "<gold>Apply to player: <white>/uitem rp apply <player> <url> [hash]", "<gold>Áp dụng cho người chơi: <white>/uitem rp apply <người chơi> <url> [hash]"); }
    public static String rpHelpStatus() { return get("rp-help-status", "<gold>Status: <white>/uitem rp status", "<gold>Trạng thái: <white>/uitem rp status"); }
    public static String rpGenerated() { return get("rp-generated", "Generated resource pack at data/resourcepack/", "Đã tạo resource pack tại data/resourcepack/"); }
    public static String rpExported(String path) { return get("rp-exported", "Exported zip: " + path, "Đã xuất zip: " + path).replace("{path}", path); }
    public static String rpExportFailed(String msg) { return get("rp-export-failed", "Export failed: " + msg, "Xuất thất bại: " + msg).replace("{error}", msg); }
    public static String rpApplyUsage() { return get("rp-apply-usage", "Usage: /uitem rp apply <player> <url> [hash]", "Cách dùng: /uitem rp apply <người chơi> <url> [hash]"); }
    public static String rpSent(String name) { return get("rp-sent", "Sent resource pack to " + name, "Đã gửi resource pack cho " + name).replace("{player}", name); }
    public static String rpUsage() { return get("rp-usage", "Usage: /uitem rp <generate|export|apply|status>", "Cách dùng: /uitem rp <generate|export|apply|status>"); }
    public static String rpStatusNotExported() { return get("rp-status-not-exported", "Not exported", "Chưa xuất"); }
    public static String rpStatusNone() { return get("rp-status-none", "None", "Không có"); }
    public static String rpKickMessage() { return get("rp-kick", "Unable to load resource packs, please rejoin", "Không tải được resource packs, vui lòng vào lại"); }

    // ==================== GUI: Item Editor ====================
    public static String guiEditTitle(String id) { return get("gui-edit-title", "<gradient:#ff6b6b:#ffd93d>Edit: </gradient><white>" + id, "<gradient:#ff6b6b:#ffd93d>Sua: </gradient><white>" + id).replace("{id}", id); }
    public static String guiEditorTitle() { return get("gui-editor-title", "<gradient:#ff6b6b:#ffd93d><bold>UItem Editor</bold></gradient>", "<gradient:#ff6b6b:#ffd93d><bold>Trinh Sua UItem</bold></gradient>"); }
    public static String guiManageItems() { return get("gui-manage-items", "<gray>Manage all custom items", "<gray>Quan ly tat ca custom items"); }
    public static String guiCreateNew() { return get("gui-create-new", "<green><bold>+ Create New Item</bold>", "<green><bold>+ Tao Item Moi</bold>"); }
    public static String guiCreateHint() { return get("gui-create-hint", "<gray>Click to create a new custom item", "<gray>Nhan de tao custom item moi"); }
    public static String guiCreatePrompt() { return get("gui-create-prompt", "<yellow>Enter ID for new item <gray>(letters, numbers, underscores only):", "<yellow>Nhap ID cho item moi <gray>(chu cai, so, dau gach duoi):"); }
    public static String guiCreateHint2() { return get("gui-create-hint2", "<yellow>You will be prompted to enter an ID", "<yellow>Ban se duoc yeu cau nhap ID"); }
    public static String guiSearchButton() { return get("gui-search-btn", "<aqua><bold>Search Item</bold>", "<aqua><bold>Tim Kiem Item</bold>"); }
    public static String guiSearchHint1() { return get("gui-search-hint1", "<gray>Enter keyword to filter list", "<gray>Nhap tu khoa de loc danh sach"); }
    public static String guiSearchHint2() { return get("gui-search-hint2", "<gray>Type 'clear' to clear filter", "<gray>Go 'clear' de xoa bo loc"); }
    public static String guiSearchPrompt() { return get("gui-search-prompt", "<yellow>Enter search keyword (<gray>or 'clear' to clear):", "<yellow>Nhap tu khoa tim kiem (<gray>hoac 'clear' de xoa):"); }

    // ==================== GUI: Material Picker ====================
    public static String guiMaterialTitle() { return get("gui-mat-title", "<gold><bold>Choose Material</bold>", "<gold><bold>Chon Vat Lieu</bold>"); }
    public static String guiMaterialSearch() { return get("gui-mat-search", "<gold><bold>Search</bold>", "<gold><bold>Tim Kiem</bold>"); }
    public static String guiMaterialSearchHint() { return get("gui-mat-search-hint", "<gray>Click to search material by name", "<gray>Nhan de tim vat lieu theo ten"); }
    public static String guiMaterialSelectHint() { return get("gui-mat-select", "<gray>Click to select this material", "<gray>Nhan de chon vat lieu nay"); }
    public static String guiMaterialPrompt() { return get("gui-mat-prompt", "<yellow>Enter material name to search (Enter to clear filter):", "<yellow>Nhap ten vat lieu de tim (Enter de xoa loc):"); }

    // ==================== Item Editor fields ====================
    public static String guiMaterialField() { return get("gui-field-material", "<gold><bold>Material</bold>", "<gold><bold>Vat Lieu</bold>"); }
    public static String guiMaterialClick() { return get("gui-field-material-click", "<yellow>Click to open material list", "<yellow>Nhan de mo danh sach vat lieu"); }
    public static String guiDisplayNameField() { return get("gui-field-name", "<gold><bold>Display Name</bold>", "<gold><bold>Ten Hien Thi</bold>"); }
    public static String guiDisplayNameClick() { return get("gui-field-name-click", "<yellow>Click to change name", "<yellow>Nhan de doi ten"); }
    public static String guiDisplayNameSupport() { return get("gui-field-name-support", "<gray>Supports MiniMessage format", "<gray>Ho tro dinh dang MiniMessage"); }
    public static String guiDisplayNameExample() { return get("gui-field-name-example", "<gray>Example: <red>Ruby <gold>Sword", "<gray>Vi du: <red>Ruby <gold>Sword"); }
    public static String guiDisplayNamePrompt() { return get("gui-field-name-prompt", "<yellow>Enter display name <gray>(supports MiniMessage):", "<yellow>Nhap ten hien thi <gray>(ho tro MiniMessage):"); }
    public static String renamedTo(String name) { return get("renamed-to", "Renamed to: " + name, "Da doi ten thanh: " + name).replace("{name}", name); }
    public static String guiLoreField() { return get("gui-field-lore", "<gold><bold>Lore</bold>", "<gold><bold>Mo Ta</bold>"); }
    public static String guiLoreClick() { return get("gui-field-lore-click", "<yellow>Click to edit lore", "<yellow>Nhan de sua mo ta"); }
    public static String guiLoreOpen() { return get("gui-field-lore-open", "<gray>Open Lore Editor GUI", "<gray>Mo trinh sua Lore"); }
    public static String guiEnchantsField() { return get("gui-field-ench", "<gold><bold>Enchantments</bold>", "<gold><bold>Phu Phep</bold>"); }
    public static String guiEnchantsClick() { return get("gui-field-ench-click", "<yellow>Click to edit enchantments", "<yellow>Nhan de sua phu phep"); }
    public static String guiFlagsField() { return get("gui-field-flags", "<gold><bold>Item Flags</bold>", "<gold><bold>Co Item</bold>"); }
    public static String guiFlagsClick() { return get("gui-field-flags-click", "<yellow>Click to edit flags", "<yellow>Nhan de sua co"); }
    public static String guiCmdField() { return get("gui-field-cmd", "<gold><bold>Custom Model Data</bold>", "<gold><bold>Custom Model Data</bold>"); }
    public static String guiCmdClick() { return get("gui-field-cmd-click", "<yellow>Click to change", "<yellow>Nhan de thay doi"); }
    public static String guiCmdHint() { return get("gui-field-cmd-hint", "<gray>Enter number (-1 to disable)", "<gray>Nhap so (-1 de tat)"); }
    public static String guiCmdPrompt() { return get("gui-field-cmd-prompt", "<yellow>Enter Custom Model Data number <gray>(-1 to disable):", "<yellow>Nhap so Custom Model Data <gray>(-1 de tat):"); }
    public static String guiUnbreakableField() { return get("gui-field-unbreak", "<gold><bold>Unbreakable</bold>", "<gold><bold>Khong The Pha</bold>"); }
    public static String guiUnbreakableOn() { return get("gui-field-unbreak-on", "<green>ON", "<green>BAT"); }
    public static String guiUnbreakableOff() { return get("gui-field-unbreak-off", "<red>OFF", "<red>TAT"); }
    public static String guiUnbreakableToggle() { return get("gui-field-unbreak-toggle", "<yellow>Click to toggle", "<yellow>Nhan de chuyen doi"); }
    public static String guiDurabilityField() { return get("gui-field-dur", "<gold><bold>Durability</bold>", "<gold><bold>Do Ben</bold>"); }
    public static String guiDurabilityClick() { return get("gui-field-dur-click", "<yellow>Click to set max durability", "<yellow>Nhan de dat do ben toi da"); }
    public static String guiDurabilityHint1() { return get("gui-field-dur-hint1", "<gray>Enter -1 for default material durability", "<gray>Nhap -1 cho do ben mac dinh"); }
    public static String guiDurabilityHint2() { return get("gui-field-dur-hint2", "<gray>Enter 0 for unbreakable-like (no damage bar)", "<gray>Nhap 0 de khong co thanh hu hong"); }
    public static String guiDurabilityHint3() { return get("gui-field-dur-hint3", "<gray>Or enter any number > 0", "<gray>Hoac nhap so > 0"); }
    public static String guiDurabilityPrompt() { return get("gui-field-dur-prompt", "<yellow>Enter max durability <gray>(-1 = default, 0 = no damage bar, >0 = custom):", "<yellow>Nhap do ben toi da <gray>(-1 = mac dinh, 0 = khong thanh, >0 = tuy chinh):"); }
    public static String durabilitySet(String status) { return get("durability-set", "Max durability = " + status, "Do ben toi da = " + status).replace("{status}", status); }
    public static String durabilityDefault() { return get("durability-default", "Default", "Mac dinh"); }
    public static String durabilityNoBar() { return get("durability-no-bar", "No bar", "Khong thanh"); }
    public static String guiGlowingField() { return get("gui-field-glow", "<gold><bold>Glowing</bold>", "<gold><bold>Phat Sang</bold>"); }
    public static String guiGlowingToggle() { return get("gui-field-glow-toggle", "<yellow>Click to toggle", "<yellow>Nhan de chuyen doi"); }
    public static String guiGlowingHint() { return get("gui-field-glow-hint", "<gray>Add glowing effect to item", "<gray>Them hieu ung phat sang cho item"); }
    public static String guiAttributesField() { return get("gui-field-attr", "<gold><bold>Attributes</bold>", "<gold><bold>Thuoc Tinh</bold>"); }

    // ==================== GUI: Attributes ====================
    public static String guiAttributesClick() { return get("gui-attributes-click", "<yellow>Click to add attribute", "<yellow>Nhan de them thuoc tinh"); }
    public static String guiAttributesFormat() { return get("gui-attributes-format", "<gray>Format: attribute amount operation slot", "<gray>Dinh dang: thuoc_tinh gia_tri phep_toan vi_tri"); }
    public static String guiAttributesExample() { return get("gui-attributes-example", "<gray>Example: generic_attack_damage 15 ADD_NUMBER HAND", "<gray>Vi du: generic_attack_damage 15 ADD_NUMBER HAND"); }
    public static String guiAttributesPrompt() { return get("gui-attributes-prompt", "<yellow>Enter attribute <gray>(format: attribute amount operation slot)\\n<gray>Example: generic_attack_damage 15 ADD_NUMBER HAND\\n<gray>Type 'clear' to remove all attributes:", "<yellow>Nhap thuoc tinh <gray>(dinh dang: thuoc_tinh gia_tri phep_toan vi_tri)\\n<gray>Vi du: generic_attack_damage 15 ADD_NUMBER HAND\\n<gray>Go 'clear' de xoa tat ca thuoc tinh:"); }
    public static String guiAttributesCleared() { return get("gui-attributes-cleared", "Removed all attributes!", "Da xoa tat ca thuoc tinh!"); }
    public static String guiAttributesError() { return get("gui-attributes-error", "Format: attribute amount operation slot", "Dinh dang: thuoc_tinh gia_tri phep_toan vi_tri"); }
    public static String guiAttributesAdded(String attr) { return get("gui-attributes-added", "Added attribute " + attr + "!", "Da them thuoc tinh " + attr + "!").replace("{attr}", attr); }

    // ==================== GUI: Rarity ====================
    public static String guiRarityField() { return get("gui-rarity-field", "<gold><bold>Rarity</bold>", "<gold><bold>Do Hiem</bold>"); }
    public static String guiRarityNone() { return get("gui-rarity-none", "None", "Khong co"); }
    public static String guiRarityClick() { return get("gui-rarity-click", "<yellow>Click to choose rarity", "<yellow>Nhan de chon do hiem"); }
    public static String guiRarityHints() { return get("gui-rarity-hints", "<gray>COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, ULTRA", "<gray>COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, ULTRA"); }
    public static String guiRarityListHeader() { return get("gui-rarity-list-header", "<gold>Available rarities:<dark_gray> ", "<gold>Cac do hiem co san:<dark_gray> "); }
    public static String guiRarityPrompt() { return get("gui-rarity-prompt", "<yellow>Enter rarity name (or 'none' to remove):", "<yellow>Nhap ten do hiem (hoac 'none' de xoa):"); }
    public static String guiRarityRemoved() { return get("gui-rarity-removed", "Removed rarity!", "Da xoa do hiem!"); }
    public static String guiRaritySet(String rarity) { return get("gui-rarity-set", "Set rarity = " + rarity, "Da dat do hiem = " + rarity).replace("{rarity}", rarity); }
    public static String guiRarityNotFound(String rarity) { return get("gui-rarity-not-found", "Rarity '" + rarity + "' does not exist!", "Do hiem '" + rarity + "' khong ton tai!").replace("{rarity}", rarity); }


    // ==================== GUI: ExecutableItems ====================
    public static String guiEIIdField() { return get("gui-eiid-field", "<gold><bold>⚡ ExecutableItems</bold>", "<gold><bold>⚡ ExecutableItems</bold>"); }
    public static String guiEIIdCurrent() { return get("gui-eiid-current", "None", "Khong co"); }
    public static String guiEIIdClick() { return get("gui-eiid-click", "<yellow>Click to set ExecutableItems ID", "<yellow>Nhan de dat ID ExecutableItems"); }
    public static String guiEIIdHint() { return get("gui-eiid-hint", "<gray>Skills/Passives will be handled by EI", "<gray>Ky nang/Bi dong se duoc EI xu ly"); }
    public static String guiEIIdPrompt() { return get("gui-eiid-prompt", "<yellow>Enter ExecutableItems item ID <gray>(or 'none' to remove):", "<yellow>Nhap ID item ExecutableItems <gray>(hoac 'none' de xoa):"); }

    // ==================== GUI: Oraxen ====================
    public static String guiOraxenField() { return get("gui-oraxen-field", "<aqua><bold>Oraxen ID</bold>", "<aqua><bold>Oraxen ID</bold>"); }
    public static String guiOraxenCurrent() { return get("gui-oraxen-current", "None", "Khong co"); }
    public static String guiOraxenClick() { return get("gui-oraxen-click", "<yellow>Click to set Oraxen item ID", "<yellow>Nhan de dat ID item Oraxen"); }
    public static String guiOraxenHint() { return get("gui-oraxen-hint", "<gray>Use item from Oraxen as base", "<gray>Dung item tu Oraxen lam goc"); }
    public static String guiOraxenPrompt() { return get("gui-oraxen-prompt", "<yellow>Enter Oraxen item ID <gray>(or 'none' to remove):", "<yellow>Nhap ID item Oraxen <gray>(hoac 'none' de xoa):"); }


    // ==================== GUI: ItemsAdder ====================
    public static String guiIaField() { return get("gui-ia-field", "<green><bold>ItemsAdder ID</bold>", "<green><bold>ItemsAdder ID</bold>"); }
    public static String guiIaCurrent() { return get("gui-ia-current", "None", "Khong co"); }
    public static String guiIaClick() { return get("gui-ia-click", "<yellow>Click to set ItemsAdder item ID", "<yellow>Nhan de dat ID item ItemsAdder"); }
    public static String guiIaHint() { return get("gui-ia-hint", "<gray>Use item from ItemsAdder as base", "<gray>Dung item tu ItemsAdder lam goc"); }
    public static String guiIaPrompt() { return get("gui-ia-prompt", "<yellow>Enter ItemsAdder namespaced ID <gray>(or 'none' to remove):", "<yellow>Nhap ID dinh danh ItemsAdder <gray>(hoac 'none' de xoa):"); }


    // ==================== GUI: Recipe ====================
    public static String guiRecipeField() { return get("gui-recipe-field", "<gold><bold>Crafting Recipe</bold>", "<gold><bold>Cong Thuc Che Tao</bold>"); }
    public static String guiRecipeHas() { return get("gui-recipe-has", "<green>Has recipe", "<green>Co cong thuc"); }
    public static String guiRecipeNone() { return get("gui-recipe-none", "<red>No recipe", "<red>Khong co cong thuc"); }
    public static String guiRecipeClick() { return get("gui-recipe-click", "<yellow>Click to edit recipe", "<yellow>Nhan de sua cong thuc"); }
    public static String guiRecipeHint() { return get("gui-recipe-hint", "<gray>Set shaped/shapeless crafting recipe", "<gray>Dat cong thuc che tao shaped/shapeless"); }

    // ==================== GUI: Save / Delete Buttons ====================
    public static String guiSaveButton() { return get("gui-save-button", "<green><bold>💾 SAVE</bold>", "<green><bold>💾 LUU</bold>"); }
    public static String guiSaveDesc() { return get("gui-save-desc", "<gray>Save all changes to file", "<gray>Luu tat ca thay doi vao file"); }
    public static String guiSaveClick() { return get("gui-save-click", "<yellow>Click to save", "<yellow>Nhan de luu"); }
    public static String guiDeleteButton() { return get("gui-delete-button", "<red><bold>🗑 DELETE</bold>", "<red><bold>🗑 XOA</bold>"); }
    public static String guiDeleteDesc() { return get("gui-delete-desc", "<gray>Delete this item", "<gray>Xoa item nay"); }
    public static String guiDeleteWarn() { return get("gui-delete-warn", "<red>⚠ Click to delete", "<red>⚠ Nhan de xoa"); }

    // ==================== Item CRUD Messages ====================
    public static String savedItem(String id) { return get("saved-item", "Saved item '" + id + "'!", "Da luu item '" + id + "'!").replace("{id}", id); }
    public static String invalidId() { return get("invalid-id", "Invalid ID!", "ID khong hop le!"); }
    public static String itemAlreadyExists(String id) { return get("item-already-exists", "Item '" + id + "' already exists!", "Item '" + id + "' da ton tai!").replace("{id}", id); }
    public static String cannotCreateBlocked(String id) { return get("cannot-create-blocked", "Cannot create item '" + id + "' (blocked by another plugin).", "Khong the tao item '" + id + "' (bi chan boi plugin khac).").replace("{id}", id); }
    public static String createdItem(String id) { return get("created-item", "Created item '" + id + "'!", "Da tao item '" + id + "'!").replace("{id}", id); }
    public static String cannotDeleteBlocked(String id) { return get("cannot-delete-blocked", "Cannot delete item '" + id + "' (blocked by another plugin).", "Khong the xoa item '" + id + "' (bi chan boi plugin khac).").replace("{id}", id); }
    public static String deletedItem(String id) { return get("deleted-item", "Deleted item '" + id + "'!", "Da xoa item '" + id + "'!").replace("{id}", id); }

    // ==================== Confirm Delete GUI ====================
    public static String confirmDeleteTitle() { return get("confirm-delete-title", "<red><bold>Confirm Delete Item", "<red><bold>Xac Nhan Xoa Item"); }
    public static String confirmDeleteDesc() { return get("confirm-delete-desc", "Delete item: ", "Xoa item: "); }
    public static String confirmDeleteWarn() { return get("confirm-delete-warn", "<red>⚠ This action cannot be undone!", "<red>⚠ Hanh dong nay khong the hoan tac!"); }
    public static String confirmDeleteYes() { return get("confirm-delete-yes", "<green><bold>CONFIRM DELETE", "<green><bold>XAC NHAN XOA"); }
    public static String confirmDeleteNo() { return get("confirm-delete-no", "<red><bold>CANCEL", "<red><bold>HUY"); }
    public static String confirmDeleteBack() { return get("confirm-delete-back", "<gray>Back to item list", "<gray>Quay lai danh sach item"); }


    // ==================== Navigation ====================
    public static String guiBack() { return get("gui-back", "<yellow>← Back", "<yellow>← Quay Lai"); }
    public static String guiBackDesc() { return get("gui-back-desc", "<gray>Click to go back", "<gray>Nhan de quay lai"); }
    public static String guiPrevious() { return get("gui-previous", "<yellow>◄ Previous page", "<yellow>◄ Trang truoc"); }
    public static String guiNext() { return get("gui-next", "<yellow>Next page ►", "<yellow>Trang sau ►"); }

    // ==================== Lore Editor ====================
    public static String loreEditorTitle(String id) { return get("lore-editor-title", "<gold><bold>Lore Editor</bold> <dark_gray>- <white>" + id, "<gold><bold>Trinh Sua Lore</bold> <dark_gray>- <white>" + id).replace("{id}", id); }
    public static String loreEditorHeader() { return get("lore-editor-header", "<gold><bold>Lore Editor</bold>", "<gold><bold>Trinh Sua Lore</bold>"); }
    public static String loreEditorDesc() { return get("lore-editor-desc", "<gray>Edit item description", "<gray>Sua mo ta cua item"); }
    public static String loreEditorLeftClick() { return get("lore-editor-left", "<yellow>Left click: <white>Edit line", "<yellow>Trai chuot: <white>Sua dong"); }
    public static String loreEditorRightClick() { return get("lore-editor-right", "<yellow>Right click: <white>Delete line", "<yellow>Phai chuot: <white>Xoa dong"); }
    public static String loreEditorShiftClick() { return get("lore-editor-shift", "<yellow>Shift+Click: <white>Insert blank line above", "<yellow>Shift+Nhan: <white>Chen dong trong phia tren"); }
    public static String loreEditorLine(int line) { return get("lore-editor-line", "Line " + line, "Dong " + line).replace("{line}", String.valueOf(line)); }
    public static String loreEditorContent() { return get("lore-editor-content", "<gray>Content:", "<gray>Noi dung:"); }
    public static String loreEditorEmpty() { return get("lore-editor-empty", "<dark_gray>(empty)", "<dark_gray>(trong)"); }
    public static String loreEditorEdit() { return get("lore-editor-edit", "<yellow>Left click: <white>Edit", "<yellow>Trai chuot: <white>Sua"); }
    public static String loreEditorDelete() { return get("lore-editor-delete", "<red>Right click: <white>Delete", "<red>Phai chuot: <white>Xoa"); }
    public static String loreEditorInsert() { return get("lore-editor-insert", "<aqua>Shift+Click: <white>Insert blank line", "<aqua>Shift+Nhan: <white>Chen dong trong"); }
    public static String loreEditorPrompt(int line) { return get("lore-editor-prompt", "<yellow>Enter new content for line " + line + " <gray>(supports MiniMessage):", "<yellow>Nhap noi dung moi cho dong " + line + " <gray>(ho tro MiniMessage):").replace("{line}", String.valueOf(line)); }
    public static String loreEditorAddButton() { return get("lore-editor-add-btn", "<green><bold>+ Add new line</bold>", "<green><bold>+ Them dong moi</bold>"); }
    public static String loreEditorAddDesc() { return get("lore-editor-add-desc", "<gray>Add a new line at the end of lore", "<gray>Them dong moi vao cuoi lore"); }
    public static String loreEditorAddPrompt() { return get("lore-editor-add-prompt", "<yellow>Enter new line content <gray>(supports MiniMessage):", "<yellow>Nhap noi dung dong moi <gray>(ho tro MiniMessage):"); }
    public static String loreEditorClearButton() { return get("lore-editor-clear-btn", "<red><bold>Clear all lore</bold>", "<red><bold>Xoa tat ca lore</bold>"); }
    public static String loreEditorClearDesc() { return get("lore-editor-clear-desc", "<gray>Delete all lore lines", "<gray>Xoa tat ca dong lore"); }


    // ==================== Enchantment Editor ====================
    public static String enchEditorTitle(String id) { return get("ench-editor-title", "<light_purple><bold>Enchantments</bold> <dark_gray>- <white>" + id, "<light_purple><bold>Phu Phep</bold> <dark_gray>- <white>" + id).replace("{id}", id); }
    public static String enchEditorHeader() { return get("ench-editor-header", "<light_purple><bold>Enchantment Editor</bold>", "<light_purple><bold>Trinh Sua Phu Phep</bold>"); }
    public static String enchEditorDesc() { return get("ench-editor-desc", "<gray>Edit enchantments for this item", "<gray>Sua phu phep cho item nay"); }
    public static String enchEditorLeft() { return get("ench-editor-left", "<yellow>Left click: <white>Increase level", "<yellow>Trai chuot: <white>Tang cap"); }
    public static String enchEditorRight() { return get("ench-editor-right", "<yellow>Right click: <white>Decrease level", "<yellow>Phai chuot: <white>Giam cap"); }
    public static String enchEditorShift() { return get("ench-editor-shift", "<yellow>Shift+Click: <white>Remove entirely", "<yellow>Shift+Nhan: <white>Xoa hoan toan"); }
    public static String enchEditorKey(String key) { return get("ench-editor-key", "Key: " + key, "Khoa: " + key).replace("{key}", key); }
    public static String enchEditorAdded(int level) { return get("ench-editor-added", "✔ Added - Level " + level, "✔ Da them - Cap " + level).replace("{level}", String.valueOf(level)); }
    public static String enchEditorNotAdded() { return get("ench-editor-not-added", "✘ Not added", "✘ Chua them"); }

    // ==================== Flag Editor ====================
    public static String flagEditorTitle(String id) { return get("flag-editor-title", "<blue><bold>Item Flags</bold> <dark_gray>- <white>" + id, "<blue><bold>Co Item</bold> <dark_gray>- <white>" + id).replace("{id}", id); }
    public static String flagEditorOn() { return get("flag-editor-on", "<green>✔ ON", "<green>✔ BAT"); }
    public static String flagEditorOff() { return get("flag-editor-off", "<red>✘ OFF", "<red>✘ TAT"); }
    public static String flagEditorToggle() { return get("flag-editor-toggle", "<yellow>Click to toggle", "<yellow>Nhan de chuyen doi"); }


    // ==================== Recipe Editor ====================
    public static String recipeEditorTitle(String id) { return get("recipe-editor-title", "<gold>Recipe: <white>" + id, "<gold>Cong Thuc: <white>" + id).replace("{id}", id); }
    public static String recipeEditorGuide() { return get("recipe-editor-guide", "<gold><bold>Guide</bold>", "<gold><bold>Huong Dan</bold>"); }
    public static String recipeEditorGuide1() { return get("recipe-editor-guide1", "<gray>Place ingredients in the left slots", "<gray>Dat nguyen lieu vao o ben trai"); }
    public static String recipeEditorGuide2() { return get("recipe-editor-guide2", "<gray>Result is this item", "<gray>Ket qua la item nay"); }
    public static String recipeEditorGuide3() { return get("recipe-editor-guide3", "<green>Shaped: <gray>save with shape pattern", "<green>Shaped: <gray>luu voi mau hinh dang"); }
    public static String recipeEditorGuide4() { return get("recipe-editor-guide4", "<aqua>Shapeless: <gray>save without shape pattern", "<aqua>Shapeless: <gray>luu khong can mau hinh dang"); }
    public static String recipeEditorSaveShaped() { return get("recipe-editor-save-shaped", "<green><bold>Save Shaped</bold>", "<green><bold>Luu Shaped</bold>"); }
    public static String recipeEditorSaveShapedDesc() { return get("recipe-editor-save-shaped-desc", "<gray>Save shaped recipe (3x3)", "<gray>Luu cong thuc shaped (3x3)"); }
    public static String recipeEditorSaveShapeless() { return get("recipe-editor-save-shapeless", "<aqua><bold>Save Shapeless</bold>", "<aqua><bold>Luu Shapeless</bold>"); }
    public static String recipeEditorSaveShapelessDesc() { return get("recipe-editor-save-shapeless-desc", "<gray>Save shapeless recipe", "<gray>Luu cong thuc shapeless"); }
    public static String recipeEditorDelete() { return get("recipe-editor-delete", "<red><bold>Delete Recipe</bold>", "<red><bold>Xoa Cong Thuc</bold>"); }
    public static String recipeEditorDeleteDesc() { return get("recipe-editor-delete-desc", "<gray>Delete current recipe", "<gray>Xoa cong thuc hien tai"); }
    public static String recipeEditorHasRecipe() { return get("recipe-editor-has-recipe", "<green><bold>Has recipe</bold>", "<green><bold>Co cong thuc</bold>"); }
    public static String recipeEditorTypeShaped() { return get("recipe-editor-type-shaped", "Shaped", "Shaped"); }
    public static String recipeEditorTypeShapeless() { return get("recipe-editor-type-shapeless", "Shapeless", "Shapeless"); }
    public static String recipeEditorAmount(int amount) { return get("recipe-editor-amount", "Amount: " + amount, "So luong: " + amount).replace("{amount}", String.valueOf(amount)); }
    public static String recipeEditorEmptySlots() { return get("recipe-editor-empty-slots", "Please place ingredients in the crafting slots!", "Hay dat nguyen lieu vao o che tao!"); }
    public static String recipeEditorSaved(boolean shaped, String id) { String type = shaped ? "shaped" : "shapeless"; return get("recipe-editor-saved", "Saved recipe " + type + " for '" + id + "'!", "Da luu cong thuc " + type + " cho '" + id + "'!").replace("{type}", type).replace("{id}", id); }
    public static String recipeEditorDeleted(String id) { return get("recipe-editor-deleted", "Deleted recipe for '" + id + "'!", "Da xoa cong thuc cho '" + id + "'!").replace("{id}", id); }


    // ==================== Recipe List ====================
    public static String recipeListTitle() { return get("recipe-list-title", "<gradient:#ff6b6b:#ffd93d>Recipe List</gradient>", "<gradient:#ff6b6b:#ffd93d>Danh Sach Cong Thuc</gradient>"); }
    public static String recipeListHeader() { return get("recipe-list-header", "<gradient:#ff6b6b:#ffd93d><bold>Recipe List</bold></gradient>", "<gradient:#ff6b6b:#ffd93d><bold>Danh Sach Cong Thuc</bold></gradient>"); }
    public static String recipeListClick() { return get("recipe-list-click", "<gray>Click a recipe to view details", "<gray>Nhan vao cong thuc de xem chi tiet"); }
    public static String recipeListCraft() { return get("recipe-list-craft", "<green><bold>Craft</bold>", "<green><bold>Che Tao</bold>"); }
    public static String recipeListCraftDesc() { return get("recipe-list-craft-desc", "<gray>Open crafting table", "<gray>Mo ban che tao"); }
    public static String recipeListDeletedItem() { return get("recipe-list-deleted-item", "<gray>Item deleted", "<gray>Item da bi xoa"); }

    // ==================== Recipe Detail ====================
    public static String recipeDetailTitle(String id) { return get("recipe-detail-title", "<gold>Recipe: <white>" + id, "<gold>Cong Thuc: <white>" + id).replace("{id}", id); }
    public static String recipeDetailNotFound() { return get("recipe-detail-not-found", "<red>Not found", "<red>Khong tim thay"); }

    // ==================== Crafting GUI ====================
    public static String craftAmount1() { return get("craft-amount-1", "<gray>Craft 1 item", "<gray>Che tao 1 item"); }
    public static String craftAmount8() { return get("craft-amount-8", "<gray>Craft 8 items", "<gray>Che tao 8 items"); }
    public static String craftAmount64() { return get("craft-amount-64", "<gray>Craft 64 items", "<gray>Che tao 64 items"); }
    public static String craftAmountMax() { return get("craft-amount-max", "<gray>Craft as many as possible", "<gray>Che tao nhieu nhat co the"); }
    public static String craftSelected() { return get("craft-selected", "<green>✔ Selected", "<green>✔ Da chon"); }
    public static String craftClickToSelect() { return get("craft-click-to-select", "<yellow>Click to select", "<yellow>Nhan de chon"); }

    // ==================== Chat Input ====================
    public static String chatCancel() { return get("chat-cancel", "cancel", "huy"); }
    public static String chatCancelHint() { return get("chat-cancel-hint", "Type 'cancel' to cancel", "Go 'cancel' de huy"); }
    public static String invalidNumber() { return get("invalid-number", "Invalid number!", "So khong hop le!"); }
    public static String materialSet(String material) { return get("material-set", "Material set to: " + material, "Da dat vat lieu thanh: " + material).replace("{material}", material); }

    // ==================== Search ====================
    public static String searchNoPermission() { return get("search-no-permission", "You don't have permission to use search (uitem.search).", "Ban khong co quyen su dung tim kiem (uitem.search)."); }

    // ==================== Skill System (EN/VI) ====================
    public static String skillListTitle(String id) { return get("skill-list-title", "<light_purple><bold>Skills & Passives</bold> <dark_gray>- " + id, "<light_purple><bold>Ki Nang & Passive</bold> <dark_gray>- " + id).replace("{id}", id); }
    public static String skillListHeader(String id) { return get("skill-list-header", "<light_purple><bold>Skills & Passives for " + id + "</bold>", "<light_purple><bold>Ki Nang & Passive cho " + id + "</bold>").replace("{id}", id); }
    public static String skillListDesc() { return get("skill-list-desc", "<gray>Native UItem skill system (no ExecutableItems needed)", "<gray>He thong skill native cua UItem (khong can ExecutableItems)"); }
    public static String skillListAdd() { return get("skill-list-add", "<green><bold>+ ADD NEW SKILL</bold>", "<green><bold>+ THEM SKILL MOI</bold>"); }
    public static String skillListAddDesc() { return get("skill-list-add-desc", "<gray>Create active or passive skill", "<gray>Tao skill active hoac passive"); }
    public static String skillListPresetFireball() { return get("skill-preset-fireball", "<red><bold>Preset: Fireball</bold>", "<red><bold>Preset: Bong lua</bold>"); }
    public static String skillListPresetLifesteal() { return get("skill-preset-lifesteal", "<green><bold>Preset: Lifesteal</bold>", "<green><bold>Preset: Hut mau</bold>"); }
    public static String skillListPresetRegen() { return get("skill-preset-regen", "<aqua><bold>Preset: Regen Aura</bold>", "<aqua><bold>Preset: Hoi mau Aura</bold>"); }
    public static String skillListPresetFireballDesc() { return get("skill-preset-fireball-desc", "<gray>Right Click → shoot Small Fireball", "<gray>Click phai → ban Small Fireball"); }
    public static String skillListPresetLifestealDesc() { return get("skill-preset-lifesteal-desc", "<gray>On Hit → heal with chance", "<gray>Danh trung → hoi mau theo ty le"); }
    public static String skillListPresetRegenDesc() { return get("skill-preset-regen-desc", "<gray>While Holding → periodic heal", "<gray>Dang cam → hoi mau dinh ky"); }
    public static String skillListClickEdit() { return get("skill-list-click-edit", "<yellow>Left click: Edit", "<yellow>Click trai: Chinh sua"); }
    public static String skillListClickDelete() { return get("skill-list-click-delete", "<red>Right click: Delete", "<red>Click phai: Xoa"); }

    public static String skillEditorTitle(String skillId) { return get("skill-editor-title", "<light_purple><bold>Edit Skill:</bold> <white>" + skillId, "<light_purple><bold>Chinh Skill:</bold> <white>" + skillId).replace("{id}", skillId); }
    public static String skillEditorActiveTriggers() { return get("skill-editor-active-triggers", "<gold><bold>Active Triggers</bold>", "<gold><bold>Active Triggers</bold>"); }
    public static String skillEditorPassiveTriggers() { return get("skill-editor-passive-triggers", "<gold><bold>Passive / Combat Triggers</bold>", "<gold><bold>Passive / Combat Triggers</bold>"); }
    public static String skillEditorCooldown() { return get("skill-editor-cooldown", "<gold><bold>Cooldown: <white>{seconds}s</bold>", "<gold><bold>Cooldown: <white>{seconds}s</bold>").replace("{seconds}", "%s"); }
    public static String skillEditorCooldownHint() { return get("skill-editor-cooldown-hint", "<yellow>Click to enter cooldown in seconds", "<yellow>Click de nhap cooldown (giay)"); }
    public static String skillEditorChance() { return get("skill-editor-chance", "<gold><bold>Chance: <white>{percent}%</bold>", "<gold><bold>Ty le: <white>{percent}%</bold>").replace("{percent}", "%s"); }
    public static String skillEditorCancelEvent() { return get("skill-editor-cancel", "<gold><bold>Cancel Event: <white>{state}</bold>", "<gold><bold>Cancel Event: <white>{state}</bold>").replace("{state}", "%s"); }
    public static String skillEditorActionsHeader(int count) { return get("skill-editor-actions", "<gold><bold>Actions ({count})</bold>", "<gold><bold>Actions ({count})</bold>").replace("{count}", String.valueOf(count)); }
    public static String skillEditorActionsHint() { return get("skill-editor-actions-hint", "<gray>Left click: Edit value | Right click: Delete", "<gray>Click trai: Sua | Click phai: Xoa"); }
    public static String skillEditorAddAction() { return get("skill-editor-add-action", "<green><bold>+ ADD ACTION</bold>", "<green><bold>+ THEM ACTION</bold>"); }
    public static String skillEditorChooseAction() { return get("skill-editor-choose-action", "<green><bold>CHOOSE ACTION TYPE (Click to add)</bold>", "<green><bold>CHON LOAI ACTION (Click de them)</bold>"); }
    public static String skillEditorNoActions() { return get("skill-editor-no-actions", "<gray><italic>No actions yet</italic>", "<gray><italic>Chua co action</italic>"); }

    // Skill trigger display names (used in GUI + lore)
    public static String skillTriggerDisplayName(String key) {
        return switch (key.toUpperCase(Locale.ROOT)) {
            case "RIGHT_CLICK" -> get("trigger-right", "Right Click", "Click Phai");
            case "LEFT_CLICK" -> get("trigger-left", "Left Click", "Click Trai");
            case "SHIFT_RIGHT_CLICK" -> get("trigger-sright", "Shift + Right Click", "Shift + Click Phai");
            case "SHIFT_LEFT_CLICK" -> get("trigger-sleft", "Shift + Left Click", "Shift + Click Trai");
            case "DROP" -> get("trigger-drop", "Drop (Q)", "Tha Item (Q)");
            case "SWAP_HAND" -> get("trigger-swap", "Swap Hand (F)", "Doi Tay (F)");
            case "THROW" -> get("trigger-throw", "Throw", "Nem");
            case "SHOOT" -> get("trigger-shoot", "Shoot / Throw (Bow, Crossbow, Trident)", "Bắn / Ném (Cung, Nỏ, Đinh Ba)");
            case "HIT" -> get("trigger-hit", "On Hit", "Danh Trung");
            case "TAKE_DAMAGE" -> get("trigger-damage", "On Damaged", "Bi Danh");
            case "KILL" -> get("trigger-kill", "On Kill", "Giet");
            case "EQUIP" -> get("trigger-equip", "On Equip", "Trang Bi / Cam");
            case "UNEQUIP" -> get("trigger-unequip", "On Unequip", "Thao Ra");
            case "PERIODIC" -> get("trigger-periodic", "Periodic", "Dinh Ky");
            case "WHILE_HOLDING" -> get("trigger-holding", "While Holding", "Dang Cam");
            case "WHILE_WEARING" -> get("trigger-wearing", "While Wearing", "Dang Mac");
            default -> key;
        };
    }

    public static String skillTriggerDescription(String key) {
        return switch (key.toUpperCase(Locale.ROOT)) {
            case "RIGHT_CLICK" -> get("trigger-desc-right", "Activate on right click", "Kich hoat khi click phai");
            case "LEFT_CLICK" -> get("trigger-desc-left", "Activate on left click", "Kich hoat khi click trai");
            case "SHIFT_RIGHT_CLICK" -> get("trigger-desc-sright", "Activate on shift + right click", "Kich hoat khi shift + click phai");
            case "SHIFT_LEFT_CLICK" -> get("trigger-desc-sleft", "Activate on shift + left click", "Kich hoat khi shift + click trai");
            case "DROP" -> get("trigger-desc-drop", "Activate when dropping the item (Q)", "Kich hoat khi tha item (Q)");
            case "SWAP_HAND" -> get("trigger-desc-swap", "Activate when swapping hands (F)", "Kich hoat khi doi tay (F)");
            case "THROW" -> get("trigger-desc-throw", "Activate when throwing the item", "Kich hoat khi nem item");
            case "SHOOT" -> get("trigger-desc-shoot", "Activate when shooting with bow/crossbow or throwing trident", "Kich hoat khi ban cung/no hoac nem dinh ba");
            case "HIT" -> get("trigger-desc-hit", "Activate when hitting a target", "Kich hoat khi danh trung muc tieu");
            case "TAKE_DAMAGE" -> get("trigger-desc-damage", "Activate when taking damage", "Kich hoat khi bi danh");
            case "KILL" -> get("trigger-desc-kill", "Activate when killing with this item", "Kich hoat khi giet bang item nay");
            case "EQUIP" -> get("trigger-desc-equip", "Activate when equipping or holding", "Kich hoat khi mac hoac cam");
            case "UNEQUIP" -> get("trigger-desc-unequip", "Activate when unequipping", "Kich hoat khi thao ra");
            case "PERIODIC" -> get("trigger-desc-periodic", "Repeats every period (for regen, aura...)", "Chay lap lai dinh ky (regen, aura...)");
            case "WHILE_HOLDING" -> get("trigger-desc-holding", "Repeats while holding in hand", "Chay dinh ky khi dang cam tren tay");
            case "WHILE_WEARING" -> get("trigger-desc-wearing", "Repeats while wearing as armor", "Chay dinh ky khi dang mac giap");
            default -> "";
        };
    }

    // Action type display (for buttons)
    public static String skillActionDisplay(SkillAction.ActionType type) {
        String key = "action-" + type.name().toLowerCase(Locale.ROOT);
        String en = type.getDisplay();
        String vi = switch (type) {
            case MESSAGE -> "Tin nhan";
            case ACTION_BAR -> "Action bar";
            case TITLE -> "Title";
            case PLAY_SOUND -> "Phat am thanh";
            case SPAWN_PARTICLE -> "Particle";
            case GIVE_POTION -> "Cho hieu ung";
            case REMOVE_POTION -> "Xoa hieu ung";
            case HEAL -> "Hoi mau";
            case DAMAGE -> "Gay sat thuong";
            case LAUNCH -> "Day nguoi choi";
            case TELEPORT -> "Dich chuyen";
            case STRIKE_LIGHTNING -> "Goi set";
            case SHOOT_PROJECTILE -> "Ban dan";
            case PLAYER_COMMAND -> "Lenh nhu player";
            case CONSOLE_COMMAND -> "Lenh console";
            case CONSUME_DURABILITY -> "Tieu hao do ben";
            case FEED -> "Hoi doi";
            case SET_FIRE -> "Dot chay";
            default -> type.getDisplay();
        };
        return get(key, en, vi);
    }

    // Feedback messages
    public static String skillAdded(String type) { return get("skill-added", "Added " + type, "Da them " + type); }
    public static String skillUpdated(String type) { return get("skill-updated", "Updated " + type, "Da cap nhat " + type); }
    public static String skillPresetAdded(String name) { return get("skill-preset-added", "Added preset: " + name, "Da them preset: " + name).replace("{name}", name); }

    // ==================== Help ====================
    public static String helpTitle() { return get("help-title", "  <gradient:#ff6b6b:#ffd93d><bold>UItem</bold></gradient> <gray>— Ultimate Item Plugin", "  <gradient:#ff6b6b:#ffd93d><bold>UItem</bold></gradient> <gray>— Ultimate Item Plugin"); }
    public static String helpGive() { return get("help-give", "  <gold>/uitem give <id> [player] [amount] <dark_gray>— <gray>Give item", "  <gold>/uitem give <id> [nguoi choi] [so luong] <dark_gray>— <gray>Dua item"); }
    public static String helpEditor() { return get("help-editor", "  <gold>/uitem editor <dark_gray>— <gray>Open editor GUI", "  <gold>/uitem editor <dark_gray>— <gray>Mo trinh sua GUI"); }
    public static String helpList() { return get("help-list", "  <gold>/uitem list <dark_gray>— <gray>List items", "  <gold>/uitem list <dark_gray>— <gray>Danh sach items"); }
    public static String helpRp() { return get("help-rp", "  <gold>/uitem rp <generate|export|apply|status> <dark_gray>— <gray>Manage resource pack", "  <gold>/uitem rp <generate|export|apply|status> <dark_gray>— <gray>Quan ly resource pack"); }
    public static String helpCheck() { return get("help-check", "  <gold>/uitem check <dark_gray>— <gray>Check if held item is a valid UItem", "  <gold>/uitem check <dark_gray>— <gray>Kiem tra item co phai UItem hop le"); }
    public static String helpCraft() { return get("help-craft", "  <gold>/uitem craft <dark_gray>— <gray>Open UItem crafting table", "  <gold>/uitem craft <dark_gray>— <gray>Mo ban che tao UItem"); }
    public static String helpCraftingCmd() { return get("help-crafting-cmd", "  <gold>/crafting <dark_gray>— <gray>List all crafting recipes", "  <gold>/crafting <dark_gray>— <gray>Danh sach cong thuc che tao"); }
    public static String helpCraftingTable() { return get("help-crafting-table", "  <gold>/crafting craft <dark_gray>— <gray>Open UItem crafting table", "  <gold>/crafting craft <dark_gray>— <gray>Mo ban che tao UItem"); }
    public static String helpReload() { return get("help-reload", "  <gold>/uitem reload <dark_gray>— <gray>Reload plugin", "  <gold>/uitem reload <dark_gray>— <gray>Tai lai plugin"); }
    public static String helpHelp() { return get("help-help", "  <gold>/uitem help <dark_gray>— <gray>Show help", "  <gold>/uitem help <dark_gray>— <gray>Hien tro giup"); }
    public static String helpUi() { return get("help-ui", "  <gold>/ui <dark_gray>— <gray>Short alias for /uitem", "  <gold>/ui <dark_gray>— <gray>Viet tat cua /uitem"); }
}


