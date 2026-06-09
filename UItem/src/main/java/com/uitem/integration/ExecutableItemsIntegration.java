package com.uitem.integration;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * ExecutableItems integration — reflection-based, không hard-depend.
 *
 * HOT-FIX v5 — Thêm dumpAllIds() để log toàn bộ ID mà EI đang đăng ký,
 * giúp xác định đúng format ID (internal/config/display) mà EI 7.x sử dụng.
 *
 * Các phương pháp thử theo thứ tự:
 *   1. getExecutableItems() → Map / Collection
 *   2. getAll() / getItems() / values()
 *   3. Nếu manager là Map-like → keySet()
 *   4. Dùng getIds() / getKeys() nếu có
 */
public class ExecutableItemsIntegration {

    // ── API class candidates ──────────────────────────────────────────────
    private static final String[] API_CLASS_PATHS = {
        "com.ssomar.score.api.executableitems.ExecutableItemsAPI",
        "com.ssomar.executableitems.api.ExecutableItemsAPI",
        "com.ssomar.score.api.ExecutableItemsAPI",
        "com.ssomar.executableitems.ExecutableItemsAPI",
        "com.ssomar.score.api.executableitems.config.ExecutableItemsAPI",
        "com.ssomar.executableitems.config.ExecutableItemsAPI"
    };

    // ── Static getter candidates trên API class ───────────────────────────
    private static final String[] MANAGER_GETTER_NAMES = {
        "getExecutableItemsManager",
        "getManager",
        "getInstance",
        "get"
    };

    // ── Whitelist tên method cần resolve — theo thứ tự ưu tiên ──────────
    private static final String[] VALID_ID_NAMES = {
        "isValidId", "isValidID", "isValid", "existsById", "contains", "hasExecutableItem"
    };
    private static final String[] GET_BY_ID_NAMES = {
        "getExecutableItemById", "getExecutableItem", "getByID", "getById",
        "getItem", "getExecutableItemByName"
    };
    private static final String[] GET_BY_STACK_NAMES = {
        "getExecutableItemByStack", "getExecutableItem", "getByItemStack",
        "getByStack", "getItemByStack"
    };

    // ── Tên method để lấy toàn bộ item (dùng cho dump) ──────────────────
    private static final String[] LIST_ALL_NAMES = {
        "getExecutableItems",
        "getAll",
        "getItems",
        "values",
        "getIds",
        "getKeys",
        "getLoadedItems"
    };

    // ── Fields ─────────────────────────────────────────────────────────────
    private final Object manager;
    private final Method isValidIdMethod;
    private final Method getItemByIdMethod;
    private final Method getItemByStackMethod;
    private Logger logger;

    public ExecutableItemsIntegration() throws Exception {
        this(null);
    }

    public ExecutableItemsIntegration(Logger logger) throws Exception {
        this.logger = logger;

        // 1. Tìm API class
        Class<?> apiClass = loadApiClass();
        logInfo("API class: " + apiClass.getName());

        // 2. Lấy manager object qua static getter
        this.manager = loadManager(apiClass);
        logInfo("Manager class: " + manager.getClass().getName());

        // 3. Resolve đúng 3 method — không quét, chỉ thử whitelist
        this.isValidIdMethod      = findByWhitelist(manager.getClass(), VALID_ID_NAMES,    String.class,    "isValidId");
        this.getItemByIdMethod    = findByWhitelist(manager.getClass(), GET_BY_ID_NAMES,   String.class,    "getItemById");
        this.getItemByStackMethod = findByWhitelist(manager.getClass(), GET_BY_STACK_NAMES, ItemStack.class, "getItemByStack");

        // 4. Log chỉ tên method đã resolve — không dump gì thêm
        logInfo("Resolved: validate=" + isValidIdMethod.getName()
                + " getById=" + getItemByIdMethod.getName()
                + " getByStack=" + getItemByStackMethod.getName());

        // 5. HOT-FIX v5: Dump toàn bộ ID ngay sau khi hook thành công
        dumpAllIds();
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    // ─────────────────────────────────────────────────────────────────────
    // HOT-FIX v5: Dump toàn bộ ID EI đang đăng ký
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Thử tất cả các phương pháp có thể để liệt kê ID EI đang đăng ký.
     * Log kết quả ra console để debug.
     *
     * Strategy:
     *  A) Gọi no-arg method trên manager → Collection, Map, array → extract IDs
     *  B) Nếu manager implement Map → gọi keySet()
     *  C) Thử getIds()/getKeys() riêng (return Collection<String>)
     */
    public void dumpAllIds() {
        logInfo("=== EI ID DUMP START ===");
        logInfo("Manager class: " + manager.getClass().getName());

        boolean dumped = false;

        // Strategy A: thử từng tên method list
        for (String methodName : LIST_ALL_NAMES) {
            if (tryDumpViaMethod(methodName)) {
                dumped = true;
                break;
            }
        }

        // Strategy B: nếu manager implement Map
        if (!dumped && manager instanceof java.util.Map) {
            try {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) manager;
                logInfo("Manager is a Map, keys: " + map.keySet());
                dumped = true;
            } catch (Throwable t) {
                logWarn("Map keySet() failed: " + t.getMessage());
            }
        }

        // Strategy C: walk getDeclaredMethods() tìm method no-arg trả về Collection/Map/array
        if (!dumped) {
            logInfo("Trying to find any no-arg method returning Collection/Map/array...");
            dumped = tryWalkForCollectionMethod();
        }

        if (!dumped) {
            logWarn("Could not enumerate EI IDs via any known method.");
            logWarn("Please check EI manager class: " + manager.getClass().getName());
            logWarn("Available no-arg methods:");
            dumpNoArgMethods();
        }

        logInfo("=== EI ID DUMP END ===");
    }

    /**
     * Thử gọi một method cụ thể trên manager, parse kết quả để lấy IDs.
     */
    private boolean tryDumpViaMethod(String methodName) {
        // Thử không có arg
        Method m = findNoArgMethod(manager.getClass(), methodName);
        if (m == null) return false;

        try {
            Object result = m.invoke(manager);
            if (result == null) {
                logInfo(methodName + "() returned null");
                return false;
            }
            return extractAndLogIds(methodName, result);
        } catch (Throwable t) {
            logWarn(methodName + "() threw: " + t.getMessage());
            return false;
        }
    }

    /**
     * Extract IDs từ result object, hỗ trợ nhiều kiểu trả về.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean extractAndLogIds(String source, Object result) {
        // Collection<String> hoặc Collection<Object>
        if (result instanceof java.util.Collection) {
            java.util.Collection<?> col = (java.util.Collection<?>) result;
            if (col.isEmpty()) {
                logInfo(source + "() → empty collection");
                return true;
            }
            Object first = col.iterator().next();
            // Collection<String> — danh sách ID trực tiếp
            if (first instanceof String) {
                logInfo(source + "() → " + col.size() + " IDs:");
                for (Object id : col) logInfo("  EI-ID: [" + id + "]");
                return true;
            }
            // Collection<SomeEIObject> — thử lấy ID từ mỗi item
            logInfo(source + "() → Collection<" + first.getClass().getSimpleName() + ">, size=" + col.size());
            int count = 0;
            for (Object item : col) {
                String id = extractIdFromItem(item);
                logInfo("  EI-ITEM[" + count + "]: id=" + id + " class=" + item.getClass().getSimpleName());
                count++;
                if (count > 50) { logInfo("  ... (truncated at 50)"); break; }
            }
            return true;
        }

        // Map<String, ?> — keys là IDs
        if (result instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) result;
            if (map.isEmpty()) {
                logInfo(source + "() → empty map");
                return true;
            }
            logInfo(source + "() → Map with " + map.size() + " entries, keys:");
            int count = 0;
            for (Object key : map.keySet()) {
                logInfo("  EI-ID: [" + key + "]");
                count++;
                if (count > 50) { logInfo("  ... (truncated at 50)"); break; }
            }
            return true;
        }

        // Array
        if (result.getClass().isArray()) {
            Object[] arr;
            try { arr = (Object[]) result; }
            catch (ClassCastException e) { logWarn(source + "() returned primitive array, can't cast"); return false; }
            logInfo(source + "() → array[" + arr.length + "]:");
            for (int i = 0; i < Math.min(arr.length, 50); i++) {
                Object item = arr[i];
                String id = (item instanceof String) ? (String) item : extractIdFromItem(item);
                logInfo("  EI-ITEM[" + i + "]: " + id);
            }
            return true;
        }

        // Optional
        if (result instanceof Optional) {
            Object inner = ((Optional<?>) result).orElse(null);
            if (inner != null) return extractAndLogIds(source + "(Optional)", inner);
            logInfo(source + "() → Optional.empty");
            return true;
        }

        logInfo(source + "() → unknown type: " + result.getClass().getName() + " = " + result);
        return false;
    }

    /**
     * Thử extract ID từ một EI item object bằng các getter thông thường.
     */
    private String extractIdFromItem(Object item) {
        for (String name : new String[]{ "getId", "getItemId", "getId", "getKey", "getName", "getConfigId", "getInternalId" }) {
            try {
                Method m = item.getClass().getMethod(name);
                Object r = m.invoke(item);
                if (r != null) return name + "()=" + r;
            } catch (Throwable ignored) {}
        }
        return "?(" + item.getClass().getSimpleName() + ")";
    }

    /**
     * Walk toàn bộ declared methods tìm no-arg method trả về Collection/Map/array.
     */
    private boolean tryWalkForCollectionMethod() {
        Class<?> cur = manager.getClass();
        while (cur != null && cur != Object.class) {
            try {
                for (Method m : cur.getDeclaredMethods()) {
                    try {
                        if (m.getParameterCount() != 0) continue;
                        if (!java.lang.reflect.Modifier.isPublic(m.getModifiers())) continue;
                        Class<?> ret = m.getReturnType();
                        if (java.util.Collection.class.isAssignableFrom(ret)
                                || java.util.Map.class.isAssignableFrom(ret)
                                || ret.isArray()) {
                            logInfo("Found candidate: " + m.getName() + "() -> " + ret.getSimpleName());
                            if (!m.canAccess(manager)) m.setAccessible(true);
                            Object result = m.invoke(manager);
                            if (result != null) {
                                if (extractAndLogIds(m.getName(), result)) return true;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            try { cur = cur.getSuperclass(); } catch (Throwable t) { break; }
        }
        return false;
    }

    /**
     * Fallback: log tên tất cả no-arg public method để xem EI manager có gì.
     */
    private void dumpNoArgMethods() {
        Class<?> cur = manager.getClass();
        int count = 0;
        while (cur != null && cur != Object.class) {
            try {
                for (Method m : cur.getDeclaredMethods()) {
                    try {
                        if (m.getParameterCount() == 0
                                && java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                            logInfo("  METHOD: " + m.getName() + "() -> " + m.getReturnType().getSimpleName());
                            count++;
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            try { cur = cur.getSuperclass(); } catch (Throwable t) { break; }
        }
        if (count == 0) logWarn("  (no public no-arg methods found on manager hierarchy)");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    public ItemStack getItem(String eiId) {
        if (eiId == null || eiId.isBlank()) return null;
        try {
            Object execItem = lookupById(eiId);
            if (execItem == null) {
                logWarn("Item not found: '" + eiId + "'"
                        + " (validate=" + isValidIdMethod.getName()
                        + " getById=" + getItemByIdMethod.getName() + ")");
                return null;
            }
            return buildItemStack(execItem, eiId);
        } catch (Throwable t) {
            logWarn("Exception getting item '" + eiId + "': " + t);
            return null;
        }
    }

    public boolean hasItem(String eiId) {
        if (eiId == null || eiId.isBlank()) return false;
        try {
            return lookupById(eiId) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public String getExecutableItemId(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        try {
            Object raw = getItemByStackMethod.invoke(manager, item);
            Object execItem = unwrap(raw);
            if (execItem == null) return null;
            return tryGetId(execItem);
        } catch (Throwable t) {
            logWarn("Failed to get EI ID from ItemStack: " + t.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Init helpers
    // ─────────────────────────────────────────────────────────────────────

    private Class<?> loadApiClass() throws ClassNotFoundException {
        for (String path : API_CLASS_PATHS) {
            try { return Class.forName(path); }
            catch (ClassNotFoundException ignored) {}
        }
        throw new ClassNotFoundException(
            "Cannot find EI API class. Tried: " + java.util.Arrays.toString(API_CLASS_PATHS));
    }

    private Object loadManager(Class<?> apiClass) throws Exception {
        for (String name : MANAGER_GETTER_NAMES) {
            try {
                Method m = apiClass.getMethod(name);
                Object mgr = m.invoke(null);
                if (mgr != null) return mgr;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) {
                logWarn("Manager getter '" + name + "()' threw: " + t.getMessage());
            }
        }
        throw new IllegalStateException(
            "Cannot get EI manager from " + apiClass.getName());
    }

    private Method findByWhitelist(Class<?> startClass, String[] names,
                                   Class<?> paramType, String role)
            throws NoSuchMethodException {

        for (String name : names) {
            try {
                Method m = startClass.getMethod(name, paramType);
                return m;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable ignored) {}
        }

        for (String name : names) {
            Method found = walkForMethod(startClass, name, paramType);
            if (found != null) return found;
        }

        throw new NoSuchMethodException(
            "[UItem-EI] Cannot resolve method [" + role + "] accepting ("
            + paramType.getSimpleName() + ") on " + startClass.getName()
            + ". Tried: " + java.util.Arrays.toString(names));
    }

    private Method walkForMethod(Class<?> clazz, String name, Class<?> paramType) {
        Class<?> cur = clazz;
        while (cur != null && cur != Object.class) {
            Method found = scanDeclared(cur, name, paramType);
            if (found != null) return found;
            for (Class<?> iface : cur.getInterfaces()) {
                found = scanDeclared(iface, name, paramType);
                if (found != null) return found;
            }
            try { cur = cur.getSuperclass(); }
            catch (Throwable t) { break; }
        }
        return null;
    }

    private Method scanDeclared(Class<?> c, String targetName, Class<?> paramType) {
        Method[] declared;
        try { declared = c.getDeclaredMethods(); }
        catch (Throwable t) { return null; }

        for (Method m : declared) {
            try {
                if (!m.getName().equals(targetName)) continue;
                if (m.getParameterCount() != 1) continue;
                Class<?> p = m.getParameterTypes()[0];
                if (p.isAssignableFrom(paramType) || paramType.isAssignableFrom(p)) {
                    if (!m.canAccess(manager)) m.setAccessible(true);
                    return m;
                }
            } catch (Throwable t) {}
        }
        return null;
    }

    private Method findNoArgMethod(Class<?> startClass, String name) {
        Class<?> cur = startClass;
        while (cur != null && cur != Object.class) {
            try {
                for (Method m : cur.getDeclaredMethods()) {
                    try {
                        if (m.getName().equals(name) && m.getParameterCount() == 0) {
                            if (!m.canAccess(manager)) m.setAccessible(true);
                            return m;
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            try { cur = cur.getSuperclass(); } catch (Throwable t) { break; }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Lookup & build
    // ─────────────────────────────────────────────────────────────────────

    private Object lookupById(String eiId) throws Exception {
        Object valid = isValidIdMethod.invoke(manager, eiId);
        logInfo("isValid('" + eiId + "') = " + valid);
        if (!Boolean.TRUE.equals(valid)) return null;

        Object raw = getItemByIdMethod.invoke(manager, eiId);
        return unwrap(raw);
    }

    private Object unwrap(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Optional<?> opt) return opt.orElse(null);
        return raw;
    }

    private ItemStack buildItemStack(Object execItem, String eiId) {
        java.util.List<Method> builds = new java.util.ArrayList<>();
        Class<?> cur = execItem.getClass();
        while (cur != null && cur != Object.class) {
            try {
                for (Method m : cur.getDeclaredMethods()) {
                    try {
                        if (java.lang.reflect.Modifier.isPublic(m.getModifiers())
                                && m.getName().equals("buildItem")) {
                            builds.add(m);
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            try { cur = cur.getSuperclass(); }
            catch (Throwable t) { break; }
        }
        builds.sort((a, b) -> b.getParameterCount() - a.getParameterCount());

        for (Method m : builds) {
            try {
                Object[] args = buildArgs(m.getParameterCount());
                Object result = m.invoke(execItem, args);
                ItemStack extracted = extractItemStack(result, eiId);
                if (extracted != null) {
                    logInfo("buildItem[" + m.getParameterCount() + " params] OK for '" + eiId + "'");
                    return extracted;
                }
            } catch (Throwable t) {}
        }

        logWarn("All buildItem attempts failed for '" + eiId + "'");
        return null;
    }

    private Object[] buildArgs(int count) {
        if (count == 0) return new Object[0];
        Object[] args = new Object[count];
        args[0] = 1;
        for (int i = 1; i < count; i++) args[i] = Optional.empty();
        return args;
    }

    private ItemStack extractItemStack(Object result, String eiId) {
        if (result == null) return null;
        if (result instanceof ItemStack is) return is.clone();
        if (result instanceof Optional<?> opt) {
            Object inner = opt.orElse(null);
            if (inner instanceof ItemStack is) return is.clone();
            if (inner != null) logWarn("buildItem Optional<" + inner.getClass().getSimpleName() + "> for '" + eiId + "'");
            return null;
        }
        logWarn("buildItem unexpected return type " + result.getClass().getName());
        return null;
    }

    private String tryGetId(Object execItem) {
        for (String name : new String[]{ "getId", "getItemId", "getName", "getKey" }) {
            try {
                Method m = execItem.getClass().getMethod(name);
                Object r = m.invoke(execItem);
                if (r != null) return r.toString();
            } catch (Throwable ignored) {}
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Logging
    // ─────────────────────────────────────────────────────────────────────

    private void logInfo(String msg) {
        if (logger != null) logger.info("[UItem-EI] " + msg);
    }

    private void logWarn(String msg) {
        if (logger != null) logger.warning("[UItem-EI] " + msg);
    }
}
