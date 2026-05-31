package com.example.vibetix.Fragments.User;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Adapters.EventAdapter;
import com.example.vibetix.Firebase.FirestoreHelper;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import com.example.vibetix.Utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EventsFragment extends Fragment {

    // ── Views ──────────────────────────────────────────────────────────────────
    private View         eventsHeaderInclude;   // include_home_header wrapper
    private EditText     edtEventsSearch;        // from expanded search row
    private LinearLayout containerCategoryChips;
    private LinearLayout btnFilterSort;
    private TextView     txtFilterSort;
    private LinearLayout btnFilterCity;
    private TextView     txtFilterCity;
    private RecyclerView rvEvents;

    // ── Data ───────────────────────────────────────────────────────────────────
    private final ArrayList<Event> allEvents    = new ArrayList<>();
    private final ArrayList<Event> displayList  = new ArrayList<>();
    private EventAdapter adapter;

    private String activeCategory = "all";
    private String activeSort     = "newest";
    private String activeCity     = "";

    // Danh mục: {key, label, iconResId}
    private static final Object[][] CATEGORIES = {
        {"all",      "Tất cả",   R.drawable.ic_cat_all},
        {"music",    "Âm nhạc",  R.drawable.ic_cat_music},
        {"arts",     "Sân khấu", R.drawable.ic_cat_arts},
        {"workshop", "Workshop", R.drawable.ic_cat_workshop},
        {"tour",     "Tour",     R.drawable.ic_cat_tour},
        {"sports",   "Thể thao", R.drawable.ic_cat_sports},
        {"festival", "Lễ hội",   R.drawable.ic_cat_festival},
    };

    private static final String[] SORT_LABELS  = {"Mới nhất", "Cũ nhất", "Giá thấp → cao", "Giá cao → thấp"};
    private static final String[] SORT_KEYS    = {"newest",   "oldest",  "price_asc",       "price_desc"};

    private static final String[] CITY_LABELS  = {"Tất cả tỉnh thành", "TP. Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Đà Lạt", "Hội An"};

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        applyInsets(view);
        buildMockData();          // Hiển thị mock ngay (UI phản hồi nhanh)
        buildCategoryChips();
        setupRecyclerView();
        setupListeners();
        applyFilters();
        loadEventsFromFirestore(); // Sau đó fetch Firestore replace mock
    }

    // ── View binding ───────────────────────────────────────────────────────────
    private void bindViews(View v) {
        eventsHeaderInclude   = v.findViewById(R.id.eventsHeaderInclude);
        // edtEventsSearch lives inside the expanded search row of include_home_header
        edtEventsSearch       = v.findViewById(R.id.etSearchQuery);
        containerCategoryChips = v.findViewById(R.id.containerCategoryChips);
        btnFilterSort         = v.findViewById(R.id.btnFilterSort);
        txtFilterSort         = v.findViewById(R.id.txtFilterSort);
        btnFilterCity         = v.findViewById(R.id.btnFilterCity);
        txtFilterCity         = v.findViewById(R.id.txtFilterCity);
        rvEvents              = v.findViewById(R.id.rvEvents);
    }

    // ── Insets — đồng bộ với HomeFragment ─────────────────────────────────────
    private void applyInsets(View root) {
        if (eventsHeaderInclude == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(eventsHeaderInclude, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, top, 0, 0);
            return insets;
        });
    }

    // ── Mock data (tái dùng từ HomeFragment, sau này thay bằng Firestore) ──────
    private void buildMockData() {
        allEvents.clear();

        // Âm nhạc
        addEvent("e1",  "Mừng Ngày Hội Non Sông - Võ Hà Trâm", "01/05/2026", "TP.Hồ Chí Minh", "music",    350000, R.drawable.event_live_non_song);
        addEvent("e2",  "Private Show in Fantasy - Quốc Thiên",  "16/05/2026", "Hà Nội",          "arts",     800000, R.drawable.event_arts_private_fantasy);
        addEvent("e3",  "BẰNG KIỀU - CÒN MƯA NGANG QUA",        "19/05/2026", "TP.Hồ Chí Minh", "music",    350000, R.drawable.event_featured_bang_kieu);
        addEvent("e4",  "Vì Lý Do Đời - Mr. Siro",               "01/06/2026", "TP.Hồ Chí Minh", "music",    600000, R.drawable.event_featured_vi_ly_doi);
        addEvent("e5",  "The Story Concert",                     "27/06/2026", "TP.Hồ Chí Minh", "music",    580000, R.drawable.event_featured_the_story);
        addEvent("e6",  "Đêm nhạc Trịnh Công Sơn",              "28/06/2026", "TP.Hồ Chí Minh", "music",    150000, R.drawable.event_live_concert_1);
        addEvent("e7",  "NON SONG Live Show",                    "05/07/2026", "Hà Nội",          "music",    250000, R.drawable.event_live_non_song);
        addEvent("e8",  "Jazz Night HCM",                        "10/07/2026", "TP.Hồ Chí Minh", "music",    300000, R.drawable.event_live_concert_2);

        // Sân khấu & Nghệ thuật
        addEvent("e9",  "Chèo Đất Việt",                        "30/06/2026", "Hà Nội",          "arts",     100000, R.drawable.event_arts_traditional);
        addEvent("e10", "Opera Night 2026",                     "07/07/2026", "TP.Hồ Chí Minh", "arts",     450000, R.drawable.event_arts_concert);
        addEvent("e11", "Quốc Thiên Live",                      "21/07/2026", "TP.Hồ Chí Minh", "arts",     300000, R.drawable.event_arts_quoc_thien);

        // Workshop
        addEvent("e12", "GSTAR SUMMIT 2026",                    "25/06/2026", "TP.Hồ Chí Minh", "workshop", 500000, R.drawable.event_ws_gstar_summit);
        addEvent("e13", "Workshop Âm nhạc & Nghệ thuật",        "01/07/2026", "TP.Hồ Chí Minh", "workshop", 100000, R.drawable.event_ws_concert);

        // Tour
        addEvent("e14", "Tour Di tích Văn Hóa - Vân Mộc",      "29/06/2026", "Hà Nội",          "tour",     350000, R.drawable.event_tour_mountain);
        addEvent("e15", "Trải nghiệm suối nước nóng Đà Lạt",   "10/07/2026", "Đà Lạt",          "tour",     200000, R.drawable.event_tour_outdoor);

        // Thể thao
        addEvent("e16", "FWS SEA 2026",                         "15/06/2026", "TP.Hồ Chí Minh", "sports",  1200000, R.drawable.event_trending_fws_sea);
        addEvent("e17", "RISING FEST 2026",                     "22/06/2026", "Hà Nội",          "festival", 900000, R.drawable.event_trending_rising_fest);
        addEvent("e18", "Ultra Vietnam 2026",                   "12/07/2026", "TP.Hồ Chí Minh", "sports",  1500000, R.drawable.event_trending_rising_2);
        addEvent("e19", "THE GLOBAL CHAMPIONSHIP 2026",         "25/06/2026", "TP.Hồ Chí Minh", "sports",   200000, R.drawable.event_sports_global_champ);
        addEvent("e20", "Playoffs 2026",                        "15/07/2026", "TP.Hồ Chí Minh", "sports",   100000, R.drawable.event_sports_playoffs);
        addEvent("e21", "G-STAR Gaming Festival",               "20/07/2026", "TP.Hồ Chí Minh", "festival", 150000, R.drawable.event_sports_esport);

        // Lễ hội
        addEvent("e22", "OSTAR SUMMIT 2026",                    "25/06/2026", "TP.Hồ Chí Minh", "festival", 500000, R.drawable.event_featured_group);
    }

    private void addEvent(String id, String title, String date, String city,
                          String category, long price, int imgRes) {
        Event e = new Event(id, title, null, date, city, category, price);
        e.setVenueCity(city);
        e.setLocalImageResId(imgRes);
        e.setStatus(Constants.EVENT_STATUS_PUBLISHED);
        allEvents.add(e);
    }

    // ── Load events từ Firestore dùng FirestoreHelper ─────────────────────────
    private void loadEventsFromFirestore() {
        FirestoreHelper.loadEvents(new FirestoreHelper.OnEventsLoaded() {
            @Override
            public void onSuccess(java.util.List<Event> events) {
                if (!isAdded() || events.isEmpty()) return;
                allEvents.clear();
                allEvents.addAll(events);
                applyFilters();
            }
            @Override
            public void onFailure(Exception e) {
                // Mock data vẫn hiển thị — không cần xử lý
            }
        });
    }

    // ── Category chips — icon đơn sắc + text ──────────────────────────────────
    private void buildCategoryChips() {
        containerCategoryChips.removeAllViews();
        float dp = getResources().getDisplayMetrics().density;

        for (int i = 0; i < CATEGORIES.length; i++) {
            final String key   = (String) CATEGORIES[i][0];
            final String label = (String) CATEGORIES[i][1];
            final int    icon  = (int)    CATEGORIES[i][2];
            final int    idx   = i;

            // Chip = horizontal LinearLayout (icon + text)
            LinearLayout chip = new LinearLayout(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd((int)(8 * dp));
            chip.setLayoutParams(lp);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int ph = (int)(12 * dp), pv = (int)(7 * dp);
            chip.setPadding(ph, pv, ph, pv);
            chip.setClickable(true);
            chip.setFocusable(true);

            // Icon
            android.widget.ImageView icView = new android.widget.ImageView(requireContext());
            icView.setLayoutParams(new LinearLayout.LayoutParams((int)(14*dp), (int)(14*dp)));
            icView.setImageResource(icon);

            // Text
            TextView txtChip = new TextView(requireContext());
            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tLp.setMarginStart((int)(5 * dp));
            txtChip.setLayoutParams(tLp);
            txtChip.setText(label);
            txtChip.setTextSize(12f);

            chip.addView(icView);
            chip.addView(txtChip);

            updateChipStyle(chip, icView, txtChip, key.equals(activeCategory));

            chip.setOnClickListener(v -> {
                activeCategory = key;
                for (int j = 0; j < containerCategoryChips.getChildCount(); j++) {
                    View child = containerCategoryChips.getChildAt(j);
                    if (child instanceof LinearLayout) {
                        LinearLayout c = (LinearLayout) child;
                        if (c.getChildCount() >= 2) {
                            android.widget.ImageView ic2 = (android.widget.ImageView) c.getChildAt(0);
                            TextView txt2 = (TextView) c.getChildAt(1);
                            updateChipStyle(c, ic2, txt2, CATEGORIES[j][0].equals(activeCategory));
                        }
                    }
                }
                applyFilters();
            });

            containerCategoryChips.addView(chip);
        }
    }

    private void updateChipStyle(LinearLayout chip, android.widget.ImageView icon,
                                  TextView text, boolean active) {
        if (active) {
            chip.setBackgroundResource(R.drawable.bg_btn_primary);
            icon.setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN);
            text.setTextColor(0xFFFFFFFF);
            text.setTypeface(null, Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_search_bar);
            icon.setColorFilter(0xFF808E92, android.graphics.PorterDuff.Mode.SRC_IN);
            text.setTextColor(0xFF808E92);
            text.setTypeface(null, Typeface.NORMAL);
        }
    }

    // ── RecyclerView ───────────────────────────────────────────────────────────
    private void setupRecyclerView() {
        adapter = new EventAdapter(requireContext(), displayList,
                event -> Toast.makeText(requireContext(), event.getTitle(), Toast.LENGTH_SHORT).show(),
                R.layout.item_event_card_grid);
        rvEvents.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvEvents.setAdapter(adapter);
    }

    // ── Listeners ──────────────────────────────────────────────────────────────
    private void setupListeners() {
        // Search icon click → mở SearchFragment (như trang chủ)
        View layoutSearchBar = requireView().findViewById(R.id.layoutSearchBar);
        if (layoutSearchBar != null) {
            layoutSearchBar.setOnClickListener(v -> {
                if (getActivity() instanceof com.example.vibetix.Activities.UserMainActivity) {
                    ((com.example.vibetix.Activities.UserMainActivity) getActivity()).openSearchFragment();
                }
            });
        }

        // Notification bell → show popup
        View btnNotification = requireView().findViewById(R.id.imgBtnNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v ->
                    com.example.vibetix.Utils.NotificationPopupHelper.show(requireContext(), v));
        }

        // Search query (expanded row)
        if (edtEventsSearch != null) {
            edtEventsSearch.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) { applyFilters(); }
                public void afterTextChanged(Editable s) {}
            });
        }

        // Sort
        btnFilterSort.setOnClickListener(v -> showSortDialog());

        // City
        btnFilterCity.setOnClickListener(v -> showCityDialog());
    }

    private void showSortDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Sắp xếp theo")
                .setItems(SORT_LABELS, (d, which) -> {
                    activeSort = SORT_KEYS[which];
                    txtFilterSort.setText(SORT_LABELS[which]);
                    applyFilters();
                })
                .show();
    }

    private void showCityDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn tỉnh thành")
                .setItems(CITY_LABELS, (d, which) -> {
                    activeCity = which == 0 ? "" : CITY_LABELS[which];
                    txtFilterCity.setText(CITY_LABELS[which]);
                    applyFilters();
                })
                .show();
    }

    // ── Filter + Sort ──────────────────────────────────────────────────────────
    private void applyFilters() {
        String query = (edtEventsSearch != null && edtEventsSearch.getText() != null)
                ? edtEventsSearch.getText().toString().trim().toLowerCase(Locale.ROOT)
                : "";

        displayList.clear();
        for (Event e : allEvents) {
            // Category filter
            if (!"all".equals(activeCategory) && !activeCategory.equals(e.getCategory())) continue;
            // City filter
            if (!activeCity.isEmpty() && !activeCity.equalsIgnoreCase(e.getVenueCity())) continue;
            // Search filter
            if (!query.isEmpty()) {
                boolean match = e.getTitle().toLowerCase(Locale.ROOT).contains(query)
                        || (e.getVenueCity() != null && e.getVenueCity().toLowerCase(Locale.ROOT).contains(query));
                if (!match) continue;
            }
            displayList.add(e);
        }

        // Sort
        switch (activeSort) {
            case "oldest":
                Collections.sort(displayList, (a, b) -> safeDate(a).compareTo(safeDate(b)));
                break;
            case "price_asc":
                Collections.sort(displayList, (a, b) -> Long.compare(a.getMinPrice(), b.getMinPrice()));
                break;
            case "price_desc":
                Collections.sort(displayList, (a, b) -> Long.compare(b.getMinPrice(), a.getMinPrice()));
                break;
            default: // newest — sort descending by date string (dd/MM/yyyy → reverse)
                Collections.sort(displayList, (a, b) -> safeDate(b).compareTo(safeDate(a)));
                break;
        }

        adapter.notifyDataSetChanged();
    }

    /** Parse dd/MM/yyyy → "yyyy/MM/dd" cho sort đúng */
    private String safeDate(Event e) {
        if (e.getDate() == null) return "0000/00/00";
        String[] p = e.getDate().split("/");
        if (p.length != 3) return e.getDate();
        return p[2] + "/" + p[1] + "/" + p[0]; // yyyy/MM/dd
    }
}
