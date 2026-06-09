/**
 * UItem Public API — dành cho developer muốn tích hợp với UItem.
 *
 * <h2>Quick Start</h2>
 *
 * <p>Thêm UItem vào {@code depend} hoặc {@code softdepend} trong {@code plugin.yml} của bạn:
 * <pre>
 * softdepend:
 *   - UItem
 * </pre>
 *
 * <p>Trong {@code onEnable()}:
 * <pre>{@code
 * if (!UItemAPI.isAvailable()) {
 *     getLogger().warning("UItem không tìm thấy, tắt tính năng liên quan.");
 *     return;
 * }
 * UItemAPI api = UItemAPI.get();
 * }</pre>
 *
 * <h2>Các class chính</h2>
 * <ul>
 *   <li>{@link com.uitem.api.UItemAPI}           — entry point duy nhất</li>
 *   <li>{@link com.uitem.UItemData}               — data của một custom item</li>
 * </ul>
 *
 * <h2>Events (trong package {@code com.uitem.api.events})</h2>
 * <ul>
 *   <li>{@link com.uitem.api.events.UItemCreateEvent}   — item mới được tạo (cancellable)</li>
 *   <li>{@link com.uitem.api.events.UItemDeleteEvent}   — item bị xóa (cancellable)</li>
 *   <li>{@link com.uitem.api.events.UItemGiveEvent}     — item được give cho player (cancellable, modifiable)</li>
 *   <li>{@link com.uitem.api.events.UItemInteractEvent} — player click với UItem (cancellable)</li>
 *   <li>{@link com.uitem.api.events.UItemPickupEvent}   — player nhặt UItem (cancellable)</li>
 *   <li>{@link com.uitem.api.events.UItemEquipEvent}    — player mặc/tháo UItem (cancellable)</li>
 * </ul>
 */
package com.uitem.api;
