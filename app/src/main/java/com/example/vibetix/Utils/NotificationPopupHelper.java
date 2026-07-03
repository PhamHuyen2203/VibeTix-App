package com.example.vibetix.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.vibetix.Activities.User.NotificationDetailActivity;
import com.example.vibetix.Activities.User.NotificationsActivity;

import com.example.vibetix.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper hiển thị popup thông báo khi click icon chuông.
 * Load từ Firestore collection "notifications" của user hiện tại.
 * Fallback sang mock data nếu chưa có data hoặc chưa đăng nhập.
 */
public class NotificationPopupHelper {

    public static class NotifItem {
        public final String id;
        public final String title;
        public final String message;
        public final String time;
        public final long timestampMs;
        public boolean isUnread;
        public String type;  // notification type để điều hướng
        public String refId; // ref_id (orderId / eventId) để điều hướng sâu hơn

        public NotifItem(String id, String title, String message, String time, boolean isUnread, long timestampMs) {
            this.id          = id;
            this.title       = title;
            this.message     = message;
            this.time        = time;
            this.isUnread    = isUnread;
            this.timestampMs = timestampMs;
        }
    }

    // ── Show popup — load Firestore, không dùng mock khi đã login ───────────
    public static void show(Context context, View anchorView) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showWithItems(context, anchorView, new ArrayList<>());
            return;
        }

        // Không dùng orderBy để tránh cần composite index — sort trong memory
        FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("user_id", user.getUid())
            .limit(20)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<NotifItem> items = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot) {
                    String id    = doc.getId();
                    String title = doc.getString("title");
                    // Firebase lưu nội dung ở field "body"
                    String body  = doc.getString("body");
                    if (body == null) body = doc.getString("message"); // fallback
                    Boolean isReadField = doc.getBoolean("is_read");
                    boolean isUnread = isReadField == null || !isReadField;

                    // Ưu tiên sent_at, fallback created_at
                    com.google.firebase.Timestamp ts = doc.getTimestamp("sent_at");
                    if (ts == null) ts = doc.getTimestamp("created_at");
                    String time = formatRelativeTime(ts);

                    if (title != null && !title.isEmpty()) {
                        NotifItem item = new NotifItem(id, title, body != null ? body : "", time, isUnread,
                                ts != null ? ts.toDate().getTime() : 0L);
                        item.type  = doc.getString("type");
                        item.refId = doc.getString("ref_id");
                        items.add(item);
                    }
                }

                // Sort: unread trước, trong cùng nhóm thì mới nhất trước
                items.sort((a, b) -> {
                    if (a.isUnread != b.isUnread) return a.isUnread ? -1 : 1;
                    return Long.compare(b.timestampMs, a.timestampMs);
                });

                // Giới hạn 10 hiển thị
                if (items.size() > 10) items = items.subList(0, 10);
                showWithItems(context, anchorView, items);
            })
            .addOnFailureListener(e -> showWithItems(context, anchorView, new ArrayList<>()));
    }

    private static String formatRelativeTime(com.google.firebase.Timestamp ts) {
        if (ts == null) return "Vừa xong";
        long diffMs = System.currentTimeMillis() - ts.toDate().getTime();
        long mins = diffMs / 60000;
        if (mins < 1)    return "Vừa xong";
        if (mins < 60)   return mins + " phút trước";
        if (mins < 1440) return (mins / 60) + " giờ trước";
        return (mins / 1440) + " ngày trước";
    }

    // ── Build và hiển thị PopupWindow ─────────────────────────────────────────
    private static void showWithItems(Context context, View anchorView, List<NotifItem> items) {
        View popupView = LayoutInflater.from(context)
                .inflate(R.layout.layout_notification_popup, null);

        // Tạo popup trước để có thể truyền vào buildNotifRow
        PopupWindow popup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(16f);
        popup.setOutsideTouchable(true);

        LinearLayout container = popupView.findViewById(R.id.containerNotifications);
        if (items.isEmpty()) {
            // Empty state
            TextView empty = new TextView(context);
            empty.setText("Bạn chưa có thông báo nào.");
            empty.setTextColor(0xFF888888);
            empty.setTextSize(14f);
            empty.setPadding(48, 48, 48, 48);
            empty.setGravity(android.view.Gravity.CENTER);
            container.addView(empty);
        } else {
            for (NotifItem item : items) buildNotifRow(context, container, item, popup);
        }

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        float density = context.getResources().getDisplayMetrics().density;
        int offsetX = (int)(-(320 * density) + anchorView.getWidth() + (int)(8 * density));
        int offsetY = anchorView.getHeight() + (int)(4 * density);
        popup.showAtLocation(anchorView, Gravity.NO_GRAVITY,
                location[0] + offsetX, location[1] + offsetY);

        TextView btnViewAll = popupView.findViewById(R.id.btnViewAllNotif);
        if (btnViewAll != null) {
            btnViewAll.setOnClickListener(v -> {
                popup.dismiss();
                context.startActivity(new Intent(context, NotificationsActivity.class));
            });
        }
    }

    private static void buildNotifRow(Context context, LinearLayout parent, NotifItem item, PopupWindow popup) {
        View row = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);

        TextView title   = row.findViewById(R.id.txtNotifTitle);
        TextView message = row.findViewById(R.id.txtNotifMessage);
        TextView time    = row.findViewById(R.id.txtNotifTime);
        View dot         = row.findViewById(R.id.dotUnread);

        if (title   != null) title.setText(item.title);
        if (message != null) message.setText(item.message);
        if (time    != null) time.setText(item.time);
        if (dot     != null) dot.setVisibility(item.isUnread ? View.VISIBLE : View.INVISIBLE);

        if (item.isUnread) row.setBackgroundColor(0xFFF0F4FF);

        row.setOnClickListener(v -> {
            // Mark read
            if (item.isUnread) {
                item.isUnread = false;
                if (dot != null) dot.setVisibility(View.INVISIBLE);
                row.setBackgroundColor(Color.TRANSPARENT);
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && item.id != null) {
                    FirebaseFirestore.getInstance()
                        .collection("notifications")
                        .document(item.id)
                        .update("is_read", true);
                }
            }
            // Đóng popup và mở trang chi tiết
            popup.dismiss();
            Intent intent = new Intent(context, NotificationDetailActivity.class);
            intent.putExtra("title", item.title);
            intent.putExtra("body", item.message);
            intent.putExtra("time", item.time);
            intent.putExtra("type", item.type);
            intent.putExtra("refId", item.refId);
            if (context instanceof Activity) {
                context.startActivity(intent);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

        parent.addView(row);

        View divider = new View(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.setMarginStart((int)(34 * context.getResources().getDisplayMetrics().density));
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(0xFFF0F0F0);
        parent.addView(divider);
    }
}
