package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.EventStaff;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * EventStaffRepository — Quản lý phân quyền nhân viên cho events.
 */
public class EventStaffRepository extends BaseRepository {

    /**
     * Assign nhân viên vào event.
     */
    public Task<Void> assignStaff(EventStaff staff) {
        return db.collection(FirebaseCollections.EVENT_STAFF)
                .document(staff.getStaffId())
                .set(staff);
    }

    /**
     * Thu hồi quyền nhân viên.
     */
    public Task<Void> revokeStaff(String staffId) {
        return db.collection(FirebaseCollections.EVENT_STAFF)
                .document(staffId)
                .delete();
    }

    /**
     * Lấy danh sách staff của 1 event.
     */
    public Task<QuerySnapshot> getStaffByEventId(String eventId) {
        return db.collection(FirebaseCollections.EVENT_STAFF)
                .whereEqualTo("event_id", eventId)
                .get();
    }

    /**
     * Lấy tất cả events mà 1 user được assign làm staff.
     */
    public Task<QuerySnapshot> getAssignmentsByUserId(String userId) {
        return db.collection(FirebaseCollections.EVENT_STAFF)
                .whereEqualTo("user_id", userId)
                .get();
    }

    /**
     * Kiểm tra role của user trong 1 event cụ thể.
     * Dùng để xác định quyền trên màn hình Event Detail.
     */
    public Task<QuerySnapshot> getUserRoleForEvent(String userId, String eventId) {
        return db.collection(FirebaseCollections.EVENT_STAFF)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("event_id", eventId)
                .limit(1)
                .get();
    }

    /**
     * Lấy tất cả staff của 1 organizer (tất cả events).
     */
    public Task<QuerySnapshot> getStaffByOrganizerId(String organizerId) {
        return db.collection(FirebaseCollections.EVENT_STAFF)
                .whereEqualTo("organizer_id", organizerId)
                .get();
    }

    /**
     * Cập nhật role của staff.
     */
    public Task<Void> updateStaffRole(String staffId, String newRole) {
        return db.collection(FirebaseCollections.EVENT_STAFF)
                .document(staffId)
                .update("role", newRole);
    }
}
