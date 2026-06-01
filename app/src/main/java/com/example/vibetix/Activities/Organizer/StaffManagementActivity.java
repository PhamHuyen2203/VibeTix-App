package com.example.vibetix.Activities.Organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Adapters.Organizer.StaffAdapter;
import com.example.vibetix.Models.EventStaff;
import com.example.vibetix.Models.User;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class StaffManagementActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";

    private Toolbar toolbarStaff;
    private RecyclerView rvStaffList;
    private LinearLayout layoutEmptyStaff;
    private ExtendedFloatingActionButton fabAddStaff;

    private StaffAdapter staffAdapter;
    private final List<EventStaff> staffList = new ArrayList<>();

    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_management);

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
        loadStaffList();
    }

    private void initViews() {
        toolbarStaff = findViewById(R.id.toolbarStaff);
        setSupportActionBar(toolbarStaff);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbarStaff.setNavigationOnClickListener(v -> finish());

        rvStaffList = findViewById(R.id.rvStaffList);
        layoutEmptyStaff = findViewById(R.id.layoutEmptyStaff);
        fabAddStaff = findViewById(R.id.fabAddStaff);

        fabAddStaff.setOnClickListener(v -> showAddStaffBottomSheet());
    }

    private void setupRecyclerView() {
        staffAdapter = new StaffAdapter(staffList, new StaffAdapter.OnStaffInteractionListener() {
            @Override
            public void onOptionsClick(EventStaff staff, View anchor) {
                showStaffOptions(staff, anchor);
            }

            @Override
            public void onStatusChange(EventStaff staff, boolean isActive) {
                Toast.makeText(StaffManagementActivity.this, 
                        "Staff status changed", Toast.LENGTH_SHORT).show();
            }
        });
        rvStaffList.setLayoutManager(new LinearLayoutManager(this));
        rvStaffList.setAdapter(staffAdapter);
    }

    private void loadStaffList() {
        db.collection("event_staff")
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(query -> {
                    staffList.clear();
                    if (query != null && !query.isEmpty()) {
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            EventStaff staff = doc.toObject(EventStaff.class);
                            if (staff != null) {
                                // Load thêm user info
                                loadStaffUserInfo(staff);
                            }
                        }
                    } else {
                        updateUI();
                    }
                })
                .addOnFailureListener(e -> {
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
        staffAdapter.notifyDataSetChanged();
        if (staffList.isEmpty()) {
            layoutEmptyStaff.setVisibility(View.VISIBLE);
            rvStaffList.setVisibility(View.GONE);
        } else {
            layoutEmptyStaff.setVisibility(View.GONE);
            rvStaffList.setVisibility(View.VISIBLE);
        }
    }

    private void showAddStaffBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_add_staff, null);
        bottomSheetDialog.setContentView(view);

        EditText etStaffEmail = view.findViewById(R.id.etStaffEmail);
        RadioGroup rgStaffRole = view.findViewById(R.id.rgStaffRole);
        MaterialButton btnSaveStaff = view.findViewById(R.id.btnSaveStaff);

        btnSaveStaff.setOnClickListener(v -> {
            String email = etStaffEmail.getText().toString().trim();
            if (email.isEmpty()) {
                etStaffEmail.setError("Vui lòng nhập email");
                return;
            }

            EventStaff.Role role = rgStaffRole.getCheckedRadioButtonId() == R.id.rbRoleManager 
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
                                Toast.makeText(this, "Nhân sự này đã tồn tại", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        // Add to event_staff
                        String activeOrgId = sessionManager.getActiveOrganizerId();
                        String currentUserId = sessionManager.getUserDetails() != null ? sessionManager.getUserDetails().getUserId() : "";
                        String staffId = UUID.randomUUID().toString();
                        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());
                        
                        EventStaff newStaff = new EventStaff(
                                staffId, targetUserId, eventId, activeOrgId, role, currentUserId, now
                        );

                        db.collection("event_staff").document(staffId).set(newStaff)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Đã thêm nhân sự", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    loadStaffList(); // Reload
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi thêm nhân sự", Toast.LENGTH_SHORT).show());

                    } else {
                        Toast.makeText(this, "Không tìm thấy user với email này", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showStaffOptions(EventStaff staff, View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add(0, 1, 0, "Xóa nhân sự");
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                db.collection("event_staff").document(staff.getStaffId()).delete()
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                            staffList.remove(staff);
                            updateUI();
                        });
                return true;
            }
            return false;
        });
        popupMenu.show();
    }
}
