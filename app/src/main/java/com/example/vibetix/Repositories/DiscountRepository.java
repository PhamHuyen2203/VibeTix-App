package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Discount;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * DiscountRepository — CRUD cho discounts collection.
 */
public class DiscountRepository extends BaseRepository {

    public Task<Void> createDiscount(Discount discount) {
        return db.collection(FirebaseCollections.DISCOUNTS)
                .document(discount.getId())
                .set(discount);
    }

    public Task<Void> updateDiscount(Discount discount) {
        return db.collection(FirebaseCollections.DISCOUNTS)
                .document(discount.getId())
                .set(discount);
    }

    public Task<Void> deleteDiscount(String discountId) {
        return db.collection(FirebaseCollections.DISCOUNTS)
                .document(discountId)
                .delete();
    }

    public Task<DocumentSnapshot> getDiscountById(String discountId) {
        return db.collection(FirebaseCollections.DISCOUNTS)
                .document(discountId)
                .get();
    }

    /**
     * Lấy tất cả discounts của 1 event.
     */
    public Task<QuerySnapshot> getDiscountsByEventId(String eventId) {
        return db.collection(FirebaseCollections.DISCOUNTS)
                .whereEqualTo("eventId", eventId)
                .get();
    }

    /**
     * Lấy tất cả discounts của 1 organizer (tất cả events).
     */
    public Task<QuerySnapshot> getDiscountsByOrganizerId(String organizerId) {
        return db.collection(FirebaseCollections.DISCOUNTS)
                .whereEqualTo("organizer_id", organizerId)
                .get();
    }

    /**
     * Kiểm tra mã code có trùng trong cùng event không.
     */
    public Task<QuerySnapshot> checkCodeExists(String code, String eventId) {
        return db.collection(FirebaseCollections.DISCOUNTS)
                .whereEqualTo("code", code)
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get();
    }

    /**
     * Tăng usage_count sau khi dùng mã.
     */
    public Task<Void> incrementUsageCount(String discountId) {
        return db.collection(FirebaseCollections.DISCOUNTS)
                .document(discountId)
                .update("used_count",
                        com.google.firebase.firestore.FieldValue.increment(1));
    }

    /**
     * Toggle trạng thái active.
     */
    public Task<Void> setActive(String discountId, boolean isActive) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isActive", isActive);
        return db.collection(FirebaseCollections.DISCOUNTS)
                .document(discountId)
                .update(updates);
    }
}
