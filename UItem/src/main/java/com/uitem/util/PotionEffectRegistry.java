package com.uitem.util;

import org.bukkit.Registry;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.Locale;

/**
 * Danh sách tất cả Minecraft potion effects với display name và icon.
 * Dùng cho UI editor và quick presets.
 */
public class PotionEffectRegistry {

    public static class PotionInfo {
        public final String key;           // e.g., "STRENGTH"
        public final String displayName;   // e.g., "Sức Mạnh"
        public final String description;   // e.g., "Tăng sát thương"
        
        public PotionInfo(String key, String displayName, String description) {
            this.key = key;
            this.displayName = displayName;
            this.description = description;
        }
    }

    // Danh sách tất cả potion effects trong Minecraft 1.21.x
    private static final Map<String, PotionInfo> POTION_MAP = new LinkedHashMap<>();

    static {
        // Hỗ trợ toàn bộ potion types trong 1.21.x
        register("SPEED", "Tốc Độ", "Tăng tốc độ di chuyển");
        register("SLOWNESS", "Chậm Lại", "Giảm tốc độ di chuyển");
        register("HASTE", "Vội Vàng", "Tăng tốc độ khai thác");
        register("MINING_FATIGUE", "Mệt Mỏi", "Giảm tốc độ khai thác");
        register("STRENGTH", "Sức Mạnh", "Tăng sát thương");
        register("INSTANT_HEALTH", "Hồi Máu Tức Thì", "Hồi máu ngay lập tức");
        register("INSTANT_DAMAGE", "Sát Thương Tức Thì", "Gây sát thương ngay lập tức");
        register("JUMP_BOOST", "Tăng Nhảy", "Nhảy cao hơn");
        register("NAUSEA", "Buồn Nôn", "Làm méo màn hình");
        register("REGENERATION", "Tái Sinh", "Hồi máu từ từ");
        register("RESISTANCE", "Kháng Cự", "Giảm sát thương");
        register("FIRE_RESISTANCE", "Kháng Cự Lửa", "Miễn nhiễt lửa");
        register("WATER_BREATHING", "Hô Hấp Nước", "Thở dưới nước");
        register("INVISIBILITY", "Vô Hình", "Trở nên vô hình");
        register("BLINDNESS", "Mù Mắt", "Không thể nhìn thấy xa");
        register("NIGHT_VISION", "Thị Lực Đêm", "Nhìn rõ trong bóng tối");
        register("HUNGER", "Đói", "Tăng mức đói");
        register("WEAKNESS", "Yếu Ớt", "Giảm sát thương");
        register("POISON", "Độc", "Gây sát thương từ từ");
        register("WITHER", "Héo Úa", "Gây sát thương từ từ (không drop loot)");
        register("HEALTH_BOOST", "Tăng Máu Tối Đa", "Tăng máu tối đa");
        register("ABSORPTION", "Hấp Thụ", "Tạo shield tạm");
        register("SATURATION", "No Nê", "Hồi độ no ngay lập tức");
        register("GLOWING", "Phát Sáng", "Trở nên phát sáng");
        register("LEVITATION", "Nổi Lơ Lửng", "Nổi lên trong không khí");
        register("LUCK", "May Mắn", "Tăng may mắn khi đánh cá/loot");
        register("UNLUCK", "Xui Xẻo", "Giảm may mắn khi đánh cá/loot");
        register("SLOW_FALLING", "Rơi Chậm", "Rơi chậm từ từ");
        register("CONDUIT_POWER", "Quyền Lực Conduit", "Tăng tốc độ, thị lực dưới nước, hô hấp nước");
        register("DOLPHINS_GRACE", "Ân Huệ Cá Heo", "Tăng tốc độ bơi");
        register("BAD_OMEN", "Lời Tiên Tri Xấu", "Kích hoạt raid");
        register("HERO_OF_THE_VILLAGE", "Anh Hùng Làng", "Tăng giảm giá tại thương nhân");
        register("DARKNESS", "Bóng Tối", "Làm tối hơn và có mob spawn");
        register("WIND_CHARGED", "Năng Lượng Gió", "Sẽ tạo lightning khi bị đánh");
        register("WEAVING", "Dệt May", "Di chuyển nhanh hơn");
    }

    private static void register(String key, String displayName, String description) {
        POTION_MAP.put(key, new PotionInfo(key, displayName, description));
    }

    /**
     * Lấy danh sách tất cả potion effects
     */
    public static Map<String, PotionInfo> getAllEffects() {
        return new LinkedHashMap<>(POTION_MAP);
    }

    /**
     * Lấy thông tin potion effect từ key
     */
    public static PotionInfo getInfo(String key) {
        return POTION_MAP.get(key.toUpperCase(Locale.ROOT));
    }

    /**
     * Kiểm tra xem potion effect có hợp lệ không
     */
    public static boolean isValidEffect(String key) {
        try {
            Registry.EFFECT.get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy tất cả potion effect keys
     */
    public static List<String> getAllEffectKeys() {
        return new ArrayList<>(POTION_MAP.keySet());
    }

    /**
     * Lấy potion effect theo index (cho GUI pagination)
     */
    public static PotionInfo getByIndex(int index) {
        List<PotionInfo> list = new ArrayList<>(POTION_MAP.values());
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    /**
     * Tổng số potion effects
     */
    public static int getTotalEffects() {
        return POTION_MAP.size();
    }
}
