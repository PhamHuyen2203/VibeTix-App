package com.example.vibetix.Activities.Organizer;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vibetix.Adapters.Organizer.OrganizerOrderAdapter;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Order;
import com.example.vibetix.Models.OrderItem;
import com.example.vibetix.Models.TicketType;
import com.example.vibetix.R;
import com.example.vibetix.Utils.NotificationTriggerManager;
import com.example.vibetix.databinding.ActivityOrderManagementBinding;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
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

public class OrderManagementActivity extends AppCompatActivity {

    private ActivityOrderManagementBinding binding;

    private FirebaseFirestore db;
    private OrganizerOrderAdapter adapter;
    private final List<OrderItem> allOrderItems = new ArrayList<>();

    private final NumberFormat vndFmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        setSupportActionBar(binding.toolbarOrders);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbarOrders.setNavigationOnClickListener(v -> finish());

        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrganizerOrderAdapter(new ArrayList<>());
        binding.rvOrders.setAdapter(adapter);

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    binding.layoutOrdersTab.setVisibility(View.VISIBLE);
                    binding.svRevenue.setVisibility(View.GONE);
                    if (!allOrderItems.isEmpty()) {
                        filterOrders();
                    } else {
                        showEmptyState();
                    }
                } else {
                    binding.layoutOrdersTab.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(View.GONE);
                    binding.svRevenue.setVisibility(View.VISIBLE);
                    setupRevenueChart();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Search listener
        binding.etSearchOrder.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                filterOrders();
            }
        });

        // Filter listener
        binding.cgOrderStatus.setOnCheckedChangeListener((group, checkedId) -> {
            filterOrders();
        });

        adapter.setOnItemClickListener(this::showOrderDetails);

        loadOrders();
    }

    private void loadOrders() {
        String eventId = getIntent().getStringExtra("EXTRA_EVENT_ID");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Không có sự kiện được chọn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.pbLoading.setVisibility(View.VISIBLE);
        binding.layoutOrdersTab.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);

        db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(orderItemsSnap -> {
                    allOrderItems.clear();
                    if (orderItemsSnap == null || orderItemsSnap.isEmpty()) {
                        binding.pbLoading.setVisibility(View.GONE);
                        updateStatsHeader();
                        showEmptyState();
                        return;
                    }

                    for (DocumentSnapshot doc : orderItemsSnap) {
                        OrderItem item = doc.toObject(OrderItem.class);
                        if (item != null) {
                            if (item.getOrderItemId() == null) item.setOrderItemId(doc.getId());
                            allOrderItems.add(item);
                        }
                    }

                    Set<String> orderIdSet = new HashSet<>();
                    for (OrderItem item : allOrderItems) {
                        if (item.getOrderId() != null) orderIdSet.add(item.getOrderId());
                    }
                    List<String> orderIds = new ArrayList<>(orderIdSet);
                    if (orderIds.isEmpty()) {
                        binding.pbLoading.setVisibility(View.GONE);
                        updateStatsHeader();
                        showEmptyState();
                        return;
                    }

                    List<Task<QuerySnapshot>> tasks = new ArrayList<>();
                    for (int i = 0; i < orderIds.size(); i += 10) {
                        List<String> chunk = orderIds.subList(i, Math.min(orderIds.size(), i + 10));
                        tasks.add(db.collection(FirebaseCollections.ORDERS)
                                .whereIn(FieldPath.documentId(), chunk).get());
                    }

                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                        Map<String, Order> orderMap = new HashMap<>();
                        for (Object result : results) {
                            QuerySnapshot snap = (QuerySnapshot) result;
                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                Order o = doc.toObject(Order.class);
                                if (o != null) {
                                    if (o.getOrderId() == null) o.setOrderId(doc.getId());
                                    orderMap.put(o.getOrderId(), o);
                                }
                            }
                        }

                        db.collection(FirebaseCollections.TICKET_TYPES)
                                .whereEqualTo("event_id", eventId)
                                .get()
                                .addOnSuccessListener(ticketTypesSnap -> {
                                    Map<String, String> ticketTypeMap = new HashMap<>();
                                    if (ticketTypesSnap != null) {
                                        for (DocumentSnapshot doc : ticketTypesSnap) {
                                            TicketType tt = doc.toObject(TicketType.class);
                                            if (tt != null) {
                                                ticketTypeMap.put(doc.getId(), tt.getName());
                                            }
                                        }
                                    }

                                    for (OrderItem item : allOrderItems) {
                                        item.setParentOrder(orderMap.get(item.getOrderId()));
                                        item.setTicketTypeName(ticketTypeMap.get(item.getTicketTypeId()));
                                    }

                                    allOrderItems.sort((i1, i2) -> {
                                        Order o1 = i1.getParentOrder();
                                        Order o2 = i2.getParentOrder();
                                        if (o1 == null || o2 == null || o1.getOrderDate() == null || o2.getOrderDate() == null) return 0;
                                        return o2.getOrderDate().compareTo(o1.getOrderDate());
                                    });

                                    binding.pbLoading.setVisibility(View.GONE);
                                    updateStatsHeader();

                                    if (allOrderItems.isEmpty()) {
                                        showEmptyState();
                                    } else {
                                        binding.layoutOrdersTab.setVisibility(View.VISIBLE);
                                        filterOrders();
                                    }

                                    if (binding.tabLayout.getSelectedTabPosition() == 1) {
                                        setupRevenueChart();
                                    }
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void updateStatsHeader() {
        // Đếm số đơn hàng unique
        Set<String> uniqueOrders = new HashSet<>();
        long totalTickets = 0;
        long totalRevenue = 0;

        for (OrderItem item : allOrderItems) {
            if (item.getOrderId() != null) uniqueOrders.add(item.getOrderId());
            Order o = item.getParentOrder();
            boolean isPaid = o != null && o.getStatusStr() != null &&
                    (o.getStatusStr().equalsIgnoreCase("completed") ||
                     o.getStatusStr().equalsIgnoreCase("confirmed") ||
                     o.getStatusStr().equalsIgnoreCase("paid"));
            if (isPaid) {
                totalTickets += item.getQuantity();
                totalRevenue += item.getQuantity() * item.getPricePerTicket();
            }
        }

        binding.tvTotalOrders.setText(String.valueOf(uniqueOrders.size()));
        binding.tvTotalTicketsSold.setText(String.valueOf(totalTickets));
        binding.tvTotalRevenue.setText(vndFmt.format((long) totalRevenue) + " ₫");
    }

    private void filterOrders() {
        String query = binding.etSearchOrder.getText() != null ? binding.etSearchOrder.getText().toString().toLowerCase().trim() : "";
        int selectedChipId = binding.cgOrderStatus.getCheckedChipId();
        String statusFilter = "all";
        if (selectedChipId == R.id.chipSuccess) statusFilter = "success";
        else if (selectedChipId == R.id.chipPending) statusFilter = "pending";
        else if (selectedChipId == R.id.chipCancelled) statusFilter = "cancelled";

        List<OrderItem> filteredList = new ArrayList<>();
        for (OrderItem item : allOrderItems) {
            Order o = item.getParentOrder();
            String id = item.getOrderId() != null ? item.getOrderId().toLowerCase() : "";
            String shortId = id.length() > 8 ? id.substring(0, 8) : id;
            
            boolean matchesSearch = id.contains(query) || shortId.contains(query);
            boolean matchesStatus = true;
            
            if (o != null && o.getStatusStr() != null) {
                String status = o.getStatusStr().toLowerCase();
                if (statusFilter.equals("success")) {
                    matchesStatus = status.equals("completed") || status.equals("confirmed") || status.equals("paid");
                } else if (statusFilter.equals("cancelled")) {
                    matchesStatus = status.equals("cancelled") || status.equals("refunded");
                } else if (statusFilter.equals("pending")) {
                    matchesStatus = status.equals("pending");
                }
            } else if (!statusFilter.equals("all")) {
                matchesStatus = statusFilter.equals("pending");
            }
            
            if (matchesSearch && matchesStatus) {
                filteredList.add(item);
            }
        }
        
        adapter.updateData(filteredList);
        
        if (filteredList.isEmpty()) {
            binding.rvOrders.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.rvOrders.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showOrderList() {
        adapter.updateData(allOrderItems);
        binding.layoutOrdersTab.setVisibility(View.VISIBLE);
        binding.layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        binding.layoutOrdersTab.setVisibility(View.GONE);
        binding.svRevenue.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.VISIBLE);
    }

    private void showOrderDetails(OrderItem item) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog);
        com.example.vibetix.databinding.BottomSheetOrderDetailsBinding sheetBinding = com.example.vibetix.databinding.BottomSheetOrderDetailsBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        Order o = item.getParentOrder();
        String id = item.getOrderId();
        if (id != null && id.length() > 8) {
            id = id.substring(0, 8).toUpperCase();
        }
        sheetBinding.tvOrderIdDetails.setText("#" + id);

        if (o != null && o.getOrderDate() != null) {
            SimpleDateFormat dispFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            sheetBinding.tvOrderDateDetails.setText(dispFmt.format(o.getOrderDate().toDate()));
        }

        String status = (o != null && o.getStatusStr() != null) ? o.getStatusStr() : "pending";
        switch (status.toLowerCase()) {
            case "completed": case "confirmed": case "paid":
                sheetBinding.tvOrderStatusDetails.setText("Thành công");
                sheetBinding.tvOrderStatusDetails.setTextColor(0xFF4CAF50);
                sheetBinding.tvOrderStatusDetails.setBackgroundResource(R.drawable.bg_ticket_type_active);
                break;
            case "cancelled":
                sheetBinding.tvOrderStatusDetails.setText("Đã hủy");
                sheetBinding.tvOrderStatusDetails.setTextColor(0xFFF44336);
                sheetBinding.tvOrderStatusDetails.setBackgroundResource(R.drawable.bg_ticket_type_inactive);
                break;
            case "refunded":
                sheetBinding.tvOrderStatusDetails.setText("Hoàn tiền");
                sheetBinding.tvOrderStatusDetails.setTextColor(0xFFFF9800);
                sheetBinding.tvOrderStatusDetails.setBackgroundResource(R.drawable.bg_ticket_type_inactive);
                break;
            default:
                sheetBinding.tvOrderStatusDetails.setText("Chờ xử lý");
                sheetBinding.tvOrderStatusDetails.setTextColor(0xFF226CEB);
                sheetBinding.tvOrderStatusDetails.setBackgroundResource(R.drawable.bg_ticket_type_active);
                sheetBinding.btnConfirmOrder.setVisibility(View.VISIBLE);
                break;
        }

        sheetBinding.btnConfirmOrder.setOnClickListener(v -> {
            if (o != null && o.getOrderId() != null) {
                binding.pbLoading.setVisibility(View.VISIBLE);
                db.collection(FirebaseCollections.ORDERS).document(o.getOrderId())
                        .update("status", "confirmed")
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Đã duyệt đơn hàng", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            NotificationTriggerManager.triggerOrderConfirmed(o.getUserId(), o.getOrderId());
                            loadOrders();
                        })
                        .addOnFailureListener(e -> {
                            binding.pbLoading.setVisibility(View.GONE);
                            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        if (o != null && o.getUserId() != null) {
            db.collection(FirebaseCollections.USERS).document(o.getUserId()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String email = doc.getString("email");
                        String name = doc.getString("full_name");
                        sheetBinding.tvCustomerEmail.setText((name != null ? name + "\n" : "") + (email != null ? email : "ID: " + o.getUserId()));
                    } else {
                        sheetBinding.tvCustomerEmail.setText("Người dùng ID: " + o.getUserId());
                    }
                })
                .addOnFailureListener(e -> sheetBinding.tvCustomerEmail.setText("Người dùng ID: " + o.getUserId()));
        } else {
            sheetBinding.tvCustomerEmail.setText("Không xác định");
        }

        String ticketName = item.getTicketTypeName() != null ? item.getTicketTypeName() : "Vé không rõ";
        sheetBinding.tvTicketNameAndQty.setText(item.getQuantity() + "x " + ticketName);

        long totalAmount = item.getQuantity() * item.getPricePerTicket();
        sheetBinding.tvTicketPriceDetails.setText(vndFmt.format(totalAmount) + " ₫");
        sheetBinding.tvTotalAmountDetails.setText(vndFmt.format(totalAmount) + " ₫");

        sheetBinding.btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupRevenueChart() {
        Map<String, Long> revenueByDate = new HashMap<>();
        SimpleDateFormat sdfInput  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        SimpleDateFormat sdfInput2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
        SimpleDateFormat sdfOutput = new SimpleDateFormat("dd/MM", Locale.getDefault());

        for (OrderItem item : allOrderItems) {
            Order o = item.getParentOrder();
            if (o == null) continue;
            String status = o.getStatusStr();
            if (status != null && (status.equalsIgnoreCase("completed") || status.equalsIgnoreCase("confirmed") || status.equalsIgnoreCase("paid"))) {
                try {
                    if (o.getOrderDate() != null) {
                        Date date = o.getOrderDate().toDate();
                        if (date != null) {
                            String dateStr = sdfOutput.format(date);
                            long current = revenueByDate.containsKey(dateStr) ? revenueByDate.get(dateStr) : 0L;
                            revenueByDate.put(dateStr, current + item.getQuantity() * item.getPricePerTicket());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels   = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            String dateStr = sdfOutput.format(cal.getTime());
            labels.add(dateStr);
            long val = revenueByDate.containsKey(dateStr) ? revenueByDate.get(dateStr) : 0L;
            entries.add(new BarEntry(i, (float) val));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Doanh thu (₫)");
        dataSet.setColor(Color.parseColor("#226CEB"));
        dataSet.setValueTextSize(9f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        binding.revenueChart.setData(barData);
        binding.revenueChart.getDescription().setEnabled(false);
        binding.revenueChart.getLegend().setEnabled(false);

        XAxis xAxis = binding.revenueChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(7);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.parseColor("#1C1B1B"));

        YAxis leftAxis = binding.revenueChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(Color.parseColor("#1C1B1B"));

        binding.revenueChart.getAxisRight().setEnabled(false);
        binding.revenueChart.animateY(800);
        binding.revenueChart.invalidate();
    }
}
