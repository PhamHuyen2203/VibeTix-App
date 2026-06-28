package com.example.vibetix.Activities.Organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Adapters.Organizer.EventRevenueAdapter;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.OrderItem;
import com.example.vibetix.Models.Order;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class OrganizerRevenueActivity extends AppCompatActivity {

    public static final String EXTRA_ORGANIZER_ID = "EXTRA_ORGANIZER_ID";
    public static final String EXTRA_ORGANIZER_NAME = "EXTRA_ORGANIZER_NAME";

    private RecyclerView rvEvents;
    private ProgressBar pbLoading;
    private View layoutEmpty;
    private TextView tvHeaderTotalRevenue, tvHeaderTotalTickets, tvHeaderTotalEvents;
    private TextView tvOrgName;
    // Organizer info card views
    private ImageView ivOrgLogo;
    private TextView tvOrgBrandName, tvOrgVerifiedBadge, tvOrgDescription, tvOrgWebsite;

    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private EventRevenueAdapter adapter;
    private final List<EventRevenueAdapter.EventRevenueSummary> summaries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_revenue);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        setupRecyclerView();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadData();
    }

    private void bindViews() {
        rvEvents = findViewById(R.id.rvEvents);
        pbLoading = findViewById(R.id.pbLoading);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvHeaderTotalRevenue = findViewById(R.id.tvHeaderTotalRevenue);
        tvHeaderTotalTickets = findViewById(R.id.tvHeaderTotalTickets);
        tvHeaderTotalEvents = findViewById(R.id.tvHeaderTotalEvents);
        tvOrgName = findViewById(R.id.tvOrganizerName);
        ivOrgLogo = findViewById(R.id.ivOrgLogo);
        tvOrgBrandName = findViewById(R.id.tvOrgBrandName);
        tvOrgVerifiedBadge = findViewById(R.id.tvOrgVerifiedBadge);
        tvOrgDescription = findViewById(R.id.tvOrgDescription);
        tvOrgWebsite = findViewById(R.id.tvOrgWebsite);

        // Load organizer profile info
        String orgId = getIntent().getStringExtra(EXTRA_ORGANIZER_ID);
        if (orgId != null) loadOrganizerProfile(orgId);
    }

    private void loadOrganizerProfile(String orgId) {
        db.collection(FirebaseCollections.ORGANIZERS).document(orgId).get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) return;

                    String brandName = doc.getString("brand_name");
                    String description = doc.getString("description");
                    String website = doc.getString("website_url");
                    String logoUrl = doc.getString("logo_url");
                    Boolean isVerified = doc.getBoolean("is_verified");

                    if (tvOrgName != null)
                        tvOrgName.setText(brandName != null ? brandName : "Ban tổ chức");
                    if (tvOrgBrandName != null)
                        tvOrgBrandName.setText(brandName != null ? brandName : "Ban tổ chức");

                    if (tvOrgVerifiedBadge != null && Boolean.TRUE.equals(isVerified)) {
                        tvOrgVerifiedBadge.setVisibility(View.VISIBLE);
                    }

                    if (tvOrgDescription != null && description != null && !description.isEmpty()) {
                        tvOrgDescription.setText(description);
                        tvOrgDescription.setVisibility(View.VISIBLE);
                    }

                    if (tvOrgWebsite != null && website != null && !website.isEmpty()) {
                        tvOrgWebsite.setText(website);
                        tvOrgWebsite.setVisibility(View.VISIBLE);
                    }

                    if (ivOrgLogo != null && logoUrl != null && !logoUrl.isEmpty()) {
                        Glide.with(this).load(logoUrl).circleCrop()
                                .placeholder(R.drawable.bg_avatar_circle).into(ivOrgLogo);
                    }
                });
    }

    private void setupRecyclerView() {
        adapter = new EventRevenueAdapter(summaries, summary -> {
            Intent intent = new Intent(this, EventRevenueDetailActivity.class);
            intent.putExtra(EventRevenueDetailActivity.EXTRA_EVENT_ID, summary.eventId);
            intent.putExtra(EventRevenueDetailActivity.EXTRA_EVENT_TITLE, summary.title);
            intent.putExtra(EventRevenueDetailActivity.EXTRA_EVENT_DATE, summary.date);
            intent.putExtra(EventRevenueDetailActivity.EXTRA_EVENT_POSTER, summary.posterUrl);
            startActivity(intent);
        });
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);
    }

    private void loadData() {
        // If launched from organizer profile list, we already know the organizer ID
        String passedOrgId = getIntent().getStringExtra(EXTRA_ORGANIZER_ID);
        if (passedOrgId != null && !passedOrgId.isEmpty()) {
            showLoading(true);
            loadEventsByOrgId(passedOrgId, null);
            return;
        }

        // Fallback: resolve from session / current user
        String userId = sessionManager.getUserDetails() != null
                ? sessionManager.getUserDetails().getUserId() : null;
        if (userId == null) {
            com.google.firebase.auth.FirebaseUser fu =
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (fu != null) userId = fu.getUid();
        }
        if (userId == null) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);
        final String finalUserId = userId;
        final String activeOrgId = sessionManager.getActiveOrganizerId();

        if (activeOrgId != null && !activeOrgId.isEmpty()) {
            loadEventsByOrgId(activeOrgId, finalUserId);
        } else {
            loadEventsByUserId(finalUserId);
        }
    }

    /** Query events WHERE organizer_id == orgId */
    private void loadEventsByOrgId(String orgId, String fallbackUserId) {
        db.collection(FirebaseCollections.EVENTS)
                .whereEqualTo("organizer_id", orgId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        List<String> ids = new ArrayList<>();
                        for (DocumentSnapshot d : snap.getDocuments()) ids.add(d.getId());
                        fetchEventsById(ids);
                    } else if (fallbackUserId != null) {
                        loadEventsByUserId(fallbackUserId);
                    } else {
                        showLoading(false);
                        showEmpty(true);
                        updateHeader(0, 0, 0);
                    }
                })
                .addOnFailureListener(e -> {
                    if (fallbackUserId != null) loadEventsByUserId(fallbackUserId);
                    else { showLoading(false); showEmpty(true); }
                });
    }

    /** Chiến lược 3 (fallback cuối): query organizers WHERE user_id == userId, rồi dùng organizer_id */
    private void loadEventsByUserId(String userId) {
        db.collection(FirebaseCollections.ORGANIZERS)
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(orgSnap -> {
                    if (orgSnap == null || orgSnap.isEmpty()) {
                        showLoading(false);
                        showEmpty(true);
                        updateHeader(0, 0, 0);
                        return;
                    }
                    List<String> orgIds = new ArrayList<>();
                    for (DocumentSnapshot d : orgSnap.getDocuments()) orgIds.add(d.getId());
                    loadEventsByOrgIds(orgIds);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmpty(true);
                });
    }

    private void loadEventsByOrgIds(List<String> orgIds) {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (int i = 0; i < orgIds.size(); i += 10) {
            List<String> chunk = orgIds.subList(i, Math.min(i + 10, orgIds.size()));
            tasks.add(db.collection(FirebaseCollections.EVENTS)
                    .whereIn("organizer_id", chunk).get());
        }
        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            List<String> ids = new ArrayList<>();
            for (Object r : results) {
                QuerySnapshot snap = (QuerySnapshot) r;
                for (DocumentSnapshot d : snap.getDocuments()) ids.add(d.getId());
            }
            if (ids.isEmpty()) {
                showLoading(false);
                showEmpty(true);
                updateHeader(0, 0, 0);
            } else {
                fetchEventsById(ids);
            }
        }).addOnFailureListener(e -> {
            showLoading(false);
            showEmpty(true);
        });
    }

    /** Fetch events by document IDs (chunked by 10) then build summaries */
    private void fetchEventsById(List<String> eventIds) {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (int i = 0; i < eventIds.size(); i += 10) {
            List<String> chunk = eventIds.subList(i, Math.min(i + 10, eventIds.size()));
            tasks.add(db.collection(FirebaseCollections.EVENTS)
                    .whereIn(FieldPath.documentId(), chunk).get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            summaries.clear();
            List<String> foundIds = new ArrayList<>();

            for (Object result : results) {
                QuerySnapshot snap = (QuerySnapshot) result;
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Event e = doc.toObject(Event.class);
                    if (e != null) {
                        if (e.getEventId() == null) e.setEventId(doc.getId());
                        String dateStr = e.getStartTime() != null ? e.getStartTime() : "—";
                        EventRevenueAdapter.EventRevenueSummary s =
                                new EventRevenueAdapter.EventRevenueSummary(
                                        e.getEventId(), e.getTitle(),
                                        e.getPosterUrl(), dateStr, e.getStatusStr());
                        summaries.add(s);
                        foundIds.add(e.getEventId());
                    }
                }
            }

            if (summaries.isEmpty()) {
                showLoading(false);
                showEmpty(true);
                updateHeader(0, 0, 0);
                return;
            }

            loadRevenue(foundIds, summaries);
        }).addOnFailureListener(e -> {
            showLoading(false);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showEmpty(true);
        });
    }

    private void loadRevenue(List<String> eventIds,
                             List<EventRevenueAdapter.EventRevenueSummary> summaries) {
        // Step 1: query order_items for all events (chunked by 10)
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (int i = 0; i < eventIds.size(); i += 10) {
            List<String> chunk = eventIds.subList(i, Math.min(i + 10, eventIds.size()));
            tasks.add(db.collection(FirebaseCollections.ORDER_ITEMS)
                    .whereIn("event_id", chunk).get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            // Map eventId -> list of order items
            Map<String, List<OrderItem>> itemsByEvent = new HashMap<>();
            Set<String> orderIdSet = new HashSet<>();

            for (Object result : results) {
                QuerySnapshot snap = (QuerySnapshot) result;
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    OrderItem item = doc.toObject(OrderItem.class);
                    if (item == null) continue;
                    if (item.getOrderItemId() == null) item.setOrderItemId(doc.getId());
                    String eid = item.getEventId();
                    if (eid != null) {
                        itemsByEvent.computeIfAbsent(eid, k -> new ArrayList<>()).add(item);
                        if (item.getOrderId() != null) orderIdSet.add(item.getOrderId());
                    }
                }
            }

            if (orderIdSet.isEmpty()) {
                finalizeWithNoRevenue(summaries);
                return;
            }

            // Step 2: fetch orders to get status
            List<String> orderIds = new ArrayList<>(orderIdSet);
            List<Task<QuerySnapshot>> orderTasks = new ArrayList<>();
            for (int i = 0; i < orderIds.size(); i += 10) {
                List<String> chunk = orderIds.subList(i, Math.min(i + 10, orderIds.size()));
                orderTasks.add(db.collection(FirebaseCollections.ORDERS)
                        .whereIn(FieldPath.documentId(), chunk).get());
            }

            Tasks.whenAllSuccess(orderTasks).addOnSuccessListener(orderResults -> {
                Map<String, String> orderStatus = new HashMap<>();
                Map<String, Set<String>> ordersByEvent = new HashMap<>();

                for (Object r : orderResults) {
                    QuerySnapshot snap = (QuerySnapshot) r;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String st = doc.getString("status");
                        orderStatus.put(doc.getId(), st != null ? st.toLowerCase() : "pending");
                    }
                }

                // Build ordersByEvent for unique order count
                for (Map.Entry<String, List<OrderItem>> entry : itemsByEvent.entrySet()) {
                    Set<String> ids = new HashSet<>();
                    for (OrderItem oi : entry.getValue()) {
                        if (oi.getOrderId() != null) ids.add(oi.getOrderId());
                    }
                    ordersByEvent.put(entry.getKey(), ids);
                }

                // Calculate per-event revenue
                // Tính TẤT CẢ đơn (kể cả pending) để hiển thị doanh thu kỳ vọng
                // Đơn cancelled/refunded thì bỏ qua
                long totalRevenue = 0, totalTickets = 0;
                for (EventRevenueAdapter.EventRevenueSummary s : summaries) {
                    List<OrderItem> items = itemsByEvent.get(s.eventId);
                    if (items == null) continue;
                    long rev = 0, tickets = 0;
                    for (OrderItem oi : items) {
                        String st = orderStatus.get(oi.getOrderId());
                        // Bỏ qua đơn đã hủy hoặc hoàn tiền
                        boolean cancelled = st != null && (st.equals("cancelled") || st.equals("refunded"));
                        if (!cancelled) {
                            tickets += oi.getQuantity();
                            rev += oi.getQuantity() * oi.getPricePerTicket();
                        }
                    }
                    s.ticketsSold = tickets;
                    s.revenue = rev;
                    Set<String> oids = ordersByEvent.get(s.eventId);
                    s.totalOrders = oids != null ? oids.size() : 0;
                    totalRevenue += rev;
                    totalTickets += tickets;
                }

                long finalTotalRevenue = totalRevenue;
                long finalTotalTickets = totalTickets;
                runOnUiThread(() -> {
                    showLoading(false);
                    adapter.updateData(summaries);
                    if (summaries.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                    }
                    updateHeader(finalTotalRevenue, finalTotalTickets, summaries.size());
                });
            }).addOnFailureListener(e -> {
                finalizeWithNoRevenue(summaries);
            });
        }).addOnFailureListener(e -> {
            finalizeWithNoRevenue(summaries);
        });
    }

    private void finalizeWithNoRevenue(List<EventRevenueAdapter.EventRevenueSummary> summaries) {
        runOnUiThread(() -> {
            showLoading(false);
            adapter.updateData(summaries);
            showEmpty(summaries.isEmpty());
            updateHeader(0, 0, summaries.size());
        });
    }

    private void updateHeader(long revenue, long tickets, int events) {
        tvHeaderTotalRevenue.setText(formatRevenue(revenue));
        tvHeaderTotalTickets.setText(formatCompact(tickets));
        tvHeaderTotalEvents.setText(String.valueOf(events));
    }

    private void showLoading(boolean show) {
        pbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmpty(boolean show) {
        layoutEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private String formatCompact(long n) {
        if (n >= 1_000_000) return String.format(Locale.getDefault(), "%.1fM", n / 1_000_000.0);
        if (n >= 1_000) return String.format(Locale.getDefault(), "%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    private String formatRevenue(long amount) {
        if (amount == 0) return "0đ";
        if (amount >= 1_000_000_000) return String.format(Locale.getDefault(), "%.1fB₫", amount / 1_000_000_000.0);
        if (amount >= 1_000_000) return String.format(Locale.getDefault(), "%.1fM₫", amount / 1_000_000.0);
        if (amount >= 1_000) return String.format(Locale.getDefault(), "%.0fK₫", amount / 1_000.0);
        return new DecimalFormat("#,###").format(amount) + "đ";
    }
}
