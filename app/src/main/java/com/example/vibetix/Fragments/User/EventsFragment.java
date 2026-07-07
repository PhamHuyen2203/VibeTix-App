package com.example.vibetix.Fragments.User;

import android.graphics.Typeface;
import android.os.Bundle;
import com.example.vibetix.Activities.User.UserMainActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

    private ProgressBar  pbEventsLoading;
    private LinearLayout layoutErrorEmptyState;
    private TextView     txtErrorEmptyTitle;
    private TextView     txtErrorEmptySub;
    private TextView     btnRetryEvents;

    // ── Data ───────────────────────────────────────────────────────────────────
    private final ArrayList<Event> allEvents        = new ArrayList<>();
    private final ArrayList<Event> filteredFullList = new ArrayList<>();
    private final ArrayList<Event> displayList      = new ArrayList<>();
    private EventAdapter adapter;

    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

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

    public static EventsFragment newInstance(String categoryKey, String cityKeyword) {
        EventsFragment fragment = new EventsFragment();
        Bundle args = new Bundle();
        if (categoryKey != null) args.putString("preset_category", categoryKey);
        if (cityKeyword != null) args.putString("preset_city", cityKeyword);
        fragment.setArguments(args);
        return fragment;
    }

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

        if (getArguments() != null) {
            String cat = getArguments().getString("preset_category");
            if (cat != null && !cat.isEmpty()) {
                // Resolve app-key (music, arts, ...) to UUID if needed
                String uuid = FirestoreHelper.CAT_KEY_TO_ID.get(cat);
                activeCategory = (uuid != null && !uuid.isEmpty()) ? uuid : cat;
            }
            String city = getArguments().getString("preset_city");
            if (city != null && !city.isEmpty()) {
                activeCity = city;
                activeCityLabel = city;
                if (txtFilterCity != null) txtFilterCity.setText(city);
            }
        }

        // Load category cache trước → rebuild chips → rồi load events
        FirestoreHelper.loadCategoryCache(() -> {
            if (!isAdded()) return;
            // Re-resolve app-key → UUID now that cache is populated
            if (getArguments() != null) {
                String cat = getArguments().getString("preset_category");
                if (cat != null && !cat.isEmpty() && !"all".equals(cat)) {
                    String uuid = FirestoreHelper.CAT_KEY_TO_ID.get(cat);
                    if (uuid != null && !uuid.isEmpty()) activeCategory = uuid;
                }
            }
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

        pbEventsLoading       = v.findViewById(R.id.pbEventsLoading);
        layoutErrorEmptyState = v.findViewById(R.id.layoutErrorEmptyState);
        txtErrorEmptyTitle    = v.findViewById(R.id.txtErrorEmptyTitle);
        txtErrorEmptySub      = v.findViewById(R.id.txtErrorEmptySub);
        btnRetryEvents        = v.findViewById(R.id.btnRetryEvents);

        if (btnRetryEvents != null) {
            btnRetryEvents.setOnClickListener(view -> {
                showLoadingState();
                loadEventsFromFirestore();
            });
        }
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

    private void showLoadingState() {
        if (pbEventsLoading != null) pbEventsLoading.setVisibility(View.VISIBLE);
        if (layoutErrorEmptyState != null) layoutErrorEmptyState.setVisibility(View.GONE);
        if (rvEvents != null) rvEvents.setVisibility(View.GONE);
    }

    private void showErrorOrEmptyState(boolean isError, String message) {
        if (pbEventsLoading != null) pbEventsLoading.setVisibility(View.GONE);
        if (rvEvents != null) rvEvents.setVisibility(View.GONE);
        if (layoutErrorEmptyState != null) {
            layoutErrorEmptyState.setVisibility(View.VISIBLE);
            if (txtErrorEmptyTitle != null) {
                txtErrorEmptyTitle.setText(isError ? getString(R.string.str_cannot_load_data) : getString(R.string.str_event_not_found_label));
            }
            if (txtErrorEmptySub != null) {
                txtErrorEmptySub.setText(message != null ? message : (isError ? getString(R.string.str_check_network_retry) : getString(R.string.str_try_other_filter)));
            }
            if (btnRetryEvents != null) {
                btnRetryEvents.setVisibility(isError ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void showSuccessState() {
        if (pbEventsLoading != null) pbEventsLoading.setVisibility(View.GONE);
        if (layoutErrorEmptyState != null) layoutErrorEmptyState.setVisibility(View.GONE);
        if (rvEvents != null) rvEvents.setVisibility(View.VISIBLE);
    }

    // ── Load events từ Firestore dùng FirestoreHelper ─────────────────────────
    private void loadEventsFromFirestore() {
        showLoadingState();
        FirestoreHelper.loadEvents(new FirestoreHelper.OnEventsLoaded() {
            @Override
            public void onSuccess(java.util.List<Event> events) {
                if (!isAdded()) return;
                allEvents.clear();
                if (events != null && !events.isEmpty()) {
                    allEvents.addAll(events);
                    applyFilters();
                } else {
                    showErrorOrEmptyState(false, "Hiện chưa có sự kiện nào.");
                }
            }
            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                showErrorOrEmptyState(true, "Không thể kết nối với máy chủ. Vui lòng thử lại.");
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
                event -> openEventDetail(event.getId()),
                R.layout.item_event_card_grid);
        GridLayoutManager glm = new GridLayoutManager(requireContext(), 2);
        rvEvents.setLayoutManager(glm);
        rvEvents.setAdapter(adapter);

        rvEvents.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int visibleItemCount = glm.getChildCount();
                    int totalItemCount = glm.getItemCount();
                    int pastVisibleItems = glm.findFirstVisibleItemPosition();
                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 2) {
                        if (displayList.size() < filteredFullList.size()) {
                            currentPage++;
                            loadNextPage();
                        }
                    }
                }
            }
        });
    }

    private void openEventDetail(String eventId) {
        if (getActivity() instanceof UserMainActivity) {
            ((UserMainActivity) getActivity()).openSubFragment(EventDetailFragment.newInstance(eventId));
        }
    }

    // ── Listeners ──────────────────────────────────────────────────────────────
    private void setupListeners() {
        // Search icon click → mở SearchFragment (như trang chủ)
        View layoutSearchBar = requireView().findViewById(R.id.layoutSearchBar);
        if (layoutSearchBar != null) {
            layoutSearchBar.setOnClickListener(v -> {
                if (getActivity() instanceof com.example.vibetix.Activities.User.UserMainActivity) {
                    ((com.example.vibetix.Activities.User.UserMainActivity) getActivity()).openSearchFragment();
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
                txtFilterSort.setText(getString(R.string.str_sort_latest));
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
                activeCityLabel = which == 0 ? getString(R.string.str_all_cities) : CITY_LABELS[which];
                txtFilterCity.setText(activeCityLabel);
                applyFilters();
                sheet.dismiss();
            },
            () -> { // Xóa bộ lọc thành phố
                activeCity = ""; activeCityLabel = getString(R.string.str_all_cities);
                txtFilterCity.setText(getString(R.string.str_all_cities));
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

        filteredFullList.clear();
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
            filteredFullList.add(e);
        }

        // Sort
        switch (activeSort) {
            case "oldest":
                Collections.sort(filteredFullList, (a, b) -> safeDate(a).compareTo(safeDate(b)));
                break;
            case "price_asc":
                Collections.sort(filteredFullList, (a, b) -> Double.compare(a.getMinPrice(), b.getMinPrice()));
                break;
            case "price_desc":
                Collections.sort(filteredFullList, (a, b) -> Double.compare(b.getMinPrice(), a.getMinPrice()));
                break;
            default: // newest — sort descending by date string (dd/MM/yyyy → reverse)
                Collections.sort(filteredFullList, (a, b) -> safeDate(b).compareTo(safeDate(a)));
                break;
        }

        currentPage = 1;
        loadNextPage();
    }

    private void loadNextPage() {
        int end = Math.min(currentPage * PAGE_SIZE, filteredFullList.size());
        displayList.clear();
        displayList.addAll(filteredFullList.subList(0, end));
        adapter.notifyDataSetChanged();
        if (displayList.isEmpty()) {
            showErrorOrEmptyState(false, getString(R.string.str_try_other_filter));
        } else {
            showSuccessState();
        }
    }

    /** Parse dd/MM/yyyy → "yyyy/MM/dd" cho sort đúng */
    private String safeDate(Event e) {
        if (e.getDate() == null) return "0000/00/00";
        String[] p = e.getDate().split("/");
        if (p.length != 3) return e.getDate();
        return p[2] + "/" + p[1] + "/" + p[0]; // yyyy/MM/dd
    }
}
