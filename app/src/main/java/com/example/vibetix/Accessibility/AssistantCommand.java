package com.example.vibetix.Accessibility;

import androidx.annotation.NonNull;

/**
 * AssistantCommand — lệnh đã được parse từ giọng nói của người dùng.
 * type xác định hành động, argument chứa phần tham số thô (từ khóa tìm kiếm,
 * số thứ tự sự kiện...) — có thể rỗng với các lệnh không cần tham số.
 */
public class AssistantCommand {

    public enum Type {
        SEARCH_EVENT,    // "tìm sự kiện nhạc rock"
        OPEN_EVENT,      // "mở số 1" / "mở sự kiện ..."
        READ_DETAIL,     // "đọc chi tiết"
        DESCRIBE_POSTER, // "mô tả poster"
        BOOK_TICKET,     // "đặt vé" / "mua vé"
        GO_BACK,         // "quay lại"
        GO_HOME,         // "về trang chủ"
        OPEN_MY_TICKETS, // "mở vé của tôi" / "xem vé"
        OPEN_EVENTS_TAB, // "mở trang sự kiện" / "danh sách sự kiện"
        OPEN_PROFILE,    // "mở trang cá nhân" / "hồ sơ"
        DESCRIBE_SCREEN, // "màn hình này có gì" / "đang ở đâu"
        HELP,            // "trợ giúp"
        STOP,            // "dừng lại" / "im lặng"
        REPEAT,          // "nhắc lại"
        UNKNOWN
    }

    private final Type type;
    private final String argument;
    private final String rawText;

    public AssistantCommand(Type type, String argument, String rawText) {
        this.type = type;
        this.argument = argument != null ? argument.trim() : "";
        this.rawText = rawText != null ? rawText : "";
    }

    public static AssistantCommand unknown(String rawText) {
        return new AssistantCommand(Type.UNKNOWN, "", rawText);
    }

    public Type getType() { return type; }

    /** Tham số kèm theo lệnh (từ khóa, số thứ tự...), đã trim, có thể rỗng. */
    public String getArgument() { return argument; }

    /** Câu gốc người dùng nói — dùng để log / hiển thị. */
    public String getRawText() { return rawText; }

    public boolean hasArgument() { return !argument.isEmpty(); }

    @NonNull
    @Override
    public String toString() {
        return "AssistantCommand{" + type + ", arg='" + argument + "'}";
    }
}
