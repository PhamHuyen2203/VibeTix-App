package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.TicketType;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * TicketTypeRepository — CRUD cho ticket_types collection.
 */
public class TicketTypeRepository extends BaseRepository {

    public Task<Void> createTicketType(TicketType ticketType) {
        return db.collection(FirebaseCollections.TICKET_TYPES)
                .document(ticketType.getTicketTypeId())
                .set(ticketType);
    }

    public Task<Void> updateTicketType(TicketType ticketType) {
        return db.collection(FirebaseCollections.TICKET_TYPES)
                .document(ticketType.getTicketTypeId())
                .set(ticketType);
    }

    public Task<Void> deleteTicketType(String ticketTypeId) {
        return db.collection(FirebaseCollections.TICKET_TYPES)
                .document(ticketTypeId)
                .delete();
    }

    public Task<QuerySnapshot> getTicketTypesByEventId(String eventId) {
        return db.collection(FirebaseCollections.TICKET_TYPES)
                .whereEqualTo("event_id", eventId)
                .orderBy("price", Query.Direction.ASCENDING)
                .get();
    }

    public Task<DocumentSnapshot> getTicketTypeById(String ticketTypeId) {
        return db.collection(FirebaseCollections.TICKET_TYPES)
                .document(ticketTypeId)
                .get();
    }

    /**
     * Cập nhật quantity_sold sau khi bán vé.
     */
    public Task<Void> incrementSoldCount(String ticketTypeId, int increment) {
        return db.collection(FirebaseCollections.TICKET_TYPES)
                .document(ticketTypeId)
                .update("quantity_sold",
                        com.google.firebase.firestore.FieldValue.increment(increment));
    }

    /**
     * Toggle trạng thái active của loại vé.
     */
    public Task<Void> setActive(String ticketTypeId, boolean isActive) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("is_active", isActive);
        return db.collection(FirebaseCollections.TICKET_TYPES)
                .document(ticketTypeId)
                .update(updates);
    }

    /**
     * Kiểm tra xem event đã có orders nào confirmed chưa (để lock edit TicketType).
     * Nếu có → không cho sửa/xóa ticket type.
     */
    public Task<QuerySnapshot> checkConfirmedOrdersForEvent(String eventId) {
        return db.collection(FirebaseCollections.ORDERS)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("status", "paid")
                .limit(1)
                .get();
    }
}
