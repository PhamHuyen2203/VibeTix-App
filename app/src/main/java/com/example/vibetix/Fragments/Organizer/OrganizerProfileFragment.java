package com.example.vibetix.Fragments.Organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Activities.Auth.AuthActivity;
import com.example.vibetix.Activities.Organizer.OrganizerRevenueActivity;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.gms.tasks.Tasks;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrganizerProfileFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    private TextView tvUserDisplayName, tvOrgCount, tvProfileError;
    private TextView tvUserInfoName, tvUserInfoEmail, tvUserInfoPhone;
    private android.widget.ImageView ivUserAvatar;
    private ProgressBar pbProfileLoading;
    private RecyclerView rvOrganizerProfiles;
    private LinearLayout layoutOrgEmpty;
    private MaterialButton btnAddOrgProfile, btnOrgLogout;

    private OrgProfileAdapter adapter;
    private final List<OrgCardData> orgList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(requireContext());

        tvUserDisplayName = view.findViewById(R.id.tvUserDisplayName);
        tvOrgCount = view.findViewById(R.id.tvOrgCount);
        tvProfileError = view.findViewById(R.id.tvProfileError);
        pbProfileLoading = view.findViewById(R.id.pbProfileLoading);
        tvUserInfoName = view.findViewById(R.id.tvUserInfoName);
        tvUserInfoEmail = view.findViewById(R.id.tvUserInfoEmail);
        tvUserInfoPhone = view.findViewById(R.id.tvUserInfoPhone);
        ivUserAvatar = view.findViewById(R.id.ivUserAvatar);
        rvOrganizerProfiles = view.findViewById(R.id.rvOrganizerProfiles);
        layoutOrgEmpty = view.findViewById(R.id.layoutOrgEmpty);
        btnAddOrgProfile = view.findViewById(R.id.btnAddOrgProfile);
        btnOrgLogout = view.findViewById(R.id.btnOrgLogout);

        adapter = new OrgProfileAdapter(orgList, this::openOrgDetail);
        rvOrganizerProfiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrganizerProfiles.setAdapter(adapter);
        rvOrganizerProfiles.setNestedScrollingEnabled(false);

        btnOrgLogout.setOnClickListener(v -> logout());
        btnAddOrgProfile.setOnClickListener(v -> {
            // Navigate to CreateOrganizerActivity if available
            try {
                Intent intent = new Intent(requireContext(),
                        Class.forName("com.example.vibetix.Activities.User.CreateOrganizerActivity"));
                startActivity(intent);
            } catch (ClassNotFoundException ignored) { }
        });

        loadData();
    }

    private void loadData() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) { logout(); return; }

        // Load user info from Firestore
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    if (!isAdded() || userDoc == null) return;
                    String name = userDoc.getString("full_name");
                    String email = userDoc.getString("email");
                    String phone = userDoc.getString("phone");
                    String avatarUrl = userDoc.getString("avatar_url");

                    String displayName = name != null && !name.isEmpty() ? name : "Ban tổ chức";
                    tvUserDisplayName.setText(displayName);
                    if (tvUserInfoName != null) tvUserInfoName.setText(displayName);
                    if (tvUserInfoEmail != null && email != null) tvUserInfoEmail.setText(email);
                    if (tvUserInfoPhone != null && phone != null && !phone.isEmpty()) {
                        tvUserInfoPhone.setText(phone);
                        tvUserInfoPhone.setVisibility(View.VISIBLE);
                    }
                    if (ivUserAvatar != null && avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this).load(avatarUrl).circleCrop()
                                .placeholder(R.drawable.bg_avatar_circle).into(ivUserAvatar);
                    }
                });

        showLoading(true);
        db.collection("organizers")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    showLoading(false);
                    if (snap == null || snap.isEmpty()) {
                        showEmpty(true);
                        return;
                    }

                    List<Organizer> organizers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Organizer org = doc.toObject(Organizer.class);
                        org.setOrganizerId(doc.getId());
                        organizers.add(org);
                    }

                    tvOrgCount.setText(organizers.size() + " hồ sơ ban tổ chức");
                    showEmpty(false);

                    // Load events+tickets stats for each organizer in parallel
                    loadStatsForOrgs(organizers);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    tvProfileError.setText("Không thể tải thông tin. Thử lại.");
                    tvProfileError.setVisibility(View.VISIBLE);
                });
    }

    private void loadStatsForOrgs(List<Organizer> organizers) {
        orgList.clear();

        // Add cards with placeholder stats first so UI appears immediately
        for (Organizer org : organizers) {
            OrgCardData card = new OrgCardData(org);
            orgList.add(card);
        }
        adapter.notifyDataSetChanged();

        // Then load stats per organizer
        for (int i = 0; i < organizers.size(); i++) {
            Organizer org = organizers.get(i);
            String orgId = org.getOrganizerId();
            final int idx = i;

            db.collection("events")
                    .whereEqualTo("organizer_id", orgId)
                    .get()
                    .addOnSuccessListener(eventSnap -> {
                        if (eventSnap == null || idx >= orgList.size()) return;

                        List<String> eventIds = new ArrayList<>();
                        for (DocumentSnapshot d : eventSnap.getDocuments()) {
                            eventIds.add(d.getId());
                        }

                        orgList.get(idx).eventCount = eventSnap.size();

                        if (eventIds.isEmpty()) {
                            adapter.notifyItemChanged(idx);
                            return;
                        }

                        loadTicketStats(eventIds, idx);
                    });
        }
    }

    private void loadTicketStats(List<String> eventIds, int idx) {
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < eventIds.size(); i += 10) {
            chunks.add(eventIds.subList(i, Math.min(i + 10, eventIds.size())));
        }

        // Step 1: query order_items for revenue + ticket count
        List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
        final long[] totalTickets = {0};
        final java.util.Set<String> orderIdSet = new java.util.HashSet<>();
        final java.util.Map<String, long[]> orderTotals = new java.util.HashMap<>();

        for (List<String> chunk : chunks) {
            var task = db.collection("order_items")
                    .whereIn("event_id", chunk)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap == null) return;
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            Long qty = d.getLong("quantity");
                            Long unitPrice = d.getLong("price_per_ticket");
                            String orderId = d.getString("order_id");
                            if (orderId != null) {
                                orderIdSet.add(orderId);
                                long q = qty != null ? qty : 0;
                                long p = unitPrice != null ? unitPrice : 0;
                                orderTotals.merge(orderId, new long[]{q, q * p},
                                        (a, b) -> new long[]{a[0] + b[0], a[1] + b[1]});
                            }
                        }
                    });
            tasks.add(task);
        }

        // Step 2: after all order_items loaded, filter by order status
        Tasks.whenAllSuccess(tasks).addOnSuccessListener(r -> {
            if (orderIdSet.isEmpty()) {
                // No orders → use user_tickets count only
                countTicketsOnly(eventIds, idx);
                return;
            }

            List<String> orderIds = new ArrayList<>(orderIdSet);
            List<com.google.android.gms.tasks.Task<?>> orderTasks = new ArrayList<>();
            final long[] revenue = {0};
            final long[] tickets = {0};

            List<List<String>> orderChunks = new ArrayList<>();
            for (int i = 0; i < orderIds.size(); i += 10) {
                orderChunks.add(orderIds.subList(i, Math.min(i + 10, orderIds.size())));
            }

            for (List<String> chunk : orderChunks) {
                var t = db.collection("orders")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                        .get()
                        .addOnSuccessListener(snap -> {
                            if (snap == null) return;
                            for (DocumentSnapshot d : snap.getDocuments()) {
                                String status = d.getString("status");
                                boolean cancelled = status != null &&
                                        (status.equalsIgnoreCase("cancelled") || status.equalsIgnoreCase("refunded"));
                                if (!cancelled) {
                                    long[] totals = orderTotals.get(d.getId());
                                    if (totals != null) {
                                        tickets[0] += totals[0];
                                        revenue[0] += totals[1];
                                    }
                                }
                            }
                        });
                orderTasks.add(t);
            }

            Tasks.whenAllSuccess(orderTasks).addOnCompleteListener(done -> {
                if (idx < orgList.size()) {
                    orgList.get(idx).totalTickets = tickets[0];
                    orgList.get(idx).totalRevenue = revenue[0];
                    if (adapter != null) adapter.notifyItemChanged(idx);
                }
            });
        });
    }

    private void countTicketsOnly(List<String> eventIds, int idx) {
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < eventIds.size(); i += 10) {
            chunks.add(eventIds.subList(i, Math.min(i + 10, eventIds.size())));
        }
        List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
        final long[] count = {0};
        for (List<String> chunk : chunks) {
            tasks.add(db.collection("user_tickets").whereIn("event_id", chunk).get()
                    .addOnSuccessListener(s -> { if (s != null) count[0] += s.size(); }));
        }
        Tasks.whenAllSuccess(tasks).addOnCompleteListener(done -> {
            if (idx < orgList.size()) {
                orgList.get(idx).totalTickets = count[0];
                if (adapter != null) adapter.notifyItemChanged(idx);
            }
        });
    }

    private void openOrgDetail(OrgCardData card) {
        Organizer org = card.organizer;
        sessionManager.setActiveOrganizer(
                org.getOrganizerId(),
                org.getBrandName(),
                org.getLogoUrl()
        );

        Intent intent = new Intent(requireContext(), OrganizerRevenueActivity.class);
        intent.putExtra(OrganizerRevenueActivity.EXTRA_ORGANIZER_ID, org.getOrganizerId());
        intent.putExtra(OrganizerRevenueActivity.EXTRA_ORGANIZER_NAME, org.getBrandName());
        intent.putExtra("extra_is_verified", org.isVerified());
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        pbProfileLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(boolean show) {
        layoutOrgEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        rvOrganizerProfiles.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void logout() {
        auth.signOut();
        sessionManager.logoutUser();
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // ── Data holder ─────────────────────────────────────────────────────────

    static class OrgCardData {
        Organizer organizer;
        long eventCount = 0;
        long totalTickets = 0;
        long totalRevenue = 0;

        OrgCardData(Organizer organizer) {
            this.organizer = organizer;
        }
    }

    // ── Adapter ─────────────────────────────────────────────────────────────

    interface OnOrgClickListener {
        void onOrgClick(OrgCardData card);
    }

    static class OrgProfileAdapter extends RecyclerView.Adapter<OrgProfileAdapter.VH> {

        private final List<OrgCardData> items;
        private final OnOrgClickListener listener;

        OrgProfileAdapter(List<OrgCardData> items, OnOrgClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_organizer_profile_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            OrgCardData card = items.get(position);
            Organizer org = card.organizer;

            h.tvBrandName.setText(org.getBrandName() != null ? org.getBrandName() : "Ban tổ chức");

            // Description line: website or description fallback
            String desc = org.getWebsite();
            if (desc == null || desc.isEmpty()) desc = org.getDescription();
            if (desc == null || desc.isEmpty()) desc = "Đơn vị tổ chức sự kiện";
            h.tvDescription.setText(desc);

            // Verified badge — solid pill
            boolean verified = org.isVerified();
            android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
            pill.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            pill.setCornerRadius(100f);
            if (verified) {
                pill.setColor(0x1A27AE60); // light green bg
                h.tvVerifiedChip.setText("● Đã xác thực");
                h.tvVerifiedChip.setTextColor(0xFF27AE60);
            } else {
                pill.setColor(0x1AE6A817); // light amber bg
                h.tvVerifiedChip.setText("● Chờ xác thực");
                h.tvVerifiedChip.setTextColor(0xFFB8860B);
            }
            h.tvVerifiedChip.setBackground(pill);

            // Accent bar
            h.viewAccentBar.setBackgroundColor(
                    verified ? 0xFF2563EB : 0xFFE6A817);

            // Stats
            h.tvStatEvents.setText(String.valueOf(card.eventCount));
            h.tvStatTickets.setText(formatCompact(card.totalTickets));
            h.tvStatRevenue.setText(formatRevenue(card.totalRevenue));

            // Logo
            if (org.getLogoUrl() != null && !org.getLogoUrl().isEmpty()) {
                Glide.with(h.ivOrgLogo.getContext())
                        .load(org.getLogoUrl())
                        .circleCrop()
                        .placeholder(R.drawable.bg_avatar_circle)
                        .into(h.ivOrgLogo);
            } else {
                h.ivOrgLogo.setImageResource(R.drawable.ic_role_building);
            }

            h.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOrgClick(card);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        private String formatCompact(long n) {
            if (n >= 1_000_000) return String.format(Locale.getDefault(), "%.1fM", n / 1_000_000.0);
            if (n >= 1_000) return String.format(Locale.getDefault(), "%.1fK", n / 1_000.0);
            return String.valueOf(n);
        }

        private String formatRevenue(long amount) {
            if (amount == 0) return "0đ";
            if (amount >= 1_000_000_000) return String.format(Locale.getDefault(), "%.1fBđ", amount / 1_000_000_000.0);
            if (amount >= 1_000_000) return String.format(Locale.getDefault(), "%.1fMđ", amount / 1_000_000.0);
            if (amount >= 1_000) return String.format(Locale.getDefault(), "%.0fKđ", amount / 1_000.0);
            return new DecimalFormat("#,###").format(amount) + "đ";
        }

        static class VH extends RecyclerView.ViewHolder {
            android.widget.ImageView ivOrgLogo;
            TextView tvBrandName, tvVerifiedChip, tvDescription;
            TextView tvStatEvents, tvStatTickets, tvStatRevenue;
            View viewAccentBar;

            VH(@NonNull View v) {
                super(v);
                ivOrgLogo = v.findViewById(R.id.ivOrgLogo);
                tvBrandName = v.findViewById(R.id.tvOrgBrandName);
                tvVerifiedChip = v.findViewById(R.id.tvVerifiedChip);
                tvDescription = v.findViewById(R.id.tvOrgDescription);
                tvStatEvents = v.findViewById(R.id.tvStatEvents);
                tvStatTickets = v.findViewById(R.id.tvStatTickets);
                tvStatRevenue = v.findViewById(R.id.tvStatRevenue);
                viewAccentBar = v.findViewById(R.id.viewAccentBar);
            }
        }
    }
}
