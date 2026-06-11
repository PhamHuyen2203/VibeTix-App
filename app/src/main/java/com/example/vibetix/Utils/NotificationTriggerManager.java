package com.example.vibetix.Utils;

import android.util.Log;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Notification;
import com.example.vibetix.Models.UserStarFollow;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NotificationTriggerManager {

    private static final String TAG = "NotifTriggerMgr";

    /**
     * Trigger 1: ORDER confirmed
     * Creates a pending notification for the user who placed the order.
     */
    public static void triggerOrderConfirmed(String userId, String orderId) {
        if (userId == null || userId.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String notifId = UUID.randomUUID().toString();

        Notification notif = new Notification();
        notif.setNotificationId(notifId);
        notif.setUserId(userId);
        notif.setType("order_confirmed");
        notif.setTitle("Đơn hàng đã được xác nhận!");
        notif.setBody("Đơn hàng #" + orderId.substring(0, Math.min(8, orderId.length())) + " của bạn đã được duyệt thành công. Bạn có thể kiểm tra vé ngay lúc này.");
        notif.setRefType("order");
        notif.setRefId(orderId);
        notif.setChannel("push_email");
        notif.setStatus("pending");
        notif.setRead(false);
        notif.setCreatedAt(Timestamp.now());

        db.collection(FirebaseCollections.NOTIFICATIONS).document(notifId)
                .set(notif)
                .addOnSuccessListener(unused -> Log.d(TAG, "Order confirmed notification created: " + notifId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create order notif", e));
    }

    /**
     * Trigger 3: EVENT approved
     * Find all event_stars -> their followers + event_interests -> create notifications.
     */
    public static void triggerEventApproved(String eventId, String eventTitle) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Get all stars for this event
        db.collection(FirebaseCollections.EVENT_STARS)
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(starSnap -> {
                    List<String> starIds = new ArrayList<>();
                    if (starSnap != null) {
                        for (DocumentSnapshot doc : starSnap) {
                            String sid = doc.getString("star_id");
                            if (sid != null) starIds.add(sid);
                        }
                    }

                    // Collect users to notify
                    Set<String> userIdsToNotify = new HashSet<>();

                    // 2. We could fetch event_interest users here if the collection existed.
                    // For now, we fetch star followers.
                    if (starIds.isEmpty()) {
                        createBatchEventApproved(userIdsToNotify, eventId, eventTitle);
                        return;
                    }

                    // Firestore 'in' query supports up to 10 items. We assume an event has <= 10 stars for simplicity,
                    // or we iterate over chunks. Let's iterate in chunks of 10.
                    List<com.google.android.gms.tasks.Task<QuerySnapshot>> tasks = new ArrayList<>();
                    for (int i = 0; i < starIds.size(); i += 10) {
                        List<String> chunk = starIds.subList(i, Math.min(i + 10, starIds.size()));
                        tasks.add(db.collection(FirebaseCollections.USER_STAR_FOLLOWS)
                                .whereIn("star_id", chunk)
                                .whereEqualTo("notify_new_event", true)
                                .get());
                    }

                    com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                        for (Object result : results) {
                            QuerySnapshot qs = (QuerySnapshot) result;
                            for (DocumentSnapshot doc : qs) {
                                UserStarFollow usf = doc.toObject(UserStarFollow.class);
                                if (usf != null && usf.getUserId() != null) {
                                    userIdsToNotify.add(usf.getUserId());
                                }
                            }
                        }
                        createBatchEventApproved(userIdsToNotify, eventId, eventTitle);
                    });
                });
    }

    private static void createBatchEventApproved(Set<String> userIds, String eventId, String eventTitle) {
        if (userIds.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        int count = 0;

        for (String uid : userIds) {
            String notifId = UUID.randomUUID().toString();
            Notification notif = new Notification();
            notif.setNotificationId(notifId);
            notif.setUserId(uid);
            notif.setType("star_new_event");
            notif.setTitle("Sự kiện mới mở bán!");
            notif.setBody("Sự kiện " + eventTitle + " có nghệ sĩ bạn yêu thích vừa được mở bán. Mua vé ngay!");
            notif.setRefType("event");
            notif.setRefId(eventId);
            notif.setChannel("push_email");
            notif.setStatus("pending");
            notif.setRead(false);
            notif.setCreatedAt(Timestamp.now());

            batch.set(db.collection(FirebaseCollections.NOTIFICATIONS).document(notifId), notif);
            count++;

            // Firestore batch limit is 500
            if (count >= 490) {
                batch.commit();
                batch = db.batch();
                count = 0;
            }
        }
        if (count > 0) {
            batch.commit().addOnSuccessListener(unused -> Log.d(TAG, "Event approved batch created"));
        }
    }

    /**
     * Trigger 5: EVENT cancelled
     * Notify all users who have 'completed' or 'confirmed' orders for this event.
     */
    public static void triggerEventCancelled(String eventId, String eventTitle) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(FirebaseCollections.ORDERS)
                .whereEqualTo("event_id", eventId)
                .whereIn("status", java.util.Arrays.asList("completed", "confirmed", "paid"))
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) return;

                    Set<String> buyerIds = new HashSet<>();
                    for (DocumentSnapshot doc : snap) {
                        String uid = doc.getString("user_id");
                        if (uid != null) buyerIds.add(uid);
                    }

                    if (buyerIds.isEmpty()) return;

                    WriteBatch batch = db.batch();
                    int count = 0;
                    for (String uid : buyerIds) {
                        String notifId = UUID.randomUUID().toString();
                        Notification notif = new Notification();
                        notif.setNotificationId(notifId);
                        notif.setUserId(uid);
                        notif.setType("event_cancelled");
                        notif.setTitle("Sự kiện bị huỷ: " + eventTitle);
                        notif.setBody("Sự kiện này đã bị ban tổ chức huỷ. Tiền vé sẽ được hoàn trả theo chính sách. Rất xin lỗi vì sự bất tiện này!");
                        notif.setRefType("event");
                        notif.setRefId(eventId);
                        notif.setChannel("push_email"); // Important notification
                        notif.setStatus("pending");
                        notif.setRead(false);
                        notif.setCreatedAt(Timestamp.now());

                        batch.set(db.collection(FirebaseCollections.NOTIFICATIONS).document(notifId), notif);
                        count++;

                        if (count >= 490) {
                            batch.commit();
                            batch = db.batch();
                            count = 0;
                        }
                    }
                    if (count > 0) {
                        batch.commit().addOnSuccessListener(unused -> Log.d(TAG, "Event cancelled batch created"));
                    }
                });
    }
}
