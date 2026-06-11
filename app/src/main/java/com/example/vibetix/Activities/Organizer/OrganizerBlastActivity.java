package com.example.vibetix.Activities.Organizer;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

        binding.rgTarget.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.rbTicketType.getId()) {
                binding.layoutTicketType.setVisibility(View.VISIBLE);
                if (ticketTypes.isEmpty()) {
                    loadTicketTypes();
                }
            } else {
                binding.layoutTicketType.setVisibility(View.GONE);
                selectedTicketType = null;
            }
        });

        binding.btnSendBlast.setOnClickListener(v -> sendBlast());
    }

    private void loadTicketTypes() {
        binding.pbLoading.setVisibility(View.VISIBLE);
        db.collection(FirebaseCollections.TICKET_TYPES)
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    binding.pbLoading.setVisibility(View.GONE);
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
                    binding.ddTicketType.setAdapter(adapter);
                    binding.ddTicketType.setOnItemClickListener((parent, view, position, id) -> {
                        selectedTicketType = ticketTypes.get(position);
                    });
                })
                .addOnFailureListener(e -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải loại vé", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendBlast() {
        String title = binding.etTitle.getText() != null ? binding.etTitle.getText().toString().trim() : "";
        String body = binding.etBody.getText() != null ? binding.etBody.getText().toString().trim() : "";

        if (title.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề và nội dung", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isTicketTypeTarget = binding.rbTicketType.isChecked();
        boolean isConfirmedOnlyTarget = binding.rbConfirmedOnly.isChecked();

        if (isTicketTypeTarget && selectedTicketType == null) {
            Toast.makeText(this, "Vui lòng chọn loại vé", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.pbLoading.setVisibility(View.VISIBLE);
        binding.btnSendBlast.setEnabled(false);

        // Fetch user ids
        if (isTicketTypeTarget) {
            // Need to query order_items first
            db.collection(FirebaseCollections.ORDER_ITEMS)
                    .whereEqualTo("event_id", eventId)
                    .whereEqualTo("ticket_type_id", selectedTicketType.getTicketTypeId())
                    .get()
                    .addOnSuccessListener(snap -> {
                        Set<String> orderIds = new HashSet<>();
                        for (DocumentSnapshot doc : snap) {
                            OrderItem item = doc.toObject(OrderItem.class);
                            if (item != null && item.getOrderId() != null) {
                                orderIds.add(item.getOrderId());
                            }
                        }
                        if (orderIds.isEmpty()) {
                            showNoTargetAlert();
                            return;
                        }
                        fetchOrdersAndFilter(new ArrayList<>(orderIds), isConfirmedOnlyTarget, title, body);
                    })
                    .addOnFailureListener(e -> showFailureAlert(e));
        } else {
            // Fetch all orders for event
            db.collection(FirebaseCollections.ORDERS)
                    .whereEqualTo("event_id", eventId)
                    .get()
                    .addOnSuccessListener(snap -> {
                        Set<String> userIds = new HashSet<>();
                        for (DocumentSnapshot doc : snap) {
                            Order order = doc.toObject(Order.class);
                            if (order != null && order.getUserId() != null) {
                                boolean isValidStatus = "completed".equalsIgnoreCase(order.getStatusStr()) ||
                                        "confirmed".equalsIgnoreCase(order.getStatusStr()) ||
                                        "paid".equalsIgnoreCase(order.getStatusStr());
                                
                                if (isConfirmedOnlyTarget) {
                                    if (isValidStatus) userIds.add(order.getUserId());
                                } else {
                                    // rbAll: just exclude cancelled
                                    if (!"cancelled".equalsIgnoreCase(order.getStatusStr()) && !"refunded".equalsIgnoreCase(order.getStatusStr())) {
                                        userIds.add(order.getUserId());
                                    }
                                }
                            }
                        }
                        
                        if (userIds.isEmpty()) {
                            showNoTargetAlert();
                        } else {
                            showConfirmationDialog(userIds, title, body);
                        }
                    })
                    .addOnFailureListener(e -> showFailureAlert(e));
        }
    }

    private void fetchOrdersAndFilter(List<String> orderIds, boolean confirmedOnly, String title, String body) {
        // orderIds could be large, split into chunks of 10
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (int i = 0; i < orderIds.size(); i += 10) {
            List<String> chunk = orderIds.subList(i, Math.min(orderIds.size(), i + 10));
            tasks.add(db.collection(FirebaseCollections.ORDERS).whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk).get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            Set<String> userIds = new HashSet<>();
            for (Object res : results) {
                QuerySnapshot snap = (QuerySnapshot) res;
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Order order = doc.toObject(Order.class);
                    if (order != null && order.getUserId() != null) {
                        boolean isValidStatus = "completed".equalsIgnoreCase(order.getStatusStr()) ||
                                "confirmed".equalsIgnoreCase(order.getStatusStr()) ||
                                "paid".equalsIgnoreCase(order.getStatusStr());

                        if (confirmedOnly) {
                            if (isValidStatus) userIds.add(order.getUserId());
                        } else {
                            if (!"cancelled".equalsIgnoreCase(order.getStatusStr()) && !"refunded".equalsIgnoreCase(order.getStatusStr())) {
                                userIds.add(order.getUserId());
                            }
                        }
                    }
                }
            }

            if (userIds.isEmpty()) {
                showNoTargetAlert();
            } else {
                showConfirmationDialog(userIds, title, body);
            }
        }).addOnFailureListener(e -> showFailureAlert(e));
    }

    private void showConfirmationDialog(Set<String> userIds, String title, String body) {
        binding.pbLoading.setVisibility(View.GONE);
        binding.btnSendBlast.setEnabled(true);

        new AlertDialog.Builder(this)
                .setTitle("⚠️ Xác nhận gửi")
                .setMessage("Bạn chuẩn bị gửi thông báo này tới " + userIds.size() + " khách hàng.\n\nHành động này không thể hoàn tác. Bạn đã kiểm tra kỹ nội dung chưa?")
                .setPositiveButton("Xác nhận Gửi", (dialog, which) -> {
                    binding.pbLoading.setVisibility(View.VISIBLE);
                    binding.btnSendBlast.setEnabled(false);
                    executeBatchInsert(userIds, title, body);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void executeBatchInsert(Set<String> userIds, String title, String body) {
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
                batch.commit();
                batch = db.batch();
                count = 0;
            }
        }

        if (count > 0) {
            batch.commit().addOnSuccessListener(unused -> {
                binding.pbLoading.setVisibility(View.GONE);
                new AlertDialog.Builder(this)
                        .setTitle("Thành công")
                        .setMessage("Đã đưa " + total + " thông báo vào hàng đợi. Thời gian gửi dự kiến: ~2 phút.")
                        .setPositiveButton("Đóng", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }).addOnFailureListener(this::showFailureAlert);
        } else {
            // Already committed all 500 batches perfectly without remainder
            binding.pbLoading.setVisibility(View.GONE);
            new AlertDialog.Builder(this)
                    .setTitle("Thành công")
                    .setMessage("Đã đưa " + total + " thông báo vào hàng đợi. Thời gian gửi dự kiến: ~2 phút.")
                    .setPositiveButton("Đóng", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        }
    }

    private void showNoTargetAlert() {
        binding.pbLoading.setVisibility(View.GONE);
        binding.btnSendBlast.setEnabled(true);
        new AlertDialog.Builder(this)
                .setTitle("Không có đối tượng")
                .setMessage("Không tìm thấy người dùng nào phù hợp với điều kiện để gửi thông báo.")
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void showFailureAlert(Exception e) {
        binding.pbLoading.setVisibility(View.GONE);
        binding.btnSendBlast.setEnabled(true);
        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
