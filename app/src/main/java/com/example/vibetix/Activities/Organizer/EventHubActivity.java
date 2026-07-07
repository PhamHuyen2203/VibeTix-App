package com.example.vibetix.Activities.Organizer;

import android.content.Context;
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
import com.example.vibetix.Utils.LocaleHelper;
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

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

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
    private String eventStatus = ""; // trạng thái sự kiện, dùng để ẩn chức năng không phù hợp
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
            Toast.makeText(this, getString(R.string.str_event_not_found_label), Toast.LENGTH_SHORT).show();
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
        // Feature grid sẽ được dựng sau khi load xong thông tin sự kiện
        loadEventDetails();
        loadStats();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (eventId != null && db != null) {
            loadStats();
        }
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
            Toast.makeText(this, getString(R.string.str_toast_org_profile_coming_soon), Toast.LENGTH_SHORT).show();
            return true;
        }
        if (item.getItemId() == 3) {
            confirmCancelEvent();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmCancelEvent() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Huỷ sự kiện")
            .setMessage("Bạn có chắc chắn muốn huỷ sự kiện này? Hành động này không thể hoàn tác và sẽ gửi thông báo đến tất cả khách hàng đã mua vé.")
            .setPositiveButton("Huỷ sự kiện", (dialog, which) -> {
                db.collection(FirebaseCollections.EVENTS).document(eventId)
                    .update("status", "cancelled")
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, getString(R.string.str_toast_event_cancelled_ok), Toast.LENGTH_SHORT).show();
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
        boolean isCancelled = "cancelled".equalsIgnoreCase(eventStatus);

        // Ẩn stats với check_in_staff hoặc khi sự kiện bị huỷ
        if (cardStats != null) {
            cardStats.setVisibility((!isOwner && !isManager) ? View.GONE : View.VISIBLE);
        }

        applyRoleBadge();

        List<FeatureAdapter.FeatureItem> features = buildFeatureList(isOwner, isManager, isCancelled);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        rvHubFeatures.setLayoutManager(gridLayoutManager);
        rvHubFeatures.setAdapter(new FeatureAdapter(this, features));
        rvHubFeatures.setNestedScrollingEnabled(false);
    }

    private List<FeatureAdapter.FeatureItem> buildFeatureList(boolean isOwner, boolean isManager, boolean isCancelled) {
        List<FeatureAdapter.FeatureItem> list = new ArrayList<>();

        // Khi sự kiện bị Cancelled: chỉ cho phép xem thông tin, không cho thực hiện các chức năng vận hành
        if (isCancelled) {
            list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_trending_up, "Báo cáo doanh thu", isOwner || isManager, false,
                () -> {
                    String title = tvEventTitle != null ? tvEventTitle.getText().toString() : "Sự kiện";
                    String date  = tvEventDate  != null ? tvEventDate.getText().toString()  : "—";
                    Intent i = new Intent(this, EventRevenueDetailActivity.class);
                    i.putExtra(EventRevenueDetailActivity.EXTRA_EVENT_ID,    eventId);
                    i.putExtra(EventRevenueDetailActivity.EXTRA_EVENT_TITLE, title);
                    i.putExtra(EventRevenueDetailActivity.EXTRA_EVENT_DATE,  date);
                    startActivity(i);
                }));
            list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_role_person, "D.S Khách", true, false,
                () -> {
                    Intent i = new Intent(this, AttendeesActivity.class);
                    i.putExtra(AttendeesActivity.EXTRA_EVENT_ID, eventId);
                    startActivity(i);
                }));
            list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_trending_up, "Đơn hàng", isOwner || isManager, false,
                () -> {
                    Intent i = new Intent(this, OrderManagementActivity.class);
                    i.putExtra("EXTRA_EVENT_ID", eventId);
                    startActivity(i);
                }));
            return list; // Trả sớm, không hiện QR Scan, Nhân sự, Thông báo mới
        }

        // Sự kiện bình thường — ưu tiên cao: 2 tính năng real-time nổi bật (isHighlighted=true)
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
                    i.putExtra(TicketTypeManagementActivity.EXTRA_EVENT_ID,    eventId);
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

        list.add(new FeatureAdapter.FeatureItem(
                R.drawable.ic_trending_up, "Báo cáo doanh thu", isOwner || isManager, false,
                () -> {
                    String title = tvEventTitle != null ? tvEventTitle.getText().toString() : "Sự kiện";
                    String date  = tvEventDate  != null ? tvEventDate.getText().toString()  : "—";
                    Intent i = new Intent(this, EventRevenueDetailActivity.class);
                    i.putExtra(EventRevenueDetailActivity.EXTRA_EVENT_ID,    eventId);
                    i.putExtra(EventRevenueDetailActivity.EXTRA_EVENT_TITLE, title);
                    i.putExtra(EventRevenueDetailActivity.EXTRA_EVENT_DATE,  date);
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

                    // Lưu status và dựng feature grid ngay sau khi có dữ liệu
                    eventStatus = event.getStatusStr() != null ? event.getStatusStr() : "";
                    setupFeatureGrid();

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
                                            String name    = venueDoc.getString("name");
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
                        Toast.makeText(this, getString(R.string.str_toast_cannot_load_event_info), Toast.LENGTH_SHORT).show());
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

        com.example.vibetix.Firebase.FirestoreHelper.calculateEventStats(eventId, (totalTickets, totalRevenue) -> {
            if (tvStatRevenue != null) tvStatRevenue.setText(getString(R.string.dash_price_vnd, vndFmt.format(totalRevenue)));
            if (tvStatTickets != null) tvStatTickets.setText(String.valueOf(totalTickets));
        });

        // Check-ins hôm nay
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        com.google.firebase.Timestamp todayStart = new com.google.firebase.Timestamp(cal.getTime());

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
