package com.example.vibetix.Activities.Organizer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vibetix.Adapters.Organizer.AttendeeAdapter;
import com.example.vibetix.Models.UserTicket;
import com.example.vibetix.Utils.CosineSimilarityUtils;
import com.example.vibetix.databinding.ActivityAttendeesBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AttendeesActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";

    private ActivityAttendeesBinding binding;
    private AttendeeAdapter adapter;
    private final List<UserTicket> allTickets = new ArrayList<>();
    private final List<AttendeeAdapter.AttendeeGroup> allGroups = new ArrayList<>();
    private final List<AttendeeAdapter.AttendeeGroup> filteredGroups = new ArrayList<>();
    private FirebaseFirestore db;
    private String eventId;
    private String currentFilterStatus = "all"; // all, checked_in, not_checked_in
    private ListenerRegistration ticketsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttendeesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Không tìm thấy sự kiện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        setSupportActionBar(binding.toolbarAttendees);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarAttendees.setNavigationOnClickListener(v -> finish());

        binding.rvAttendees.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendeeAdapter(filteredGroups, this::onManualCheckIn);
        binding.rvAttendees.setAdapter(adapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnFilter.setOnClickListener(v -> showFilterMenu());

        loadTickets();
    }
    
    private void showFilterMenu() {
        PopupMenu popup = new PopupMenu(this, binding.btnFilter);
        popup.getMenu().add(0, 1, 0, "Tất cả").setChecked("all".equals(currentFilterStatus));
        popup.getMenu().add(0, 2, 0, "Đã check-in").setChecked("checked_in".equals(currentFilterStatus));
        popup.getMenu().add(0, 3, 0, "Chưa check-in").setChecked("not_checked_in".equals(currentFilterStatus));
        
        popup.getMenu().setGroupCheckable(0, true, true);
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: currentFilterStatus = "all"; break;
                case 2: currentFilterStatus = "checked_in"; break;
                case 3: currentFilterStatus = "not_checked_in"; break;
            }
            filterList(binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "");
            return true;
        });
        popup.show();
    }

    private void loadTickets() {
        binding.pbLoading.setVisibility(View.VISIBLE);
        binding.rvAttendees.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);

        if (ticketsListener != null) {
            ticketsListener.remove();
        }

        ticketsListener = db.collection("user_tickets")
                .whereEqualTo("event_id", eventId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                        binding.pbLoading.setVisibility(View.GONE);
                        showEmptyState();
                        return;
                    }

                    if (value == null || value.isEmpty()) {
                        binding.pbLoading.setVisibility(View.GONE);
                        showEmptyState();
                        return;
                    }

                    List<UserTicket> fetchedTickets = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        UserTicket t = doc.toObject(UserTicket.class);
                        if (t != null) {
                            if (t.getUserTicketId() == null) t.setUserTicketId(doc.getId());
                            fetchedTickets.add(t);
                        }
                    }

                    // Fetch order_items
                    db.collection("order_items").whereEqualTo("event_id", eventId).get().addOnSuccessListener(itemSnap -> {
                        Map<String, String> itemToOrderMap = new HashMap<>();
                        Map<String, String> itemToTicketTypeMap = new HashMap<>();
                        Set<String> orderIdsToFetch = new HashSet<>();

                        if (itemSnap != null) {
                            for (DocumentSnapshot doc : itemSnap.getDocuments()) {
                                com.example.vibetix.Models.OrderItem oi = doc.toObject(com.example.vibetix.Models.OrderItem.class);
                                if (oi != null) {
                                    if (oi.getOrderItemId() == null) oi.setOrderItemId(doc.getId());
                                    itemToOrderMap.put(oi.getOrderItemId(), oi.getOrderId());
                                    itemToTicketTypeMap.put(oi.getOrderItemId(), oi.getTicketTypeId());
                                    if (oi.getOrderId() != null) {
                                        orderIdsToFetch.add(oi.getOrderId());
                                    }
                                }
                            }
                        }

                        // Fetch ticket types
                        db.collection("ticket_types").whereEqualTo("event_id", eventId).get().addOnSuccessListener(ttSnap -> {
                            Map<String, String> ttMap = new HashMap<>();
                            if (ttSnap != null) {
                                for (DocumentSnapshot doc : ttSnap.getDocuments()) {
                                    ttMap.put(doc.getId(), doc.getString("name"));
                                }
                            }

                            // Fetch orders for status
                            List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot>> orderTasks = new ArrayList<>();
                            List<String> orderIdList = new ArrayList<>(orderIdsToFetch);
                            for (int i = 0; i < orderIdList.size(); i += 10) {
                                List<String> chunk = orderIdList.subList(i, Math.min(i + 10, orderIdList.size()));
                                orderTasks.add(db.collection("orders").whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk).get());
                            }

                            com.google.android.gms.tasks.Tasks.whenAllSuccess(orderTasks).addOnSuccessListener(orderResults -> {
                                Set<String> validOrderIds = new HashSet<>();
                                for (Object res : orderResults) {
                                    com.google.firebase.firestore.QuerySnapshot snap = (com.google.firebase.firestore.QuerySnapshot) res;
                                    for (DocumentSnapshot doc : snap.getDocuments()) {
                                        String status = doc.getString("status");
                                        if (status != null && (status.equalsIgnoreCase("completed") || status.equalsIgnoreCase("paid") || status.equalsIgnoreCase("confirmed"))) {
                                            validOrderIds.add(doc.getId());
                                        }
                                    }
                                }

                                allTickets.clear();
                                Set<String> userIdsToFetch = new HashSet<>();

                                for (UserTicket t : fetchedTickets) {
                                    String orderId = itemToOrderMap.get(t.getOrderItemId());
                                    if (orderId != null && validOrderIds.contains(orderId)) {
                                        String ttId = itemToTicketTypeMap.get(t.getOrderItemId());
                                        if (ttId != null && ttMap.containsKey(ttId)) {
                                            t.setTicketTypeName(ttMap.get(ttId));
                                        }
                                        allTickets.add(t);
                                        if (t.getOwnerId() != null) {
                                            userIdsToFetch.add(t.getOwnerId());
                                        }
                                    }
                                }

                                if (allTickets.isEmpty()) {
                                    binding.pbLoading.setVisibility(View.GONE);
                                    showEmptyState();
                                    return;
                                }

                                enrichWithUserInfo(new ArrayList<>(userIdsToFetch));
                            }).addOnFailureListener(e -> {
                                binding.pbLoading.setVisibility(View.GONE);
                                showEmptyState();
                            });
                        }).addOnFailureListener(e -> {
                            binding.pbLoading.setVisibility(View.GONE);
                            showEmptyState();
                        });
                    }).addOnFailureListener(e -> {
                        binding.pbLoading.setVisibility(View.GONE);
                        showEmptyState();
                    });
                });
    }

    private void enrichWithUserInfo(List<String> userIds) {
        Map<String, com.example.vibetix.Models.User> userMap = new HashMap<>();
        List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot>> tasks = new ArrayList<>();

        for (int i = 0; i < userIds.size(); i += 10) {
            List<String> chunk = userIds.subList(i, Math.min(i + 10, userIds.size()));
            tasks.add(db.collection("users")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get());
        }

        com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            binding.pbLoading.setVisibility(View.GONE);
            for (Object result : results) {
                com.google.firebase.firestore.QuerySnapshot snap =
                        (com.google.firebase.firestore.QuerySnapshot) result;
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    com.example.vibetix.Models.User u = doc.toObject(com.example.vibetix.Models.User.class);
                    if (u != null) {
                        userMap.put(doc.getId(), u);
                    }
                }
            }
            for (UserTicket t : allTickets) {
                com.example.vibetix.Models.User u = userMap.get(t.getOwnerId());
                if (u != null) {
                    t.setFullName(u.getFullName());
                    t.setEmail(u.getEmail());
                }
            }
            buildGroupsAndFilter();
        }).addOnFailureListener(e -> {
            binding.pbLoading.setVisibility(View.GONE);
            buildGroupsAndFilter();
        });
    }

    private void buildGroupsAndFilter() {
        Map<String, AttendeeAdapter.AttendeeGroup> groupMap = new HashMap<>();
        for (UserTicket t : allTickets) {
            String uid = t.getOwnerId();
            if (uid == null) uid = "unknown";
            
            AttendeeAdapter.AttendeeGroup group = groupMap.get(uid);
            if (group == null) {
                group = new AttendeeAdapter.AttendeeGroup();
                group.userId = uid;
                group.userName = t.getFullName();
                group.userEmail = t.getEmail();
                group.tickets = new ArrayList<>();
                groupMap.put(uid, group);
            }
            group.tickets.add(t);
        }
        allGroups.clear();
        allGroups.addAll(groupMap.values());
        
        updateStats();
        filterList(binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "");
    }

    private void updateStats() {
        int total = allTickets.size();
        int checkedIn = 0;
        for (UserTicket t : allTickets) {
            if (UserTicket.Status.USED.equals(t.getStatus())) checkedIn++;
        }
        int notCheckedIn = total - checkedIn;

        binding.tvTotalTickets.setText(String.valueOf(total));
        binding.tvCheckedIn.setText(String.valueOf(checkedIn));
        binding.tvNotCheckedIn.setText(String.valueOf(notCheckedIn));
    }

    private void filterList(String query) {
        String q = query.trim();
        filteredGroups.clear();

        for (AttendeeAdapter.AttendeeGroup group : allGroups) {
            boolean groupMatches = false;
            if (q.isEmpty()) {
                groupMatches = true;
            } else {
                double nameSim = CosineSimilarityUtils.calculateSimilarity(q, group.userName != null ? group.userName : "");
                double emailSim = CosineSimilarityUtils.calculateSimilarity(q, group.userEmail != null ? group.userEmail : "");
                
                if (nameSim > 0.3 || emailSim > 0.3 || 
                    (group.userName != null && group.userName.toLowerCase().contains(q.toLowerCase())) || 
                    (group.userEmail != null && group.userEmail.toLowerCase().contains(q.toLowerCase()))) {
                    groupMatches = true;
                }
                
                if (!groupMatches) {
                    for (UserTicket t : group.tickets) {
                        if (t.getTicketCode() != null && t.getTicketCode().toLowerCase().contains(q.toLowerCase())) {
                            groupMatches = true;
                            break;
                        }
                    }
                }
            }

            if (groupMatches) {
                // Now filter tickets within group based on currentFilterStatus
                List<UserTicket> matchingTickets = new ArrayList<>();
                for (UserTicket t : group.tickets) {
                    boolean matchStatus = true;
                    if ("checked_in".equals(currentFilterStatus) && !UserTicket.Status.USED.equals(t.getStatus())) {
                        matchStatus = false;
                    } else if ("not_checked_in".equals(currentFilterStatus) && UserTicket.Status.USED.equals(t.getStatus())) {
                        matchStatus = false;
                    }
                    if (matchStatus) {
                        matchingTickets.add(t);
                    }
                }
                
                if (!matchingTickets.isEmpty()) {
                    AttendeeAdapter.AttendeeGroup filteredGroup = new AttendeeAdapter.AttendeeGroup();
                    filteredGroup.userId = group.userId;
                    filteredGroup.userName = group.userName;
                    filteredGroup.userEmail = group.userEmail;
                    filteredGroup.isExpanded = !q.isEmpty() || group.isExpanded; // Auto-expand if searching
                    filteredGroup.tickets = matchingTickets;
                    filteredGroups.add(filteredGroup);
                }
            }
        }

        // Optional: sort by similarity score if searching
        if (!q.isEmpty()) {
            filteredGroups.sort((g1, g2) -> {
                double s1 = Math.max(CosineSimilarityUtils.calculateSimilarity(q, g1.userName != null ? g1.userName : ""),
                                     CosineSimilarityUtils.calculateSimilarity(q, g1.userEmail != null ? g1.userEmail : ""));
                double s2 = Math.max(CosineSimilarityUtils.calculateSimilarity(q, g2.userName != null ? g2.userName : ""),
                                     CosineSimilarityUtils.calculateSimilarity(q, g2.userEmail != null ? g2.userEmail : ""));
                return Double.compare(s2, s1);
            });
        }

        adapter.updateData(filteredGroups);

        if (filteredGroups.isEmpty()) {
            showEmptyState();
        } else {
            binding.rvAttendees.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
        binding.rvAttendees.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.VISIBLE);
    }
    
    private void onManualCheckIn(UserTicket ticket) {
        String name = ticket.getFullName() != null ? ticket.getFullName() : "Khách";
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Xác nhận Check-in")
            .setMessage("Check-in cho: " + name + "?")
            .setPositiveButton("Check-in", (dialog, which) -> {
                String ticketId = ticket.getUserTicketId();
                if (ticketId == null || ticketId.isEmpty()) return;
                db.collection("user_tickets").document(ticketId)
                    .update("status", "used", "checked_in_at", new java.util.Date())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Check-in thành công!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(this, "Lỗi check-in: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ticketsListener != null) {
            ticketsListener.remove();
        }
    }
}
