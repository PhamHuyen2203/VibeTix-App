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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

    private String activeCategory  = "all";
    private String activeSort      = "newest";
    private String activeCity      = "";      // keyword để filter (match với Firestore venue_city)
    private String activeCityLabel = "";      // label hiển thị trên button

    private final java.util.List<Object[]> categoryList = new java.util.ArrayList<>();

    private static final String[] SORT_LABELS   = {"Mới nhất", "Cũ nhất", "Giá thấp → cao", "Giá cao → thấp"};
    private static final String[] SORT_KEYS     = {"newest",   "oldest",  "price_asc",       "price_desc"};

    // CITY_LABELS: tên hiển thị | CITY_KEYWORDS: keyword filter (khớp Firestore venue_city)
    private static final String[] CITY_LABELS   = {"Tất cả tỉnh thành", "TP. Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Đà Lạt", "Hội An"};
    private static final String[] CITY_KEYWORDS = {"",                  "Hồ Chí Minh",     "Hà Nội", "Đà Nẵng", "Đà Lạt", "Hội An"};

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
        setupRecyclerView();
        setupListeners();
        // Load category cache trước → rebuild chips → rồi load events
        FirestoreHelper.loadCategoryCache(() -> {
            if (!isAdded()) return;
            buildCategoryChipsDefault(); // Build chips với UUIDs mới từ cache
            loadCategoriesFromFirestore(); // Rebuild với tên thật
            loadEventsFromFirestore();     // Load events
        });
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

    /** Build chips dùng UUIDs từ FirestoreHelper.CAT_KEY_TO_ID (dynamic) */
    private void buildCategoryChipsDefault() {
        categoryList.clear();
        categoryList.add(new Object[]{"all", "Tất cả", R.drawable.ic_cat_all});
        // Dùng getCatId() để lấy UUID thật — không hardcode
        addCatChip("music",    "Nhạc sống",          R.drawable.ic_cat_music);
        addCatChip("arts",     "Sân khấu & NT",       R.drawable.ic_cat_arts);
        addCatChip("workshop", "Hội thảo & Workshop", R.drawable.ic_cat_workshop);
        addCatChip("tour",     "Tham quan & TN",      R.drawable.ic_cat_tour);
        addCatChip("sports",   "Thể thao",            R.drawable.ic_cat_sports);
        addCatChip("festival", "Khác",                R.drawable.ic_cat_festival);
        buildCategoryChips();
    }

    private void addCatChip(String appKey, String defaultLabel, int icon) {
        String uuid = FirestoreHelper.CAT_KEY_TO_ID.get(appKey);
        if (uuid == null || uuid.isEmpty()) return; // skip nếu chưa load
        // Lấy tên hiển thị từ cache, fallback về defaultLabel
        String slugKey;
        switch (appKey) {
            case "music":    slugKey = "nhac-song"; break;
            case "arts":     slugKey = "san-khau-nghe-thuat"; break;
            case "workshop": slugKey = "hoi-thao-workshop"; break;
            case "tour":     slugKey = "tham-quan-trai-nghiem"; break;
            case "sports":   slugKey = "the-thao"; break;
            default:         slugKey = "khac"; break;
        }
        String label = FirestoreHelper.CAT_SLUG_TO_NAME.containsKey(slugKey)
            ? FirestoreHelper.CAT_SLUG_TO_NAME.get(slugKey) : defaultLabel;
        categoryList.add(new Object[]{uuid, label, icon});
    }

    /** Load categories từ Firestore → rebuild chips với tên khớp Firebase */
    private void loadCategoriesFromFirestore() {
        FirebaseFirestore.getInstance().collection("categories").get()
            .addOnSuccessListener(snap -> {
                if (!isAdded() || snap.isEmpty()) return;
                categoryList.clear();
                categoryList.add(new Object[]{"all", "Tất cả", R.drawable.ic_cat_all});
                for (QueryDocumentSnapshot doc : snap) {
                    String catId = doc.getString("category_id");
                    String name  = doc.getString("name");
                    String slug  = doc.getString("slug");
                    if (catId == null || name == null) continue;
                    int icon = getIconForSlug(slug);
                    categoryList.add(new Object[]{catId, name, icon});
                }
                buildCategoryChips(); // Rebuild với tên thật từ Firebase
            });
    }

    private int getIconForSlug(String slug) {
        if (slug == null) return R.drawable.ic_cat_all;
        switch (slug) {
            case "nhac-song":               return R.drawable.ic_cat_music;
            case "san-khau-nghe-thuat":     return R.drawable.ic_cat_arts;
            case "hoi-thao-workshop":       return R.drawable.ic_cat_workshop;
            case "tham-quan-trai-nghiem":   return R.drawable.ic_cat_tour;
            case "the-thao":                return R.drawable.ic_cat_sports;
            default:                        return R.drawable.ic_cat_festival;
        }
    }

    // ── Category chips — icon đơn sắc + text ──────────────────────────────────
    private void buildCategoryChips() {
        containerCategoryChips.removeAllViews();
        float dp = getResources().getDisplayMetrics().density;

        for (int i = 0; i < categoryList.size(); i++) {
            final String key   = (String) categoryList.get(i)[0];
            final String label = (String) categoryList.get(i)[1];
            final int    icon  = (int)    categoryList.get(i)[2];
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
                            String chipKey = j < categoryList.size() ? (String) categoryList.get(j)[0] : "";
                            updateChipStyle(c, ic2, txt2, chipKey.equals(activeCategory));
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

    /** Bottom sheet chọn sắp xếp — có nút Xóa bộ lọc */
    private void showSortDialog() {
        android.app.Dialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        android.view.View v = buildOptionSheet("Sắp xếp theo", SORT_LABELS, activeSort.equals("newest") ? -1 : getActiveSortIndex(),
            which -> {
                activeSort = SORT_KEYS[which];
                txtFilterSort.setText(SORT_LABELS[which]);
                applyFilters();
                sheet.dismiss();
            },
            () -> { // Xóa sắp xếp
                activeSort = "newest";
                txtFilterSort.setText("Mới nhất");
                applyFilters();
                sheet.dismiss();
            });
        sheet.setContentView(v);
        sheet.show();
    }

    /** Bottom sheet chọn tỉnh thành — có nút Xóa bộ lọc */
    private void showCityDialog() {
        android.app.Dialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        android.view.View v = buildOptionSheet("Chọn tỉnh thành", CITY_LABELS, getActiveCityIndex(),
            which -> {
                activeCity      = CITY_KEYWORDS[which];
                activeCityLabel = which == 0 ? "Tất cả tỉnh thành" : CITY_LABELS[which];
                txtFilterCity.setText(activeCityLabel);
                applyFilters();
                sheet.dismiss();
            },
            () -> { // Xóa bộ lọc thành phố
                activeCity = ""; activeCityLabel = "Tất cả tỉnh thành";
                txtFilterCity.setText("Tất cả tỉnh thành");
                applyFilters();
                sheet.dismiss();
            });
        sheet.setContentView(v);
        sheet.show();
    }

    private int getActiveSortIndex() {
        for (int i = 0; i < SORT_KEYS.length; i++) if (SORT_KEYS[i].equals(activeSort)) return i;
        return 0;
    }
    private int getActiveCityIndex() {
        for (int i = 0; i < CITY_KEYWORDS.length; i++) if (CITY_KEYWORDS[i].equals(activeCity)) return i;
        return 0;
    }

    /** Tạo view cho bottom sheet option picker */
    private android.view.View buildOptionSheet(String title, String[] options, int activeIdx,
                                                java.util.function.IntConsumer onSelect, Runnable onClear) {
        float dp = getResources().getDisplayMetrics().density;
        android.widget.LinearLayout root = new android.widget.LinearLayout(requireContext());
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);
        int ph = (int)(20*dp);

        // Drag handle
        android.view.View handle = new android.view.View(requireContext());
        android.widget.LinearLayout.LayoutParams hLp = new android.widget.LinearLayout.LayoutParams((int)(40*dp), (int)(4*dp));
        hLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        hLp.topMargin = (int)(12*dp); hLp.bottomMargin = (int)(8*dp);
        handle.setLayoutParams(hLp);
        handle.setBackgroundColor(0xFFDDDDDD);
        root.addView(handle);

        // Title
        android.widget.TextView txtTitle = new android.widget.TextView(requireContext());
        txtTitle.setText(title);
        txtTitle.setTextSize(16f);
        txtTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        txtTitle.setTextColor(0xFF1C1B1B);
        android.widget.LinearLayout.LayoutParams tLp = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.setMarginStart(ph); tLp.topMargin = (int)(8*dp); tLp.bottomMargin = (int)(12*dp);
        txtTitle.setLayoutParams(tLp);
        root.addView(txtTitle);

        // Divider
        android.view.View div = new android.view.View(requireContext());
        div.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1));
        div.setBackgroundColor(0xFFF0F0F0);
        root.addView(div);

        // Options
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            boolean isActive = (i == activeIdx);

            android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(ph, (int)(14*dp), ph, (int)(14*dp));
            row.setClickable(true); row.setFocusable(true);
            row.setBackgroundColor(isActive ? 0xFFF0F4FF : 0xFFFFFFFF);

            android.widget.TextView optTxt = new android.widget.TextView(requireContext());
            android.widget.LinearLayout.LayoutParams oLp = new android.widget.LinearLayout.LayoutParams(0,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            optTxt.setLayoutParams(oLp);
            optTxt.setText(options[i]);
            optTxt.setTextSize(14f);
            optTxt.setTextColor(isActive ? 0xFF226CEB : 0xFF1C1B1B);
            if (isActive) optTxt.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(optTxt);

            if (isActive) {
                android.widget.TextView check = new android.widget.TextView(requireContext());
                check.setText("✓");
                check.setTextColor(0xFF226CEB);
                check.setTextSize(16f);
                row.addView(check);
            }

            row.setOnClickListener(v -> onSelect.accept(idx));
            root.addView(row);

            // Thin divider
            android.view.View d2 = new android.view.View(requireContext());
            d2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1));
            d2.setBackgroundColor(0xFFF8F8F8);
            root.addView(d2);
        }

        // Bottom padding
        android.view.View bottomPad = new android.view.View(requireContext());
        bottomPad.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, (int)(16*dp)));
        root.addView(bottomPad);

        return root;
    }

    // ── Filter + Sort ──────────────────────────────────────────────────────────
    private void applyFilters() {
        String query = (edtEventsSearch != null && edtEventsSearch.getText() != null)
                ? edtEventsSearch.getText().toString().trim().toLowerCase(Locale.ROOT)
                : "";

        displayList.clear();
        for (Event e : allEvents) {
            // Category filter — activeCategory là UUID hoặc "all"
            // e.getCategory() là app key (music/arts/...) từ CAT_MAP
            if (!"all".equals(activeCategory)) {
                // Lấy app key tương ứng của activeCategory UUID
                String catKey = FirestoreHelper.CAT_MAP.getOrDefault(activeCategory, activeCategory);
                if (!catKey.equals(e.getCategory())) continue;
            }
            // City filter — dùng contains vì Firestore có thể lưu "Hồ Chí Minh" khác "TP. Hồ Chí Minh"
            if (!activeCity.isEmpty()) {
                String city = e.getVenueCity() != null ? e.getVenueCity() : "";
                if (!city.toLowerCase(Locale.ROOT).contains(activeCity.toLowerCase(Locale.ROOT))) continue;
            }
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
