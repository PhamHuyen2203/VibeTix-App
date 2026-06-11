package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * EventRepository — CRUD operations cho Events collection.
 */
public class EventRepository extends BaseRepository {

    public interface OnEventLoadedListener {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }

    public interface OnTicketTypesLoadedListener {
        void onSuccess(java.util.List<com.example.vibetix.Models.TicketType> ticketTypes);
        void onFailure(Exception e);
    }

    /**
     * Lấy 1 event theo ID (với callback).
     */
    public void getEventById(String eventId, OnEventLoadedListener listener) {
        db.collection(FirebaseCollections.EVENTS)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null && e.getId() == null) e.setId(doc.getId());
                        listener.onSuccess(e);
                    } else {
                        listener.onSuccess(null);
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    /**
     * Lấy danh sách loại vé của một event.
     */
    public void getTicketTypesForEvent(String eventId, OnTicketTypesLoadedListener listener) {
        db.collection(FirebaseCollections.TICKET_TYPES)
                .whereEqualTo("event_id", eventId)
                .orderBy("sort_order", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.vibetix.Models.TicketType> list = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.vibetix.Models.TicketType tt = doc.toObject(com.example.vibetix.Models.TicketType.class);
                        if (tt != null) {
                            if (tt.getTicketTypeId() == null) tt.setTicketTypeId(doc.getId());
                            list.add(tt);
                        }
                    }
                    listener.onSuccess(list);
                })
                .addOnFailureListener(listener::onFailure);
    }

    /**
     * Tạo event mới.
     */
    public Task<Void> createEvent(Event event) {
        return db.collection(FirebaseCollections.EVENTS)
                .document(event.getId())
                .set(event);
    }

    /**
     * Cập nhật thông tin event.
     */
    public Task<Void> updateEvent(Event event) {
        return db.collection(FirebaseCollections.EVENTS)
                .document(event.getId())
                .set(event);
    }

    /**
     * Huỷ event: chỉ đổi status sang "cancelled", không xóa document.
     */
    public Task<Void> cancelEvent(String eventId) {
        return db.collection(FirebaseCollections.EVENTS)
                .document(eventId)
                .update("status", "cancelled");
    }

    /**
     * Xóa event vĩnh viễn.
     */
    public Task<Void> deleteEvent(String eventId) {
        return db.collection(FirebaseCollections.EVENTS)
                .document(eventId)
                .delete();
    }

    /**
     * Lấy 1 event theo ID (Task version).
     */
    public Task<DocumentSnapshot> getEventById(String eventId) {
        return db.collection(FirebaseCollections.EVENTS)
                .document(eventId)
                .get();
    }

    /**
     * Lấy tất cả events của một organizer.
     */
    public Task<QuerySnapshot> getEventsByOrganizerId(String organizerId) {
        return db.collection(FirebaseCollections.EVENTS)
                .whereEqualTo("organizer_id", organizerId)
                .orderBy("start_time", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lấy events sắp diễn ra.
     */
    public Task<QuerySnapshot> getUpcomingEventsByOrganizerId(String organizerId, int limit) {
        return db.collection(FirebaseCollections.EVENTS)
                .whereEqualTo("organizer_id", organizerId)
                .whereIn("status", java.util.Arrays.asList("approved", "ongoing"))
                .orderBy("start_time", Query.Direction.ASCENDING)
                .limit(limit)
                .get();
    }

    /**
     * Lấy events do user tạo.
     */
    public Task<QuerySnapshot> getEventsByCreatorUserId(String userId) {
        return db.collection(FirebaseCollections.EVENTS)
                .whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Cập nhật status.
     */
    public Task<Void> updateEventStatus(String eventId, String newStatus) {
        return db.collection(FirebaseCollections.EVENTS)
                .document(eventId)
                .update("status", newStatus);
    }

    /**
     * Cập nhật poster_url.
     */
    public Task<Void> updateEventPosterUrl(String eventId, String posterUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("poster_url", posterUrl);
        return db.collection(FirebaseCollections.EVENTS)
                .document(eventId)
                .update(updates);
    }

    /**
     * Kiểm tra xem organizer có ít nhất 1 event không.
     */
    public Task<QuerySnapshot> checkHasEvents(String organizerId) {
        return db.collection(FirebaseCollections.EVENTS)
                .whereEqualTo("organizer_id", organizerId)
                .limit(1)
                .get();
    }
}
