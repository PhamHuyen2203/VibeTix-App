package com.example.vibetix.Activities.Organizer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.vibetix.Models.Order;
import com.example.vibetix.Models.UserTicket;
import com.example.vibetix.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.example.vibetix.Models.OrderItem;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.QuerySnapshot;
import android.view.View;
import android.widget.ProgressBar;

public class EventAnalyticsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";
    private String eventId;
    
    private TextView tvTotalRevenue, tvTotalTickets;
    private LineChart lineChart;
    private Button btnExportCSV;
    private ImageView btnBack;
    private View pbLoading;
    
    private FirebaseFirestore db;
    private List<UserTicket> cachedTickets = new ArrayList<>();
    
    // Maps to enrich CSV
    private Map<String, OrderItem> orderItemMap = new HashMap<>();
    private Map<String, Order> orderMap = new HashMap<>();
    private Map<String, String> ticketTypeNameMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_analytics);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalTickets = findViewById(R.id.tvTotalTickets);
        lineChart = findViewById(R.id.lineChart);
        btnExportCSV = findViewById(R.id.btnExportCSV);
        btnBack = findViewById(R.id.btnBack);
        pbLoading = findViewById(R.id.pbLoading);

        btnBack.setOnClickListener(v -> finish());
        btnExportCSV.setOnClickListener(v -> exportToCSV());

        setupChart();
        loadAnalyticsData();
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.clr_text_secondary, null));
        xAxis.setAxisLineColor(getResources().getColor(R.color.clr_grey_1, null));
        
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setTextColor(getResources().getColor(R.color.clr_text_secondary, null));
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(getResources().getColor(R.color.clr_grey_1, null));
        lineChart.getAxisLeft().enableGridDashedLine(10f, 10f, 0f);
        lineChart.getAxisLeft().setAxisLineColor(getResources().getColor(android.R.color.transparent, null));
    }

    private void loadAnalyticsData() {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);

        // Load Tickets for Sold/Check-in counts and CSV
        Task<QuerySnapshot> ticketsTask = db.collection(FirebaseCollections.USER_TICKETS)
                .whereEqualTo("event_id", eventId)
                .get();

        // Load OrderItems to get Orders for Revenue and Chart
        Task<QuerySnapshot> orderItemsTask = db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereEqualTo("event_id", eventId)
                .get();
                
        // Load Ticket Types for CSV enrichment
        Task<QuerySnapshot> ticketTypesTask = db.collection(FirebaseCollections.TICKET_TYPES)
                .whereEqualTo("event_id", eventId)
                .get();

        Tasks.whenAllSuccess(ticketsTask, orderItemsTask, ticketTypesTask).addOnSuccessListener(results -> {
            // Process Tickets
            QuerySnapshot ticketsSnap = (QuerySnapshot) results.get(0);
            cachedTickets.clear();
            int totalSold = 0;
            int totalCheckedIn = 0;
            
            for (DocumentSnapshot doc : ticketsSnap) {
                UserTicket ticket = doc.toObject(UserTicket.class);
                if (ticket != null) {
                    cachedTickets.add(ticket);
                    totalSold++;
                    if (UserTicket.Status.USED.equals(ticket.getStatus())) {
                        totalCheckedIn++;
                    }
                }
            }
            tvTotalTickets.setText(totalCheckedIn + " / " + totalSold);
            
            // Collect User IDs from tickets for CSV export enrichment
            List<String> userIds = new ArrayList<>();
            for (UserTicket t : cachedTickets) {
                if (t.getOwnerId() != null && !userIds.contains(t.getOwnerId())) {
                    userIds.add(t.getOwnerId());
                }
            }

            // Fetch users if needed (we do this asynchronously, it won't block chart drawing)
            if (!userIds.isEmpty()) {
                enrichTicketsWithUserInfo(userIds);
            }

            // Process Orders
            QuerySnapshot itemsSnap = (QuerySnapshot) results.get(1);
            List<OrderItem> allItems = new ArrayList<>();
            Set<String> orderIdSet = new HashSet<>();
            orderItemMap.clear();
            for (DocumentSnapshot doc : itemsSnap) {
                OrderItem item = doc.toObject(OrderItem.class);
                if (item != null) {
                    if (item.getOrderItemId() == null) item.setOrderItemId(doc.getId());
                    allItems.add(item);
                    orderItemMap.put(item.getOrderItemId(), item);
                    if (item.getOrderId() != null) orderIdSet.add(item.getOrderId());
                }
            }
            
            // Process Ticket Types
            QuerySnapshot typesSnap = (QuerySnapshot) results.get(2);
            ticketTypeNameMap.clear();
            for (DocumentSnapshot doc : typesSnap) {
                String name = doc.getString("name");
                if (name != null) ticketTypeNameMap.put(doc.getId(), name);
            }
            
            if (orderIdSet.isEmpty()) {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                tvTotalRevenue.setText("0 đ");
                drawChart(new TreeMap<>());
                return;
            }

            List<String> orderIds = new ArrayList<>(orderIdSet);
            List<Task<QuerySnapshot>> orderTasks = new ArrayList<>();
            for (int i = 0; i < orderIds.size(); i += 10) {
                List<String> chunk = orderIds.subList(i, Math.min(orderIds.size(), i + 10));
                orderTasks.add(db.collection(FirebaseCollections.ORDERS)
                        .whereIn(FieldPath.documentId(), chunk).get());
            }
            
            Tasks.whenAllSuccess(orderTasks).addOnSuccessListener(orderResults -> {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                
                orderMap.clear();
                for (Object res : orderResults) {
                    QuerySnapshot osnap = (QuerySnapshot) res;
                    for (DocumentSnapshot doc : osnap) {
                        Order o = doc.toObject(Order.class);
                        if (o != null) {
                            if (o.getOrderId() == null) o.setOrderId(doc.getId());
                            orderMap.put(doc.getId(), o);
                        }
                    }
                }
                
                double totalRevenue = 0;
                Map<String, Double> revenueByDate = new TreeMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
                
                for (OrderItem item : allItems) {
                    Order o = orderMap.get(item.getOrderId());
                    if (o != null && o.getStatusStr() != null) {
                        String status = o.getStatusStr().toLowerCase();
                        if (status.equals("completed") || status.equals("confirmed") || status.equals("paid")) {
                            double amount = item.getQuantity() * item.getPricePerTicket();
                            totalRevenue += amount;
                            
                            if (o.getOrderDate() != null) {
                                Date date = o.getOrderDate().toDate();
                                if (date != null) {
                                    String dateStr = sdf.format(date);
                                    revenueByDate.put(dateStr, revenueByDate.getOrDefault(dateStr, 0.0) + amount);
                                }
                            }
                        }
                    }
                }
                tvTotalRevenue.setText(String.format(Locale.getDefault(), "%,.0f ₫", totalRevenue));
                drawChart(revenueByDate);
            }).addOnFailureListener(e -> {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                Toast.makeText(this, getString(R.string.err_load_orders), Toast.LENGTH_SHORT).show();
            });
            
        }).addOnFailureListener(e -> {
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
            Toast.makeText(this, getString(R.string.err_load_analytics), Toast.LENGTH_SHORT).show();
        });
    }

    private void enrichTicketsWithUserInfo(List<String> userIds) {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i += 10) {
            List<String> chunk = userIds.subList(i, Math.min(i + 10, userIds.size()));
            tasks.add(db.collection(FirebaseCollections.USERS)
                    .whereIn(FieldPath.documentId(), chunk)
                    .get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            Map<String, com.example.vibetix.Models.User> userMap = new HashMap<>();
            for (Object result : results) {
                QuerySnapshot snap = (QuerySnapshot) result;
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    com.example.vibetix.Models.User u = doc.toObject(com.example.vibetix.Models.User.class);
                    if (u != null) {
                        userMap.put(doc.getId(), u);
                    }
                }
            }
            
            for (UserTicket t : cachedTickets) {
                com.example.vibetix.Models.User u = userMap.get(t.getOwnerId());
                if (u != null) {
                    t.setFullName(u.getFullName());
                    t.setEmail(u.getEmail());
                }
            }
        });
    }

    private void drawChart(Map<String, Double> dataMap) {
        if (dataMap.isEmpty()) return;

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Double> entry : dataMap.entrySet()) {
            entries.add(new Entry(index, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Doanh thu");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve
        dataSet.setCubicIntensity(0.2f);
        dataSet.setColor(getResources().getColor(R.color.clr_primary, null));
        dataSet.setCircleColor(getResources().getColor(R.color.clr_primary, null));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(getResources().getColor(R.color.clr_bg_white, null));
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(getResources().getColor(R.color.clr_text_secondary, null));
        
        // Fill background
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_chart_fill));

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        lineChart.animateX(1000); // Add animation
        lineChart.invalidate();
    }

    private void exportToCSV() {
        if (cachedTickets.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_no_tickets_to_export), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File exportDir = new File(getCacheDir(), "exports");
            if (!exportDir.exists()) exportDir.mkdirs();
            
            File file = new File(exportDir, "vibetix_tickets_" + eventId + ".csv");
            FileWriter writer = new FileWriter(file);
            
            // Header
            writer.append(getString(R.string.csv_header));
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            
            // Data
            for (UserTicket ticket : cachedTickets) {
                String code = ticket.getTicketCode() != null ? ticket.getTicketCode() : "";
                String status = ticket.getStatusStr() != null ? ticket.getStatusStr() : "";
                String owner = ticket.getOwnerId() != null ? ticket.getOwnerId() : "";
                String name = ticket.getFullName() != null ? ticket.getFullName().replace(",", " ") : "";
                String email = ticket.getEmail() != null ? ticket.getEmail() : "";
                
                // Enrich data
                String typeName = "N/A";
                String priceStr = "0";
                String dateStr = "N/A";
                
                if (ticket.getOrderItemId() != null) {
                    OrderItem item = orderItemMap.get(ticket.getOrderItemId());
                    if (item != null) {
                        priceStr = String.valueOf(item.getPricePerTicket());
                        if (item.getTicketTypeId() != null && ticketTypeNameMap.containsKey(item.getTicketTypeId())) {
                            typeName = ticketTypeNameMap.get(item.getTicketTypeId()).replace(",", " ");
                        }
                        if (item.getOrderId() != null) {
                            Order o = orderMap.get(item.getOrderId());
                            if (o != null && o.getOrderDate() != null) {
                                Date d = o.getOrderDate().toDate();
                                if (d != null) dateStr = dateFormat.format(d);
                            }
                        }
                    }
                }
                
                writer.append(code).append(",")
                      .append(typeName).append(",")
                      .append(priceStr).append(",")
                      .append(dateStr).append(",")
                      .append(status).append(",")
                      .append(owner).append(",")
                      .append(email).append(",")
                      .append(name).append(",")
                      .append(eventId).append("\n");
            }
            writer.flush();
            writer.close();
            
            shareCSVFile(file);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.err_export_csv), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCSVFile(File file) {
        // Cần cấu hình FileProvider trong AndroidManifest.xml
        Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
        
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        startActivity(Intent.createChooser(intent, getString(R.string.share_report)));
    }
}
