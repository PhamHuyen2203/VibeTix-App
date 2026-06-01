package com.example.vibetix.Fragments.User;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Activities.User.UserMainActivity;
import com.example.vibetix.Activities.Organizer.CreateEditEventActivity;
import com.example.vibetix.Adapters.Organizer.OrganizerEventAdapter;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OrganizerHubFragment — Tab "Tổ chức" trong UserMainActivity.
 *
 * State A (chưa có event): Màn hình khuyến khích → tạo event đầu tiên.
 * State B (đã có event):   Dashboard mini + Quick Actions + Recent Events.
 *
 * Logic phân nhánh:
 *   - Query Firestore events where user_id == currentUser.uid, limit 1
 *   - Nếu không có → State A
 *   - Nếu có → State B, load đầy đủ stats
 */
public class OrganizerHubFragment extends Fragment {

    // State A views
    private View layoutStateA;

    // State B views
    private View layoutStateB;
    private ImageView ivOrgLogo;
    private TextView tvOrgName, tvOrgSubtitle;
    private TextView tvRevenue, tvTicketsSold, tvCheckins;
    private RecyclerView rvRecentEvents;

    // Quick action buttons
    private View btnMyEvents, btnCreateEvent, btnRevenue;
    private View btnStaff, btnTicketTypes, btnNotification;

    // Organizer selector
    private LinearLayout btnSwitchOrganizer;

    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private String currentUserId;
    private Organizer activeOrganizer;
    private List<Event> recentEvents = new ArrayList<>();
    private OrganizerEventAdapter eventAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());

        com.google.firebase.auth.FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = fbUser != null ? fbUser.getUid() : null;

        bindViews(view);
        setupRecentEvents();
        checkOrganizerStatus();
    }

    private void bindViews(View v) {
        layoutStateA = v.findViewById(R.id.layoutBecomeOrganizer);
        layoutStateB = v.findViewById(R.id.layoutOrganizerDashboard);

        // State A
        View btnFirstEvent = v.findViewById(R.id.btnCreateFirstEvent);
        if (btnFirstEvent != null) {
            btnFirstEvent.setOnClickListener(x -> openCreateEvent());
        }

        // State B — header
        ivOrgLogo       = v.findViewById(R.id.ivOrgLogo);
        tvOrgName       = v.findViewById(R.id.tvOrgName);
        tvOrgSubtitle   = v.findViewById(R.id.tvOrgSubtitle);
        btnSwitchOrganizer = v.findViewById(R.id.btnSwitchOrganizer);

        // State B — stats
        tvRevenue      = v.findViewById(R.id.tvStatRevenue);
        tvTicketsSold  = v.findViewById(R.id.tvStatTickets);
        tvCheckins     = v.findViewById(R.id.tvStatCheckins);

        // State B — quick actions
        btnMyEvents    = v.findViewById(R.id.actionMyEvents);
        btnCreateEvent = v.findViewById(R.id.actionCreateEvent);
        btnRevenue     = v.findViewById(R.id.actionRevenue);
        btnStaff       = v.findViewById(R.id.actionStaff);
        btnTicketTypes = v.findViewById(R.id.actionTicketTypes);
        btnNotification = v.findViewById(R.id.actionNotification);

        // Recent events
        rvRecentEvents = v.findViewById(R.id.rvRecentEvents);

        setupQuickActions();
        if (btnSwitchOrganizer != null) {
            btnSwitchOrganizer.setOnClickListener(x -> showOrganizerSwitcher());
        }
    }

    private void setupRecentEvents() {
        if (rvRecentEvents == null) return;
        eventAdapter = new OrganizerEventAdapter(recentEvents, event -> {
            // Open event detail / edit
            Intent intent = new Intent(getActivity(), CreateEditEventActivity.class);
            intent.putExtra(CreateEditEventActivity.EXTRA_EVENT_ID, event.getEventId());
            startActivity(intent);
        });
        rvRecentEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentEvents.setAdapter(eventAdapter);
        rvRecentEvents.setNestedScrollingEnabled(false);
    }

    /**
     * Kiểm tra user đã tạo event nào chưa → phân nhánh State A / B.
     */
    private void checkOrganizerStatus() {
        if (currentUserId == null) {
            showState(false);
            return;
        }

        db.collection(FirebaseCollections.EVENTS)
                .whereEqualTo("user_id", currentUserId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean hasEvents = snapshot != null && !snapshot.isEmpty();
                    showState(hasEvents);
                    if (hasEvents) {
                        loadOrganizerData();
                    }
                })
                .addOnFailureListener(e -> showState(false));
    }

    private void showState(boolean isOrganizer) {
        if (isOrganizer) {
            // Redirect to OrganizerMainActivity
            Intent intent = new Intent(getActivity(), com.example.vibetix.Activities.Organizer.OrganizerMainActivity.class);
            startActivity(intent);
            
            // Switch UserMainActivity back to home to prevent back-button loop
            if (getActivity() instanceof UserMainActivity) {
                ((UserMainActivity) getActivity()).switchToTab(R.id.nav_home);
            }
        } else {
            if (layoutStateA != null) layoutStateA.setVisibility(View.VISIBLE);
            if (layoutStateB != null) layoutStateB.setVisibility(View.GONE);
        }
    }

    /**
     * Load organizer profile + stats khi user đã là organizer.
     */
    private void loadOrganizerData() {
        // 1. Load active organizer profile
        String activeOrgId = sessionManager.getActiveOrganizerId();

        if (activeOrgId != null) {
            db.collection(FirebaseCollections.ORGANIZERS)
                    .document(activeOrgId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            activeOrganizer = doc.toObject(Organizer.class);
                            if (activeOrganizer != null) {
                                updateOrgHeader(activeOrganizer);
                                loadStatsForOrganizer(activeOrgId);
                                loadRecentEvents(activeOrgId);
                            }
                        } else {
                            // Active org not found, try loading from user's events
                            loadOrganizerFromEvents();
                        }
                    });
        } else {
            loadOrganizerFromEvents();
        }
    }

    /** Fallback: ambil organizer từ event đầu tiên của user */
    private void loadOrganizerFromEvents() {
        db.collection(FirebaseCollections.EVENTS)
                .whereEqualTo("user_id", currentUserId)
                .limit(5)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) return;
                    // Dùng organizer_id từ event đầu tiên
                    String orgId = snapshot.getDocuments().get(0).getString("organizer_id");
                    if (orgId != null) {
                        loadStatsForOrganizer(orgId);
                        loadRecentEvents(orgId);
                    } else {
                        // Hiện events trực tiếp không qua organizer
                        loadEventsDirectByUserId();
                    }
                });
    }

    private void loadEventsDirectByUserId() {
        db.collection(FirebaseCollections.EVENTS)
                .whereEqualTo("user_id", currentUserId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snapshot -> {
                    recentEvents.clear();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Event e = doc.toObject(Event.class);
                            if (e != null) {
                                if (e.getEventId() == null) e.setEventId(doc.getId());
                                recentEvents.add(e);
                            }
                        }
                    }
                    if (eventAdapter != null) eventAdapter.notifyDataSetChanged();
                    updateStats(0, recentEvents.size(), 0);
                });
    }

    private void updateOrgHeader(Organizer org) {
        if (tvOrgName != null)
            tvOrgName.setText(org.getBrandName() != null ? org.getBrandName() : "Ban tổ chức");
        if (tvOrgSubtitle != null)
            tvOrgSubtitle.setText(org.isVerified() ? "✓ Đã xác minh" : "Chưa xác minh");
        if (ivOrgLogo != null && org.getLogoUrl() != null && !org.getLogoUrl().isEmpty()) {
            Glide.with(this).load(org.getLogoUrl()).circleCrop().into(ivOrgLogo);
        }
    }

    private void loadStatsForOrganizer(String orgId) {
        // Revenue: sum orders where organizer_id = orgId, status = "paid"
        db.collection(FirebaseCollections.ORDERS)
                .whereEqualTo("organizer_id", orgId)
                .whereEqualTo("status", "paid")
                .get()
                .addOnSuccessListener(orders -> {
                    long totalRevenue = 0;
                    int ticketsSold = 0;
                    if (orders != null) {
                        for (QueryDocumentSnapshot doc : orders) {
                            Object amount = doc.get("total_amount");
                            if (amount instanceof Number) totalRevenue += ((Number) amount).longValue();
                            ticketsSold++;
                        }
                    }
                    // Check-ins hôm nay
                    loadCheckInsToday(orgId, totalRevenue, ticketsSold);
                });
    }

    private void loadCheckInsToday(String orgId, long revenue, int tickets) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new java.util.Date());

        db.collection(FirebaseCollections.USER_TICKETS)
                .whereGreaterThanOrEqualTo("checked_in_at", today)
                .get()
                .addOnSuccessListener(snap -> {
                    int checkIns = snap != null ? snap.size() : 0;
                    updateStats(revenue, tickets, checkIns);
                })
                .addOnFailureListener(e -> updateStats(revenue, tickets, 0));
    }

    private void updateStats(long revenue, int tickets, int checkIns) {
        if (tvRevenue != null) {
            String formatted = NumberFormat.getNumberInstance(new Locale("vi", "VN"))
                    .format(revenue) + "đ";
            tvRevenue.setText(formatted);
        }
        if (tvTicketsSold != null) tvTicketsSold.setText(String.valueOf(tickets));
        if (tvCheckins != null) tvCheckins.setText(String.valueOf(checkIns));
    }

    private void loadRecentEvents(String orgId) {
        db.collection(FirebaseCollections.EVENTS)
                .whereEqualTo("organizer_id", orgId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snapshot -> {
                    recentEvents.clear();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Event e = doc.toObject(Event.class);
                            if (e != null) {
                                if (e.getEventId() == null) e.setEventId(doc.getId());
                                recentEvents.add(e);
                            }
                        }
                    }
                    if (eventAdapter != null) eventAdapter.notifyDataSetChanged();
                });
    }

    private void setupQuickActions() {
        if (btnMyEvents != null) btnMyEvents.setOnClickListener(v -> openMyEvents());
        if (btnCreateEvent != null) btnCreateEvent.setOnClickListener(v -> openCreateEvent());
        if (btnRevenue != null) btnRevenue.setOnClickListener(v -> openRevenue());
        if (btnStaff != null) btnStaff.setOnClickListener(v -> openStaff());
        if (btnTicketTypes != null) btnTicketTypes.setOnClickListener(v -> openTicketTypes());
        if (btnNotification != null) btnNotification.setOnClickListener(v -> openNotification());
    }

    private void openCreateEvent() {
        startActivity(new Intent(getActivity(), CreateEditEventActivity.class));
    }

    private void openMyEvents() {
        // TODO: Navigate to OrganizerEventsFragment (full screen)
        android.widget.Toast.makeText(getContext(), "Danh sách sự kiện", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void openRevenue() {
        android.widget.Toast.makeText(getContext(), "Báo cáo doanh thu", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void openStaff() {
        android.widget.Toast.makeText(getContext(), "Quản lý nhân viên", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void openTicketTypes() {
        android.widget.Toast.makeText(getContext(), "Loại vé", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void openNotification() {
        android.widget.Toast.makeText(getContext(), "Gửi thông báo", android.widget.Toast.LENGTH_SHORT).show();
    }

    /** Bottom sheet chọn organizer profile */
    private void showOrganizerSwitcher() {
        if (getContext() == null) return;

        String userId = currentUserId;
        if (userId == null) return;

        // Load tất cả organizer profiles của user
        db.collection(FirebaseCollections.ORGANIZERS)
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Organizer> orgs = new ArrayList<>();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Organizer o = doc.toObject(Organizer.class);
                            if (o != null) {
                                if (o.getOrganizerId() == null) o.setOrganizerId(doc.getId());
                                orgs.add(o);
                            }
                        }
                    }
                    if (orgs.isEmpty()) {
                        // Gợi ý tạo hồ sơ BTC
                        android.widget.Toast.makeText(getContext(),
                                "Chưa có hồ sơ Ban tổ chức", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showSwitcherBottomSheet(orgs);
                });
    }

    private void showSwitcherBottomSheet(List<Organizer> orgs) {
        if (getContext() == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext())
                .inflate(R.layout.bottom_sheet_switch_organizer, null);
        dialog.setContentView(sheetView);

        RecyclerView rv = sheetView.findViewById(R.id.rvOrganizerList);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            // Simple adapter inline
            rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull @Override
                public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View v = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_organizer_switcher, parent, false);
                    return new RecyclerView.ViewHolder(v) {};
                }
                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
                    Organizer org = orgs.get(pos);
                    TextView tvName = holder.itemView.findViewById(R.id.tvOrgSwitchName);
                    if (tvName != null) tvName.setText(org.getBrandName());
                    holder.itemView.setOnClickListener(v -> {
                        sessionManager.setActiveOrganizerId(org.getOrganizerId());
                        activeOrganizer = org;
                        updateOrgHeader(org);
                        loadRecentEvents(org.getOrganizerId());
                        dialog.dismiss();
                    });
                }
                @Override public int getItemCount() { return orgs.size(); }
            });
        }
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-check status khi quay lại (user vừa tạo event mới)
        checkOrganizerStatus();
    }
}
