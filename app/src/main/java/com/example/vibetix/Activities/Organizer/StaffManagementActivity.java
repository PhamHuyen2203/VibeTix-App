package com.example.vibetix.Activities.Organizer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vibetix.Adapters.Organizer.StaffAdapter;
import com.example.vibetix.Models.EventStaff;
import com.example.vibetix.Models.User;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.example.vibetix.databinding.ActivityStaffManagementBinding;
import com.example.vibetix.databinding.BottomSheetAddStaffBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class StaffManagementActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";

    private ActivityStaffManagementBinding binding;
    private StaffAdapter staffAdapter;
    private final List<EventStaff> staffList = new ArrayList<>();

    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStaffManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        if (eventId == null) {
            Toast.makeText(this, "Thiếu Event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        loadEventDetails();
        loadStaffList();
    }

    private void initViews() {
        setSupportActionBar(binding.toolbarStaff);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbarStaff.setNavigationOnClickListener(v -> finish());

        binding.fabAddStaff.setOnClickListener(v -> showAddStaffBottomSheet());
    }

    private void setupRecyclerView() {
        staffAdapter = new StaffAdapter(staffList, new StaffAdapter.OnStaffInteractionListener() {
            @Override
            public void onOptionsClick(EventStaff staff, View anchor) {
                // Not strictly needed since we toggle active state, but kept for future expansion
            }

            @Override
            public void onStatusChange(EventStaff staff, boolean isActive) {
                updateStaffStatus(staff, isActive);
            }
        });
        binding.rvStaffList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStaffList.setAdapter(staffAdapter);
    }

    private void loadEventDetails() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("title");
                        binding.tvEventName.setText(name != null ? name : "Sự kiện");
                    }
                });
    }

    private void loadStaffList() {
        binding.pbLoading.setVisibility(View.VISIBLE);
        db.collection("event_staff")
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(query -> {
                    staffList.clear();
                    if (query != null && !query.isEmpty()) {
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            EventStaff staff = doc.toObject(EventStaff.class);
                            if (staff != null) {
                                loadStaffUserInfo(staff);
                            }
                        }
                    } else {
                        updateUI();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải danh sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadStaffUserInfo(EventStaff staff) {
        if (staff.getUserId() == null) return;
        db.collection("users").document(staff.getUserId()).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        User user = userDoc.toObject(User.class);
                        if (user != null) {
                            staff.setStaffName(user.getFullName());
                            staff.setStaffEmail(user.getEmail());
                            staff.setStaffAvatarUrl(user.getAvatarUrl());
                        }
                    }
                    staffList.add(staff);
                    updateUI();
                });
    }

    private void updateUI() {
        binding.pbLoading.setVisibility(View.GONE);
        staffAdapter.notifyDataSetChanged();
        binding.tvStaffCount.setText(staffList.size() + " nhân sự");
        
        if (staffList.isEmpty()) {
            binding.layoutEmptyStaff.setVisibility(View.VISIBLE);
            binding.rvStaffList.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyStaff.setVisibility(View.GONE);
            binding.rvStaffList.setVisibility(View.VISIBLE);
        }
    }

    private void showAddStaffBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog);
        BottomSheetAddStaffBinding sheetBinding = BottomSheetAddStaffBinding.inflate(getLayoutInflater());
        bottomSheetDialog.setContentView(sheetBinding.getRoot());

        // Default role selection
        sheetBinding.rgStaffRole.check(R.id.rbRoleScanner);

        sheetBinding.btnSaveStaff.setOnClickListener(v -> {
            String email = sheetBinding.etStaffEmail.getText() != null ? 
                    sheetBinding.etStaffEmail.getText().toString().trim() : "";
                    
            if (TextUtils.isEmpty(email)) {
                sheetBinding.etStaffEmail.setError("Vui lòng nhập email");
                return;
            }

            EventStaff.Role role = sheetBinding.rgStaffRole.getCheckedRadioButtonId() == R.id.rbRoleManager 
                    ? EventStaff.Role.MANAGER : EventStaff.Role.CHECK_IN_STAFF;

            addStaffByEmail(email, role, bottomSheetDialog);
        });

        bottomSheetDialog.show();
    }

    private void addStaffByEmail(String email, EventStaff.Role role, BottomSheetDialog dialog) {
        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(query -> {
                    if (query != null && !query.isEmpty()) {
                        DocumentSnapshot userDoc = query.getDocuments().get(0);
                        String targetUserId = userDoc.getId();
                        
                        // Check if already in list
                        for (EventStaff s : staffList) {
                            if (s.getUserId() != null && s.getUserId().equals(targetUserId)) {
                                Toast.makeText(this, "Nhân sự này đã tồn tại trong sự kiện", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        String currentUserId = sessionManager.getUserDetails() != null ? sessionManager.getUserDetails().getUserId() : "";
                        String staffId = UUID.randomUUID().toString();
                        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();
                        
                        EventStaff newStaff = new EventStaff(
                                staffId, targetUserId, eventId, role, currentUserId, now
                        );
                        newStaff.setActive(true);

                        db.collection("event_staff").document(staffId).set(newStaff)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Đã thêm nhân sự thành công", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    loadStaffList(); // Reload
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi thêm nhân sự", Toast.LENGTH_SHORT).show());

                    } else {
                        Toast.makeText(this, "Không tìm thấy user với email này", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateStaffStatus(EventStaff staff, boolean isActive) {
        staff.setActive(isActive);
        db.collection("event_staff").document(staff.getStaffId())
                .update("is_active", isActive)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã " + (isActive ? "mở khóa" : "tạm ngưng") + " nhân sự", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                    // Revert locally if failed
                    staff.setActive(!isActive);
                    staffAdapter.notifyDataSetChanged();
                });
    }
}
