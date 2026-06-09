package com.uitem.gui;

import com.uitem.UItemData;
import com.uitem.UItemPlugin;
import com.uitem.legacy.skill.SkillDataHelper;
import com.uitem.legacy.skill.gui.SkillListGui;
import com.uitem.rarity.RarityConfig;
import com.uitem.util.Messages;
import com.uitem.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI chỉnh sửa một custom item.
 * Hiển thị tất cả thuộc tính có thể chỉnh sửa.
 */
public class ItemEditorGui extends AbstractGui {

    private final UItemData data;
    private boolean materialPickMode = false;

    public ItemEditorGui(UItemPlugin plugin, Player player, UItemData data) {
        super(plugin, player, 54,
                "<gradient:#ff6b6b:#ffd93d>Chỉnh sửa: </gradient><white>" + data.getId());
        this.data = data;
    }

    @Override
    public void open() {
        if (!player.hasPermission("uitem.editor")) {
            player.sendMessage(TextUtil.error(com.uitem.util.Messages.noPermission()));
            return;
        }
        super.open();
    }

    @Override
    public void handleBottomInventoryClick(InventoryClickEvent event) {
        if (materialPickMode) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
                Material mat = clicked.getType();
                data.setMaterial(mat);
                player.sendMessage(TextUtil.success("<green>Đã đặt vật liệu thành: <white>" + mat.name()));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                materialPickMode = false;
                saveAndRefresh();
            }
        } else {
            event.setCancelled(false);
        }
    }

    @Override
    protected void setup() {
        fillBorder(6);

        // === Preview item (slot 4) ===
        ItemStack preview = plugin.getItemManager().buildItemStack(data);
        setItem(4, preview);

        // === Material (slot 10) - Open Material Picker / Pick from Inventory ===
        Material currentMat = data.getMaterial() != null ? data.getMaterial() : Material.STONE;
        setItem(10, createItem(currentMat,
                "<gold><bold>Vật Liệu</bold>",
                List.of(
                        "<gray>Hiện tại: <white>" + currentMat.name(),
                        "",
                        "<yellow>Click để mở danh sách vật liệu",
                        "<green>Shift+Click để pick từ inventory",
                        "<gray>Pick trực tiếp item từ túi đồ của bạn"
                )        ), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            if (event.isShiftClick()) {
                materialPickMode = !materialPickMode;
                if (materialPickMode) {
                    player.sendMessage(TextUtil.info("<green>Click vào item trong inventory để chọn làm vật liệu. Click lại slot để thoát."));
                    refresh();
                } else {
                    refresh();
                }
            } else {
                new MaterialPickerGui(plugin, player, data, null).openAsChildOf(this);
            }
        });

        // Pick mode indicator (hiển thị khi đang chọn material từ inventory)
        if (materialPickMode) {
            setItem(48, createItem(Material.LIME_STAINED_GLASS_PANE,
                    "<green><bold>📦 PICK MODE: Click item trong inventory</bold>",
                    List.of(
                            "<gray>Click vào bất kỳ item nào trong túi đồ",
                            "<gray>để đặt làm vật liệu cho item này",
                            "",
                            "<red>Right-click để thoát"
                    )), ev -> {
                materialPickMode = false;
                refresh();
            });
        }

        // === Display Name (slot 11) ===
        setItem(11, createItem(Material.NAME_TAG,
                "<gold><bold>Tên Hiển Thị</bold>",
                List.of(
                        "<gray>Hiện tại: " + (data.getDisplayName() != null ? data.getDisplayName() : "<gray>Chưa đặt"),
                        "",
                        "<yellow>Click để đổi tên",
                        "<gray>Hỗ trợ định dạng MiniMessage",
                        "<gray>Ví dụ: <red>Ruby <gold>Sword"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập tên hiển thị <gray>(hỗ trợ MiniMessage):",
                    input -> {
                        data.setDisplayName(input);
                        saveAndReopen();
                        player.sendMessage(TextUtil.success(Messages.renamedTo(input)));
                    });
        });

        // === Lore (slot 12) ===
        setItem(12, createItem(Material.WRITABLE_BOOK,
                "<gold><bold>Lore</bold>",
                List.of(
                        "<gray>Số dòng: <white>" + data.getLore().size(),
                        "",
                        "<yellow>Click để chỉnh lore",
                        "<gray>Mở GUI Chỉnh Lore"
                )        ), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new LoreEditorGui(plugin, player, data).openAsChildOf(this);
        });

        // === Enchantments (slot 13) ===
        setItem(13, createItem(Material.ENCHANTING_TABLE,
                "<gold><bold>Phù Phép</bold>",
                List.of(
                        "<gray>Phù phép: <white>" + data.getEnchantments().size(),
                        "",
                        enchantmentSummary(),
                        "",
                        "<yellow>Click để chỉnh phù phép"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new EnchantmentGui(plugin, player, data).openAsChildOf(this);
        });

        // === Flags (slot 14) ===
        setItem(14, createItem(Material.WHITE_BANNER,
                "<gold><bold>Cờ Item (Flags)</bold>",
                List.of(
                        "<gray>Cờ: <white>" + data.getFlags().size(),
                        "",
                        flagsSummary(),
                        "",
                        "<yellow>Click để chỉnh cờ"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new FlagGui(plugin, player, data).openAsChildOf(this);
        });

        // === Custom Model Data (slot 15) ===
        setItem(15, createItem(Material.PAINTING,
                "<gold><bold>Custom Model Data</bold>",
                List.of(
                        "<gray>Hiện tại: <white>" + (data.getCustomModelData() > 0 ? data.getCustomModelData() : "Không có"),
                        "",
                        "<yellow>Click để thay đổi",
                        "<gray>Nhập số (-1 để tắt)"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập số Custom Model Data <gray>(-1 để tắt):",
                    input -> {
                        try {
                            int cmd = Integer.parseInt(input.trim());
                            data.setCustomModelData(cmd);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success("Custom Model Data = " + cmd));
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Số không hợp lệ!"));
                            new ItemEditorGui(plugin, player, data).open();
                        }
                    });
        });

        // === Custom Model (for resource pack, slot 18) - allows mapping any model file (Oraxen style)
        String currentModel = data.getModel();
        setItem(18, createItem(Material.BOOK,
                "<aqua><bold>Custom Model (RP)</bold>",
                List.of(
                        "<gray>Hiện tại: <white>" + (currentModel != null ? currentModel : "Không có (dùng item ID)"),
                        "",
                        "<yellow>Click để đặt tên/path model tùy chỉnh",
                        "<gray>ví dụ 'my_sword' hoặc 'uitem:item/library/sword'",
                        "<gray>hoặc 'itemsadder:item/some_ia_model' cho model ItemsAdder",
                        "<gray>Thả file .json (bất kỳ tên) vào plugins/UItem/models/",
                        "<gray>Tốt để copy-paste model ngoài từ IA/Oraxen"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập tên model tùy chỉnh (hoặc 'none' để xóa):",
                    input -> {
                        String val = input.trim();
                        if (val.equalsIgnoreCase("none") || val.isEmpty()) {
                            data.setModel(null);
                            player.sendMessage(TextUtil.success("Custom model removed (will use item ID).")); // TODO: add to Messages if needed
                        } else {
                            data.setModel(val);
                            player.sendMessage(TextUtil.success("Custom model set: " + val));
                        }
                        saveAndReopen();
                    });
        });

        // === Unbreakable (slot 16) ===
        setItem(16, createItem(
                data.isUnbreakable() ? Material.BEDROCK : Material.COBBLESTONE,
                "<gold><bold>Unbreakable</bold>",
                List.of(
                        "<gray>Hiện tại: " + (data.isUnbreakable() ? "<green>BẬT" : "<red>TẮT"),
                        "",
                        "<yellow>Click để bật/tắt"
                )), event -> {
            data.setUnbreakable(!data.isUnbreakable());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f,
                    data.isUnbreakable() ? 1.2f : 0.8f);
            saveAndRefresh();
        });

        // === Durability (slot 17) ===
        setItem(17, createItem(Material.ANVIL,
                "<gold><bold>Độ Bền</bold>",
                List.of(
                        "<gray>Độ bền tối đa: <white>" + (data.getMaxDurability() > 0 ? data.getMaxDurability() : "Mặc định"),
                        "",
                        "<yellow>Click để đặt độ bền tối đa",
                        "<gray>Nhập -1 cho độ bền mặc định vật liệu",
                        "<gray>Nhập 0 cho không có thanh độ bền (như không thể phá)",
                        "<gray>Hoặc nhập số > 0"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập độ bền tối đa <gray>(-1 = mặc định, 0 = không thanh, >0 = tùy chỉnh):",
                    input -> {
                        try {
                            int dur = Integer.parseInt(input.trim());
                            data.setMaxDurability(dur);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success(Messages.durabilitySet((dur > 0 ? String.valueOf(dur) : (dur == 0 ? "No bar" : "Default")))));
                        } catch (NumberFormatException e) {
                            player.sendMessage(TextUtil.error("Số không hợp lệ!"));
                            new ItemEditorGui(plugin, player, data).open();
                        }
                    });
        });

        // === Glowing (slot 19) ===
        setItem(19, createItem(
                data.isGlowing() ? Material.GLOWSTONE : Material.STONE,
                "<gold><bold>Phát Sáng</bold>",
                List.of(
                        "<gray>Hiện tại: " + (data.isGlowing() ? "<green>BẬT" : "<red>TẮT"),
                        "",
                        "<yellow>Click để bật/tắt",
                        "<gray>Thêm hiệu ứng phát sáng cho item"
                )), event -> {
            data.setGlowing(!data.isGlowing());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f,
                    data.isGlowing() ? 1.2f : 0.8f);
            saveAndRefresh();
        });

        // === Attributes (slot 20) - now opens AttributePresetGui ===
        setItem(20, createItem(Material.IRON_SWORD,
                "<gold><bold>Thuộc Tính (Attributes)</bold>",
                List.of(
                        "<gray>Thuộc tính: <white>" + data.getAttributes().size(),
                        "",
                        attributeSummary(),
                        "",
                        "<yellow>Click để mở GUI chọn thuộc tính",
                        "<gray>Chọn preset hoặc nhập tùy chỉnh"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new AttributePresetGui(plugin, player, data).openAsChildOf(this);
        });

        // === Rarity (slot 21) ===
        String currentRarity = data.getRarity();
        String rarityDisplay = currentRarity != null ? currentRarity : "Không có";
        setItem(21, createItem(Material.NETHER_STAR,
                "<gold><bold>Độ Hiếm (Rarity)</bold>",
                List.of(
                        "<gray>Hiện tại: " + (currentRarity != null ? RarityConfig.getRarityColor(currentRarity) + currentRarity : "<gray>Không có"),
                        "",
                        "<yellow>Click để chọn độ hiếm",
                        "<gray>COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, ULTRA"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();
            StringBuilder msg = new StringBuilder("<gold>Các độ hiếm có sẵn:<dark_gray> ");
            for (String r : RarityConfig.getKeys()) {
                msg.append("\n").append(RarityConfig.getRarityColor(r)).append("▸ ").append(r);
            }
            msg.append("\n\n<yellow>Nhập tên độ hiếm (hoặc 'none' để xóa):");
            plugin.getChatInputManager().requestInput(player,
                    msg.toString(),
                    input -> {
                        if (input.equalsIgnoreCase("none")) {
                            data.setRarity(null);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success(Messages.guiRarityRemoved()));
                            return;
                        }
                        String upper = input.toUpperCase().trim();
                        if (RarityConfig.getKeys().contains(upper)) {
                            data.setRarity(upper);
                            saveAndReopen();
                            player.sendMessage(TextUtil.success(Messages.guiRaritySet(upper)));
                        } else {
                            // Allow creating new rarity on the fly (copy-paste friendly)
                            data.setRarity(upper);
                            // Ask for definition
                            plugin.getChatInputManager().requestInput(player,
                                "<yellow>Độ hiếm mới '" + upper + "'. Nhập color (MiniMessage, ví dụ <gold>) hoặc enter cho mặc định:",
                                colorInput -> {
                                    String color = colorInput.trim().isEmpty() ? "<white>" : colorInput.trim();
                                    plugin.getChatInputManager().requestInput(player,
                                        "<yellow>Prefix (ví dụ <gold>Legendary ) hoặc enter:",
                                        prefixInput -> {
                                            String prefix = prefixInput.trim();
                                            plugin.getChatInputManager().requestInput(player,
                                                "<yellow>Lore lines (phân cách bằng | ) hoặc enter để bỏ qua:",
                                                loreInput -> {
                                                    List<String> lore = loreInput.trim().isEmpty() ? List.of() : Arrays.asList(loreInput.split("\\|"));
                                                    RarityConfig.setRarity(upper, color, prefix, lore);
                                                    RarityConfig.saveRarities();
                                                    saveAndReopen();
                                                    player.sendMessage(TextUtil.success("Đã tạo/sửa rarity '" + upper + "' (lưu vào rarity.yml)"));
                                                });
                                        });
                                });
                        }
                    });
        });

        // === Manage Rarities (new button for full GUI management) ===
        setItem(30, createItem(Material.NETHER_STAR,
                "<gold><bold>⚙ Quản lý Rarities</bold>",
                List.of(
                        "<gray>Mở danh sách rarity để tạo/sửa/xóa",
                        "<yellow>Định nghĩa rarity được lưu tại rarity.yml",
                        "<gray>Dễ copy-paste và chỉnh sửa"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new RarityListGui(plugin, player).openAsChildOf(this);
        });

        // === ExecutableItems ID (slot 22) - only show if ExecutableItems is present ===
        if (plugin.getIntegrationManager().hasExecutableItems()) {
            setItem(22, createItem(Material.BLAZE_POWDER,
                    "<gold><bold>⚡ ExecutableItems</bold>",
                    List.of(
                            "<gray>Hiện tại: <white>" + (data.getExecutableItemsId() != null ? data.getExecutableItemsId() : "Không có"),
                            "",
                            "<yellow>Click để đặt ID ExecutableItems",
                            "<gray>Kỹ năng/Passive sẽ do EI xử lý"
                    )), event -> {
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Enter ExecutableItems item ID <gray>(or 'none' to remove):",
                        input -> {
                            if (input.equalsIgnoreCase("none")) {
                                data.setExecutableItemsId(null);
                            } else {
                                data.setExecutableItemsId(input.trim());
                            }
                            saveAndReopen();
                        });
            });
        }

        // === Oraxen ID (slot 23) - only show if Oraxen is present ===
        if (plugin.getIntegrationManager().hasOraxen()) {
            setItem(23, createItem(Material.DIAMOND,
                    "<aqua><bold>Oraxen ID</bold>",
                    List.of(
                            "<gray>Hiện tại: <white>" + (data.getOraxenId() != null ? data.getOraxenId() : "Không có"),
                            "",
                            "<yellow>Click để đặt ID item Oraxen",
                            "<gray>Dùng item từ Oraxen làm base"
                    )), event -> {
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Enter Oraxen item ID <gray>(or 'none' to remove):",
                        input -> {
                            if (input.equalsIgnoreCase("none")) {
                                data.setOraxenId(null);
                            } else {
                                data.setOraxenId(input.trim());
                            }
                            saveAndReopen();
                        });
            });
        }

        // === ItemsAdder ID (slot 24) - only show if ItemsAdder is present ===
        if (plugin.getIntegrationManager().hasItemsAdder()) {
            setItem(24, createItem(Material.EMERALD,
                    "<green><bold>ItemsAdder ID</bold>",
                    List.of(
                            "<gray>Hiện tại: <white>" + (data.getItemsAdderId() != null ? data.getItemsAdderId() : "Không có"),
                            "",
                            "<yellow>Click để đặt ID item ItemsAdder",
                            "<gray>Dùng item từ ItemsAdder làm base"
                    )), event -> {
                plugin.getChatInputManager().requestInput(player,
                        "<yellow>Enter ItemsAdder namespaced ID <gray>(or 'none' to remove):",
                        input -> {
                            if (input.equalsIgnoreCase("none")) {
                                data.setItemsAdderId(null);
                            } else {
                                data.setItemsAdderId(input.trim());
                            }
                            saveAndReopen();
                        });
            });
        }

        // === SKILLS & PASSIVES (Native UItem system - dễ và ổn định hơn ExecutableItems) ===
        int skillCount = SkillDataHelper.skillCount(data);
        setItem(26, createItem(Material.ENCHANTED_BOOK,
                "<light_purple><bold>⚡ Kỹ Năng & Passive (UItem)</bold>",
                List.of(
                        "<gray>Số skill: <white>" + skillCount,
                        "",
                        "<yellow>Click để mở Skill Editor",
                        "<gray>Hệ thống skill native của UItem",
                        "<gray>→ Dễ cấu hình, ổn định, không cần EI",
                        "<gray>Hỗ trợ: Right/Left Click, Hit, Equip, Periodic, v.v."
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new SkillListGui(plugin, player, data).openAsChildOf(this);
        });

        // === Recipe (slot 25) - Open Recipe Editor ===
        boolean hasRecipe = plugin.getRecipeManager().hasRecipe(data.getId());
        setItem(25, createItem(
                hasRecipe ? Material.CRAFTING_TABLE : Material.FURNACE,
                "<gold><bold>Công Thức Chế Tạo</bold>",
                List.of(
                        hasRecipe ? "<green>Đã có công thức" : "<red>Chưa có công thức",
                        "",
                        "<yellow>Click để chỉnh công thức",
                        "<gray>Shaped / Shapeless + nguyên liệu UItem tùy chỉnh",
                        "<gray>Support output x1-x64 per craft"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new RecipeEditorGui(plugin, player, data).openAsChildOfEditor(this);
        });

        // === Hiệu Ứng (phản đòn / sát thương thêm khi hit/damage/equip/fullset/die) slot 27 ===
        int effCount = (data.getEffects() != null) ? data.getEffects().size() : 0;
        setItem(27, createItem(
                effCount > 0 ? Material.BOOK : Material.PAPER,
                "<light_purple><bold>Hiệu Ứng Đã Gắn</bold>",
                List.of(
                        "<gray>Hiệu ứng: <white>" + effCount,
                        "",
                        "<yellow>Click để mở GUI Chỉnh Hiệu Ứng",
                        "<gray>Chọn trigger (HIT, DAMAGE, DIE, EQUIP, FULL_SET)",
                        "<gray>Chọn loại REFLECT hoặc EXTRA_DAMAGE",
                        "<gray>Đặt tỷ lệ + giá trị; tùy chọn target khi HIT",
                        "<gray>FULL_SET kích hoạt khi mặc đủ 4 món giáp"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new EffectEditorGui(plugin, player, data).openAsChildOf(this);
        });
 
        // === Quick Actions (slot 47) - Duplicate item ===
        setItem(47, createItem(Material.PAPER,
                "<aqua><bold>📋 Nhân Bản Item</bold>",
                List.of(
                        "<gray>Tạo bản sao của item này",
                        "<gray>với ID mới",
                        "",
                        "<yellow>Click để nhân bản"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getChatInputManager().requestInput(player,
                    "<yellow>Nhập ID cho bản sao <gray>(chỉ chữ cái, số, gạch dưới):",
                    input -> {
                        String newId = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                        if (newId.isEmpty()) {
                            player.sendMessage(TextUtil.error("ID không hợp lệ!"));
                            new ItemEditorGui(plugin, player, data).open();
                            return;
                        }
                        if (plugin.getItemManager().hasItem(newId)) {
                            player.sendMessage(TextUtil.error("Item '" + newId + "' đã tồn tại!"));
                            new ItemEditorGui(plugin, player, data).open();
                            return;
                        }
                        // Deep clone with new ID
                        UItemData clone = data.clone();
                        // Use reflection-free approach: create new with same fields
                        UItemData newData = new UItemData(newId);
                        newData.setMaterial(data.getMaterial());
                        newData.setDisplayName(data.getDisplayName());
                        newData.setLore(new java.util.ArrayList<>(data.getLore()));
                        newData.setEnchantments(new java.util.LinkedHashMap<>(data.getEnchantments()));
                        newData.setFlags(new java.util.LinkedHashSet<>(data.getFlags()));
                        newData.setCustomModelData(data.getCustomModelData());
                        newData.setUnbreakable(data.isUnbreakable());
                        newData.setGlowing(data.isGlowing());
                        newData.setMaxDurability(data.getMaxDurability());
                        newData.setRarity(data.getRarity());
                        newData.setModel(data.getModel());
                        for (UItemData.AttributeData a : data.getAttributes()) newData.getAttributes().add(a.copy());
                        for (UItemData.ItemEffect e : data.getEffects()) newData.getEffects().add(e.copy());
                        java.util.List<com.ultra.ucore.skill.Skill> clonedSkills = new java.util.ArrayList<>();
                        for (com.ultra.ucore.skill.Skill s : SkillDataHelper.getSkills(data)) {
                            clonedSkills.add(s.copy());
                        }
                        SkillDataHelper.setSkills(newData, clonedSkills);

                        plugin.getItemManager().registerItem(newData);
                        plugin.getResourcePackManager().syncCmdsToLiveItems();
                        plugin.getStorageManager().saveItem(plugin.getItemManager(), newData);
                        player.sendMessage(TextUtil.success("Đã nhân bản '" + data.getId() + "' → '" + newId + "'!"));
                        new ItemEditorGui(plugin, player, newData).open();
                    });
        });

        // === Back button (slot 46) ===
        setItem(46, createBackButton(), event -> goBack());


        // === SAVE button (slot 49) ===
        setItem(49, createItem(Material.EMERALD,
                "<green><bold>💾 SAVE</bold>",
                List.of(
                        "<gray>Save this item to its individual file (items/<id>.yml)",
                        "",
                        "<yellow>Click to save"
                )), event -> {
            plugin.getItemManager().registerItem(data);
            plugin.getResourcePackManager().syncCmdsToLiveItems();
            plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
            player.sendMessage(TextUtil.success(Messages.savedItem(data.getId())));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
            refresh();
        });

        // === DELETE button (slot 52) ===
        setItem(52, createItem(Material.BARRIER,
                "<red><bold>🗑 DELETE</bold>",
                List.of(
                        "<gray>Delete this item",
                        "",
                        "<red>⚠ Click to delete"
                )), event -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.5f);
            new ConfirmDeleteGui(plugin, player, data, 0).openAsChildOf(this);
        });
    }

    private void saveAndReopen() {
        plugin.getItemManager().registerItem(data);
        plugin.getResourcePackManager().syncCmdsToLiveItems();
        // Save as individual file (items/<id>.yml) - copy-paste friendly, avoids recreating big item.yml
        plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
        new ItemEditorGui(plugin, player, data).open();
    }

    private void saveAndRefresh() {
        plugin.getItemManager().registerItem(data);
        plugin.getResourcePackManager().syncCmdsToLiveItems();
        plugin.getStorageManager().saveItem(plugin.getItemManager(), data);
        refresh();
    }

    private String attributeSummary() {
        if (data.getAttributes().isEmpty()) return "<gray>Không có thuộc tính";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (UItemData.AttributeData attr : data.getAttributes()) {
            if (count >= 2) {
                sb.append("<gray> (+").append(data.getAttributes().size() - 2).append(" more)");
                break;
            }
            if (count > 0) sb.append("<gray>, ");
            sb.append("<white>").append(attr.getAttribute().replace("generic_", "")).append(" +").append(attr.getAmount());
            count++;
        }
        return sb.toString();
    }

    private String enchantmentSummary() {
        if (data.getEnchantments().isEmpty()) return "<gray>Không có phù phép";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (var entry : data.getEnchantments().entrySet()) {
            if (count >= 3) {
                sb.append("<gray> (+").append(data.getEnchantments().size() - 3).append(" more)");
                break;
            }
            if (count > 0) sb.append("<gray>, ");
            sb.append("<white>").append(entry.getKey()).append(" ").append(entry.getValue());
            count++;
        }
        return sb.toString();
    }

    private String flagsSummary() {
        if (data.getFlags().isEmpty()) return "<gray>Không có cờ";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String flag : data.getFlags()) {
            if (count >= 3) {
                sb.append("<gray> (+").append(data.getFlags().size() - 3).append(" more)");
                break;
            }
            if (count > 0) sb.append("<gray>, ");
            sb.append("<white>").append(flag);
            count++;
        }
        return sb.toString();
    }
}
