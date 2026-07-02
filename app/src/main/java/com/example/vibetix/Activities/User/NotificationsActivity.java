package com.example.vibetix.Activities.User;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.R;
import com.example.vibetix.Utils.NotificationPopupHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private TextView txtEmpty;
    private final List<NotificationPopupHelper.NotifItem> items = new ArrayList<>();
    private NotifListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        rvNotifications = findViewById(R.id.rvNotifications);
        txtEmpty        = findViewById(R.id.txtEmpty);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        adapter = new NotifListAdapter(items, this::onItemClick);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { showEmpty(); return; }

        FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("user_id", user.getUid())
            .limit(50)
            .get()
            .addOnSuccessListener(snap -> {
                items.clear();
                for (QueryDocumentSnapshot doc : snap) {
                    String id    = doc.getId();
                    String title = doc.getString("title");
                    String body  = doc.getString("body");
                    if (body == null) body = doc.getString("message");
                    Boolean isReadField = doc.getBoolean("is_read");
                    boolean isUnread = isReadField == null || !isReadField;
                    com.google.firebase.Timestamp ts = doc.getTimestamp("sent_at");
                    if (ts == null) ts = doc.getTimestamp("created_at");
                    String time = formatRelativeTime(ts);
                    long ms = ts != null ? ts.toDate().getTime() : 0L;
                    if (title != null && !title.isEmpty())
                        items.add(new NotificationPopupHelper.NotifItem(id, title,
                                body != null ? body : "", time, isUnread, ms));
                }
                // Sort: unread first → mới nhất trước
                items.sort((a, b) -> {
                    if (a.isUnread != b.isUnread) return a.isUnread ? -1 : 1;
                    return Long.compare(b.timestampMs, a.timestampMs);
                });
                if (items.isEmpty()) showEmpty();
                else { adapter.notifyDataSetChanged(); }
            })
            .addOnFailureListener(e -> showEmpty());
    }

    private void onItemClick(NotificationPopupHelper.NotifItem item, int position) {
        if (item.isUnread) {
            item.isUnread = false;
            FirebaseFirestore.getInstance()
                .collection("notifications").document(item.id)
                .update("is_read", true);
            adapter.notifyItemChanged(position);
        }
        Intent intent = new Intent(this, NotificationDetailActivity.class);
        intent.putExtra("title", item.title);
        intent.putExtra("body", item.message);
        intent.putExtra("time", item.time);
        startActivity(intent);
    }

    private void showEmpty() {
        rvNotifications.setVisibility(View.GONE);
        txtEmpty.setVisibility(View.VISIBLE);
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

    // ── Adapter ──────────────────────────────────────────────────────────────
    interface OnNotifClick {
        void onClick(NotificationPopupHelper.NotifItem item, int position);
    }

    static class NotifListAdapter extends RecyclerView.Adapter<NotifListAdapter.VH> {
        private final List<NotificationPopupHelper.NotifItem> data;
        private final OnNotifClick listener;

        NotifListAdapter(List<NotificationPopupHelper.NotifItem> data, OnNotifClick listener) {
            this.data     = data;
            this.listener = listener;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification_full, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            NotificationPopupHelper.NotifItem item = data.get(position);
            h.title.setText(item.title);
            h.body.setText(item.message);
            h.time.setText(item.time);
            h.dot.setVisibility(item.isUnread ? View.VISIBLE : View.INVISIBLE);
            h.itemView.setBackgroundColor(item.isUnread ? 0xFFEEF3FF : 0xFFFFFFFF);
            h.itemView.setOnClickListener(v -> listener.onClick(item, h.getAdapterPosition()));
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, body, time;
            View dot;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.txtNotifTitle);
                body  = v.findViewById(R.id.txtNotifMessage);
                time  = v.findViewById(R.id.txtNotifTime);
                dot   = v.findViewById(R.id.dotUnread);
            }
        }
    }
}
