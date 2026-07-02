package com.example.vibetix.Activities.Organizer;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vibetix.R;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Notification;
import com.example.vibetix.Models.Order;
import com.example.vibetix.Models.OrderItem;
import com.example.vibetix.Models.TicketType;
import com.example.vibetix.databinding.ActivityOrganizerBlastBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OrganizerBlastActivity extends AppCompatActivity {

    private ActivityOrganizerBlastBinding binding;
    private FirebaseFirestore db;
    private String eventId;

    private List<TicketType> ticketTypes = new ArrayList<>();
    private TicketType selectedTicketType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrganizerBlastBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        eventId = getIntent().getStringExtra("EXTRA_EVENT_ID");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Không có sự kiện được chọn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Setup blast history RecyclerView
        blastHistoryAdapter = new BlastHistoryAdapter(blastHistoryList, this::showBlastDetailsDialog);
        binding.rvBlastHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvBlastHistory.setAdapter(blastHistoryAdapter);

        // Fab button to add blast
        binding.fabAddBlast.setOnClickListener(v -> showAddBlastBottomSheet());

        loadBlastHistory();
    }

    private List<BlastHistoryItem> blastHistoryList = new ArrayList<>();
    private BlastHistoryAdapter blastHistoryAdapter;

    private void loadTicketTypes(AutoCompleteTextView ddTicketType, View pbLoading) {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        db.collection(FirebaseCollections.TICKET_TYPES)
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    ticketTypes.clear();
                    List<String> names = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        TicketType tt = doc.toObject(TicketType.class);
                        if (tt != null) {
                            tt.setTicketTypeId(doc.getId());
                            ticketTypes.add(tt);
                            names.add(tt.getName());
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
                    if (ddTicketType != null) {
                        ddTicketType.setAdapter(adapter);
                        ddTicketType.setOnItemClickListener((parent, view, position, id) -> {
                            selectedTicketType = ticketTypes.get(position);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải loại vé", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddBlastBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_create_blast, null);
        dialog.setContentView(view);

        com.google.android.material.textfield.TextInputEditText etTitle = view.findViewById(R.id.etTitle);
        com.google.android.material.textfield.TextInputEditText etBody = view.findViewById(R.id.etBody);
        RadioGroup rgTarget = view.findViewById(R.id.rgTarget);
        RadioButton rbTicketType = view.findViewById(R.id.rbTicketType);
        RadioButton rbConfirmedOnly = view.findViewById(R.id.rbConfirmedOnly);
        View layoutTicketType = view.findViewById(R.id.layoutTicketType);
        AutoCompleteTextView ddTicketType = view.findViewById(R.id.ddTicketType);
        Button btnSendBlast = view.findViewById(R.id.btnSendBlast);
        ProgressBar pbLoading = view.findViewById(R.id.pbLoading);
        View btnCloseSheet = view.findViewById(R.id.btnCloseSheet);

        selectedTicketType = null;

        if (btnCloseSheet != null) {
            btnCloseSheet.setOnClickListener(v -> dialog.dismiss());
        }

        rgTarget.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rbTicketType.getId()) {
                layoutTicketType.setVisibility(View.VISIBLE);
                if (ticketTypes.isEmpty()) {
                    loadTicketTypes(ddTicketType, pbLoading);
                } else {
                    List<String> names = new ArrayList<>();
                    for (TicketType tt : ticketTypes) {
                        names.add(tt.getName());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
                    ddTicketType.setAdapter(adapter);
                    ddTicketType.setOnItemClickListener((parent, v2, position, id) -> {
                        selectedTicketType = ticketTypes.get(position);
                    });
                }
            } else {
                layoutTicketType.setVisibility(View.GONE);
                selectedTicketType = null;
            }
        });

        btnSendBlast.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String body = etBody.getText() != null ? etBody.getText().toString().trim() : "";

            if (title.isEmpty() || body.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tiêu đề và nội dung", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isTicketTypeTarget = rbTicketType.isChecked();
            if (isTicketTypeTarget && selectedTicketType == null) {
                Toast.makeText(this, "Vui lòng chọn loại vé", Toast.LENGTH_SHORT).show();
                return;
            }

            fetchTargetUsersAndSend(title, body, rbConfirmedOnly.isChecked(), isTicketTypeTarget, dialog, pbLoading, btnSendBlast);
        });

        dialog.show();
    }

    private void fetchTargetUsersAndSend(String title, String body, boolean isConfirmedOnlyTarget, boolean isTicketTypeTarget, 
                                         com.google.android.material.bottomsheet.BottomSheetDialog dialog, ProgressBar pbLoading, Button btnSendBlast) {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        if (btnSendBlast != null) btnSendBlast.setEnabled(false);

        com.google.firebase.firestore.Query itemQuery = db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereEqualTo("event_id", eventId);
        
        if (isTicketTypeTarget && selectedTicketType != null) {
            itemQuery = itemQuery.whereEqualTo("ticket_type_id", selectedTicketType.getTicketTypeId());
        }

        itemQuery.get()
                .addOnSuccessListener(itemSnap -> {
                    if (itemSnap == null || itemSnap.isEmpty()) {
                        showNoTargetAlert(pbLoading, btnSendBlast);
                        return;
                    }

                    Set<String> orderIds = new HashSet<>();
                    for (DocumentSnapshot doc : itemSnap.getDocuments()) {
                        String orderId = doc.getString("order_id");
                        if (orderId != null && !orderId.isEmpty()) {
                            orderIds.add(orderId);
                        }
                    }

                    if (orderIds.isEmpty()) {
                        showNoTargetAlert(pbLoading, btnSendBlast);
                        return;
                    }

                    List<String> orderIdList = new ArrayList<>(orderIds);
                    List<Task<QuerySnapshot>> tasks = new ArrayList<>();
                    int chunkSize = 30;

                    for (int i = 0; i < orderIdList.size(); i += chunkSize) {
                        List<String> chunk = orderIdList.subList(i, Math.min(i + chunkSize, orderIdList.size()));
                        tasks.add(db.collection(FirebaseCollections.ORDERS)
                                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                                .get());
                    }

                    Tasks.whenAllSuccess(tasks)
                            .addOnSuccessListener(results -> {
                                Set<String> userIds = new HashSet<>();
                                for (Object obj : results) {
                                    QuerySnapshot snap = (QuerySnapshot) obj;
                                    if (snap != null) {
                                        for (DocumentSnapshot doc : snap.getDocuments()) {
                                            String status = doc.getString("status");
                                            String userId = doc.getString("user_id");

                                            if (userId != null && status != null) {
                                                boolean isValidStatus = status.equalsIgnoreCase("completed") ||
                                                        status.equalsIgnoreCase("confirmed") ||
                                                        status.equalsIgnoreCase("paid");
                                                boolean isNotCancelled = !status.equalsIgnoreCase("cancelled") &&
                                                        !status.equalsIgnoreCase("refunded");

                                                boolean shouldInclude = isConfirmedOnlyTarget ? isValidStatus : isNotCancelled;
                                                if (shouldInclude) {
                                                    userIds.add(userId);
                                                }
                                            }
                                        }
                                    }
                                }

                                if (userIds.isEmpty()) {
                                    showNoTargetAlert(pbLoading, btnSendBlast);
                                } else {
                                    showConfirmationDialog(userIds, title, body, dialog, pbLoading, btnSendBlast);
                                }
                            })
                            .addOnFailureListener(e -> showFailureAlert(e, pbLoading, btnSendBlast));
                })
                .addOnFailureListener(e -> showFailureAlert(e, pbLoading, btnSendBlast));
    }

    private void showConfirmationDialog(Set<String> userIds, String title, String body, 
                                         com.google.android.material.bottomsheet.BottomSheetDialog sheet, ProgressBar pbLoading, Button btnSendBlast) {
        if (pbLoading != null) pbLoading.setVisibility(View.GONE);
        if (btnSendBlast != null) btnSendBlast.setEnabled(true);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ Xác nhận gửi")
                .setMessage("Bạn chuẩn bị gửi thông báo này tới " + userIds.size() + " khách hàng.\n\nHành động này không thể hoàn tác. Bạn đã kiểm tra kỹ nội dung chưa?")
                .setPositiveButton("Xác nhận Gửi", (dialog, which) -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
                    if (btnSendBlast != null) btnSendBlast.setEnabled(false);
                    executeBatchInsert(userIds, title, body, sheet, pbLoading, btnSendBlast);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void executeBatchInsert(Set<String> userIds, String title, String body, 
                                     com.google.android.material.bottomsheet.BottomSheetDialog sheet, ProgressBar pbLoading, Button btnSendBlast) {
        List<Task<Void>> batchTasks = new ArrayList<>();
        WriteBatch batch = db.batch();
        int count = 0;
        int total = userIds.size();

        for (String uid : userIds) {
            String notifId = UUID.randomUUID().toString();
            Notification notif = new Notification();
            notif.setNotificationId(notifId);
            notif.setUserId(uid);
            notif.setType("organizer_blast");
            notif.setTitle(title);
            notif.setBody(body);
            notif.setRefType("event");
            notif.setRefId(eventId);
            notif.setChannel("push_email");
            notif.setStatus("pending");
            notif.setRead(false);
            notif.setCreatedAt(Timestamp.now());

            batch.set(db.collection(FirebaseCollections.NOTIFICATIONS).document(notifId), notif);
            count++;

            if (count >= 490) {
                batchTasks.add(batch.commit());
                batch = db.batch();
                count = 0;
            }
        }

        if (count > 0) {
            batchTasks.add(batch.commit());
        }

        Tasks.whenAll(batchTasks).addOnSuccessListener(unused -> {
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
            if (sheet != null) sheet.dismiss();
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Thành công")
                    .setMessage("Đã đưa " + total + " thông báo vào hàng đợi. Thời gian gửi dự kiến: ~2 phút.")
                    .setPositiveButton("Đóng", (dialog, which) -> {
                        loadBlastHistory();
                    })
                    .setCancelable(false)
                    .show();
        }).addOnFailureListener(e -> showFailureAlert(e, pbLoading, btnSendBlast));
    }

    private void showNoTargetAlert(ProgressBar pbLoading, Button btnSendBlast) {
        if (pbLoading != null) pbLoading.setVisibility(View.GONE);
        if (btnSendBlast != null) btnSendBlast.setEnabled(true);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Không có đối tượng")
                .setMessage("Không tìm thấy người dùng nào phù hợp với điều kiện để gửi thông báo.")
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void showFailureAlert(Exception e, ProgressBar pbLoading, Button btnSendBlast) {
        if (pbLoading != null) pbLoading.setVisibility(View.GONE);
        if (btnSendBlast != null) btnSendBlast.setEnabled(true);
        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void loadBlastHistory() {
        binding.pbLoading.setVisibility(View.VISIBLE);
        db.collection(FirebaseCollections.NOTIFICATIONS)
                .whereEqualTo("ref_type", "event")
                .whereEqualTo("ref_id", eventId)
                .whereEqualTo("type", "organizer_blast")
                .get()
                .addOnSuccessListener(snap -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    if (snap == null || snap.isEmpty()) {
                        blastHistoryList.clear();
                        blastHistoryAdapter.notifyDataSetChanged();
                        if (binding.layoutEmpty != null) {
                            binding.layoutEmpty.setVisibility(View.VISIBLE);
                        }
                        return;
                    }

                    Map<String, BlastHistoryItem> groups = new HashMap<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String title = doc.getString("title");
                        String body = doc.getString("body");
                        com.google.firebase.Timestamp createdAt = doc.getTimestamp("created_at");
                        String status = doc.getString("status");
                        String notifId = doc.getId();

                        if (title == null || body == null || createdAt == null) continue;

                        long sec = createdAt.getSeconds();
                        long roundedSec = sec - (sec % 5);
                        String key = title + "_" + body + "_" + roundedSec;

                        BlastHistoryItem item = groups.get(key);
                        if (item == null) {
                            item = new BlastHistoryItem();
                            item.setTitle(title);
                            item.setBody(body);
                            item.setCreatedAt(createdAt);
                            item.setStatus(status != null ? status : "pending");
                            item.setRecipientCount(0);
                            item.setNotificationIds(new ArrayList<>());
                            groups.put(key, item);
                        }

                        String currentStatus = item.getStatus();
                        if (status != null) {
                            if ("pending".equals(status)) {
                                item.setStatus("pending");
                            } else if ("sent".equals(status) && !"pending".equals(currentStatus)) {
                                item.setStatus("sent");
                            } else if ("failed".equals(status) && !"pending".equals(currentStatus) && !"sent".equals(currentStatus)) {
                                item.setStatus("failed");
                            } else if ("cancelled".equals(status) && currentStatus == null) {
                                item.setStatus("cancelled");
                            }
                        }

                        item.getNotificationIds().add(notifId);
                        item.setRecipientCount(item.getRecipientCount() + 1);
                    }

                    blastHistoryList.clear();
                    blastHistoryList.addAll(groups.values());

                    java.util.Collections.sort(blastHistoryList, (o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));

                    blastHistoryAdapter.notifyDataSetChanged();
                    if (binding.layoutEmpty != null) {
                        binding.layoutEmpty.setVisibility(blastHistoryList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải lịch sử: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showBlastDetailsDialog(BlastHistoryItem item) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
        String timeStr = sdf.format(item.getCreatedAt().toDate());
        
        String statusLabel = "Chờ gửi";
        if ("sent".equals(item.getStatus())) statusLabel = "Đã gửi";
        else if ("cancelled".equals(item.getStatus())) statusLabel = "Đã hủy";
        else if ("failed".equals(item.getStatus())) statusLabel = "Lỗi gửi";

        String message = "Tiêu đề: " + item.getTitle() + "\n\n"
                + "Nội dung:\n" + item.getBody() + "\n\n"
                + "Thời gian tạo: " + timeStr + "\n"
                + "Số người nhận: " + item.getRecipientCount() + "\n"
                + "Trạng thái: " + statusLabel;

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Chi tiết thông báo")
                .setMessage(message)
                .setPositiveButton("Đóng", null);

        long diffMinutes = (Timestamp.now().getSeconds() - item.getCreatedAt().getSeconds()) / 60;
        boolean canCancel = "pending".equalsIgnoreCase(item.getStatus()) && diffMinutes < 10;

        if (canCancel) {
            builder.setNegativeButton("Hủy thông báo", (dialog, which) -> {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Xác nhận hủy")
                        .setMessage("Bạn có chắc chắn muốn hủy thông báo này không? Hành động này sẽ ngăn không cho gửi thông báo tới " + item.getRecipientCount() + " người nhận.")
                        .setPositiveButton("Đồng ý hủy", (d2, w2) -> cancelBlast(item))
                        .setNegativeButton("Quay lại", null)
                        .show();
            });
        }

        builder.show();
    }

    private void cancelBlast(BlastHistoryItem item) {
        binding.pbLoading.setVisibility(View.VISIBLE);
        WriteBatch batch = db.batch();
        for (String id : item.getNotificationIds()) {
            batch.update(db.collection(FirebaseCollections.NOTIFICATIONS).document(id), "status", "cancelled");
        }
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Đã hủy thông báo thành công", Toast.LENGTH_SHORT).show();
                    loadBlastHistory();
                })
                .addOnFailureListener(e -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi khi hủy: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public static class BlastHistoryItem {
        private String title;
        private String body;
        private com.google.firebase.Timestamp createdAt;
        private String status;
        private int recipientCount;
        private List<String> notificationIds;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }

        public com.google.firebase.Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(com.google.firebase.Timestamp createdAt) { this.createdAt = createdAt; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getRecipientCount() { return recipientCount; }
        public void setRecipientCount(int recipientCount) { this.recipientCount = recipientCount; }

        public List<String> getNotificationIds() { return notificationIds; }
        public void setNotificationIds(List<String> notificationIds) { this.notificationIds = notificationIds; }
    }

    public static class BlastHistoryAdapter extends RecyclerView.Adapter<BlastHistoryAdapter.ViewHolder> {
        public interface OnItemClickListener {
            void onItemClick(BlastHistoryItem item);
        }

        private List<BlastHistoryItem> list;
        private OnItemClickListener listener;

        public BlastHistoryAdapter(List<BlastHistoryItem> list, OnItemClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blast_history, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BlastHistoryItem item = list.get(position);
            holder.tvTitle.setText(item.getTitle());
            holder.tvBody.setText(item.getBody());
            
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
            holder.tvTime.setText(sdf.format(item.getCreatedAt().toDate()));
            holder.tvRecipients.setText(item.getRecipientCount() + " người nhận");

            String status = item.getStatus();
            if ("pending".equalsIgnoreCase(status)) {
                holder.tvStatus.setText("Chờ gửi");
                holder.tvStatus.setTextColor(0xFFD97706);
                holder.tvStatus.setBackgroundColor(0xFFFEF3C7);
            } else if ("sent".equalsIgnoreCase(status)) {
                holder.tvStatus.setText("Đã gửi");
                holder.tvStatus.setTextColor(0xFF16A34A);
                holder.tvStatus.setBackgroundColor(0xFFDCFCE7);
            } else if ("cancelled".equalsIgnoreCase(status)) {
                holder.tvStatus.setText("Đã hủy");
                holder.tvStatus.setTextColor(0xFFDC2626);
                holder.tvStatus.setBackgroundColor(0xFFFEE2E2);
            } else {
                holder.tvStatus.setText("Lỗi gửi");
                holder.tvStatus.setTextColor(0xFF7C2D12);
                holder.tvStatus.setBackgroundColor(0xFFFFEDD5);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvStatus, tvBody, tvTime, tvRecipients;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvBody = itemView.findViewById(R.id.tvBody);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvRecipients = itemView.findViewById(R.id.tvRecipients);
            }
        }
    }
}
