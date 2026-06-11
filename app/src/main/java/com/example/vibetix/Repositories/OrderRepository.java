package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * OrderRepository — Query orders & tính doanh thu.
 *
 * Lưu ý: Bảng `orders` KHÔNG có trường event_id.
 * Để lấy orders của 1 event, phải đi qua order_items:
 *   1. Query order_items WHERE event_id = ?  → lấy tập order_id
 *   2. Query orders WHERE documentId IN (order_ids)
 */
public class OrderRepository extends BaseRepository {

    /**
     * Lấy tất cả order_items của 1 event (bước 1 để tìm orders).
     */
    public Task<QuerySnapshot> getOrderItemsByEventId(String eventId) {
        return db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereEqualTo("event_id", eventId)
                .get();
    }

    /**
     * Lấy orders theo danh sách order_id (bước 2, chunk ≤ 10 mỗi lần).
     * Caller chịu trách nhiệm chunk và dùng Tasks.whenAllSuccess.
     */
    public Task<QuerySnapshot> getOrdersByIds(List<String> orderIds) {
        return db.collection(FirebaseCollections.ORDERS)
                .whereIn(FieldPath.documentId(), orderIds)
                .get();
    }

    /**
     * Lấy order items theo event (alias rõ ràng hơn).
     */
    public Task<QuerySnapshot> getOrderItemsByEventIdTask(String eventId) {
        return db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereEqualTo("event_id", eventId)
                .get();
    }

    /**
     * Lấy 1 order theo ID (document ID = order_id).
     */
    public Task<DocumentSnapshot> getOrderById(String orderId) {
        return db.collection(FirebaseCollections.ORDERS)
                .document(orderId)
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

    // ─── Legacy Callback methods (for backward compatibility) ───────────────────
    public interface OnOrderActionListener {
        void onSuccess(String orderId);
        void onFailure(Exception e);
    }

    public void saveOrder(com.example.vibetix.Models.Order order, OnOrderActionListener listener) {
        db.collection(FirebaseCollections.ORDERS)
                .document(order.getOrderId())
                .set(order)
                .addOnSuccessListener(aVoid -> listener.onSuccess(order.getOrderId()))
                .addOnFailureListener(listener::onFailure);
    }
}
