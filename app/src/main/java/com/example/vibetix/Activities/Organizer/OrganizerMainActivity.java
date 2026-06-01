package com.example.vibetix.Activities.Organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.vibetix.Activities.User.UserMainActivity;
import com.example.vibetix.Fragments.Organizer.DashboardOverviewFragment;
import com.example.vibetix.Fragments.Organizer.OrganizerEventsFragment;
import com.example.vibetix.Fragments.Organizer.OrganizerProfileFragment;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * OrganizerMainActivity — Giao diện quản lý sự kiện.
 *
 * Tính năng:
 *  - Header: Brand name + logo của organizer đang active
 *  - Dropdown chuyển organizer nếu user có nhiều hồ sơ BTC
 *  - Nút "← Quay về User Mode"
 *  - Bottom nav: Dashboard | Events | Profile
 *  - Role-based UI: owner / manager / check_in_staff
 */
public class OrganizerMainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavOrganizer;
    private TextView tvOrganizerName;
    private ImageView ivOrganizerLogo;
    private View btnSwitchOrganizer;
    private View btnBackToUser;

    private SessionManager sessionManager;
    private FirebaseFirestore db;

    private List<Organizer> myOrganizers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_main);

        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupHeader();
        setupBottomNavigation();
        applyRoleBasedUI();
        loadOrganizerProfiles();

        // Default fragment
        if (savedInstanceState == null) {
            loadFragment(new DashboardOverviewFragment());
            bottomNavOrganizer.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    private void bindViews() {
        bottomNavOrganizer = findViewById(R.id.bottomNavOrganizer);
        tvOrganizerName    = findViewById(R.id.tvOrganizerName);
        ivOrganizerLogo    = findViewById(R.id.ivOrganizerLogo);
        btnSwitchOrganizer = findViewById(R.id.btnSwitchOrganizer);
        btnBackToUser      = findViewById(R.id.btnBackToUser);
    }

    private void setupHeader() {
        // Hiện brand name từ session
        String name = sessionManager.getActiveOrganizerName();
        if (tvOrganizerName != null) {
            tvOrganizerName.setText(name != null && !name.isEmpty() ? name : "Ban tổ chức");
        }

        // Logo
        String logoUrl = sessionManager.getActiveOrganizerLogoUrl();
        if (ivOrganizerLogo != null && logoUrl != null && !logoUrl.isEmpty()) {
            Glide.with(this).load(logoUrl)
                    .placeholder(R.drawable.ic_organizer_placeholder)
                    .circleCrop()
                    .into(ivOrganizerLogo);
        }

        // Nút chuyển BTC
        if (btnSwitchOrganizer != null) {
            btnSwitchOrganizer.setOnClickListener(v -> showSwitchOrganizerSheet());
        }

        // Nút quay về User mode
        if (btnBackToUser != null) {
            btnBackToUser.setOnClickListener(v -> backToUserMode());
        }
    }

    private void setupBottomNavigation() {
        bottomNavOrganizer.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                loadFragment(new DashboardOverviewFragment());
                return true;
            } else if (id == R.id.nav_events) {
                loadFragment(new OrganizerEventsFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                loadFragment(new OrganizerProfileFragment());
                return true;
            }
            return false;
        });
    }

    /**
     * Điều chỉnh UI theo role của user.
     * Check-in staff: ẩn tab Profile.
     * Manager/Check-in: không ảnh hưởng BottomNav, nhưng các Fragment sẽ tự ẩn nút.
     */
    private void applyRoleBasedUI() {
        String role = sessionManager.getStaffRole();
        if ("check_in_staff".equals(role)) {
            // Ẩn tab Profile với check_in_staff
            bottomNavOrganizer.getMenu().findItem(R.id.nav_profile).setVisible(false);
        }
    }

    /**
     * Load danh sách organizer profiles để hiện trong bottom sheet.
     */
    private void loadOrganizerProfiles() {
        String userId = sessionManager.getUserDetails() != null
                ? sessionManager.getUserDetails().getUserId() : null;
        if (userId == null) return;

        db.collection(FirebaseCollections.ORGANIZERS)
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    myOrganizers.clear();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Organizer org = doc.toObject(Organizer.class);
                            if (org != null) {
                                if (org.getOrganizerId() == null) org.setOrganizerId(doc.getId());
                                myOrganizers.add(org);
                            }
                        }
                    }
                    // Ẩn nút switch nếu chỉ có 1 BTC
                    if (btnSwitchOrganizer != null) {
                        btnSwitchOrganizer.setVisibility(myOrganizers.size() > 1 ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void showSwitchOrganizerSheet() {
        if (myOrganizers.isEmpty()) return;

        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.Theme_Design_BottomSheetDialog);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_switch_organizer, null);
        sheet.setContentView(view);

        androidx.recyclerview.widget.RecyclerView rv = view.findViewById(R.id.rvOrganizerList);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        
        com.example.vibetix.Adapters.Organizer.OrganizerSwitcherAdapter adapter = 
                new com.example.vibetix.Adapters.Organizer.OrganizerSwitcherAdapter(this, myOrganizers, sessionManager.getActiveOrganizerId(), org -> {
            switchToOrganizer(org);
            sheet.dismiss();
        });
        rv.setAdapter(adapter);

        View btnAdd = view.findViewById(R.id.btnAddOrganizer);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                startActivity(new Intent(this, com.example.vibetix.Activities.User.CreateOrganizerActivity.class));
                sheet.dismiss();
            });
        }

        sheet.show();
    }

    /**
     * Switch sang organizer khác: cập nhật session + reload header + fragments.
     */
    private void switchToOrganizer(Organizer org) {
        sessionManager.setActiveOrganizer(
                org.getOrganizerId(),
                org.getBrandName(),
                org.getLogoUrl()
        );
        setupHeader();
        // Reload current tab
        int selectedId = bottomNavOrganizer.getSelectedItemId();
        bottomNavOrganizer.setSelectedItemId(selectedId);
    }

    private void backToUserMode() {
        Intent intent = new Intent(this, UserMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /** Public: Dashboard gọi để navigate sang Events tab. */
    public void navigateToEventsTab() {
        bottomNavOrganizer.setSelectedItemId(R.id.nav_events);
    }

    /** Public: Lấy role hiện tại để các Fragment check permission. */
    public String getCurrentRole() {
        return sessionManager.getStaffRole();
    }

    /** Public: Lấy active organizer ID. */
    public String getActiveOrganizerId() {
        return sessionManager.getActiveOrganizerId();
    }
}
