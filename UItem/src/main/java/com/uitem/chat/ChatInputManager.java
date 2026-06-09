package com.uitem.chat;

import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import com.uitem.util.TextUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Quản lý việc capture chat input từ player.
 * Khi cần player nhập text (ví dụ: tên item, material...),
 * đăng ký callback và chặn tin nhắn chat tiếp theo của player đó.
 */
public class ChatInputManager implements Listener {

    private final UItemPlugin plugin;
    private final Map<UUID, ChatInputRequest> pendingInputs;

    public ChatInputManager(UItemPlugin plugin) {
        this.plugin = plugin;
        this.pendingInputs = new ConcurrentHashMap<>();
    }

    /**
     * Yêu cầu player nhập text qua chat.
     *
     * @param player   Player cần nhập
     * @param prompt   Tin nhắn hướng dẫn
     * @param callback Callback nhận text đã nhập (chạy trên main thread)
     */
    public void requestInput(Player player, String prompt, Consumer<String> callback) {
        requestInput(player, prompt, callback, null);
    }

    /**
     * Yêu cầu player nhập text qua chat, với callback khi hủy.
     *
     * @param player   Player cần nhập
     * @param prompt   Tin nhắn hướng dẫn
     * @param callback Callback nhận text đã nhập (chạy trên main thread)
     * @param onCancel Chạy khi người dùng gõ 'cancel' (thường là reopen GUI)
     */
    public void requestInput(Player player, String prompt, Consumer<String> callback, Runnable onCancel) {
        pendingInputs.put(player.getUniqueId(), new ChatInputRequest(callback, onCancel));

        player.closeInventory();
        player.sendMessage(TextUtil.prefixed(prompt));
        player.sendMessage(TextUtil.info("Type 'cancel' to abort."));
    }

    /**
     * Kiểm tra player có đang pending input không.
     */
    public boolean hasPendingInput(UUID uuid) {
        return pendingInputs.containsKey(uuid);
    }

    /**
     * Hủy pending input.
     */
    public void cancelInput(UUID uuid) {
        pendingInputs.remove(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ChatInputRequest request = pendingInputs.remove(player.getUniqueId());

        if (request == null) return;

        // Chặn tin nhắn chat
        event.setCancelled(true);

        // Lấy text thuần
        String input = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Chạy lại trên global scheduler để an toàn khi mở lại GUI.
        SchedulerUtil.runSync(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(TextUtil.info("Cancelled."));
                if (request.onCancel() != null) {
                    request.onCancel().run();
                }
                return;
            }
            request.callback().accept(input);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
        // Clean up SearchManager query for this player (null-safe)
        var inst = com.uitem.UItemPlugin.getInstance();
        if (inst != null) {
            inst.getSearchManager().clearQuery(event.getPlayer());
        }
    }

    private record ChatInputRequest(Consumer<String> callback, Runnable onCancel) {
        ChatInputRequest(Consumer<String> callback) {
            this(callback, null);
        }
    }
}
