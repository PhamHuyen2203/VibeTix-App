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
                PopupMenu popup = new PopupMenu(StaffManagementActivity.this, anchor);
                popup.getMenu().add(0, 1, 0, "Đổi vai trò (Role)");
                popup.getMenu().add(0, 2, 0, "Xóa nhân sự");
                
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        showRoleChangeDialog(staff);
                        return true;
                    } else if (item.getItemId() == 2) {
                        confirmDeleteStaff(staff);
                        return true;
                    }
                    return false;
                });
                popup.show();
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
                        List<EventStaff> tempStaffList = new ArrayList<>();
                        List<String> userIds = new ArrayList<>();
                        
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            EventStaff staff = doc.toObject(EventStaff.class);
                            if (staff != null) {
                                tempStaffList.add(staff);
                                if (staff.getUserId() != null && !userIds.contains(staff.getUserId())) {
                                    userIds.add(staff.getUserId());
                                }
                            }
                        }
                        
                        if (userIds.isEmpty()) {
                            staffList.addAll(tempStaffList);
                            updateUI();
                        } else {
                            fetchUsersAndEnrichStaff(tempStaffList, userIds);
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

    private void fetchUsersAndEnrichStaff(List<EventStaff> tempStaffList, List<String> userIds) {
        List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot>> tasks = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i += 10) {
            List<String> chunk = userIds.subList(i, Math.min(userIds.size(), i + 10));
            tasks.add(db.collection("users").whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk).get());
        }
        
        com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            Map<String, User> userMap = new HashMap<>();
            for (Object res : results) {
                com.google.firebase.firestore.QuerySnapshot snap = (com.google.firebase.firestore.QuerySnapshot) res;
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    User u = doc.toObject(User.class);
                    if (u != null) {
                        userMap.put(doc.getId(), u);
                    }
                }
            }
            
            for (EventStaff staff : tempStaffList) {
                if (staff.getUserId() != null && userMap.containsKey(staff.getUserId())) {
                    User u = userMap.get(staff.getUserId());
                    staff.setStaffName(u.getFullName());
                    staff.setStaffEmail(u.getEmail());
                    staff.setStaffAvatarUrl(u.getAvatarUrl());
                }
                staffList.add(staff);
            }
            updateUI();
        }).addOnFailureListener(e -> {
            binding.pbLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Lỗi tải thông tin user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void showRoleChangeDialog(EventStaff staff) {
        String[] roles = {"Quản lý (Manager)", "Soát vé (Check-in Staff)"};
        int checkedItem = staff.getRole() == EventStaff.Role.MANAGER ? 0 : 1;
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Đổi vai trò")
            .setSingleChoiceItems(roles, checkedItem, (dialog, which) -> {
                EventStaff.Role newRole = which == 0 ? EventStaff.Role.MANAGER : EventStaff.Role.CHECK_IN_STAFF;
                if (newRole != staff.getRole()) {
                    updateStaffRole(staff, newRole);
                }
                dialog.dismiss();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void updateStaffRole(EventStaff staff, EventStaff.Role newRole) {
        db.collection("event_staff").document(staff.getStaffId())
            .update("role", newRole)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Đã cập nhật vai trò", Toast.LENGTH_SHORT).show();
                staff.setRole(newRole);
                staffAdapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật vai trò", Toast.LENGTH_SHORT).show());
    }

    private void confirmDeleteStaff(EventStaff staff) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Xóa nhân sự")
            .setMessage("Bạn có chắc chắn muốn xóa nhân sự này khỏi sự kiện?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                db.collection("event_staff").document(staff.getStaffId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã xóa nhân sự", Toast.LENGTH_SHORT).show();
                        staffList.remove(staff);
                        updateUI();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi xóa", Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
}
