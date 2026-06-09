package com.uitem.gui;

import java.util.Stack;

/**
 * Stack điều hướng cho GUI.
 * Cho phép back button quay lại GUI trước đó thay vì đóng hẳn.
 */
public class GuiStack {

    private static final Stack<Runnable> history = new Stack<>();

    /**
     * Đẩy GUI hiện tại vào stack trước khi mở GUI mới.
     */
    public static void push(Runnable parentOpener) {
        history.push(parentOpener);
    }

    /**
     * Quay lại GUI trước đó.
     * Trả về true nếu có GUI để quay lại.
     */
    public static void pop() {
        if (!history.isEmpty()) {
            history.pop().run();
        }
    }

    /**
     * Xóa toàn bộ lịch sử (khi player thoát hẳn)
     */
    public static void clear() {
        history.clear();
    }

    /**
     * Kiểm tra stack có đang rỗng không
     */
    public static boolean isEmpty() {
        return history.isEmpty();
    }

    /**
     * Lấy kích thước stack
     */
    public static int size() {
        return history.size();
    }
}
