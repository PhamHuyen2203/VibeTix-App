package com.example.vibetix.Activities.Organizer;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.OrderItem;
import com.example.vibetix.Models.Order;
import com.example.vibetix.Models.TicketType;
import com.example.vibetix.Models.UserTicket;
import com.example.vibetix.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TicketDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TICKET_ID = "EXTRA_TICKET_ID";
    public static final String EXTRA_EVENT_TITLE = "EXTRA_EVENT_TITLE";
    public static final String EXTRA_EVENT_DATE = "EXTRA_EVENT_DATE";
    public static final String EXTRA_EVENT_POSTER = "EXTRA_EVENT_POSTER";

    private ProgressBar pbLoading;
    private TextView tvStatusIcon, tvTicketStatus, tvTicketCode;
    private TextView tvCustomerName, tvCustomerEmail, tvCustomerUserId;
    private TextView tvTicketTypeName, tvPrice, tvOrderId, tvOrderDate;
    private TextView tvCheckinStatus;
    private View layoutCheckinTimeRow;
    private TextView tvCheckinTime;
    private ImageView ivEventPoster;
    private TextView tvEventTitle, tvEventDate;

    private FirebaseFirestore db;
    private String ticketId;

    private final NumberFormat vndFmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final SimpleDateFormat dispFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);

        db = FirebaseFirestore.getInstance();
        ticketId = getIntent().getStringExtra(EXTRA_TICKET_ID);
        if (ticketId == null) {
            Toast.makeText(this, "Không tìm thấy vé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Fill event info from intent (so it shows immediately)
        String eTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);
        String eDate = getIntent().getStringExtra(EXTRA_EVENT_DATE);
        String ePoster = getIntent().getStringExtra(EXTRA_EVENT_POSTER);
        if (eTitle != null) tvEventTitle.setText(eTitle);
        if (eDate != null) tvEventDate.setText(eDate);
        if (ePoster != null && !ePoster.isEmpty()) {
            Glide.with(this).load(ePoster).centerCrop()
                    .placeholder(R.color.clr_bg_section).into(ivEventPoster);
        }

        loadTicket();
    }

    private void bindViews() {
        pbLoading = findViewById(R.id.pbLoading);
        tvStatusIcon = findViewById(R.id.tvStatusIcon);
        tvTicketStatus = findViewById(R.id.tvTicketStatus);
        tvTicketCode = findViewById(R.id.tvTicketCode);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvCustomerEmail = findViewById(R.id.tvCustomerEmail);
        tvCustomerUserId = findViewById(R.id.tvCustomerUserId);
        tvTicketTypeName = findViewById(R.id.tvTicketTypeName);
        tvPrice = findViewById(R.id.tvPrice);
        tvOrderId = findViewById(R.id.tvOrderId);
        tvOrderDate = findViewById(R.id.tvOrderDate);
        tvCheckinStatus = findViewById(R.id.tvCheckinStatus);
        layoutCheckinTimeRow = findViewById(R.id.layoutCheckinTimeRow);
        tvCheckinTime = findViewById(R.id.tvCheckinTime);
        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvEventDate = findViewById(R.id.tvEventDate);
    }

    private void loadTicket() {
        showLoading(true);

        db.collection(FirebaseCollections.USER_TICKETS).document(ticketId).get()
                .addOnSuccessListener(ticketDoc -> {
                    if (!ticketDoc.exists()) {
                        showLoading(false);
                        Toast.makeText(this, "Không tìm thấy vé", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    UserTicket ticket = ticketDoc.toObject(UserTicket.class);
                    if (ticket == null) {
                        showLoading(false);
                        finish();
                        return;
                    }
                    if (ticket.getUserTicketId() == null) ticket.setUserTicketId(ticketDoc.getId());

                    // Display ticket code and check-in status immediately
                    fillTicketBaseInfo(ticket);

                    // Now fetch order_item + order + user in parallel
                    String orderItemId = ticket.getOrderItemId();
                    String ownerId = ticket.getOwnerId();

                    List<Task<?>> parallelTasks = new ArrayList<>();

                    Task<QuerySnapshot> orderItemTask = null;
                    if (orderItemId != null) {
                        orderItemTask = db.collection(FirebaseCollections.ORDER_ITEMS)
                                .whereIn(FieldPath.documentId(), Collections.singletonList(orderItemId)).get();
                        parallelTasks.add(orderItemTask);
                    }

                    Task<DocumentSnapshot> userTask = null;
                    if (ownerId != null) {
                        userTask = db.collection(FirebaseCollections.USERS).document(ownerId).get();
                        parallelTasks.add(userTask);
                    }

                    if (parallelTasks.isEmpty()) {
                        showLoading(false);
                        return;
                    }

                    Task<QuerySnapshot> finalOrderItemTask = orderItemTask;
                    Task<DocumentSnapshot> finalUserTask = userTask;

                    Tasks.whenAll(parallelTasks.toArray(new Task[0]))
                            .addOnSuccessListener(unused -> {
                                // Parse order item
                                OrderItem orderItem = null;
                                if (finalOrderItemTask != null && finalOrderItemTask.isSuccessful()) {
                                    QuerySnapshot snap = finalOrderItemTask.getResult();
                                    if (snap != null && !snap.isEmpty()) {
                                        orderItem = snap.getDocuments().get(0).toObject(OrderItem.class);
                                    }
                                }

                                // Parse user
                                String customerName = null, customerEmail = null;
                                if (finalUserTask != null && finalUserTask.isSuccessful()) {
                                    DocumentSnapshot userDoc = finalUserTask.getResult();
                                    if (userDoc != null && userDoc.exists()) {
                                        customerName = userDoc.getString("full_name");
                                        customerEmail = userDoc.getString("email");
                                    }
                                }

                                String finalCustomerName = customerName;
                                String finalCustomerEmail = customerEmail;
                                OrderItem finalOrderItem = orderItem;

                                if (orderItem != null && orderItem.getOrderId() != null) {
                                    // Fetch the order for date/status
                                    db.collection(FirebaseCollections.ORDERS)
                                            .document(orderItem.getOrderId()).get()
                                            .addOnSuccessListener(orderDoc -> {
                                                Order order = orderDoc.exists() ? orderDoc.toObject(Order.class) : null;
                                                finalizeUI(ticket, finalOrderItem, order,
                                                        finalCustomerName, finalCustomerEmail, ownerId);
                                            })
                                            .addOnFailureListener(e ->
                                                    finalizeUI(ticket, finalOrderItem, null,
                                                            finalCustomerName, finalCustomerEmail, ownerId));
                                } else {
                                    finalizeUI(ticket, finalOrderItem, null,
                                            finalCustomerName, finalCustomerEmail, ownerId);
                                }
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Lỗi tải thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi tải vé: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fillTicketBaseInfo(UserTicket ticket) {
        boolean isUsed = ticket.isUsed();
        if (isUsed) {
            tvStatusIcon.setText("✅");
            tvTicketStatus.setText("Đã check-in");
            tvTicketStatus.setTextColor(getColor(R.color.clr_success));
            tvCheckinStatus.setText("Đã check-in");
            tvCheckinStatus.setTextColor(getColor(R.color.clr_success));

            // Check-in time
            Object checkedAt = ticket.getCheckedInAt();
            if (checkedAt instanceof com.google.firebase.Timestamp) {
                layoutCheckinTimeRow.setVisibility(View.VISIBLE);
                tvCheckinTime.setText(dispFmt.format(((com.google.firebase.Timestamp) checkedAt).toDate()));
            }
        } else {
            tvStatusIcon.setText("🎟️");
            tvTicketStatus.setText("Hợp lệ");
            tvTicketStatus.setTextColor(getColor(R.color.clr_primary_blue));
            tvCheckinStatus.setText("Chưa check-in");
            tvCheckinStatus.setTextColor(getColor(R.color.clr_warning));
        }

        // Ticket code
        String code = ticket.getDisplayCode() != null ? ticket.getDisplayCode() :
                (ticket.getUserTicketId() != null && ticket.getUserTicketId().length() >= 8
                        ? ticket.getUserTicketId().substring(0, 8).toUpperCase() : ticket.getUserTicketId());
        tvTicketCode.setText("Mã vé: #" + (code != null ? code : "—"));

        // Owner ID
        tvCustomerUserId.setText(ticket.getOwnerId() != null ? ticket.getOwnerId() : "—");
    }

    private void finalizeUI(UserTicket ticket, OrderItem orderItem, Order order,
                             String customerName, String customerEmail, String ownerId) {
        runOnUiThread(() -> {
            showLoading(false);

            // Customer info
            tvCustomerName.setText(customerName != null ? customerName : "—");
            tvCustomerEmail.setText(customerEmail != null ? customerEmail : "—");
            tvCustomerUserId.setText(ownerId != null ? ownerId : "—");

            // Ticket type & price
            if (orderItem != null) {
                // Ticket type name fetched below if available; for now just show price
                tvPrice.setText(vndFmt.format(orderItem.getPricePerTicket()) + " ₫");

                // Order ID
                String oid = orderItem.getOrderId();
                if (oid != null) {
                    String shortId = oid.length() >= 8 ? oid.substring(0, 8).toUpperCase() : oid.toUpperCase();
                    tvOrderId.setText("#" + shortId);
                }

                // Fetch ticket type name
                if (orderItem.getTicketTypeId() != null) {
                    db.collection(FirebaseCollections.TICKET_TYPES)
                            .document(orderItem.getTicketTypeId()).get()
                            .addOnSuccessListener(ttDoc -> {
                                if (ttDoc.exists()) {
                                    TicketType tt = ttDoc.toObject(TicketType.class);
                                    if (tt != null) tvTicketTypeName.setText(tt.getName());
                                }
                            });
                }
            } else {
                tvPrice.setText("—");
                tvOrderId.setText("—");
                tvTicketTypeName.setText("—");
            }

            // Order date
            if (order != null && order.getOrderDate() != null) {
                tvOrderDate.setText(dispFmt.format(order.getOrderDate().toDate()));
            } else {
                // Fallback: use issued_at from ticket
                Object issuedAt = ticket.getIssuedAt();
                if (issuedAt instanceof com.google.firebase.Timestamp) {
                    tvOrderDate.setText(dispFmt.format(((com.google.firebase.Timestamp) issuedAt).toDate()));
                } else {
                    tvOrderDate.setText("—");
                }
            }
        });
    }

    private void showLoading(boolean show) {
        pbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
