package com.example.vibetix.Activities.Organizer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Adapters.Organizer.TicketSaleAdapter;
import com.example.vibetix.Adapters.Organizer.TicketTypeRevenueAdapter;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.OrderItem;
import com.example.vibetix.Models.Order;
import com.example.vibetix.Models.TicketType;
import com.example.vibetix.Models.UserTicket;
import com.example.vibetix.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EventRevenueDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";
    public static final String EXTRA_EVENT_TITLE = "EXTRA_EVENT_TITLE";
    public static final String EXTRA_EVENT_DATE = "EXTRA_EVENT_DATE";
    public static final String EXTRA_EVENT_POSTER = "EXTRA_EVENT_POSTER";

    private ProgressBar pbLoading;
    private View scrollBody, layoutEmpty;
    private TextView tvToolbarTitle, tvEventTitle, tvEventDate;
    private TextView tvTotalRevenue, tvTicketsSold, tvTotalOrders;
    private TextView layoutEmptyTickets;
    private RecyclerView rvTicketTypes, rvTickets;
    private BarChart revenueChart;

    private FirebaseFirestore db;
    private String eventId;

    private final List<TicketTypeRevenueAdapter.TicketTypeStat> ticketTypeStats = new ArrayList<>();
    private final List<UserTicket> ticketList = new ArrayList<>();
    private TicketTypeRevenueAdapter ticketTypeAdapter;
    private TicketSaleAdapter ticketAdapter;

    // Loaded data holders
    private final List<OrderItem> allOrderItems = new ArrayList<>();
    private final Map<String, String> orderStatusMap = new HashMap<>();
    private final Map<String, Date> orderDateMap = new HashMap<>();
    private final Map<String, String> orderUserIdMap = new HashMap<>();

    private final NumberFormat vndFmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_revenue_detail);

        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Không tìm thấy sự kiện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String title = getIntent().getStringExtra(EXTRA_EVENT_TITLE);
        String date = getIntent().getStringExtra(EXTRA_EVENT_DATE);

        bindViews();
        setupRecyclerViews();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (title != null) {
            tvEventTitle.setText(title);
            tvToolbarTitle.setText(title);
        }
        if (date != null) tvEventDate.setText(date);

        loadData();
    }

    private void bindViews() {
        pbLoading = findViewById(R.id.pbLoading);
        scrollBody = findViewById(R.id.scrollBody);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvEventDate = findViewById(R.id.tvEventDate);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTicketsSold = findViewById(R.id.tvTicketsSold);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        rvTicketTypes = findViewById(R.id.rvTicketTypes);
        rvTickets = findViewById(R.id.rvTickets);
        revenueChart = findViewById(R.id.revenueChart);
    }

    private void setupRecyclerViews() {
        ticketTypeAdapter = new TicketTypeRevenueAdapter(ticketTypeStats);
        rvTicketTypes.setLayoutManager(new LinearLayoutManager(this));
        rvTicketTypes.setNestedScrollingEnabled(false);
        rvTicketTypes.setAdapter(ticketTypeAdapter);

        ticketAdapter = new TicketSaleAdapter(ticketList, ticket -> {
            Intent intent = new Intent(this, TicketDetailActivity.class);
            intent.putExtra(TicketDetailActivity.EXTRA_TICKET_ID, ticket.getUserTicketId());
            intent.putExtra(TicketDetailActivity.EXTRA_EVENT_TITLE,
                    getIntent().getStringExtra(EXTRA_EVENT_TITLE));
            intent.putExtra(TicketDetailActivity.EXTRA_EVENT_DATE,
                    getIntent().getStringExtra(EXTRA_EVENT_DATE));
            intent.putExtra(TicketDetailActivity.EXTRA_EVENT_POSTER,
                    getIntent().getStringExtra(EXTRA_EVENT_POSTER));
            startActivity(intent);
        });
        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        rvTickets.setNestedScrollingEnabled(false);
        rvTickets.setAdapter(ticketAdapter);
    }

    private void loadData() {
        showLoading(true);

        // Parallel: load order_items + user_tickets + ticket_types
        Task<QuerySnapshot> orderItemsTask = db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereEqualTo("event_id", eventId).get();
        Task<QuerySnapshot> userTicketsTask = db.collection(FirebaseCollections.USER_TICKETS)
                .whereEqualTo("event_id", eventId).get();
        Task<QuerySnapshot> ticketTypesTask = db.collection(FirebaseCollections.TICKET_TYPES)
                .whereEqualTo("event_id", eventId).get();

        Tasks.whenAllSuccess(orderItemsTask, userTicketsTask, ticketTypesTask)
                .addOnSuccessListener(results -> {
                    // Parse order items
                    allOrderItems.clear();
                    Set<String> orderIdSet = new HashSet<>();
                    QuerySnapshot orderItemsSnap = (QuerySnapshot) results.get(0);
                    for (DocumentSnapshot doc : orderItemsSnap.getDocuments()) {
                        OrderItem oi = doc.toObject(OrderItem.class);
                        if (oi != null) {
                            if (oi.getOrderItemId() == null) oi.setOrderItemId(doc.getId());
                            allOrderItems.add(oi);
                            if (oi.getOrderId() != null) orderIdSet.add(oi.getOrderId());
                        }
                    }

                    // Parse user tickets
                    ticketList.clear();
                    QuerySnapshot userTicketsSnap = (QuerySnapshot) results.get(1);
                    List<String> ownerIds = new ArrayList<>();
                    for (DocumentSnapshot doc : userTicketsSnap.getDocuments()) {
                        UserTicket t = doc.toObject(UserTicket.class);
                        if (t != null) {
                            if (t.getUserTicketId() == null) t.setUserTicketId(doc.getId());
                            ticketList.add(t);
                            String uid = t.getOwnerId();
                            if (uid != null && !ownerIds.contains(uid)) ownerIds.add(uid);
                        }
                    }

                    // Parse ticket types
                    Map<String, TicketType> ticketTypeMap = new HashMap<>();
                    QuerySnapshot ticketTypesSnap = (QuerySnapshot) results.get(2);
                    for (DocumentSnapshot doc : ticketTypesSnap.getDocuments()) {
                        TicketType tt = doc.toObject(TicketType.class);
                        if (tt != null) {
                            if (tt.getTicketTypeId() == null) tt.setTicketTypeId(doc.getId());
                            ticketTypeMap.put(doc.getId(), tt);
                        }
                    }

                    // Enrich order items with ticket type names
                    for (OrderItem oi : allOrderItems) {
                        if (oi.getTicketTypeId() != null && ticketTypeMap.containsKey(oi.getTicketTypeId())) {
                            // stored for later use
                        }
                    }

                    if (orderIdSet.isEmpty()) {
                        // Enrich tickets with user info then display
                        enrichTicketsWithUsers(ownerIds, ticketTypeMap, new HashMap<>());
                        return;
                    }

                    // Fetch orders for status
                    List<String> orderIds = new ArrayList<>(orderIdSet);
                    List<Task<QuerySnapshot>> orderTasks = new ArrayList<>();
                    for (int i = 0; i < orderIds.size(); i += 10) {
                        List<String> chunk = orderIds.subList(i, Math.min(i + 10, orderIds.size()));
                        orderTasks.add(db.collection(FirebaseCollections.ORDERS)
                                .whereIn(FieldPath.documentId(), chunk).get());
                    }

                    Tasks.whenAllSuccess(orderTasks).addOnSuccessListener(orderResults -> {
                        orderStatusMap.clear();
                        orderDateMap.clear();
                        orderUserIdMap.clear();
                        for (Object r : orderResults) {
                            QuerySnapshot snap = (QuerySnapshot) r;
                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                Order o = doc.toObject(Order.class);
                                if (o != null) {
                                    String st = o.getStatusStr() != null ? o.getStatusStr().toLowerCase() : "pending";
                                    orderStatusMap.put(doc.getId(), st);
                                    if (o.getOrderDate() != null) {
                                        orderDateMap.put(doc.getId(), o.getOrderDate().toDate());
                                    }
                                    if (o.getUserId() != null) {
                                        orderUserIdMap.put(doc.getId(), o.getUserId());
                                    }
                                }
                            }
                        }

                        enrichTicketsWithUsers(ownerIds, ticketTypeMap, ticketTypeMap);
                        computeTicketTypeStats(ticketTypeMap);
                    }).addOnFailureListener(e -> {
                        enrichTicketsWithUsers(ownerIds, ticketTypeMap, ticketTypeMap);
                        computeTicketTypeStats(ticketTypeMap);
                    });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showEmpty(true);
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void computeTicketTypeStats(Map<String, TicketType> ticketTypeMap) {
        // Per ticket-type: sold qty and revenue (from paid orders only)
        Map<String, Long> soldByType = new HashMap<>();
        Map<String, Long> revByType = new HashMap<>();

        for (OrderItem oi : allOrderItems) {
            String st = orderStatusMap.get(oi.getOrderId());
            boolean cancelled = st != null && (st.equals("cancelled") || st.equals("refunded"));
            if (!cancelled && oi.getTicketTypeId() != null) {
                String ttId = oi.getTicketTypeId();
                soldByType.merge(ttId, oi.getQuantity(), Long::sum);
                revByType.merge(ttId, oi.getQuantity() * oi.getPricePerTicket(), Long::sum);
            }
        }

        ticketTypeStats.clear();
        for (Map.Entry<String, TicketType> entry : ticketTypeMap.entrySet()) {
            TicketType tt = entry.getValue();
            long sold = soldByType.getOrDefault(entry.getKey(), 0L);
            long rev = revByType.getOrDefault(entry.getKey(), 0L);
            ticketTypeStats.add(new TicketTypeRevenueAdapter.TicketTypeStat(
                    tt.getName(), (long) tt.getPrice(), sold, rev));
        }
        // Nếu không có ticket types trong map, derive từ order items
        if (ticketTypeStats.isEmpty() && !allOrderItems.isEmpty()) {
            Map<String, TicketTypeRevenueAdapter.TicketTypeStat> derived = new HashMap<>();
            for (OrderItem oi : allOrderItems) {
                String st = orderStatusMap.get(oi.getOrderId());
                boolean cancelled = st != null && (st.equals("cancelled") || st.equals("refunded"));
                if (!cancelled) {
                    String key = oi.getTicketTypeId() != null ? oi.getTicketTypeId() : "unknown";
                    TicketTypeRevenueAdapter.TicketTypeStat existing = derived.get(key);
                    if (existing == null) {
                        derived.put(key, new TicketTypeRevenueAdapter.TicketTypeStat(
                                "Loại vé", oi.getPricePerTicket(), oi.getQuantity(),
                                oi.getQuantity() * oi.getPricePerTicket()));
                    } else {
                        existing.soldQuantity += oi.getQuantity();
                        existing.revenue += oi.getQuantity() * oi.getPricePerTicket();
                    }
                }
            }
            ticketTypeStats.addAll(derived.values());
        }
    }

    private void enrichTicketsWithUsers(List<String> ownerIds,
                                        Map<String, TicketType> ticketTypeMap,
                                        Map<String, TicketType> ticketTypeMapForStats) {
        if (ownerIds.isEmpty()) {
            finalizeUI(ticketTypeMap);
            return;
        }

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (int i = 0; i < ownerIds.size(); i += 10) {
            List<String> chunk = ownerIds.subList(i, Math.min(i + 10, ownerIds.size()));
            tasks.add(db.collection(FirebaseCollections.USERS)
                    .whereIn(FieldPath.documentId(), chunk).get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            Map<String, String[]> userInfo = new HashMap<>(); // uid -> [name, email]
            for (Object r : results) {
                QuerySnapshot snap = (QuerySnapshot) r;
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    String name = doc.getString("full_name");
                    String email = doc.getString("email");
                    userInfo.put(doc.getId(), new String[]{name, email});
                }
            }
            for (UserTicket t : ticketList) {
                String[] info = userInfo.get(t.getOwnerId());
                if (info != null) {
                    t.setFullName(info[0]);
                    t.setEmail(info[1]);
                }
                // Resolve ticket type name via order_item
                // (orderItemId -> ticketTypeId -> name)
                // We'll do a quick lookup
                for (OrderItem oi : allOrderItems) {
                    if (oi.getOrderItemId() != null
                            && oi.getOrderItemId().equals(t.getOrderItemId())) {
                        if (oi.getTicketTypeId() != null) {
                            TicketType tt = ticketTypeMap.get(oi.getTicketTypeId());
                            if (tt != null) t.setTicketTypeName(tt.getName());
                        }
                        break;
                    }
                }
            }
            finalizeUI(ticketTypeMap);
        }).addOnFailureListener(e -> finalizeUI(ticketTypeMap));
    }

    private void finalizeUI(Map<String, TicketType> ticketTypeMap) {
        // Compute stats
        long totalRev = 0, totalTickets = 0;
        Set<String> uniqueOrders = new HashSet<>();
        for (OrderItem oi : allOrderItems) {
            String st = orderStatusMap.get(oi.getOrderId());
            boolean cancelled = st != null && (st.equals("cancelled") || st.equals("refunded"));
            if (!cancelled) {
                totalTickets += oi.getQuantity();
                totalRev += oi.getQuantity() * oi.getPricePerTicket();
            }
            if (oi.getOrderId() != null) uniqueOrders.add(oi.getOrderId());
        }

        long finalTotalRev = totalRev;
        long finalTotalTickets = totalTickets;
        int finalOrders = uniqueOrders.size();

        runOnUiThread(() -> {
            showLoading(false);
            tvTotalRevenue.setText(formatRevenue(finalTotalRev));
            tvTicketsSold.setText(formatCompact(finalTotalTickets));
            tvTotalOrders.setText(String.valueOf(finalOrders));

            if (allOrderItems.isEmpty() && ticketList.isEmpty()) {
                showEmpty(true);
            } else {
                showEmpty(false);
                ticketTypeAdapter.updateData(ticketTypeStats);
                ticketAdapter.updateData(ticketList);

                View emptyTickets = findViewById(R.id.layoutEmptyTickets);
                if (emptyTickets != null) {
                    emptyTickets.setVisibility(ticketList.isEmpty() ? View.VISIBLE : View.GONE);
                }

                setupRevenueChart();
            }
        });
    }

    private void setupRevenueChart() {
        Map<String, Long> revByDate = new HashMap<>();
        SimpleDateFormat sdfOut = new SimpleDateFormat("dd/MM", Locale.getDefault());

        for (OrderItem oi : allOrderItems) {
            String st = orderStatusMap.get(oi.getOrderId());
            boolean cancelled = st != null && (st.equals("cancelled") || st.equals("refunded"));
            if (!cancelled) {
                Date d = orderDateMap.get(oi.getOrderId());
                if (d != null) {
                    String dateStr = sdfOut.format(d);
                    revByDate.merge(dateStr, oi.getQuantity() * oi.getPricePerTicket(), Long::sum);
                }
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            String ds = sdfOut.format(cal.getTime());
            labels.add(ds);
            entries.add(new BarEntry(i, revByDate.getOrDefault(ds, 0L)));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Doanh thu (₫)");
        dataSet.setColor(Color.parseColor("#226CEB"));
        dataSet.setValueTextSize(8f);
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        revenueChart.setData(barData);
        revenueChart.getDescription().setEnabled(false);
        revenueChart.getLegend().setEnabled(false);

        XAxis xAxis = revenueChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(7);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);

        YAxis leftAxis = revenueChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextSize(9f);
        revenueChart.getAxisRight().setEnabled(false);
        revenueChart.animateY(600);
        revenueChart.invalidate();
    }

    private void showLoading(boolean show) {
        pbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            scrollBody.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmpty(boolean show) {
        layoutEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        scrollBody.setVisibility(show ? View.GONE : View.VISIBLE);
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
