package com.example.vibetix.Fragments.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Adapters.DestinationAdapter;
import com.example.vibetix.Adapters.EventAdapter;
import com.example.vibetix.Adapters.SearchCategoryAdapter;
import com.example.vibetix.Firebase.FirestoreHelper;
import com.example.vibetix.Models.Destination;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SearchFragment extends Fragment {

    // ── Views ──────────────────────────────────────────────────────────────────
    ImageView    btnSearchBack;
    View         viewStatusBarSpacer;
    EditText     etSearch;
    LinearLayout btnDateFilter, btnEventFilter;
    TextView     txtDateFilterLabel, txtFilterLabel;
    LinearLayout sectionRecentSearch, containerRecentItems;
    ImageView    btnClearRecent;
    // Sections ẩn/hiện khi search
    LinearLayout sectionBrowseCategory, sectionBrowseCity;
    // Active filter chips container
    HorizontalScrollView scrollActiveFilters;
    LinearLayout         containerActiveFilters;
    // Suggestions
    TextView     txtSuggestionsTitle;
    RecyclerView rvCategories, rvCities, rvSuggested;
    RecyclerView rvAutoSuggest;

    // ── Adapters ───────────────────────────────────────────────────────────────
    SearchCategoryAdapter categoryAdapter;
    DestinationAdapter    destinationAdapter;
    EventAdapter          suggestedAdapter;
    com.example.vibetix.Adapters.AutoSuggestAdapter autoSuggestAdapter;
    final java.util.List<String[]> autoSuggestItems = new java.util.ArrayList<>();

    // ── Data ───────────────────────────────────────────────────────────────────
    final ArrayList<SearchCategoryAdapter.SearchCategoryItem> catItems = new ArrayList<>();
    final ArrayList<Destination> cityItems          = new ArrayList<>();
    final ArrayList<Event>       allEvents          = new ArrayList<>();
    final ArrayList<Event>       filteredFullEvents = new ArrayList<>();
    final ArrayList<Event>       displayedEvents    = new ArrayList<>();

    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    // ── Filter state ───────────────────────────────────────────────────────────
    private String   filterQuery        = "";
    private String   filterCategoryId   = "all";   // từ chip ngang
    private String   filterCategoryName = "";
    private String   filterCity         = "";       // từ chip thành phố hoặc dialog
    private Calendar filterDateStart    = null;
    private Calendar filterDateEnd      = null;
    private String   filterDateLabel    = "";
    private long     filterMinPrice     = 0;
    private long     filterMaxPrice     = Long.MAX_VALUE;
    private boolean  filterFreeOnly     = false;
    // Thể loại từ dialog (6 boolean: music/arts/workshop/tour/sports/festival)
    private boolean[] filterDialogCategories = new boolean[6];
    // Label hiển thị cho filter từ dialog
    private String   filterDialogCityLabel    = "";
    private String   filterDialogPriceLabel   = "";
    private String   filterDialogCatLabel     = "";

    // Category UUID mapping theo index của dialog (chipCatMusic=0, chipCatArts=1...)
    private static final String[] DIALOG_CAT_IDS = {
        "9e02d96f-4550-4120-8f13-969fdb92b1cf", // music
        "5bb1c15a-ac11-4d75-a439-39c2fa38a78e", // arts
        "0740fc04-68c1-42ce-a449-5a3876cf2857", // workshop
        "bafd2306-78cf-4a51-ace4-95c6c0d34bb7", // tour
        "a437787e-241f-4c98-a0b4-d30337c9f6f1", // sports
        "4c3aa3e7-7ebf-4e2e-b455-63c6605e7b45", // festival/other
    };
    private static final String[] DIALOG_CAT_NAMES = {
        "Nhạc sống","Sân khấu & NT","Hội thảo","Tham quan","Thể thao","Khác"
    };

    // Kiểm tra có đang trong chế độ search/filter không
    private boolean isSearchActive() {
        if (!filterQuery.isEmpty() || !"all".equals(filterCategoryId)
            || !filterCity.isEmpty() || filterDateStart != null
            || filterFreeOnly || filterMinPrice > 0 || filterMaxPrice < Long.MAX_VALUE)
            return true;
        for (boolean b : filterDialogCategories) if (b) return true;
        return false;
    }

    /** Kiểm tra dialog category có filter nào active không */
    private boolean hasDialogCategoryFilter() {
        for (boolean b : filterDialogCategories) if (b) return true;
        return false;
    }

    private static final String PREFS_SEARCH = "search_prefs";
    private static final String KEY_RECENT   = "recent_searches";
    private static final int    MAX_RECENT   = 10;

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        bindViews(view);
        applyInsets(view);
        setupClickListeners();
        loadRecentSearches();
        setupCategoryRecycler();
        setupCityRecycler();
        setupSuggestedRecycler();

        // Nhận preset city từ HomeFragment (click card địa điểm)
        if (getArguments() != null) {
            String presetCity = getArguments().getString("preset_city", "");
            if (!presetCity.isEmpty()) {
                filterCity = presetCity;
                if (txtFilterLabel != null) txtFilterLabel.setText("Bộ lọc (1)");
            }
        }

        loadAllEventsFromFirestore(); // Sau khi load xong sẽ applyAllFilters → tự filter theo city
        return view;
    }

    private void bindViews(View v) {
        btnSearchBack          = v.findViewById(R.id.btnSearchBack);
        viewStatusBarSpacer    = v.findViewById(R.id.viewStatusBarSpacer);
        etSearch               = v.findViewById(R.id.etSearch);
        btnDateFilter          = v.findViewById(R.id.btnDateFilter);
        btnEventFilter         = v.findViewById(R.id.btnEventFilter);
        txtDateFilterLabel     = v.findViewById(R.id.txtDateFilterLabel);
        txtFilterLabel         = v.findViewById(R.id.txtFilterLabel);
        sectionRecentSearch    = v.findViewById(R.id.sectionRecentSearch);
        containerRecentItems   = v.findViewById(R.id.containerRecentItems);
        btnClearRecent         = v.findViewById(R.id.btnClearRecent);
        sectionBrowseCategory  = v.findViewById(R.id.sectionBrowseCategory);
        sectionBrowseCity      = v.findViewById(R.id.sectionBrowseCity);
        scrollActiveFilters    = v.findViewById(R.id.scrollActiveFilters);
        containerActiveFilters = v.findViewById(R.id.containerActiveFilters);
        txtSuggestionsTitle    = v.findViewById(R.id.txtSuggestionsTitle);
        rvCategories           = v.findViewById(R.id.rvCategories);
        rvCities               = v.findViewById(R.id.rvCities);
        rvSuggested            = v.findViewById(R.id.rvSuggested);
        rvAutoSuggest          = v.findViewById(R.id.rvAutoSuggest);
    }

    private void applyInsets(View view) {
        View header = view.findViewById(R.id.headerSearch);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                if (viewStatusBarSpacer != null) {
                    ViewGroup.LayoutParams lp = viewStatusBarSpacer.getLayoutParams();
                    lp.height = top;
                    viewStatusBarSpacer.setLayoutParams(lp);
                }
                return insets;
            });
        }
    }

    // ── Click listeners ────────────────────────────────────────────────────────
    private void setupClickListeners() {
        btnSearchBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Real-time text search
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                filterQuery = s.toString().trim();
                updateAutoSuggest(filterQuery);
                applyAllFilters();
            }
            public void afterTextChanged(Editable s) {}
        });

        // Lưu lịch sử khi nhấn Enter
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String q = etSearch.getText().toString().trim();
                if (!q.isEmpty()) { saveRecentSearch(q); loadRecentSearches(); }
                if (rvAutoSuggest != null) rvAutoSuggest.setVisibility(View.GONE);
                etSearch.clearFocus();
                return true;
            }
            return false;
        });

        // Click ra ngoài search bar → ẩn dropdown + clear focus
        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && rvAutoSuggest != null) {
                rvAutoSuggest.setVisibility(View.GONE);
            }
        });

        // Bộ lọc ngày
        btnDateFilter.setOnClickListener(v -> {
            DateFilterDialog dialog = new DateFilterDialog();
            dialog.setOnDateFilterApplied((label, startDate, endDate) -> {
                if (startDate == null && endDate == null) {
                    // Reset ngày
                    filterDateStart = null; filterDateEnd = null; filterDateLabel = "";
                    txtDateFilterLabel.setText("Tất cả các ngày");
                } else {
                    filterDateStart = startDate; filterDateEnd = endDate;
                    filterDateLabel = label;
                    txtDateFilterLabel.setText(label);
                }
                applyAllFilters();
            });
            dialog.show(getChildFragmentManager(), "date_filter");
        });

        // Bộ lọc chi tiết — nhận FilterCriteria đầy đủ
        btnEventFilter.setOnClickListener(v -> {
            EventFilterDialog dialog = new EventFilterDialog();
            dialog.setOnFilterApplied(criteria -> {
                // 1. Địa điểm
                filterCity            = criteria.cityFilter;
                filterDialogCityLabel = criteria.cityFilter.isEmpty() ? "" : criteria.cityFilter;
                // 2. Giá
                filterFreeOnly  = criteria.freeOnly;
                filterMinPrice  = criteria.minPrice;
                filterMaxPrice  = criteria.maxPrice;
                filterDialogPriceLabel = filterFreeOnly ? "Miễn phí"
                        : (filterMinPrice > 0 || filterMaxPrice < Long.MAX_VALUE)
                        ? formatPrice(filterMinPrice) + "–" + (filterMaxPrice == Long.MAX_VALUE ? "∞" : formatPrice(filterMaxPrice))
                        : "";
                // 3. Thể loại từ dialog
                filterDialogCategories = criteria.categories != null ? criteria.categories.clone() : new boolean[6];
                StringBuilder catNames = new StringBuilder();
                for (int i = 0; i < filterDialogCategories.length; i++) {
                    if (filterDialogCategories[i]) {
                        if (catNames.length() > 0) catNames.append(", ");
                        catNames.append(DIALOG_CAT_NAMES[i]);
                    }
                }
                filterDialogCatLabel = catNames.toString();
                // Update button label
                txtFilterLabel.setText(criteria.count > 0 ? "Bộ lọc (" + criteria.count + ")" : "Bộ lọc");
                applyAllFilters();
            });
            dialog.show(getChildFragmentManager(), "event_filter");
        });

        if (btnClearRecent != null)
            btnClearRecent.setOnClickListener(v -> { clearRecentSearches(); loadRecentSearches(); });
    }

    // ── Load tất cả events từ Firebase ────────────────────────────────────────
    private void loadAllEventsFromFirestore() {
        FirebaseFirestore.getInstance()
            .collection("events")
            .limit(200)
            .get()
            .addOnSuccessListener(snap -> {
                if (!isAdded()) return;
                allEvents.clear();
                // Chỉ hiện approved + ongoing + completed; ẩn draft/pending/rejected/cancelled
                List<Event> ongoing   = new ArrayList<>();
                List<Event> approved  = new ArrayList<>();
                List<Event> completed = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snap) {
                    String status = doc.getString("status");
                    if (!"approved".equals(status) && !"ongoing".equals(status)
                            && !"completed".equals(status)) continue;
                    Event e = FirestoreHelper.docToEvent(doc);
                    if (e == null) continue;
                    if ("ongoing".equals(status))       ongoing.add(e);
                    else if ("approved".equals(status)) approved.add(e);
                    else                                completed.add(e);
                }
                allEvents.addAll(ongoing);
                allEvents.addAll(approved);
                allEvents.addAll(completed);
                applyAllFilters();
            })
            .addOnFailureListener(e -> {
                if (isAdded())
                    Toast.makeText(requireContext(), "Không thể tải sự kiện", Toast.LENGTH_SHORT).show();
            });
    }

    private void openEventDetail(String eventId) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameContainerMain, EventDetailFragment.newInstance(eventId))
                    .addToBackStack("search")
                    .commit();
        }
    }

    // ── Áp dụng tất cả filter + cập nhật UI ───────────────────────────────────
    private void applyAllFilters() {
        filteredFullEvents.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Bước 1: Lọc theo các bộ lọc không liên quan đến text (category, city, date, price)
        java.util.List<Event> candidates = new java.util.ArrayList<>();
        for (Event e : allEvents) {
            // Category từ chip ngang
            if (!"all".equals(filterCategoryId)) {
                String catKey = FirestoreHelper.CAT_MAP.getOrDefault(filterCategoryId, "");
                if (!catKey.equals(e.getCategory())) continue;
            }
            // Thể loại từ dialog
            if (hasDialogCategoryFilter()) {
                boolean matchAnyCat = false;
                String eCat = e.getCategory() != null ? e.getCategory() : "";
                String[] catKeys = {"music","arts","workshop","tour","sports","festival"};
                for (int i = 0; i < filterDialogCategories.length; i++) {
                    if (filterDialogCategories[i] && catKeys[i].equals(eCat)) { matchAnyCat = true; break; }
                }
                if (!matchAnyCat) continue;
            }
            // Thành phố
            if (!filterCity.isEmpty()) {
                String city = e.getVenueCity() != null ? e.getVenueCity() : "";
                if (!com.example.vibetix.Utils.VectorSearch.normalize(city)
                        .contains(com.example.vibetix.Utils.VectorSearch.normalize(filterCity))) continue;
            }
            // Khoảng ngày
            if (filterDateStart != null && filterDateEnd != null && e.getDate() != null) {
                try {
                    Date ed = sdf.parse(e.getDate());
                    if (ed != null && (ed.before(filterDateStart.getTime()) || ed.after(filterDateEnd.getTime()))) continue;
                } catch (ParseException ignored) {}
            }
            // Miễn phí
            if (filterFreeOnly && e.getMinPrice() > 0) continue;
            // Khoảng giá
            if (filterMaxPrice < Long.MAX_VALUE && e.getMinPrice() > filterMaxPrice) continue;

            candidates.add(e);
        }

        // Bước 2: Nếu có query → dùng VectorSearch xếp hạng theo mức độ liên quan
        if (!filterQuery.isEmpty()) {
            java.util.List<com.example.vibetix.Utils.VectorSearch.ScoredResult<Event>> scored =
                    com.example.vibetix.Utils.VectorSearch.search(filterQuery, candidates,
                            event -> new String[]{
                                    event.getTitle(),
                                    event.getVenueCity(),
                                    event.getOrganizerName(),
                                    event.getCategory()
                            });
            for (com.example.vibetix.Utils.VectorSearch.ScoredResult<Event> r : scored) {
                filteredFullEvents.add(r.item);
            }
        } else {
            filteredFullEvents.addAll(candidates);
        }

        currentPage = 1;
        loadNextSearchPage();
        updateSearchModeUI();
        rebuildActiveFilterChips();
    }

    /** Cập nhật dropdown autocomplete top 5 gợi ý nhanh theo VectorSearch. */
    private void updateAutoSuggest(String query) {
        if (rvAutoSuggest == null || autoSuggestAdapter == null) return;
        autoSuggestItems.clear();

        if (query.isEmpty() || query.length() < 2) {
            rvAutoSuggest.setVisibility(View.GONE);
            autoSuggestAdapter.notifyDataSetChanged();
            return;
        }

        java.util.List<com.example.vibetix.Utils.VectorSearch.ScoredResult<Event>> scored =
                com.example.vibetix.Utils.VectorSearch.search(query, allEvents,
                        event -> new String[]{ event.getTitle(), event.getVenueCity() });

        int limit = Math.min(5, scored.size());
        for (int i = 0; i < limit; i++) {
            Event e = scored.get(i).item;
            autoSuggestItems.add(new String[]{ e.getTitle(), e.getId() });
        }
        autoSuggestAdapter.notifyDataSetChanged();
        rvAutoSuggest.setVisibility(limit > 0 ? View.VISIBLE : View.GONE);
    }

    private void loadNextSearchPage() {
        int end = Math.min(currentPage * PAGE_SIZE, filteredFullEvents.size());
        displayedEvents.clear();
        displayedEvents.addAll(filteredFullEvents.subList(0, end));
        if (suggestedAdapter != null) suggestedAdapter.notifyDataSetChanged();
    }

    /** Ẩn/hiện sections theo trạng thái search */
    private void updateSearchModeUI() {
        boolean active = isSearchActive();
        if (sectionBrowseCategory != null)
            sectionBrowseCategory.setVisibility(active ? View.GONE : View.VISIBLE);
        if (sectionBrowseCity != null)
            sectionBrowseCity.setVisibility(active ? View.GONE : View.VISIBLE);
        if (sectionRecentSearch != null && active)
            sectionRecentSearch.setVisibility(View.GONE);
        if (txtSuggestionsTitle != null) {
            if (active) {
                txtSuggestionsTitle.setText("Kết quả tìm kiếm (" + filteredFullEvents.size() + ")");
            } else {
                txtSuggestionsTitle.setText("Gợi ý dành cho bạn");
                loadRecentSearches(); // Hiện lại recent khi xóa hết filter
            }
        }
    }

    /** Tạo lại các chip hiển thị điều kiện đang active */
    private void rebuildActiveFilterChips() {
        if (containerActiveFilters == null) return;
        containerActiveFilters.removeAllViews();
        boolean hasChip = false;

        // Chip text search
        if (!filterQuery.isEmpty()) {
            addFilterChip("🔍 \"" + filterQuery + "\"", () -> {
                etSearch.setText("");
                filterQuery = "";
                applyAllFilters();
            });
            hasChip = true;
        }
        // Chip category
        if (!"all".equals(filterCategoryId) && !filterCategoryName.isEmpty()) {
            addFilterChip("📂 " + filterCategoryName, () -> {
                filterCategoryId = "all"; filterCategoryName = "";
                applyAllFilters();
            });
            hasChip = true;
        }
        // Chip thành phố (từ dialog hoặc chip thành phố)
        if (!filterCity.isEmpty()) {
            addFilterChip("📍 " + filterCity, () -> {
                filterCity = ""; filterDialogCityLabel = "";
                applyAllFilters();
            });
            hasChip = true;
        }
        // Chip ngày
        if (filterDateStart != null && !filterDateLabel.isEmpty()) {
            addFilterChip("📅 " + filterDateLabel, () -> {
                filterDateStart = null; filterDateEnd = null; filterDateLabel = "";
                if (txtDateFilterLabel != null) txtDateFilterLabel.setText("Tất cả các ngày");
                applyAllFilters();
            });
            hasChip = true;
        }
        // Chip miễn phí
        if (filterFreeOnly) {
            addFilterChip("🆓 Miễn phí", () -> {
                filterFreeOnly = false; filterDialogPriceLabel = "";
                txtFilterLabel.setText("Bộ lọc");
                applyAllFilters();
            });
            hasChip = true;
        }
        // Chip giá (nếu không miễn phí)
        if (!filterFreeOnly && (filterMaxPrice < Long.MAX_VALUE || filterMinPrice > 0)) {
            String pl = "💰 " + formatPrice(filterMinPrice) + "–" + (filterMaxPrice == Long.MAX_VALUE ? "∞" : formatPrice(filterMaxPrice));
            addFilterChip(pl, () -> {
                filterMinPrice = 0; filterMaxPrice = Long.MAX_VALUE; filterDialogPriceLabel = "";
                txtFilterLabel.setText("Bộ lọc");
                applyAllFilters();
            });
            hasChip = true;
        }
        // Chip thể loại từ dialog (mỗi category được chọn là 1 chip)
        for (int i = 0; i < filterDialogCategories.length; i++) {
            if (filterDialogCategories[i]) {
                final int idx = i;
                addFilterChip("📂 " + DIALOG_CAT_NAMES[i], () -> {
                    filterDialogCategories[idx] = false;
                    // Tính lại count để cập nhật button label
                    int cnt = 0;
                    for (boolean b : filterDialogCategories) if (b) cnt++;
                    if (!filterCity.isEmpty()) cnt++;
                    if (filterFreeOnly || filterMinPrice > 0 || filterMaxPrice < Long.MAX_VALUE) cnt++;
                    txtFilterLabel.setText(cnt > 0 ? "Bộ lọc (" + cnt + ")" : "Bộ lọc");
                    applyAllFilters();
                });
                hasChip = true;
            }
        }

        if (scrollActiveFilters != null)
            scrollActiveFilters.setVisibility(hasChip ? View.VISIBLE : View.GONE);
    }

    /** Tạo 1 chip điều kiện với nút X để xóa */
    private void addFilterChip(String label, Runnable onRemove) {
        if (containerActiveFilters == null || !isAdded()) return;
        float dp = getResources().getDisplayMetrics().density;

        LinearLayout chip = new LinearLayout(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd((int)(8 * dp));
        chip.setLayoutParams(lp);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        chip.setBackgroundResource(R.drawable.bg_organizer_cta); // viền xanh nhạt
        int ph = (int)(10*dp), pv = (int)(5*dp);
        chip.setPadding(ph, pv, ph, pv);

        TextView txt = new TextView(requireContext());
        txt.setText(label);
        txt.setTextColor(0xFF226CEB);
        txt.setTextSize(12f);
        chip.addView(txt);

        TextView btnX = new TextView(requireContext());
        LinearLayout.LayoutParams xLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        xLp.setMarginStart((int)(6*dp));
        btnX.setLayoutParams(xLp);
        btnX.setText("✕");
        btnX.setTextColor(0xFF226CEB);
        btnX.setTextSize(12f);
        btnX.setClickable(true); btnX.setFocusable(true);
        btnX.setOnClickListener(v -> onRemove.run());
        chip.addView(btnX);

        containerActiveFilters.addView(chip);
    }

    private String formatPrice(long p) {
        if (p == 0) return "0đ";
        if (p >= 1_000_000) return (p/1_000_000) + "M";
        if (p >= 1_000)     return (p/1_000) + "K";
        return p + "đ";
    }

    // ── Category chips ─────────────────────────────────────────────────────────
    private void setupCategoryRecycler() {
        catItems.add(new SearchCategoryAdapter.SearchCategoryItem("Tất cả", "all", R.drawable.event_live_non_song));

        categoryAdapter = new SearchCategoryAdapter(requireContext(), catItems, item -> {
            filterCategoryId   = item.categoryId;
            filterCategoryName = "all".equals(item.categoryId) ? "" : item.name;
            applyAllFilters();
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        FirebaseFirestore.getInstance().collection("categories").get()
            .addOnSuccessListener(snap -> {
                if (!isAdded()) return;
                for (QueryDocumentSnapshot doc : snap) {
                    String name  = doc.getString("name");
                    String catId = doc.getString("category_id");
                    String slug  = doc.getString("slug");
                    if (name == null || catId == null) continue;
                    catItems.add(new SearchCategoryAdapter.SearchCategoryItem(name, catId, getDrawableForSlug(slug)));
                }
                categoryAdapter.notifyDataSetChanged();
            });
    }

    private int getDrawableForSlug(String slug) {
        if (slug == null) return R.drawable.event_trending_rising_fest;
        switch (slug) {
            case "nhac-song":             return R.drawable.event_live_non_song;
            case "san-khau-nghe-thuat":   return R.drawable.event_arts_private_fantasy;
            case "hoi-thao-workshop":     return R.drawable.event_ws_gstar_summit;
            case "tham-quan-trai-nghiem": return R.drawable.event_tour_outdoor;
            case "the-thao":              return R.drawable.event_trending_fws_sea;
            default:                      return R.drawable.event_trending_rising_fest;
        }
    }

    // ── City chips ─────────────────────────────────────────────────────────────
    private void setupCityRecycler() {
        String[] names    = {"TP. Hồ Chí Minh","Hà Nội","Đà Nẵng","Hội An","Đà Lạt"};
        String[] keywords = {"Hồ Chí Minh","Hà Nội","Đà Nẵng","Hội An","Đà Lạt"};
        int[]    drawables= {R.drawable.destination_hcmc, R.drawable.destination_hanoi,
                             R.drawable.destination_da_nang, R.drawable.destination_hoi_an, R.drawable.destination_da_lat};

        for (int i = 0; i < names.length; i++) {
            Destination d = new Destination("city_" + i, names[i], null, 0);
            d.setLocalImageResId(drawables[i]);
            cityItems.add(d);
        }

        destinationAdapter = new DestinationAdapter(requireContext(), cityItems, dest -> {
            int idx = cityItems.indexOf(dest);
            String kw = (idx >= 0 && idx < keywords.length) ? keywords[idx] : "";
            // Toggle: click lại → bỏ filter
            filterCity = filterCity.equals(kw) ? "" : kw;
            applyAllFilters();
        });
        rvCities.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCities.setAdapter(destinationAdapter);
    }

    // ── Suggested events ───────────────────────────────────────────────────────
    private void setupSuggestedRecycler() {
        suggestedAdapter = new EventAdapter(requireContext(), displayedEvents,
                event -> openEventDetail(event.getId()),
                R.layout.item_event_card_grid);
        GridLayoutManager glm = new GridLayoutManager(requireContext(), 2);
        rvSuggested.setLayoutManager(glm);
        rvSuggested.setAdapter(suggestedAdapter);

        // Autocomplete dropdown
        if (rvAutoSuggest != null) {
            autoSuggestAdapter = new com.example.vibetix.Adapters.AutoSuggestAdapter(autoSuggestItems, (title, eventId) -> {
                etSearch.setText(title);
                etSearch.setSelection(title.length());
                rvAutoSuggest.setVisibility(View.GONE);
                saveRecentSearch(title);
                loadRecentSearches();
            });
            rvAutoSuggest.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvAutoSuggest.setAdapter(autoSuggestAdapter);
        }

        rvSuggested.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int visibleItemCount = glm.getChildCount();
                    int totalItemCount = glm.getItemCount();
                    int pastVisibleItems = glm.findFirstVisibleItemPosition();
                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 2) {
                        if (displayedEvents.size() < filteredFullEvents.size()) {
                            currentPage++;
                            loadNextSearchPage();
                        }
                    }
                }
            }
        });
    }

    // ── Recent searches ────────────────────────────────────────────────────────
    private void loadRecentSearches() {
        if (!isAdded() || isSearchActive()) return;
        ArrayList<String> recent = getRecentSearches();
        if (sectionRecentSearch == null || containerRecentItems == null) return;
        containerRecentItems.removeAllViews();
        if (recent.isEmpty()) { sectionRecentSearch.setVisibility(View.GONE); return; }
        sectionRecentSearch.setVisibility(View.VISIBLE);
        for (String q : recent) addRecentSearchItem(q);
    }

    private void addRecentSearchItem(String query) {
        float dp = requireContext().getResources().getDisplayMetrics().density;
        int dp4 = (int)(4*dp), dp8 = (int)(8*dp), dp18 = (int)(18*dp);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp4, 0, dp4);
        row.setClickable(true); row.setFocusable(true);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(R.drawable.ic_history);
        LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(dp18, dp18);
        iLp.setMarginEnd(dp8); icon.setLayoutParams(iLp);

        TextView txt = new TextView(requireContext());
        txt.setText(query);
        txt.setTextColor(requireContext().getColor(R.color.clr_text_black));
        txt.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        txt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(icon); row.addView(txt);
        row.setOnClickListener(v -> { etSearch.setText(query); etSearch.setSelection(query.length()); });
        containerRecentItems.addView(row);

        View divider = new View(requireContext());
        divider.setBackgroundColor(requireContext().getColor(R.color.clr_divider));
        containerRecentItems.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private ArrayList<String> getRecentSearches() {
        ArrayList<String> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(requireContext()
                    .getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
                    .getString(KEY_RECENT, "[]"));
            for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
        } catch (JSONException ignored) {}
        return list;
    }

    private void saveRecentSearch(String query) {
        ArrayList<String> list = getRecentSearches();
        list.remove(query); list.add(0, query);
        if (list.size() > MAX_RECENT) list = new ArrayList<>(list.subList(0, MAX_RECENT));
        requireContext().getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
                .edit().putString(KEY_RECENT, new JSONArray(list).toString()).apply();
    }

    private void clearRecentSearches() {
        requireContext().getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
                .edit().remove(KEY_RECENT).apply();
    }
}
