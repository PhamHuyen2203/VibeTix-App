package com.example.vibetix.Fragments.User;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.UserTicket;
import com.example.vibetix.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * MyTicketsFragment — Tab "Vé của tôi": Danh sách vé người dùng đã mua.
 *
 * Tabs: Sắp diễn ra | Đã xem | Đã huỷ
 * Real data từ user_tickets collection
 */
public class MyTicketsFragment extends Fragment {

    private TabLayout tabLayout;
    private RecyclerView rvTickets;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyState;
    private TextView tvEmptyMessage;

    private FirebaseFirestore db;
    private String currentUserId;

    private List<UserTicket> allTickets = new ArrayList<>();
    private List<UserTicket> displayedTickets = new ArrayList<>();
    private MyTicketAdapter adapter;

    private String currentTab = "upcoming"; // upcoming | used | cancelled

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_tickets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        com.google.firebase.auth.FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = fbUser != null ? fbUser.getUid() : null;

        bindViews(view);
        setupTabs();
        setupRecyclerView();
        setupSwipeRefresh();
        loadTickets();
    }

    private void bindViews(View v) {
        tabLayout    = v.findViewById(R.id.tabMyTickets);
        rvTickets    = v.findViewById(R.id.rvMyTickets);
        swipeRefresh = v.findViewById(R.id.swipeRefreshTickets);
        emptyState   = v.findViewById(R.id.layoutEmptyTickets);
        tvEmptyMessage = v.findViewById(R.id.tvEmptyTicketMsg);
    }

    private void setupTabs() {
        if (tabLayout == null) return;
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentTab = "upcoming"; break;
                    case 1: currentTab = "used"; break;
                    case 2: currentTab = "cancelled"; break;
                }
                applyTabFilter();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new MyTicketAdapter(displayedTickets, ticket -> {
            // TODO: Open TicketDetailActivity
            android.widget.Toast.makeText(getContext(), "Chi tiết vé: " + ticket.getTicketCode(), android.widget.Toast.LENGTH_SHORT).show();
        });
        if (rvTickets != null) {
            rvTickets.setLayoutManager(new LinearLayoutManager(getContext()));
            rvTickets.setAdapter(adapter);
        }
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh == null) return;
        swipeRefresh.setColorSchemeResources(R.color.clr_primary);
        swipeRefresh.setOnRefreshListener(this::loadTickets);
    }

    private void loadTickets() {
        if (currentUserId == null) {
            showEmptyState("Vui lòng đăng nhập để xem vé");
            return;
        }
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);

        db.collection(FirebaseCollections.USER_TICKETS)
                .whereEqualTo("user_id", currentUserId)
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    allTickets.clear();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            UserTicket t = doc.toObject(UserTicket.class);
                            if (t != null) {
                                if (t.getTicketId() == null) t.setTicketId(doc.getId());
                                allTickets.add(t);
                            }
                        }
                    }
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    applyTabFilter();
                })
                .addOnFailureListener(e -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    showEmptyState("Không thể tải vé. Thử lại sau.");
                });
    }

    private void applyTabFilter() {
        displayedTickets.clear();
        String nowStr = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());

        for (UserTicket ticket : allTickets) {
            String status = ticket.getStatus() != null ? ticket.getStatus().toLowerCase() : "";
            switch (currentTab) {
                case "upcoming":
                    // valid + event chưa qua
                    if ("valid".equals(status) || "active".equals(status)) {
                        displayedTickets.add(ticket);
                    }
                    break;
                case "used":
                    if ("used".equals(status) || "checked_in".equals(status)) {
                        displayedTickets.add(ticket);
                    }
                    break;
                case "cancelled":
                    if ("cancelled".equals(status) || "refunded".equals(status)) {
                        displayedTickets.add(ticket);
                    }
                    break;
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();

        if (displayedTickets.isEmpty()) {
            String msg;
            switch (currentTab) {
                case "upcoming": msg = "Bạn chưa có vé nào\nHãy khám phá sự kiện và mua vé!"; break;
                case "used":     msg = "Chưa có vé nào đã sử dụng"; break;
                default:         msg = "Không có vé đã huỷ"; break;
            }
            showEmptyState(msg);
        } else {
            hideEmptyState();
        }
    }

    private void showEmptyState(String message) {
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        if (rvTickets != null) rvTickets.setVisibility(View.GONE);
        if (tvEmptyMessage != null) tvEmptyMessage.setText(message);
    }

    private void hideEmptyState() {
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        if (rvTickets != null) rvTickets.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (allTickets.isEmpty()) loadTickets();
    }

    // ─── Inline Adapter ────────────────────────────────────────────────────

    interface OnTicketClickListener {
        void onTicketClick(UserTicket ticket);
    }

    static class MyTicketAdapter extends RecyclerView.Adapter<MyTicketAdapter.VH> {
        private final List<UserTicket> items;
        private final OnTicketClickListener listener;

        MyTicketAdapter(List<UserTicket> items, OnTicketClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_ticket, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            UserTicket ticket = items.get(pos);
            if (h.tvEventName != null) h.tvEventName.setText(ticket.getEventId()); // Will be replaced when we load event name
            if (h.tvTicketCode != null) h.tvTicketCode.setText("# " + ticket.getTicketCode());
            if (h.tvStatus != null) {
                h.tvStatus.setText(formatStatus(ticket.getStatus()));
            }
            h.itemView.setOnClickListener(v -> { if (listener != null) listener.onTicketClick(ticket); });
        }

        private String formatStatus(String status) {
            if (status == null) return "Không xác định";
            switch (status.toLowerCase()) {
                case "valid": case "active": return "Hợp lệ";
                case "used": case "checked_in": return "Đã sử dụng";
                case "cancelled": return "Đã huỷ";
                default: return status;
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvEventName, tvTicketCode, tvStatus;
            VH(View v) {
                super(v);
                tvEventName  = v.findViewById(R.id.tvTicketEventName);
                tvTicketCode = v.findViewById(R.id.tvTicketCode);
                tvStatus     = v.findViewById(R.id.tvTicketStatus);
            }
        }
    }
}
