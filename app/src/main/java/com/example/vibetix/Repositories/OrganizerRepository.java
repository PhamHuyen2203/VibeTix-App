package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Organizer;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * OrganizerRepository — CRUD cho Organizer profiles.
 * 1 User có thể có nhiều Organizer documents.
 */
public class OrganizerRepository extends BaseRepository {

    /**
     * Tạo hồ sơ organizer mới.
     */
    public Task<Void> createOrganizer(Organizer organizer) {
        return db.collection(FirebaseCollections.ORGANIZERS)
                .document(organizer.getOrganizerId())
                .set(organizer);
    }

    /**
     * Cập nhật thông tin organizer.
     */
    public Task<Void> updateOrganizer(Organizer organizer) {
        return db.collection(FirebaseCollections.ORGANIZERS)
                .document(organizer.getOrganizerId())
                .set(organizer);
    }

    /**
     * Lấy organizer theo ID.
     */
    public Task<DocumentSnapshot> getOrganizerById(String organizerId) {
        return db.collection(FirebaseCollections.ORGANIZERS)
                .document(organizerId)
                .get();
    }

    /**
     * Lấy tất cả organizer profiles của 1 user.
     */
    public Task<QuerySnapshot> getOrganizersByUserId(String userId) {
        return db.collection(FirebaseCollections.ORGANIZERS)
                .whereEqualTo("user_id", userId)
                .get();
    }

    /**
     * Xóa hồ sơ organizer (chỉ khi không còn events active).
     */
    public Task<Void> deleteOrganizer(String organizerId) {
        return db.collection(FirebaseCollections.ORGANIZERS)
                .document(organizerId)
                .delete();
    }

    /**
     * Cập nhật default_organizer_id trên user document.
     */
    public Task<Void> setDefaultOrganizer(String userId, String organizerId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("default_organizer_id", organizerId);
        return db.collection(FirebaseCollections.USERS)
                .document(userId)
                .update(updates);
    }

    /**
     * Cập nhật logo_url sau khi upload Storage.
     */
    public Task<Void> updateLogoUrl(String organizerId, String logoUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("logo_url", logoUrl);
        return db.collection(FirebaseCollections.ORGANIZERS)
                .document(organizerId)
                .update(updates);
    }
}
