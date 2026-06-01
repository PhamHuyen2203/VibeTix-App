package com.example.vibetix.Fragments.Organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.cardview.widget.CardView;

import com.example.vibetix.Activities.Organizer.CreateEditEventActivity;
import com.example.vibetix.Adapters.Organizer.EventDashboardAdapter;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * DashboardOverviewFragment — Premium redesign (Phase 2).
 *
 * Features:
 *  - Collapsing header with gradient, greeting, organizer name
 *  - Real stats from Firestore: revenue (orders.total_amount where status=paid),
 *    ticket count (orders count), check-ins today (user_tickets.checked_in_at)
 *  - Quick Actions 2×3 grid with role-based visibility
 *  - Recent Events RecyclerView (last 5 events)
 *  - FAB → Create Event
 *  - NumberFormat for Vietnamese locale
 */
public class DashboardOverviewFragment extends Fragment {

    // Header / greeting
    private TextView tvDashGreeting;
    private TextView tvDashOrgName;

    // Stats row
    private TextView tvStatRevenue;
    private TextView tvStatTickets;
    private TextView tvStatCheckins;

    // Quick action cards
    private CardView cardCreateEvent;
    private CardView cardMyEvents;
    private CardView cardRevenue;
    private CardView cardStaff;
    private CardView cardTicketTypes;
    private CardView cardNotification;

    // "See all" link
    private TextView txtViewAllEvents;

    // Recent events
    private RecyclerView rvRecentEvents;
    private LinearLayout layoutEmptyEvents;
    private EventDashboardAdapter eventAdapter;
    private List<Event> recentEvents;

    // FAB
    private ExtendedFloatingActionButton fabCreateEvent;

    private FirebaseFirestore db;
    private SessionManager sessionManager;

    public DashboardOverviewFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());

        bindViews(view);
        setupRecyclerView();
        applyRoleBasedVisibility();
        loadGreeting();
        loadDashboardData();
        setupClickListeners();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  VIEW BINDING
    // ──────────────────────────────────────────────────────────────────────

    private void bindViews(View view) {
        tvDashGreeting    = view.findViewById(R.id.tvDashGreeting);
        tvDashOrgName     = view.findViewById(R.id.tvDashOrgName);

        tvStatRevenue     = view.findViewById(R.id.tvStatRevenue);
        tvStatTickets     = view.findViewById(R.id.tvStatTickets);
        tvStatCheckins    = view.findViewById(R.id.tvStatCheckins);

        cardCreateEvent   = view.findViewById(R.id.cardCreateEvent);
        cardMyEvents      = view.findViewById(R.id.cardMyEvents);
        cardRevenue       = view.findViewById(R.id.cardRevenue);
        cardStaff         = view.findViewById(R.id.cardStaff);
        cardTicketTypes   = view.findViewById(R.id.cardTicketTypes);
        cardNotification  = view.findViewById(R.id.cardNotification);

        txtViewAllEvents  = view.findViewById(R.id.txtViewAllEvents);
        rvRecentEvents    = view.findViewById(R.id.rvRecentEvents);
        layoutEmptyEvents = view.findViewById(R.id.layoutEmptyEvents);
        fabCreateEvent    = view.findViewById(R.id.fabCreateEvent);
    }

    private void setupRecyclerView() {
        recentEvents = new ArrayList<>();
        eventAdapter = new EventDashboardAdapter(recentEvents, new EventDashboardAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                Intent intent = new Intent(getActivity(), CreateEditEventActivity.class);
                intent.putExtra(CreateEditEventActivity.EXTRA_EVENT_ID, event.getEventId());
                startActivity(intent);
            }

            @Override
            public void onEventOptionClick(Event event, View anchor) {
                // Ignore in dashboard, or redirect to event details
            }
        });
        rvRecentEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentEvents.setAdapter(eventAdapter);
        rvRecentEvents.setNestedScrollingEnabled(false);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  ROLE-BASED VISIBILITY
    // ──────────────────────────────────────────────────────────────────────

    /**
     * check_in_staff: hide Revenue, Staff, TicketTypes, Notification cards.
     * Only show Create Event and My Events.
     */
    private void applyRoleBasedVisibility() {
        String role = sessionManager.getStaffRole();
        if ("check_in_staff".equals(role)) {
            if (cardRevenue      != null) cardRevenue.setVisibility(View.GONE);
            if (cardStaff        != null) cardStaff.setVisibility(View.GONE);
            if (cardTicketTypes  != null) cardTicketTypes.setVisibility(View.GONE);
            if (cardNotification != null) cardNotification.setVisibility(View.GONE);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GREETING
    // ──────────────────────────────────────────────────────────────────────

    private void loadGreeting() {
        // Time-based greeting
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Chào buổi sáng! ☀️";
        } else if (hour < 18) {
            greeting = "Chào buổi chiều! 🌤️";
        } else {
            greeting = "Chào buổi tối! 🌙";
        }
        if (tvDashGreeting != null) tvDashGreeting.setText(greeting);

        // Organizer name from session
        String orgName = sessionManager.getActiveOrganizerName();
        if (orgName != null && !orgName.isEmpty()) {
            if (tvDashOrgName != null) tvDashOrgName.setText(orgName);
        } else {
            // Fallback: load from Firestore
            loadOrganizerName();
        }
    }

    private void loadOrganizerName() {
        String activeOrgId = sessionManager.getActiveOrganizerId();
        if (activeOrgId == null) return;

        db.collection(FirebaseCollections.ORGANIZERS)
                .document(activeOrgId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && isAdded()) {
                        String name = doc.getString("brand_name");
                        if (name != null && tvDashOrgName != null) {
                            tvDashOrgName.setText(name);
                        }
                    }
                });
    }

    // ──────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ──────────────────────────────────────────────────────────────────────

    private void loadDashboardData() {
        String activeOrgId = sessionManager.getActiveOrganizerId();
        if (activeOrgId != null) {
            loadStats(activeOrgId);
            loadRecentEvents(activeOrgId);
        } else {
            // Try to resolve organizer from user's events
            resolveOrganizerAndLoad();
        }
    }

    /**
     * Fallback: find organizer_id from user's first event if session doesn't have it.
     */
    private void resolveOrganizerAndLoad() {
        com.google.firebase.auth.FirebaseUser fbUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) return;
        String userId = fbUser.getUid();

        db.collection(FirebaseCollections.ORGANIZERS)
                .whereEqualTo("user_id", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    if (snap != null && !snap.isEmpty()) {
                        String orgId = snap.getDocuments().get(0).getId();
                        String brandName = snap.getDocuments().get(0).getString("brand_name");
                        // Update session
                        sessionManager.setActiveOrganizer(orgId, brandName, null);
                        if (brandName != null && tvDashOrgName != null) {
                            tvDashOrgName.setText(brandName);
                        }
                        loadStats(orgId);
                        loadRecentEvents(orgId);
                    }
                })
                .addOnFailureListener(e -> {
                    // Show zeros
                    updateStats(0, 0, 0);
                });
    }

    /**
     * Load revenue + ticket count from orders, then load check-ins today.
     * Orders collection: organizer_id == activeOrgId, status == "paid"
     */
    private void loadStats(String orgId) {
        db.collection(FirebaseCollections.ORDERS)
                .whereEqualTo("organizer_id", orgId)
                .whereEqualTo("status", "paid")
                .get()
                .addOnSuccessListener(orders -> {
                    if (!isAdded()) return;
                    long totalRevenue = 0;
                    int ticketsSold = 0;
                    if (orders != null) {
                        for (QueryDocumentSnapshot doc : orders) {
                            Object amount = doc.get("total_amount");
                            if (amount instanceof Number) {
                                totalRevenue += ((Number) amount).longValue();
                            }
                            ticketsSold++;
                        }
                    }
                    final long revenue = totalRevenue;
                    final int tickets = ticketsSold;
                    loadCheckInsToday(orgId, revenue, tickets);
                })
                .addOnFailureListener(e -> updateStats(0, 0, 0));
    }

    /**
     * Count today's check-ins from user_tickets where checked_in_at >= today's start.
     */
    private void loadCheckInsToday(String orgId, long revenue, int tickets) {
        // Build start of today as String "yyyy-MM-dd"
        String todayPrefix = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        db.collection(FirebaseCollections.USER_TICKETS)
                .whereGreaterThanOrEqualTo("checked_in_at", todayPrefix)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    int checkIns = (snap != null) ? snap.size() : 0;
                    updateStats(revenue, tickets, checkIns);
                })
                .addOnFailureListener(e -> updateStats(revenue, tickets, 0));
    }

    private void updateStats(long revenue, int tickets, int checkIns) {
        if (tvStatRevenue != null) {
            tvStatRevenue.setText(formatRevenue(revenue));
        }
        if (tvStatTickets != null) {
            tvStatTickets.setText(String.valueOf(tickets));
        }
        if (tvStatCheckins != null) {
            tvStatCheckins.setText(String.valueOf(checkIns));
        }
    }

    /**
     * Load last 5 events for this organizer (ordered by created_at DESC).
     */
    private void loadRecentEvents(String orgId) {
        db.collection(FirebaseCollections.EVENTS)
                .whereEqualTo("organizer_id", orgId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
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
                    eventAdapter.notifyDataSetChanged();

                    // Toggle empty state
                    if (layoutEmptyEvents != null) {
                        layoutEmptyEvents.setVisibility(recentEvents.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    if (rvRecentEvents != null) {
                        rvRecentEvents.setVisibility(recentEvents.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    if (layoutEmptyEvents != null) layoutEmptyEvents.setVisibility(View.VISIBLE);
                    if (rvRecentEvents != null) rvRecentEvents.setVisibility(View.GONE);
                });
    }

    // ──────────────────────────────────────────────────────────────────────
    //  CLICK LISTENERS
    // ──────────────────────────────────────────────────────────────────────

    private void setupClickListeners() {
        // FAB → Create Event
        if (fabCreateEvent != null) {
            fabCreateEvent.setOnClickListener(v ->
                    startActivity(new Intent(getActivity(), CreateEditEventActivity.class)));
        }

        // Card: Tạo sự kiện
        if (cardCreateEvent != null) {
            cardCreateEvent.setOnClickListener(v ->
                    startActivity(new Intent(getActivity(), CreateEditEventActivity.class)));
        }

        // Card: Sự kiện của tôi → navigate to Events tab in OrganizerMainActivity
        if (cardMyEvents != null) {
            cardMyEvents.setOnClickListener(v -> navigateToEventsTab());
        }

        // Card: Doanh thu → OrderManagementActivity (safe launch with fallback)
        if (cardRevenue != null) {
            cardRevenue.setOnClickListener(v -> launchActivitySafe("OrderManagementActivity"));
        }

        // Card: Nhân sự → StaffManagementActivity
        if (cardStaff != null) {
            cardStaff.setOnClickListener(v -> launchActivitySafe("StaffManagementActivity"));
        }

        // Card: Loại vé → TicketTypeManagementActivity
        if (cardTicketTypes != null) {
            cardTicketTypes.setOnClickListener(v -> launchActivitySafe("TicketTypeManagementActivity"));
        }

        // Card: Thông báo → SendNotificationActivity
        if (cardNotification != null) {
            cardNotification.setOnClickListener(v -> launchActivitySafe("SendNotificationActivity"));
        }

        // "Xem tất cả" link
        if (txtViewAllEvents != null) {
            txtViewAllEvents.setOnClickListener(v -> navigateToEventsTab());
        }
    }

    /**
     * Navigate to the Events tab via OrganizerMainActivity.
     */
    private void navigateToEventsTab() {
        if (getActivity() instanceof com.example.vibetix.Activities.Organizer.OrganizerMainActivity) {
            ((com.example.vibetix.Activities.Organizer.OrganizerMainActivity) getActivity())
                    .navigateToEventsTab();
        }
    }

    /**
     * Safely launch an Organizer Activity by simple name.
     * Falls back to a Toast if the activity doesn't exist yet.
     */
    private void launchActivitySafe(String activitySimpleName) {
        if (getActivity() == null) return;
        try {
            Class<?> cls = Class.forName(
                    "com.example.vibetix.Activities.Organizer." + activitySimpleName);
            startActivity(new Intent(getActivity(), cls));
        } catch (ClassNotFoundException e) {
            Toast.makeText(getContext(), activitySimpleName + " sắp ra mắt",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Format revenue in Vietnamese locale.
     * E.g.: 150,000,000 → "150M ₫"
     *       1,500,000,000 → "1.5B ₫"
     *       500,000 → "500K ₫"
     */
    private String formatRevenue(long amount) {
        if (amount >= 1_000_000_000L) {
            double b = amount / 1_000_000_000.0;
            return String.format(new Locale("vi", "VN"), "%.1fB ₫", b);
        } else if (amount >= 1_000_000L) {
            long m = amount / 1_000_000L;
            return m + "M ₫";
        } else if (amount >= 1_000L) {
            long k = amount / 1_000L;
            return k + "K ₫";
        } else if (amount > 0) {
            return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + "₫";
        }
        return "0₫";
    }
}
