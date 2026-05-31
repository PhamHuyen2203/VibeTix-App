package com.example.vibetix.Repositories;

import com.example.vibetix.Models.Ticket;

import java.util.ArrayList;
import java.util.List;

/**
 * TicketRepository — lớp trung gian truy cập dữ liệu vé.
 * Hiện tại trả về mock data; thay bằng Firestore queries khi backend sẵn sàng.
 */
public class TicketRepository {

    // ── Listener interfaces ───────────────────────────────────────────────────

    public interface OnTicketsLoadedListener {
        void onSuccess(List<Ticket> tickets);
        void onFailure(Exception e);
    }

    public interface OnTicketActionListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    // ── Mock data ─────────────────────────────────────────────────────────────

    /** Trả về danh sách vé ACTIVE của user (mock). */
    public void getActiveTickets(String userEmail, OnTicketsLoadedListener listener) {
        // TODO: Thay bằng Firestore query:
        //   db.collection("user_tickets")
        //     .whereEqualTo("ownerEmail", userEmail)
        //     .whereIn("status", Arrays.asList("valid"))
        //     .get()
        listener.onSuccess(new ArrayList<>());
    }

    /** Trả về vé đã dùng / hết hạn của user (mock). */
    public void getEndedTickets(String userEmail, OnTicketsLoadedListener listener) {
        listener.onSuccess(new ArrayList<>());
    }

    /** Trả về vé đang bán lại của user (mock). */
    public void getResellingTicketsByUser(String userEmail, OnTicketsLoadedListener listener) {
        listener.onSuccess(new ArrayList<>());
    }

    /** Trả về TẤT CẢ vé đang được bán lại trên sàn (hiển thị ở Home resale section). */
    public void getAllResellingTickets(OnTicketsLoadedListener listener) {
        // Mock: trả về danh sách rỗng — HomeFragment sẽ ẩn section resale nếu trống
        listener.onSuccess(new ArrayList<>());
    }

    /** Hủy đăng bán vé. */
    public void cancelResale(String ticketId, OnTicketActionListener listener) {
        // TODO: Firestore update USER_TICKET.status = 'valid'
        listener.onSuccess();
    }

    /** Đăng bán lại vé. */
    public void listForResale(String ticketId, long price, OnTicketActionListener listener) {
        // TODO: Firestore update USER_TICKET.status = 'reselling', resalePrice = price
        listener.onSuccess();
    }
}
