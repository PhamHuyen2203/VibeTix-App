package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * OrderRepository — Query orders & tính doanh thu.
 * Không có create/update vì orders được tạo từ phía User khi mua vé.
 */
public class OrderRepository extends BaseRepository {

    /**
     * Lấy tất cả orders của 1 event.
     */
    public Task<QuerySnapshot> getOrdersByEventId(String eventId) {
        return db.collection(FirebaseCollections.ORDERS)
                .whereEqualTo("event_id", eventId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Lấy orders đã thanh toán (status = paid) của 1 event.
     */
    public Task<QuerySnapshot> getPaidOrdersByEventId(String eventId) {
        return db.collection(FirebaseCollections.ORDERS)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("status", "paid")
                .get();
    }

    /**
     * Lấy tất cả orders của 1 organizer (cross-event revenue).
     */
    public Task<QuerySnapshot> getPaidOrdersByOrganizerId(String organizerId) {
        return db.collection(FirebaseCollections.ORDERS)
                .whereEqualTo("organizer_id", organizerId)
                .whereEqualTo("status", "paid")
                .get();
    }

    /**
     * Lấy order items của 1 order.
     */
    public Task<QuerySnapshot> getOrderItemsByOrderId(String orderId) {
        return db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereEqualTo("order_id", orderId)
                .get();
    }

    /**
     * Lấy order items theo event (để tính breakdown theo ticket type).
     */
    public Task<QuerySnapshot> getOrderItemsByEventId(String eventId) {
        return db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereEqualTo("event_id", eventId)
                .get();
    }

    /**
     * Lấy 1 order theo ID.
     */
    public Task<DocumentSnapshot> getOrderById(String orderId) {
        return db.collection(FirebaseCollections.ORDERS)
                .document(orderId)
                .get();
    }
}
