package com.example.vibetix.Fragments.User;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.TicketType;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.EventRepository;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectTicketFragment extends Fragment {

    private String eventId = "";
    private String eventImageUrl = "";

    private final EventRepository eventRepository = new EventRepository();
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    private List<TicketType> ticketTypes = new ArrayList<>();
    private Map<String, Integer> selectedQty = new HashMap<>(); // typeId → qty
    private static final int MAX_QTY_PER_TYPE = 5;

    // Views
    private ImageView btnSelectBack;
    private ImageView imvSelectEventThumb;
    private TextView txtSelectEventTitle;
    private TextView txtSelectEventDate;
    private TextView txtSelectEventLocation;
    // Legacy hidden views (still bound to avoid NPE from old code paths)
    private TextView txtSelectTicketTypeName;
    private TextView txtSelectTicketPrice;
    private TextView txtSelectRemainingBadge;
    private ImageButton btnSelectQtyMinus;
    private ImageButton btnSelectQtyPlus;
    private TextView txtSelectQtyValue;
    // New views
    private LinearLayout layoutTicketTypeRows;
    private CardView cardSeatmap;
    private ImageView imvSeatmap;
    private ImageView imvSelectBottomThumb;
    private TextView txtSelectBottomEventTitle;
    private TextView txtSelectBottomQty;
    private TextView txtSelectBottomTotal;
    private Button btnSelectNext;

    public static SelectTicketFragment newInstance(String eventId) {
        SelectTicketFragment fragment = new SelectTicketFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId", "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_ticket, container, false);
        bindViews(view);
        applyInsets(view);
        loadEventDetails();
        setupClickListeners();
        updateBottomBar();
        return view;
    }

    private void applyInsets(View view) {
        View header = view.findViewById(R.id.layoutSelectHeader);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                if (top <= 0) {
                    int resId = v.getResources().getIdentifier("status_bar_height", "dimen", "android");
                    if (resId > 0) top = v.getResources().getDimensionPixelSize(resId);
                }
                if (top <= 0) top = (int) (28 * v.getResources().getDisplayMetrics().density);
                v.setPadding(0, top, 0, 0);
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
        }
    }

    private void bindViews(View view) {
        btnSelectBack = view.findViewById(R.id.btnSelectBack);
        imvSelectEventThumb = view.findViewById(R.id.imvSelectEventThumb);
        txtSelectEventTitle = view.findViewById(R.id.txtSelectEventTitle);
        txtSelectEventDate = view.findViewById(R.id.txtSelectEventDate);
        txtSelectEventLocation = view.findViewById(R.id.txtSelectEventLocation);
        txtSelectTicketTypeName = view.findViewById(R.id.txtSelectTicketTypeName);
        txtSelectTicketPrice = view.findViewById(R.id.txtSelectTicketPrice);
        txtSelectRemainingBadge = view.findViewById(R.id.txtSelectRemainingBadge);
        btnSelectQtyMinus = view.findViewById(R.id.btnSelectQtyMinus);
        btnSelectQtyPlus = view.findViewById(R.id.btnSelectQtyPlus);
        txtSelectQtyValue = view.findViewById(R.id.txtSelectQtyValue);
        layoutTicketTypeRows = view.findViewById(R.id.layoutTicketTypeRows);
        cardSeatmap = view.findViewById(R.id.cardSeatmap);
        imvSeatmap = view.findViewById(R.id.imvSeatmap);
        imvSelectBottomThumb = view.findViewById(R.id.imvSelectBottomThumb);
        txtSelectBottomEventTitle = view.findViewById(R.id.txtSelectBottomEventTitle);
        txtSelectBottomQty = view.findViewById(R.id.txtSelectBottomQty);
        txtSelectBottomTotal = view.findViewById(R.id.txtSelectBottomTotal);
        btnSelectNext = view.findViewById(R.id.btnSelectNext);
    }

    private void loadEventDetails() {
        eventRepository.getEventById(eventId, new EventRepository.OnEventLoadedListener() {
            @Override
            public void onSuccess(Event event) {
                if (!isAdded() || event == null) return;
                txtSelectEventTitle.setText(event.getTitle() != null ? event.getTitle() : "");
                txtSelectBottomEventTitle.setText(event.getTitle() != null ? event.getTitle() : "");
                txtSelectEventDate.setText(event.getDate() != null ? event.getDate() : "--");
                txtSelectEventLocation.setText(event.getLocation() != null ? event.getLocation() : "--");

                String img = event.getImageUrl();
                if (img != null && !img.isEmpty()) {
                    eventImageUrl = img;
                    Glide.with(requireContext()).load(img)
                            .placeholder(R.drawable.event_live_non_song)
                            .into(imvSelectEventThumb);
                    Glide.with(requireContext()).load(img)
                            .placeholder(R.drawable.event_live_non_song)
                            .into(imvSelectBottomThumb);
                }

                // Seatmap
                String seatmapUrl = event.getSeatmapUrl();
                if (seatmapUrl != null && !seatmapUrl.isEmpty() && cardSeatmap != null) {
                    cardSeatmap.setVisibility(View.VISIBLE);
                    Glide.with(requireContext()).load(seatmapUrl).into(imvSeatmap);
                }
            }
            @Override
            public void onFailure(Exception e) {}
        });

        eventRepository.getTicketTypesForEvent(eventId, new EventRepository.OnTicketTypesLoadedListener() {
            @Override
            public void onSuccess(List<TicketType> types) {
                if (!isAdded()) return;
                ticketTypes = types != null ? types : new ArrayList<>();
                if (ticketTypes.isEmpty()) {
                    // Fallback: show 1 generic ticket type
                    TicketType mock = new TicketType();
                    mock.setTicketTypeId("default");
                    mock.setName("Vé Tiêu chuẩn");
                    mock.setPrice(400000);
                    mock.setAvailableQuantity(100);
                    ticketTypes.add(mock);
                }
                populateTicketTypeRows();
            }
            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                TicketType mock = new TicketType();
                mock.setTicketTypeId("default");
                mock.setName("Vé Tiêu chuẩn");
                mock.setPrice(400000);
                mock.setAvailableQuantity(100);
                ticketTypes.add(mock);
                populateTicketTypeRows();
            }
        });
    }

    private void populateTicketTypeRows() {
        if (layoutTicketTypeRows == null) return;
        layoutTicketTypeRows.removeAllViews();

        int dp1 = dp(1); int dp12 = dp(12); int dp16 = dp(16);

        for (int i = 0; i < ticketTypes.size(); i++) {
            TicketType t = ticketTypes.get(i);
            String tid = t.getTypeId() != null ? t.getTypeId() : ("t" + i);
            selectedQty.put(tid, 0);

            // Row container
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.topMargin = dp12;
            rowLp.bottomMargin = dp12;
            row.setLayoutParams(rowLp);

            // Left: name + price + remaining
            LinearLayout left = new LinearLayout(requireContext());
            left.setOrientation(LinearLayout.VERTICAL);
            left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvName = new TextView(requireContext());
            tvName.setText(t.getName() != null ? t.getName() : "Vé");
            tvName.setTextColor(0xFF1A2533);
            tvName.setTextSize(15f);
            tvName.setTypeface(null, Typeface.BOLD);

            TextView tvPrice = new TextView(requireContext());
            tvPrice.setText(formatter.format(t.getPrice()) + " đ");
            tvPrice.setTextColor(0xFF22C55E);
            tvPrice.setTextSize(14f);

            left.addView(tvName);
            left.addView(tvPrice);

            TextView tvRemaining = new TextView(requireContext());
            if (t.getAvailableQuantity() > 0) {
                tvRemaining.setText("Còn " + t.getAvailableQuantity() + " vé");
                tvRemaining.setTextColor(0xFF808E92);
            } else {
                tvRemaining.setText("Hết vé");
                tvRemaining.setTextColor(0xFFEF4444);
                tvRemaining.setTypeface(null, Typeface.BOLD);
            }
            tvRemaining.setTextSize(12f);
            left.addView(tvRemaining);

            // Right: - qty + controls
            LinearLayout qtyRow = new LinearLayout(requireContext());
            qtyRow.setOrientation(LinearLayout.HORIZONTAL);
            qtyRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            qtyRow.setBackground(requireContext().getDrawable(R.drawable.bg_input_normal));
            qtyRow.setPadding(dp(4), dp(4), dp(4), dp(4));

            Button btnMinus = new Button(requireContext());
            btnMinus.setText("−");
            btnMinus.setTextSize(18f);
            btnMinus.setTextColor(0xFF22C55E);
            btnMinus.setBackgroundResource(android.R.color.transparent);
            btnMinus.setMinWidth(dp(36));
            btnMinus.setMinHeight(dp(36));
            btnMinus.setPadding(dp(4), 0, dp(4), 0);

            final TextView tvQty = new TextView(requireContext());
            tvQty.setText("0");
            tvQty.setTextColor(0xFF1A2533);
            tvQty.setTextSize(16f);
            tvQty.setTypeface(null, Typeface.BOLD);
            tvQty.setGravity(android.view.Gravity.CENTER);
            tvQty.setMinWidth(dp(40));

            Button btnPlus = new Button(requireContext());
            btnPlus.setText("+");
            btnPlus.setTextSize(18f);
            btnPlus.setTextColor(0xFF22C55E);
            btnPlus.setBackgroundResource(android.R.color.transparent);
            btnPlus.setMinWidth(dp(36));
            btnPlus.setMinHeight(dp(36));
            btnPlus.setPadding(dp(4), 0, dp(4), 0);

            qtyRow.addView(btnMinus);
            qtyRow.addView(tvQty);
            qtyRow.addView(btnPlus);

            row.addView(left);
            row.addView(qtyRow);
            layoutTicketTypeRows.addView(row);

            // Add divider except last
            if (i < ticketTypes.size() - 1) {
                View divider = new View(requireContext());
                divider.setBackgroundColor(0xFFEEF0F2);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp1));
                layoutTicketTypeRows.addView(divider);
            }

            // Wire buttons
            final String finalTid = tid;
            final long price = t.getPrice();
            btnMinus.setOnClickListener(v -> {
                int cur = selectedQty.getOrDefault(finalTid, 0);
                if (cur > 0) {
                    selectedQty.put(finalTid, cur - 1);
                    tvQty.setText(String.valueOf(cur - 1));
                    updateBottomBar();
                }
            });
            btnPlus.setOnClickListener(v -> {
                int cur = selectedQty.getOrDefault(finalTid, 0);
                long avail = t.getAvailableQuantity();
                int limit = avail > 0 ? (int) Math.min(avail, MAX_QTY_PER_TYPE) : 0;
                if (cur < limit) {
                    selectedQty.put(finalTid, cur + 1);
                    tvQty.setText(String.valueOf(cur + 1));
                    updateBottomBar();
                }
            });
        }
    }

    private void updateBottomBar() {
        long total = 0;
        int totalQty = 0;
        for (int i = 0; i < ticketTypes.size(); i++) {
            TicketType t = ticketTypes.get(i);
            String tid = t.getTypeId() != null ? t.getTypeId() : ("t" + i);
            int qty = selectedQty.getOrDefault(tid, 0);
            total += t.getPrice() * qty;
            totalQty += qty;
        }

        if (txtSelectBottomQty != null) txtSelectBottomQty.setText("x" + totalQty);
        if (txtSelectBottomTotal != null) txtSelectBottomTotal.setText(formatter.format(total) + " đ");

        if (btnSelectNext != null) {
            if (totalQty > 0) {
                btnSelectNext.setEnabled(true);
                btnSelectNext.setText("Tiếp tục ›");
                btnSelectNext.setBackgroundTintList(ColorStateList.valueOf(0xFF22C55E));
            } else {
                btnSelectNext.setEnabled(false);
                btnSelectNext.setText("Vui lòng chọn vé");
                btnSelectNext.setBackgroundTintList(ColorStateList.valueOf(0xFF9CA3AF));
            }
        }
    }

    private void setupClickListeners() {
        if (btnSelectBack != null) {
            btnSelectBack.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        if (btnSelectNext != null) {
            btnSelectNext.setOnClickListener(v -> proceedToFillInfo());
        }
    }

    private void proceedToFillInfo() {
        // Build summary for FillAttendeeInfoFragment
        StringBuilder nameBuilder = new StringBuilder();
        long totalPrice = 0;
        int totalQty = 0;
        long firstPrice = 0;

        for (int i = 0; i < ticketTypes.size(); i++) {
            TicketType t = ticketTypes.get(i);
            String tid = t.getTypeId() != null ? t.getTypeId() : ("t" + i);
            int qty = selectedQty.getOrDefault(tid, 0);
            if (qty <= 0) continue;
            if (nameBuilder.length() > 0) nameBuilder.append(", ");
            nameBuilder.append(qty).append("x ").append(t.getName());
            totalPrice += t.getPrice() * qty;
            if (totalQty == 0) firstPrice = t.getPrice();
            totalQty += qty;
        }

        if (totalQty == 0) return;

        // Serialize chi tiết từng loại vé thành JSON để truyền qua Bundle
        org.json.JSONArray ticketItemsJson = new org.json.JSONArray();
        for (int i = 0; i < ticketTypes.size(); i++) {
            TicketType t = ticketTypes.get(i);
            String tid = t.getTypeId() != null ? t.getTypeId() : ("t" + i);
            int qty = selectedQty.getOrDefault(tid, 0);
            if (qty <= 0) continue;
            try {
                org.json.JSONObject item = new org.json.JSONObject();
                item.put("typeId", tid);
                item.put("name", t.getName());
                item.put("qty", qty);
                item.put("price", t.getPrice());
                ticketItemsJson.put(item);
            } catch (org.json.JSONException ignored) {}
        }

        FillAttendeeInfoFragment fillFrag = FillAttendeeInfoFragment.newInstance(
                eventId,
                nameBuilder.toString(),
                totalQty,
                firstPrice,
                totalPrice,
                eventImageUrl
        );
        // Truyền thêm chi tiết loại vé
        if (fillFrag.getArguments() != null) {
            fillFrag.getArguments().putString("ticketItemsJson", ticketItemsJson.toString());
        }

        if (getActivity() instanceof com.example.vibetix.Activities.User.UserMainActivity) {
            ((com.example.vibetix.Activities.User.UserMainActivity) getActivity()).openSubFragment(fillFrag);
        }
    }

    private int dp(int dpVal) {
        return (int) (dpVal * requireContext().getResources().getDisplayMetrics().density);
    }
}
