package com.example.vibetix.Fragments.Organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.example.vibetix.Activities.User.CreateOrganizerActivity;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.Models.User;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OrganizerProfileFragment — Redesigned with:
 * - Gradient blue header, avatar, brand name, verified badge, edit profile button
 * - 3-column stats (events, tickets, revenue) from real Firestore data
 * - Info card (email, phone, website)
 * - Multi-org RecyclerView with "+ Thêm hồ sơ" button
 * - Settings section (notification, payment, password)
 * - Logout button
 */
public class OrganizerProfileFragment extends Fragment {

    // Header
    private ImageView ivOrgAvatar;
    private TextView tvOrgBrandName;
    private LinearLayout layoutVerifiedBadge;
    private TextView tvVerifiedBadge;
    private MaterialButton btnEditProfile;

    // Stats
    private TextView tvTotalEvents;
    private TextView tvMonthlyRevenue;
    private TextView tvTotalTicketsSold;

    // Info card
    private TextView tvOrgEmail;
    private TextView tvOrgPhone;
    private TextView tvOrgWebsite;

    // Multi-org section
    private RecyclerView rvOrganizerProfiles;
    private MaterialButton btnAddOrgProfile;

    // State
    private ProgressBar pbProfileLoading;
    private TextView tvProfileError;

    // Logout
    private MaterialButton btnOrgLogout;

    // Firebase & session
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    // Adapter data
    private final List<Organizer> organizerList = new ArrayList<>();
    private OrgProfileMiniAdapter orgAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_organizer_profile, container, false);
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());

        bindViews(view);
        setupRecyclerView();
        setupClickListeners();
        loadData();

        return view;
    }

    private void bindViews(View view) {
        ivOrgAvatar         = view.findViewById(R.id.ivOrgAvatar);
        tvOrgBrandName      = view.findViewById(R.id.tvOrgBrandName);
        layoutVerifiedBadge = view.findViewById(R.id.layoutVerifiedBadge);
        tvVerifiedBadge     = view.findViewById(R.id.tvVerifiedBadge);
        btnEditProfile      = view.findViewById(R.id.btnEditProfile);

        tvTotalEvents       = view.findViewById(R.id.tvTotalEvents);
        tvMonthlyRevenue    = view.findViewById(R.id.tvMonthlyRevenue);
        tvTotalTicketsSold  = view.findViewById(R.id.tvTotalTicketsSold);

        tvOrgEmail          = view.findViewById(R.id.tvOrgEmail);
        tvOrgPhone          = view.findViewById(R.id.tvOrgPhone);
        tvOrgWebsite        = view.findViewById(R.id.tvOrgWebsite);

        rvOrganizerProfiles = view.findViewById(R.id.rvOrganizerProfiles);
        btnAddOrgProfile    = view.findViewById(R.id.btnAddOrgProfile);

        pbProfileLoading    = view.findViewById(R.id.pbProfileLoading);
        tvProfileError      = view.findViewById(R.id.tvProfileError);
        btnOrgLogout        = view.findViewById(R.id.btnOrgLogout);
    }

    private void setupRecyclerView() {
        orgAdapter = new OrgProfileMiniAdapter(organizerList, organizer -> {
            // Switch active organizer on tap
            sessionManager.setActiveOrganizer(
                    organizer.getOrganizerId(),
                    organizer.getBrandName(),
                    organizer.getLogoUrl()
            );
            // Reload header display
            displayProfile(organizer, sessionManager.getUserDetails());
        });
        rvOrganizerProfiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrganizerProfiles.setNestedScrollingEnabled(false);
        rvOrganizerProfiles.setAdapter(orgAdapter);
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CreateOrganizerActivity.class));
        });

        btnAddOrgProfile.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CreateOrganizerActivity.class));
        });

        btnOrgLogout.setOnClickListener(v -> logout());
    }

    private void loadData() {
        User currentUser = sessionManager.getUserDetails();
        if (currentUser == null) {
            showError("Không tìm thấy thông tin người dùng.");
            return;
        }
        showLoading(true);

        // Load ALL organizer profiles for this user
        db.collection("organizers")
                .whereEqualTo("user_id", currentUser.getUserId())
                .get()
                .addOnSuccessListener(query -> {
                    showLoading(false);
                    organizerList.clear();

                    if (query != null && !query.isEmpty()) {
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            Organizer org = doc.toObject(Organizer.class);
                            if (org != null) {
                                // Ensure ID populated from doc ID as fallback
                                if (org.getOrganizerId() == null) {
                                    org.setOrganizerId(doc.getId());
                                }
                                organizerList.add(org);
                            }
                        }
                        orgAdapter.notifyDataSetChanged();

                        // Show active organizer in header
                        String activeId = sessionManager.getActiveOrganizerId();
                        Organizer activeOrg = null;
                        for (Organizer o : organizerList) {
                            if (o.getOrganizerId().equals(activeId)) {
                                activeOrg = o;
                                break;
                            }
                        }
                        if (activeOrg == null) activeOrg = organizerList.get(0);

                        displayProfile(activeOrg, currentUser);
                        final String orgId = activeOrg.getOrganizerId();
                        loadEventStats(orgId);  // also loads revenue inside

                    } else {
                        displayFallbackProfile(currentUser);
                        displayMockStats();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    displayFallbackProfile(currentUser);
                    displayMockStats();
                });
    }

    private void displayProfile(Organizer org, User fallbackUser) {
        // Brand name
        String brandName = org.getBrandName();
        if (brandName == null || brandName.isEmpty()) {
            brandName = fallbackUser != null && fallbackUser.getFullName() != null
                    ? fallbackUser.getFullName() : "Organizer";
        }
        tvOrgBrandName.setText(brandName);

        // Avatar via Glide
        String logoUrl = org.getLogoUrl();
        if (logoUrl != null && !logoUrl.isEmpty() && isAdded()) {
            Glide.with(this)
                    .load(logoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_role_building)
                    .into(ivOrgAvatar);
        } else {
            ivOrgAvatar.setImageResource(R.drawable.ic_role_building);
        }

        // Email
        String email = org.getContactEmail();
        if (email == null || email.isEmpty()) {
            email = fallbackUser != null ? fallbackUser.getEmail() : null;
        }
        tvOrgEmail.setText(email != null ? email : "—");

        // Phone
        String phone = org.getContactPhone();
        if (phone == null || phone.isEmpty()) {
            phone = fallbackUser != null ? fallbackUser.getPhone() : null;
        }
        tvOrgPhone.setText(phone != null ? phone : "—");

        // Website
        String website = org.getWebsiteUrl();
        tvOrgWebsite.setText(website != null && !website.isEmpty() ? website : "—");

        // Verified badge
        updateVerifiedBadge(org.isVerified());
    }

    private void displayFallbackProfile(User user) {
        String name = user.getFullName() != null ? user.getFullName() : user.getEmail();
        tvOrgBrandName.setText(name != null ? name : "Organizer");
        tvOrgEmail.setText(user.getEmail() != null ? user.getEmail() : "—");
        tvOrgPhone.setText(user.getPhone() != null ? user.getPhone() : "—");
        tvOrgWebsite.setText("—");
        updateVerifiedBadge(false);
    }

    private void loadEventStats(String organizerId) {
        // Query events where user created them (via event_staff as owner)
        // Use organizer_id if available, fallback to user_id from session
        String userId = sessionManager.getUserDetails() != null
                ? sessionManager.getUserDetails().getUserId() : null;

        com.google.firebase.firestore.Query eventsQuery = userId != null
                ? db.collection("events").whereEqualTo("user_id", userId)
                : db.collection("events").whereEqualTo("organizer_id", organizerId);

        eventsQuery.get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    int totalEvents = snapshot != null ? snapshot.size() : 0;
                    tvTotalEvents.setText(String.valueOf(totalEvents));

                    if (snapshot == null || snapshot.isEmpty()) {
                        tvTotalTicketsSold.setText("0");
                        tvMonthlyRevenue.setText("0đ");
                        return;
                    }

                    // Collect all event IDs
                    List<String> eventIds = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        eventIds.add(doc.getId());
                    }

                    loadRevenueFromOrderItems(eventIds);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    tvTotalEvents.setText("—");
                    tvTotalTicketsSold.setText("—");
                });
    }

    /**
     * Tính doanh thu tổng bằng cách:
     * 1. Query order_items WHERE event_id IN (eventIds) — chunk mỗi 10
     * 2. Tính tổng price_per_ticket * quantity
     */
    private void loadRevenueFromOrderItems(List<String> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            if (isAdded()) {
                tvMonthlyRevenue.setText("0đ");
                tvTotalTicketsSold.setText("0");
            }
            return;
        }

        List<com.google.android.gms.tasks.Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (int i = 0; i < eventIds.size(); i += 10) {
            List<String> chunk = eventIds.subList(i, Math.min(i + 10, eventIds.size()));
            tasks.add(db.collection(com.example.vibetix.Firebase.FirebaseCollections.ORDER_ITEMS)
                    .whereIn("event_id", chunk)
                    .get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            if (!isAdded()) return;
            
            List<com.example.vibetix.Models.OrderItem> allItems = new ArrayList<>();
            java.util.Set<String> orderIds = new java.util.HashSet<>();
            for (Object result : results) {
                QuerySnapshot snap = (QuerySnapshot) result;
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    com.example.vibetix.Models.OrderItem item = doc.toObject(com.example.vibetix.Models.OrderItem.class);
                    if (item != null) {
                        allItems.add(item);
                        if (item.getOrderId() != null) {
                            orderIds.add(item.getOrderId());
                        }
                    }
                }
            }

            if (orderIds.isEmpty()) {
                tvMonthlyRevenue.setText("0đ");
                tvTotalTicketsSold.setText("0");
                return;
            }

            List<String> orderIdList = new ArrayList<>(orderIds);
            List<com.google.android.gms.tasks.Task<QuerySnapshot>> orderTasks = new ArrayList<>();
            for (int i = 0; i < orderIdList.size(); i += 10) {
                List<String> chunk = orderIdList.subList(i, Math.min(i + 10, orderIdList.size()));
                orderTasks.add(db.collection(com.example.vibetix.Firebase.FirebaseCollections.ORDERS)
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                        .get());
            }

            Tasks.whenAllSuccess(orderTasks).addOnSuccessListener(orderResults -> {
                if (!isAdded()) return;
                
                java.util.Map<String, String> orderStatusMap = new java.util.HashMap<>();
                for (Object orderResult : orderResults) {
                    QuerySnapshot orderSnap = (QuerySnapshot) orderResult;
                    for (DocumentSnapshot doc : orderSnap.getDocuments()) {
                        String status = doc.getString("status");
                        orderStatusMap.put(doc.getId(), status != null ? status.toLowerCase() : "pending");
                    }
                }

                double totalRevenue = 0;
                long totalTickets = 0;
                for (com.example.vibetix.Models.OrderItem item : allItems) {
                    String status = orderStatusMap.get(item.getOrderId());
                    boolean isPaid = status != null && (status.equals("completed") || status.equals("confirmed") || status.equals("paid"));
                    
                    if (isPaid) {
                        long q = item.getQuantity();
                        totalTickets += q;
                        totalRevenue += item.getPricePerTicket() * q;
                    }
                }

                tvMonthlyRevenue.setText(formatRevenue(totalRevenue));
                tvTotalTicketsSold.setText(formatCompact(totalTickets));
            });
        }).addOnFailureListener(e -> {
            if (!isAdded()) return;
            tvMonthlyRevenue.setText("—");
            tvTotalTicketsSold.setText("—");
        });
    }

    private void displayMockStats() {
        tvTotalEvents.setText("—");
        tvMonthlyRevenue.setText("—");
        tvTotalTicketsSold.setText("—");
    }

    private void updateVerifiedBadge(boolean isVerified) {
        if (!isAdded()) return;
        if (isVerified) {
            tvVerifiedBadge.setText("✓ Đã xác minh");
            tvVerifiedBadge.setTextColor(requireContext().getColor(R.color.clr_success));
        } else {
            tvVerifiedBadge.setText("⏳ Chờ xác minh");
            tvVerifiedBadge.setTextColor(requireContext().getColor(R.color.clr_warning));
        }
    }

    private String formatCompact(long number) {
        if (number >= 1_000_000) return String.format(Locale.getDefault(), "%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format(Locale.getDefault(), "%.1fK", number / 1_000.0);
        return String.valueOf(number);
    }

    private String formatRevenue(double amount) {
        if (amount == 0) return "0đ";
        if (amount >= 1_000_000_000) return String.format(Locale.getDefault(), "%.1fB", amount / 1_000_000_000.0);
        if (amount >= 1_000_000) return String.format(Locale.getDefault(), "%.1fM", amount / 1_000_000.0);
        if (amount >= 1_000) return String.format(Locale.getDefault(), "%.0fK", amount / 1_000.0);
        return new DecimalFormat("#,###").format(amount) + "đ";
    }

    private void showLoading(boolean show) {
        if (pbProfileLoading != null)
            pbProfileLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        hideError();
    }

    private void showError(String message) {
        if (tvProfileError != null) {
            tvProfileError.setText(message);
            tvProfileError.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {
        if (tvProfileError != null) tvProfileError.setVisibility(View.GONE);
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        sessionManager.logoutUser();
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inline mini-adapter for organizer profiles list
    // ─────────────────────────────────────────────────────────────────────────

    interface OnOrgClickListener {
        void onOrgClick(Organizer organizer);
    }

    static class OrgProfileMiniAdapter extends RecyclerView.Adapter<OrgProfileMiniAdapter.VH> {

        private final List<Organizer> items;
        private final OnOrgClickListener listener;

        OrgProfileMiniAdapter(List<Organizer> items, OnOrgClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_organizer_switcher, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Organizer org = items.get(position);
            holder.tvName.setText(org.getBrandName() != null ? org.getBrandName() : "—");
            String email = org.getContactEmail();
            holder.tvSub.setText(email != null && !email.isEmpty() ? email :
                    (org.isVerified() ? "✓ Đã xác minh" : "⏳ Chờ xác minh"));

            if (org.getLogoUrl() != null && !org.getLogoUrl().isEmpty()) {
                Glide.with(holder.ivLogo.getContext())
                        .load(org.getLogoUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_organizer_placeholder)
                        .into(holder.ivLogo);
            } else {
                holder.ivLogo.setImageResource(R.drawable.ic_organizer_placeholder);
            }

            holder.itemView.setOnClickListener(v -> listener.onOrgClick(org));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView ivLogo;
            TextView tvName, tvSub;

            VH(@NonNull View v) {
                super(v);
                ivLogo = v.findViewById(R.id.ivOrgSwitchLogo);
                tvName = v.findViewById(R.id.tvOrgSwitchName);
                tvSub  = v.findViewById(R.id.tvOrgSwitchSub);
            }
        }
    }
}
