package com.example.vibetix.Accessibility;

/**
 * ScreenContext — biểu diễn màn hình hiện tại mà trợ lý đang thao tác.
 * AssistantManager dùng để quyết định lệnh nào hợp lệ trong ngữ cảnh nào.
 */
public enum ScreenContext {
    HOME,             // Trang chủ
    EVENT_LIST,       // Danh sách sự kiện / kết quả tìm kiếm
    EVENT_DETAIL,     // Chi tiết sự kiện
    TICKET_SELECTION, // Màn chọn vé
    MY_TICKETS,       // Vé của tôi
    OTHER
}
