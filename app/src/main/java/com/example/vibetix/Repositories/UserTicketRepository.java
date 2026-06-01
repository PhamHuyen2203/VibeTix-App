package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * UserTicketRepository — Query vé của người dùng cho check-in và attendee list.
 */
public class UserTicketRepository extends BaseRepository {

    /**
     * Lấy vé theo ticket_code (dùng khi quét QR).
     */
    public Task<QuerySnapshot> getTicketByCode(String ticketCode) {
        return db.collection(FirebaseCollections.USER_TICKETS)
                .whereEqualTo("ticket_code", ticketCode)
                .limit(1)
                .get();
    }

    /**
     * Lấy tất cả vé của 1 event (danh sách attendees).
     */
    public Task<QuerySnapshot> getTicketsByEventId(String eventId) {
        return db.collection(FirebaseCollections.USER_TICKETS)
                .whereEqualTo("event_id", eventId)
                .get();
    }

    /**
     * Lấy vé đã check-in của 1 event.
     */
    public Task<QuerySnapshot> getCheckedInTickets(String eventId) {
        return db.collection(FirebaseCollections.USER_TICKETS)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("status", "used")
                .get();
    }

    /**
     * Lấy vé theo user (My Tickets).
     */
    public Task<QuerySnapshot> getTicketsByUserId(String userId) {
        return db.collection(FirebaseCollections.USER_TICKETS)
                .whereEqualTo("owner_id", userId)
                .orderBy("issued_at", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lấy 1 vé theo ID.
     */
    public Task<DocumentSnapshot> getTicketById(String ticketId) {
        return db.collection(FirebaseCollections.USER_TICKETS)
                .document(ticketId)
                .get();
    }

    /**
     * Cập nhật status vé sang "used" (check-in).
     */
    public Task<Void> checkInTicket(String ticketId, String checkedInAt) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", "used");
        updates.put("checked_in_at", checkedInAt);
        return db.collection(FirebaseCollections.USER_TICKETS)
                .document(ticketId)
                .update(updates);
    }
}
