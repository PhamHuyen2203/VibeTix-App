package com.example.vibetix.Activities.User;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.TicketType;
import com.example.vibetix.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * EventDetailActivity — Xem chi tiết sự kiện từ góc nhìn người mua.
 *
 * Hiển thị: poster, tên, mô tả, thời gian, địa điểm, ban tổ chức.
 * Ticket types: danh sách loại vé + nút mua.
 */
public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";

    private ImageView ivPoster;
    private TextView tvTitle, tvDate, tvTime, tvVenue, tvDescription;
    private TextView tvOrgName, tvOrgStatus;
    private RecyclerView rvTicketTypes;
    private View btnBuyTicket;
    private View loadingView;

    private FirebaseFirestore db;
    private String eventId;
    private Event currentEvent;
    private List<TicketType> ticketTypes = new ArrayList<>();
    private TicketTypeAdapter ticketAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        bindViews();
        setupToolbar();
        setupTicketRecycler();

        if (eventId != null) {
            loadEventDetail(eventId);
        } else {
            finish();
        }
    }

    private void bindViews() {
        ivPoster     = findViewById(R.id.ivEventDetailPoster);
        tvTitle      = findViewById(R.id.tvEventDetailTitle);
        tvDate       = findViewById(R.id.tvEventDetailDate);
        tvTime       = findViewById(R.id.tvEventDetailTime);
        tvVenue      = findViewById(R.id.tvEventDetailVenue);
        tvDescription = findViewById(R.id.tvEventDetailDescription);
        tvOrgName    = findViewById(R.id.tvOrganizerName);
        tvOrgStatus  = findViewById(R.id.tvOrganizerStatus);
        rvTicketTypes = findViewById(R.id.rvTicketTypes);
        btnBuyTicket = findViewById(R.id.btnBuyTicket);
        loadingView  = findViewById(R.id.progressLoading);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarEventDetail);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("");
            }
        }
    }

    private void setupTicketRecycler() {
        ticketAdapter = new TicketTypeAdapter(ticketTypes);
        if (rvTicketTypes != null) {
            rvTicketTypes.setLayoutManager(new LinearLayoutManager(this));
            rvTicketTypes.setAdapter(ticketAdapter);
            rvTicketTypes.setNestedScrollingEnabled(false);
        }
        if (btnBuyTicket != null) {
            btnBuyTicket.setOnClickListener(v -> handleBuyTicket());
        }
    }

    private void loadEventDetail(String id) {
        if (loadingView != null) loadingView.setVisibility(View.VISIBLE);

        db.collection(FirebaseCollections.EVENTS).document(id).get()
                .addOnSuccessListener(doc -> {
                    if (loadingView != null) loadingView.setVisibility(View.GONE);
                    if (doc.exists()) {
                        currentEvent = doc.toObject(Event.class);
                        if (currentEvent != null) {
                            populateUI(currentEvent);
                            loadTicketTypes(id);
                            loadOrganizerInfo(currentEvent.getOrganizerId());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (loadingView != null) loadingView.setVisibility(View.GONE);
                    Toast.makeText(this, "Không thể tải sự kiện", Toast.LENGTH_SHORT).show();
                });
    }

    private void populateUI(Event event) {
        if (tvTitle != null) tvTitle.setText(event.getTitle());
        if (tvDescription != null) tvDescription.setText(event.getDescription());

        // Format date/time
        String startTime = event.getStartTime();
        if (startTime != null && startTime.length() >= 16) {
            if (tvDate != null) tvDate.setText(formatDate(startTime.substring(0, 10)));
            if (tvTime != null) tvTime.setText(startTime.substring(11, 16));
        }

        // Venue
        String venueLine = "";
        if (event.getVenueName() != null) venueLine = event.getVenueName();
        if (event.getVenueCity() != null && !event.getVenueCity().isEmpty()) {
            venueLine += (!venueLine.isEmpty() ? " • " : "") + event.getVenueCity();
        }
        if (tvVenue != null) tvVenue.setText(venueLine.isEmpty() ? "Chưa có địa điểm" : venueLine);

        // Poster
        if (ivPoster != null) {
            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(this).load(event.getPosterUrl()).centerCrop().into(ivPoster);
            } else {
                ivPoster.setImageResource(R.drawable.ic_cat_music);
            }
        }
    }

    private void loadTicketTypes(String eId) {
        db.collection(FirebaseCollections.TICKET_TYPES)
                .whereEqualTo("event_id", eId)
                .get()
                .addOnSuccessListener(snap -> {
                    ticketTypes.clear();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            try {
                                TicketType tt = doc.toObject(TicketType.class);
                                if (tt != null && tt.isActive()) { // Lọc locally để tránh lỗi index
                                    if (tt.getTypeId() == null) tt.setTypeId(doc.getId());
                                    ticketTypes.add(tt);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    ticketAdapter.notifyDataSetChanged();
                });
    }

    private void loadOrganizerInfo(String orgId) {
        if (orgId == null) return;
        db.collection(FirebaseCollections.ORGANIZERS).document(orgId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("brand_name");
                        boolean verified = Boolean.TRUE.equals(doc.getBoolean("is_verified"));
                        if (tvOrgName != null) tvOrgName.setText(name != null ? name : "Ban tổ chức");
                        if (tvOrgStatus != null)
                            tvOrgStatus.setText(verified ? "✓ Đã xác minh" : "Chưa xác minh");
                    }
                });
    }

    private void handleBuyTicket() {
        if (ticketTypes.isEmpty()) {
            Toast.makeText(this, "Chưa có loại vé nào", Toast.LENGTH_SHORT).show();
            return;
        }
        // TODO: Open purchase flow (Phase sau)
        Toast.makeText(this, "Tính năng mua vé sẽ được bổ sung sớm!", Toast.LENGTH_SHORT).show();
    }

    private String formatDate(String isoDate) {
        try {
            String[] parts = isoDate.split("-");
            return parts[2] + "/" + parts[1] + "/" + parts[0];
        } catch (Exception e) {
            return isoDate;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // ─── Inline TicketType Adapter ─────────────────────────────────────────

    static class TicketTypeAdapter extends RecyclerView.Adapter<TicketTypeAdapter.VH> {
        private final List<TicketType> items;
        TicketTypeAdapter(List<TicketType> items) { this.items = items; }

        @Override @androidx.annotation.NonNull
        public VH onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup p, int t) {
            View v = android.view.LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_ticket_selection, p, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull VH h, int pos) {
            TicketType tt = items.get(pos);
            h.tvName.setText(tt.getName() != null ? tt.getName() : "Vé");
            h.tvPrice.setText(tt.getPrice() == 0 ? "Miễn phí" :
                    NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(tt.getPrice()) + " ₫");

            if (tt.getDescription() != null && !tt.getDescription().trim().isEmpty()) {
                h.tvDesc.setText(tt.getDescription());
                h.tvDesc.setVisibility(View.VISIBLE);
            } else {
                h.tvDesc.setVisibility(View.GONE);
            }

            long qty = tt.getTotalQuantity() - tt.getSoldCount();

            // Check sale dates
            boolean isBeforeSale = false;
            boolean isAfterSale = false;
            java.util.Date now = new java.util.Date();

            try {
                if (tt.getSaleStart() != null) {
                    java.util.Date start = tt.getSaleStart().toDate();
                    if (start != null && now.before(start)) isBeforeSale = true;
                }
                if (tt.getSaleEnd() != null) {
                    java.util.Date end = tt.getSaleEnd().toDate();
                    if (end != null) {
                        end = new java.util.Date(end.getTime() + 24L * 60 * 60 * 1000 - 1);
                        if (now.after(end)) isAfterSale = true;
                    }
                }
            } catch (Exception ignored) {}

            if (isBeforeSale) {
                h.tvStatus.setText("Sắp mở bán");
                h.tvStatus.setTextColor(0xFFF2994A); // Orange
                h.tvQty.setText(formatDateForDisplay(tt.getSaleStart()));
                h.tvQty.setTextColor(0xFFF2994A);
            } else if (isAfterSale) {
                h.tvStatus.setText("Đã kết thúc bán");
                h.tvStatus.setTextColor(0xFFEB5757); // Red
                h.tvQty.setText("");
            } else if (qty <= 0) {
                h.tvStatus.setText("Hết vé");
                h.tvStatus.setTextColor(0xFFEB5757); // Red
                h.tvQty.setText("");
            } else {
                h.tvStatus.setText("Đang mở bán");
                h.tvStatus.setTextColor(0xFF27AE60); // Green
                h.tvQty.setText("Còn " + qty + " vé");
                h.tvQty.setTextColor(0xFF27AE60);
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPrice, tvQty, tvDesc, tvStatus;
            VH(View v) {
                super(v);
                tvName  = v.findViewById(R.id.tvTicketName);
                tvPrice = v.findViewById(R.id.tvTicketPrice);
                tvQty   = v.findViewById(R.id.tvTicketQuantity);
                tvDesc  = v.findViewById(R.id.tvTicketDesc);
                tvStatus= v.findViewById(R.id.tvTicketStatus);
            }
        }
    }
    private static java.util.Date parseIsoOrDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            if (dateStr.contains("T")) {
                java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                return isoFormat.parse(dateStr.split("\\.")[0]);
            } else {
                java.text.SimpleDateFormat displayFmt = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return displayFmt.parse(dateStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String formatDateForDisplay(com.google.firebase.Timestamp ts) {
        if (ts == null) return "";
        return new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ts.toDate());
    }
}
