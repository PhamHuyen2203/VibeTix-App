package com.example.vibetix.Utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vibetix.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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
        public boolean isUnread;

        public NotifItem(String id, String title, String message, String time, boolean isUnread) {
            this.id      = id;
            this.title   = title;
            this.message = message;
            this.time    = time;
            this.isUnread = isUnread;
        }
    }

    // ── Show popup — load Firestore trước, fallback mock ─────────────────────
    public static void show(Context context, View anchorView) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Chưa đăng nhập → dùng mock
            showWithItems(context, anchorView, getMockNotifications());
            return;
        }

        // Load từ Firestore
        FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("user_id", user.getUid())
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<NotifItem> items = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot) {
                    String id      = doc.getId();
                    String title   = doc.getString("title");
                    String message = doc.getString("message");
                    Boolean unread = doc.getBoolean("is_read");
                    boolean isUnread = unread == null || !unread;

                    // Format thời gian từ Timestamp
                    String time = "Vừa xong";
                    com.google.firebase.Timestamp ts = doc.getTimestamp("created_at");
                    if (ts != null) {
                        long diffMs = System.currentTimeMillis() - ts.toDate().getTime();
                        long mins   = diffMs / 60000;
                        if      (mins < 1)    time = "Vừa xong";
                        else if (mins < 60)   time = mins + " phút trước";
                        else if (mins < 1440) time = (mins/60) + " giờ trước";
                        else                  time = (mins/1440) + " ngày trước";
                    }

                    if (title != null)
                        items.add(new NotifItem(id, title, message != null ? message : "", time, isUnread));
                }

                // Nếu Firestore không có data → dùng mock để UI không trống
                if (items.isEmpty()) items = getMockNotifications();
                showWithItems(context, anchorView, items);
            })
            .addOnFailureListener(e -> showWithItems(context, anchorView, getMockNotifications()));
    }

    // ── Mock fallback ──────────────────────────────────────────────────────────
    private static List<NotifItem> getMockNotifications() {
        List<NotifItem> list = new ArrayList<>();
        list.add(new NotifItem("1", "🎉 Sự kiện mới: VinhVerse Concert",
                "VinhVerse Concert vừa mở bán vé. Đặt ngay trước khi hết!",
                "5 phút trước", true));
        list.add(new NotifItem("2", "🎫 Vé đã được xác nhận",
                "Vé của bạn cho Private Show in Fantasy đã được xác nhận thành công.",
                "1 giờ trước", true));
        list.add(new NotifItem("3", "💰 Ưu đãi hết hạn sau 24h",
                "Giảm 30% vé nhóm từ 3 người. Đừng bỏ lỡ!",
                "2 giờ trước", true));
        list.add(new NotifItem("4", "📢 RISING FEST 2026 mở bán sớm",
                "Vé Early Bird cho RISING FEST 2026 đã có. Số lượng có hạn.",
                "1 ngày trước", false));
        list.add(new NotifItem("5", "✅ Ban tổ chức đã được xác minh",
                "VibeFest Production đã được xác minh. Bạn có thể tạo sự kiện ngay.",
                "2 ngày trước", false));
        return list;
    }

    // ── Build và hiển thị PopupWindow ─────────────────────────────────────────
    private static void showWithItems(Context context, View anchorView, List<NotifItem> items) {
        View popupView = LayoutInflater.from(context)
                .inflate(R.layout.layout_notification_popup, null);

        LinearLayout container = popupView.findViewById(R.id.containerNotifications);
        for (NotifItem item : items) buildNotifRow(context, container, item);

        PopupWindow popup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(16f);
        popup.setOutsideTouchable(true);

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        float density = context.getResources().getDisplayMetrics().density;
        int offsetX = (int)(-(320 * density) + anchorView.getWidth() + (int)(8 * density));
        int offsetY = anchorView.getHeight() + (int)(4 * density);
        popup.showAtLocation(anchorView, Gravity.NO_GRAVITY,
                location[0] + offsetX, location[1] + offsetY);

        // Đánh dấu đã đọc tất cả
        TextView btnMarkAll = popupView.findViewById(R.id.btnMarkAllRead);
        if (btnMarkAll != null) {
            btnMarkAll.setOnClickListener(v -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                for (int i = 0; i < container.getChildCount(); i++) {
                    View dot = container.getChildAt(i).findViewById(R.id.dotUnread);
                    if (dot != null) dot.setVisibility(View.INVISIBLE);
                }
                // Cập nhật Firestore
                if (user != null) {
                    for (NotifItem n : items) {
                        if (n.isUnread) {
                            FirebaseFirestore.getInstance()
                                .collection("notifications")
                                .document(n.id)
                                .update("is_read", true);
                        }
                    }
                }
                Toast.makeText(context, "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
            });
        }

        TextView btnViewAll = popupView.findViewById(R.id.btnViewAllNotif);
        if (btnViewAll != null) {
            btnViewAll.setOnClickListener(v -> {
                popup.dismiss();
                Toast.makeText(context, "Tính năng đang phát triển 🔔", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private static void buildNotifRow(Context context, LinearLayout parent, NotifItem item) {
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
            item.isUnread = false;
            if (dot != null) dot.setVisibility(View.INVISIBLE);
            row.setBackgroundColor(Color.TRANSPARENT);
            // Mark read trên Firestore
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && item.id != null && !item.id.startsWith("1")) {
                FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .document(item.id)
                    .update("is_read", true);
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
