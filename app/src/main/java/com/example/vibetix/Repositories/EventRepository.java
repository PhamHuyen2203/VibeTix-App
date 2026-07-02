package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * EventRepository — CRUD operations cho Events collection.
 */
public class EventRepository extends BaseRepository {

    /**
     * Tạo event mới. Document ID = eventId (UUID v4).
     */
    public Task<Void> createEvent(Event event) {
        return db.collection(FirebaseCollections.EVENTS)
                .document(event.getEventId())
                .set(event);
    }

    /**
     * Cập nhật thông tin event.
     */
    public Task<Void> updateEvent(Event event) {
        return db.collection(FirebaseCollections.EVENTS)
                .document(event.getEventId())
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
     * Xóa event vĩnh viễn (owner only, chỉ khi chưa có orders confirmed).
     */
    public Task<Void> deleteEvent(String eventId) {
        return db.collection(FirebaseCollections.EVENTS)
                .document(eventId)
                .delete();
    }

    /**
     * Lấy 1 event theo ID.
     */
    public Task<DocumentSnapshot> getEventById(String eventId) {
        return db.collection(FirebaseCollections.EVENTS)
                .document(eventId)
                .get();
    }

    /**
     * Lấy tất cả events của một organizer, sort theo start_time DESC.
     */
    public Task<QuerySnapshot> getEventsByOrganizerId(String organizerId) {
        return db.collection(FirebaseCollections.EVENTS)
                .whereEqualTo("organizer_id", organizerId)
                .orderBy("start_time", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lấy events sắp diễn ra (APPROVED | ONGOING) của organizer, giới hạn số lượng.
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
     * Lấy events do user tạo (dùng user_id làm creator).
     */
    public Task<QuerySnapshot> getEventsByCreatorUserId(String userId) {
        return db.collection(FirebaseCollections.EVENTS)
                .whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Cập nhật chỉ 1 field status.
     */
    public Task<Void> updateEventStatus(String eventId, String newStatus) {
        return db.collection(FirebaseCollections.EVENTS)
                .document(eventId)
                .update("status", newStatus);
    }

    /**
     * Cập nhật poster_url sau khi upload lên Storage.
     */
    public Task<Void> updateEventPosterUrl(String eventId, String posterUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("poster_url", posterUrl);
        return db.collection(FirebaseCollections.EVENTS)
                .document(eventId)
                .update(updates);
    }

    /**
     * Kiểm tra xem organizer có ít nhất 1 event không (để xác định "là organizer").
     */
    public Task<QuerySnapshot> checkHasEvents(String organizerId) {
        return db.collection(FirebaseCollections.EVENTS)
                .whereEqualTo("organizer_id", organizerId)
                .limit(1)
                .get();
    }

    // ─── Legacy Callback methods (for backward compatibility) ───────────────────
    public interface OnEventLoadedListener {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }

    public interface OnSessionsLoadedListener {
        void onSuccess(java.util.List<com.example.vibetix.Models.EventSession> sessions);
        void onFailure(Exception e);
    }

    public void getSessionsForEvent(String eventId, OnSessionsLoadedListener listener) {
        db.collection(FirebaseCollections.EVENTS).document(eventId)
                .collection(FirebaseCollections.SESSIONS)
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    java.util.List<com.example.vibetix.Models.EventSession> list = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        com.example.vibetix.Models.EventSession s = doc.toObject(com.example.vibetix.Models.EventSession.class);
                        s.setSessionId(doc.getId());
                        list.add(s);
                    }
                    listener.onSuccess(list);
                })
                .addOnFailureListener(listener::onFailure);
    }

    public interface OnTicketTypesLoadedListener {
        void onSuccess(java.util.List<com.example.vibetix.Models.TicketType> ticketTypes);
        void onFailure(Exception e);
    }

    public void getEventById(String eventId, OnEventLoadedListener listener) {
        getEventById(eventId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Use FirestoreHelper to map Firestore field names (banner_url, start_time, etc.)
                        Event event = com.example.vibetix.Firebase.FirestoreHelper.docToEvent(documentSnapshot);
                        if (event != null) {
                            if (event.getId() == null) event.setId(documentSnapshot.getId());
                            listener.onSuccess(event);
                            return;
                        }
                    }
                    listener.onFailure(new Exception("Event not found"));
                })
                .addOnFailureListener(listener::onFailure);
    }

    public void getTicketTypesForEvent(String eventId, OnTicketTypesLoadedListener listener) {
        db.collection("ticket_types")
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.vibetix.Models.TicketType> list = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.vibetix.Models.TicketType t = doc.toObject(com.example.vibetix.Models.TicketType.class);
                        t.setTicketTypeId(doc.getId());
                        list.add(t);
                    }
                    if (!list.isEmpty()) {
                        listener.onSuccess(list);
                    } else {
                        fetchSubcollectionTicketTypes(eventId, listener);
                    }
                })
                .addOnFailureListener(e -> fetchSubcollectionTicketTypes(eventId, listener));
    }

    private void fetchSubcollectionTicketTypes(String eventId, OnTicketTypesLoadedListener listener) {
        db.collection(FirebaseCollections.EVENTS).document(eventId)
                .collection(FirebaseCollections.TICKET_TYPES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.vibetix.Models.TicketType> list = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.vibetix.Models.TicketType t = doc.toObject(com.example.vibetix.Models.TicketType.class);
                        t.setTicketTypeId(doc.getId());
                        list.add(t);
                    }
                    listener.onSuccess(list);
                })
                .addOnFailureListener(listener::onFailure);
    }
}
