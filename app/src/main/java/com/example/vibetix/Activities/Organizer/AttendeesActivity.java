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
import com.example.vibetix.databinding.ActivityAttendeesBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AttendeesActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";

    private ActivityAttendeesBinding binding;
    private AttendeeAdapter adapter;
    private final List<UserTicket> allTickets = new ArrayList<>();
    private final List<UserTicket> filteredTickets = new ArrayList<>();
    private FirebaseFirestore db;
    private String eventId;
    private String currentFilterStatus = "all"; // all, checked_in, not_checked_in

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
        adapter = new AttendeeAdapter(filteredTickets);
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

        db.collection("user_tickets")
                .whereEqualTo("event_id", eventId)
                .addSnapshotListener((value, error) -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                        return;
                    }
                    if (value != null) {
                        allTickets.clear();
                        List<String> userIds = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value) {
                            UserTicket t = doc.toObject(UserTicket.class);
                            if (t != null) {
                                if (t.getUserTicketId() == null) t.setUserTicketId(doc.getId());
                                allTickets.add(t);
                                // Collect distinct user IDs to fetch
                                String uid = t.getOwnerId();
                                if (uid != null && !uid.isEmpty() && !userIds.contains(uid)) {
                                    userIds.add(uid);
                                }
                            }
                        }
                        if (!userIds.isEmpty()) {
                            enrichWithUserInfo(userIds);
                        } else {
                            updateStats();
                            filterList(binding.etSearch.getText() != null
                                    ? binding.etSearch.getText().toString() : "");
                        }
                    }
                });
    }

    /** Batch-fetch user info and attach to tickets */
    private void enrichWithUserInfo(List<String> userIds) {
        // Chunk into groups of 10 (Firestore whereIn limit)
        java.util.Map<String, com.example.vibetix.Models.User> userMap = new java.util.HashMap<>();
        java.util.List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot>> tasks = new java.util.ArrayList<>();

        for (int i = 0; i < userIds.size(); i += 10) {
            java.util.List<String> chunk = userIds.subList(i, Math.min(i + 10, userIds.size()));
            tasks.add(db.collection("users")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get());
        }

        com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            for (Object result : results) {
                com.google.firebase.firestore.QuerySnapshot snap =
                        (com.google.firebase.firestore.QuerySnapshot) result;
                for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                    com.example.vibetix.Models.User u = doc.toObject(com.example.vibetix.Models.User.class);
                    if (u != null) {
                        userMap.put(doc.getId(), u);
                    }
                }
            }
            // Attach user info to each ticket
            for (UserTicket t : allTickets) {
                com.example.vibetix.Models.User u = userMap.get(t.getOwnerId());
                if (u != null) {
                    t.setFullName(u.getFullName());
                    t.setEmail(u.getEmail());
                }
            }
            updateStats();
            filterList(binding.etSearch.getText() != null
                    ? binding.etSearch.getText().toString() : "");
        }).addOnFailureListener(e -> {
            // If user fetch fails, still show tickets without names
            updateStats();
            filterList(binding.etSearch.getText() != null
                    ? binding.etSearch.getText().toString() : "");
        });
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
        String q = query.toLowerCase();
        filteredTickets.clear();
        for (UserTicket t : allTickets) {
            boolean matchName  = t.getFullName() != null && t.getFullName().toLowerCase().contains(q);
            boolean matchEmail = t.getEmail() != null && t.getEmail().toLowerCase().contains(q);
            boolean matchCode  = t.getTicketCode() != null && t.getTicketCode().toLowerCase().contains(q);
            boolean matchId    = t.getUserTicketId() != null && t.getUserTicketId().toLowerCase().contains(q);

            boolean matchStatus = true;
            if ("checked_in".equals(currentFilterStatus) && !UserTicket.Status.USED.equals(t.getStatus())) {
                matchStatus = false;
            } else if ("not_checked_in".equals(currentFilterStatus) && UserTicket.Status.USED.equals(t.getStatus())) {
                matchStatus = false;
            }

            if (matchStatus && (q.isEmpty() || matchName || matchEmail || matchCode || matchId)) {
                filteredTickets.add(t);
            }
        }
        adapter.updateData(filteredTickets);

        if (filteredTickets.isEmpty()) {
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
}

