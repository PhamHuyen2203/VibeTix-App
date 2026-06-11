package com.example.vibetix.Activities.Organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Adapters.Organizer.FeatureAdapter;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import com.example.vibetix.Utils.NotificationTriggerManager;
import com.example.vibetix.Utils.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * EventHubActivity — Trung tâm quản lý một sự kiện cụ thể.
 *
 * Nhận vào:
 *   - EXTRA_EVENT_ID: ID sự kiện
 *   - EXTRA_ROLE:     "owner" | "manager" | "check_in_staff"
 *
 * Hiển thị:
 *   - Thông tin sự kiện + role badge
 *   - Stats Dashboard (ẩn với check_in_staff)
 *   - Feature Grid 2 cột qua RecyclerView + FeatureAdapter
 */
public class EventHubActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";
    public static final String EXTRA_ROLE     = "EXTRA_ROLE";

    // Header views
    private ShapeableImageView ivEventPoster;
    private TextView tvEventTitle, tvEventDate, tvEventVenue;
    private TextView tvRoleBadge, tvStatusBadge;

    // Stats card
    private MaterialCardView cardStats;
    private TextView tvStatRevenue, tvStatTickets, tvStatCheckins;

    // Feature Grid
    private RecyclerView rvHubFeatures;

    private String eventId, role;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    private final NumberFormat vndFmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_hub);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        role    = getIntent().getStringExtra(EXTRA_ROLE);

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy sự kiện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (role == null) role = "check_in_staff"; // fallback an toàn

        db             = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Lưu context sự kiện vào session cho các Activity con
        sessionManager.setActiveEvent(eventId, role);

        bindViews();
        setupToolbar();
        setupFeatureGrid();
        loadEventDetails();
        loadStats();
    }

    // ── View Binding ──────────────────────────────────────────────────────────

    private void bindViews() {
        ivEventPoster  = findViewById(R.id.ivHubEventPoster);
        tvEventTitle   = findViewById(R.id.tvHubEventTitle);
        tvEventDate    = findViewById(R.id.tvHubEventDate);
        tvEventVenue   = findViewById(R.id.tvHubEventVenue);
        tvRoleBadge    = findViewById(R.id.tvHubRoleBadge);
        tvStatusBadge  = findViewById(R.id.tvHubStatusBadge);

        cardStats      = findViewById(R.id.layoutHubStats);
        tvStatRevenue  = findViewById(R.id.tvHubStatRevenue);
        tvStatTickets  = findViewById(R.id.tvHubStatTickets);
        tvStatCheckins = findViewById(R.id.tvHubStatCheckins);

        rvHubFeatures  = findViewById(R.id.rvHubFeatures);
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarEventHub);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý sự kiện");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if ("owner".equalsIgnoreCase(role)) {
            menu.add(Menu.NONE, 1, Menu.NONE, "Xem trước (User view)")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menu.add(Menu.NONE, 2, Menu.NONE, "Hồ sơ Ban tổ chức")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menu.add(Menu.NONE, 3, Menu.NONE, "Huỷ sự kiện")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        if (item.getItemId() == 1) {
            Intent i = new Intent(this, com.example.vibetix.Activities.User.EventDetailActivity.class);
            i.putExtra("EXTRA_EVENT_ID", eventId);
            startActivity(i);
            return true;
        }
        if (item.getItemId() == 2) {
            Toast.makeText(this, "Hồ sơ Ban tổ chức (sắp ra mắt)", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (item.getItemId() == 3) {
            confirmCancelEvent();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmCancelEvent() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Huỷ sự kiện")
            .setMessage("Bạn có chắc chắn muốn huỷ sự kiện này? Hành động này không thể hoàn tác và sẽ gửi thông báo đến tất cả khách hàng đã mua vé.")
            .setPositiveButton("Huỷ sự kiện", (dialog, which) -> {
                db.collection(FirebaseCollections.EVENTS).document(eventId)
                    .update("status", "cancelled")
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Đã huỷ sự kiện thành công", Toast.LENGTH_SHORT).show();
                        String title = tvEventTitle != null ? tvEventTitle.getText().toString() : "Sự kiện";
                        NotificationTriggerManager.triggerEventCancelled(eventId, title);
                        loadEventDetails();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Đóng", null)
            .show();
    }

    // ── Feature Grid (Role-based) ─────────────────────────────────────────────

    private void setupFeatureGrid() {
        boolean isOwner   = "owner".equalsIgnoreCase(role);
        boolean isManager = "manager".equalsIgnoreCase(role);

        // Ẩn stats với check_in_staff
        if (cardStats != null) {
            cardStats.setVisibility((!isOwner && !isManager) ? View.GONE : View.VISIBLE);
        }

        // Apply role badge
        applyRoleBadge();

        // Xây dựng danh sách tính năng theo role
        List<FeatureAdapter.FeatureItem> features = buildFeatureList(isOwner, isManager);

        // Gắn vào RecyclerView với GridLayoutManager 2 cột
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        rvHubFeatures.setLayoutManager(gridLayoutManager);
        rvHubFeatures.setAdapter(new FeatureAdapter(this, features));
        rvHubFeatures.setNestedScrollingEnabled(false);
    }

    private List<FeatureAdapter.FeatureItem> buildFeatureList(boolean isOwner, boolean isManager) {
        List<FeatureAdapter.FeatureItem> list = new ArrayList<>();

        // Ưu tiên cao: 2 tính năng real-time luôn enabled và nổi bật (isHighlighted=true)
        list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_camera, "Quét QR", true, true,
                () -> {
                    Intent i = new Intent(this, QrScannerActivity.class);
                    i.putExtra(QrScannerActivity.EXTRA_EVENT_ID, eventId);
                    startActivity(i);
                }));

        list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_role_person, "D.S Khách", true, true,
                () -> {
                    Intent i = new Intent(this, AttendeesActivity.class);
                    i.putExtra(AttendeesActivity.EXTRA_EVENT_ID, eventId);
                    startActivity(i);
                }));

        // Tính năng quản lý theo role
        list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_edit, "Chỉnh sửa", isOwner, false,
                () -> {
                    Intent i = new Intent(this, CreateEditEventActivity.class);
                    i.putExtra(CreateEditEventActivity.EXTRA_EVENT_ID, eventId);
                    startActivity(i);
                }));

        list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_ticket, "Loại vé", isOwner || isManager, false,
                () -> {
                    String title = tvEventTitle != null ? tvEventTitle.getText().toString() : "Sự kiện";
                    Intent i = new Intent(this, TicketTypeManagementActivity.class);
                    i.putExtra(TicketTypeManagementActivity.EXTRA_EVENT_ID, eventId);
                    i.putExtra(TicketTypeManagementActivity.EXTRA_EVENT_TITLE, title);
                    startActivity(i);
                }));

        list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_role_staff, "Nhân sự", isOwner, false,
                () -> {
                    Intent i = new Intent(this, StaffManagementActivity.class);
                    i.putExtra(StaffManagementActivity.EXTRA_EVENT_ID, eventId);
                    startActivity(i);
                }));

        list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_trending_up, "Đơn hàng", isOwner || isManager, false,
                () -> {
                    Intent i = new Intent(this, OrderManagementActivity.class);
                    i.putExtra("EXTRA_EVENT_ID", eventId);
                    startActivity(i);
                }));

        list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_org_star, "Nghệ sĩ", isOwner, false,
                () -> {
                    Intent i = new Intent(this, EventStarManagementActivity.class);
                    i.putExtra("EXTRA_EVENT_ID", eventId);
                    startActivity(i);
                }));

        list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_star_rating, "Thống kê sao", isOwner, false,
                () -> {
                    Intent i = new Intent(this, EventStarStatsActivity.class);
                    i.putExtra("EXTRA_EVENT_ID", eventId);
                    startActivity(i);
                }));

        list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_discount, "Giảm giá", isOwner, false,
                () -> {
                    Intent i = new Intent(this, DiscountManagementActivity.class);
                    i.putExtra("EXTRA_EVENT_ID", eventId);
                    startActivity(i);
                }));

        list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_bell, "Thông báo", isOwner || isManager, false,
                () -> {
                    Intent i = new Intent(this, OrganizerBlastActivity.class);
                    i.putExtra("EXTRA_EVENT_ID", eventId);
                    startActivity(i);
                }));

        return list;
    }

    private void applyRoleBadge() {
        if (tvRoleBadge == null) return;
        if ("owner".equalsIgnoreCase(role)) {
            tvRoleBadge.setText("👑 OWNER");
            tvRoleBadge.setBackgroundResource(R.drawable.bg_role_owner);
        } else if ("manager".equalsIgnoreCase(role)) {
            tvRoleBadge.setText("👔 MANAGER");
            tvRoleBadge.setBackgroundResource(R.drawable.bg_role_manager);
        } else {
            tvRoleBadge.setText("🎫 CHECK-IN STAFF");
            tvRoleBadge.setBackgroundResource(R.drawable.bg_role_staff);
        }
    }

    // ── Load Event Details ────────────────────────────────────────────────────

    private void loadEventDetails() {
        db.collection(FirebaseCollections.EVENTS).document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    Event event = doc.toObject(Event.class);
                    if (event == null) return;

                    if (tvEventTitle != null) tvEventTitle.setText(event.getTitle());
                    if (tvEventDate  != null) tvEventDate.setText(
                            event.getStartTime() != null ? "📅 " + event.getStartTime() : "");
                    if (tvEventVenue != null) {
                        String venueId = event.getVenueId();
                        if (venueId != null && !venueId.isEmpty()) {
                            db.collection(FirebaseCollections.VENUES).document(venueId).get()
                                    .addOnSuccessListener(venueDoc -> {
                                        if (venueDoc.exists()) {
                                            String address = venueDoc.getString("address");
                                            String name = venueDoc.getString("name");
                                            if (address != null && !address.isEmpty()) {
                                                String displayText = name != null ? name + " - " + address : address;
                                                tvEventVenue.setText("📍 " + displayText);
                                            } else {
                                                tvEventVenue.setText(event.getVenueCity() != null ? "📍 " + event.getVenueCity() : "");
                                            }
                                        } else {
                                            tvEventVenue.setText(event.getVenueCity() != null ? "📍 " + event.getVenueCity() : "");
                                        }
                                    });
                        } else {
                            tvEventVenue.setText(event.getVenueCity() != null ? "📍 " + event.getVenueCity() : "");
                        }
                    }

                    if (getSupportActionBar() != null && event.getTitle() != null) {
                        getSupportActionBar().setTitle(event.getTitle());
                    }

                    if (tvStatusBadge != null && event.getStatusStr() != null) {
                        applyStatusBadge(event.getStatusStr());
                    }

                    if (ivEventPoster != null && event.getPosterUrl() != null) {
                        Glide.with(this)
                                .load(event.getPosterUrl())
                                .placeholder(R.drawable.ic_organizer_placeholder)
                                .into(ivEventPoster);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không thể tải thông tin sự kiện", Toast.LENGTH_SHORT).show());
    }

    private void applyStatusBadge(String status) {
        if (tvStatusBadge == null) return;
        switch (status.toLowerCase()) {
            case "approved":
            case "ongoing":
                tvStatusBadge.setText("● " + status.toUpperCase());
                tvStatusBadge.setTextColor(getColor(R.color.clr_success));
                break;
            case "pending":
                tvStatusBadge.setText("● PENDING");
                tvStatusBadge.setTextColor(getColor(R.color.clr_warning));
                break;
            case "draft":
                tvStatusBadge.setText("● DRAFT");
                tvStatusBadge.setTextColor(getColor(R.color.clr_grey_1));
                break;
            case "cancelled":
                tvStatusBadge.setText("● CANCELLED");
                tvStatusBadge.setTextColor(getColor(R.color.clr_error));
                break;
            default:
                tvStatusBadge.setText("● " + status.toUpperCase());
        }
    }

    // ── Load Stats (Owner/Manager only) ───────────────────────────────────────

    private void loadStats() {
        if ("check_in_staff".equalsIgnoreCase(role)) return;

        db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) {
                        if (tvStatRevenue != null) tvStatRevenue.setText("0 ₫");
                        if (tvStatTickets != null) tvStatTickets.setText("0");
                        return;
                    }

                    List<com.example.vibetix.Models.OrderItem> allItems = new ArrayList<>();
                    java.util.Set<String> orderIds = new java.util.HashSet<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        com.example.vibetix.Models.OrderItem item = doc.toObject(com.example.vibetix.Models.OrderItem.class);
                        if (item != null) {
                            allItems.add(item);
                            if (item.getOrderId() != null) orderIds.add(item.getOrderId());
                        }
                    }

                    if (orderIds.isEmpty()) {
                        if (tvStatRevenue != null) tvStatRevenue.setText("0 ₫");
                        if (tvStatTickets != null) tvStatTickets.setText("0");
                        return;
                    }

                    List<String> orderIdList = new ArrayList<>(orderIds);
                    List<com.google.android.gms.tasks.Task<QuerySnapshot>> orderTasks = new ArrayList<>();
                    for (int i = 0; i < orderIdList.size(); i += 10) {
                        List<String> chunk = orderIdList.subList(i, Math.min(i + 10, orderIdList.size()));
                        orderTasks.add(db.collection(FirebaseCollections.ORDERS)
                                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                                .get());
                    }

                    com.google.android.gms.tasks.Tasks.whenAllSuccess(orderTasks).addOnSuccessListener(orderResults -> {
                        java.util.Map<String, String> orderStatusMap = new java.util.HashMap<>();
                        for (Object orderResult : orderResults) {
                            QuerySnapshot orderSnap = (QuerySnapshot) orderResult;
                            for (DocumentSnapshot doc : orderSnap.getDocuments()) {
                                String status = doc.getString("status");
                                orderStatusMap.put(doc.getId(), status != null ? status.toLowerCase() : "pending");
                            }
                        }

                        double totalRevenue = 0;
                        long ticketCount = 0;
                        for (com.example.vibetix.Models.OrderItem item : allItems) {
                            String status = orderStatusMap.get(item.getOrderId());
                            boolean isPaid = status != null &&
                                    (status.equals("completed") || status.equals("confirmed") || status.equals("paid"));
                            if (isPaid) {
                                long q = item.getQuantity();
                                ticketCount += q;
                                totalRevenue += item.getPricePerTicket() * q;
                            }
                        }

                        final long finalTicketCount = ticketCount;
                        final long finalRevenue = (long) totalRevenue;
                        if (tvStatRevenue != null) tvStatRevenue.setText(vndFmt.format(finalRevenue) + " ₫");
                        if (tvStatTickets != null) tvStatTickets.setText(String.valueOf(finalTicketCount));
                    });
                });

        // Check-ins hôm nay
        String todayStart = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date()) + "T00:00:00";
        db.collection(FirebaseCollections.USER_TICKETS)
                .whereEqualTo("event_id", eventId)
                .whereGreaterThanOrEqualTo("checked_in_at", todayStart)
                .get()
                .addOnSuccessListener(snap -> {
                    int count = snap != null ? snap.size() : 0;
                    if (tvStatCheckins != null) tvStatCheckins.setText(String.valueOf(count));
                });
    }
}
