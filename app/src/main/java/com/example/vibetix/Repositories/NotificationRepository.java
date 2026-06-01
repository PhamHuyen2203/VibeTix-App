package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Notification;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationRepository — Gửi & quản lý thông báo.
 * Thông báo được lưu vào Firestore, app User đọc real-time.
 */
public class NotificationRepository extends BaseRepository {

    /**
     * Lưu 1 thông báo.
     */
    public Task<Void> sendNotification(Notification notification) {
        return db.collection(FirebaseCollections.NOTIFICATIONS)
                .document(notification.getNotificationId())
                .set(notification);
    }

    /**
     * Gửi thông báo đến nhiều user cùng lúc (batch write).
     * Firestore batch limit: 500 documents per batch.
     */
    public Task<Void> sendBulkNotifications(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return Tasks.forResult(null);
        }

        List<Task<Void>> tasks = new ArrayList<>();
        int batchSize = 400; // An toàn dưới limit 500

        for (int i = 0; i < notifications.size(); i += batchSize) {
            int end = Math.min(i + batchSize, notifications.size());
            List<Notification> chunk = notifications.subList(i, end);

            WriteBatch batch = db.batch();
            for (Notification n : chunk) {
                batch.set(
                        db.collection(FirebaseCollections.NOTIFICATIONS).document(n.getNotificationId()),
                        n
                );
            }
            tasks.add(batch.commit());
        }

        return Tasks.whenAll(tasks);
    }

    /**
     * Lấy thông báo của 1 user, sort mới nhất trước.
     */
    public Task<QuerySnapshot> getNotificationsByUserId(String userId) {
        return db.collection(FirebaseCollections.NOTIFICATIONS)
                .whereEqualTo("user_id", userId)
                .orderBy("sent_at", Query.Direction.DESCENDING)
                .limit(50)
                .get();
    }

    /**
     * Đánh dấu đã đọc.
     */
    public Task<Void> markAsRead(String notificationId) {
        return db.collection(FirebaseCollections.NOTIFICATIONS)
                .document(notificationId)
                .update("is_read", true);
    }

    /**
     * Đếm số thông báo chưa đọc của user.
     */
    public Task<QuerySnapshot> getUnreadCount(String userId) {
        return db.collection(FirebaseCollections.NOTIFICATIONS)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("is_read", false)
                .get();
    }

    /**
     * Lấy lịch sử thông báo đã gửi từ organizer.
     */
    public Task<QuerySnapshot> getSentNotificationsByEvent(String eventId) {
        return db.collection(FirebaseCollections.NOTIFICATIONS)
                .whereEqualTo("event_id", eventId)
                .orderBy("sent_at", Query.Direction.DESCENDING)
                .get();
    }
}
