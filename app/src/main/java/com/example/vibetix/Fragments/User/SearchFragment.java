package com.example.vibetix.Fragments.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
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
import com.example.vibetix.Utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class SearchFragment extends Fragment {

    // Header
    ImageView btnSearchBack;
    View viewStatusBarSpacer;

    // Search bar
    EditText etSearch;

    // Filter chips
    LinearLayout btnDateFilter, btnEventFilter;
    TextView txtDateFilterLabel, txtFilterLabel;

    // Recent searches
    LinearLayout sectionRecentSearch, containerRecentItems;
    ImageView btnClearRecent;

    // RecyclerViews
    RecyclerView rvCategories, rvCities, rvSuggested;

    // Adapters
    SearchCategoryAdapter categoryAdapter;
    DestinationAdapter destinationAdapter;
    EventAdapter suggestedAdapter;

    // Data
    ArrayList<SearchCategoryAdapter.SearchCategoryItem> catItems = new ArrayList<>();
    ArrayList<Destination> cityItems = new ArrayList<>();
    ArrayList<Event> danhSachSuggested = new ArrayList<>();

    // Filter state
    String activeDateFilter = "Tất cả các ngày";
    int activeFilterCount = 0;

    private static final String PREFS_SEARCH = "search_prefs";
    private static final String KEY_RECENT   = "recent_searches";
    private static final int MAX_RECENT = 10;

    @Nullable
    @Override
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
        loadMockSuggestions();      // Mock hiển thị ngay
        setupSuggestedRecycler();
        loadSuggestionsFromFirestore(); // Sau đó replace bằng data thật
        return view;
    }

    private void bindViews(View view) {
        btnSearchBack        = view.findViewById(R.id.btnSearchBack);
        viewStatusBarSpacer  = view.findViewById(R.id.viewStatusBarSpacer);
        etSearch             = view.findViewById(R.id.etSearch);
        btnDateFilter        = view.findViewById(R.id.btnDateFilter);
        btnEventFilter       = view.findViewById(R.id.btnEventFilter);
        txtDateFilterLabel   = view.findViewById(R.id.txtDateFilterLabel);
        txtFilterLabel       = view.findViewById(R.id.txtFilterLabel);
        sectionRecentSearch  = view.findViewById(R.id.sectionRecentSearch);
        containerRecentItems = view.findViewById(R.id.containerRecentItems);
        btnClearRecent       = view.findViewById(R.id.btnClearRecent);
        rvCategories         = view.findViewById(R.id.rvCategories);
        rvCities             = view.findViewById(R.id.rvCities);
        rvSuggested          = view.findViewById(R.id.rvSuggested);
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

    private void setupClickListeners() {
        btnSearchBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    saveRecentSearch(query);
                    loadRecentSearches();
                    Toast.makeText(requireContext(),
                            "Không tìm thấy kết quả cho: " + query, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        btnDateFilter.setOnClickListener(v -> {
            DateFilterDialog dialog = new DateFilterDialog();
            dialog.setOnDateFilterApplied((label, startDate, endDate) -> {
                activeDateFilter = label;
                txtDateFilterLabel.setText(label);
            });
            dialog.show(getChildFragmentManager(), "date_filter");
        });

        btnEventFilter.setOnClickListener(v -> {
            EventFilterDialog dialog = new EventFilterDialog();
            dialog.setOnFilterApplied(count -> {
                activeFilterCount = count;
                txtFilterLabel.setText(count > 0 ? "Bộ lọc (" + count + ")" : "Bộ lọc");
            });
            dialog.show(getChildFragmentManager(), "event_filter");
        });

        if (btnClearRecent != null) {
            btnClearRecent.setOnClickListener(v -> {
                clearRecentSearches();
                loadRecentSearches();
            });
        }
    }

    // ── Category horizontal scroll ────────────────────────────────────────────────
    private void setupCategoryRecycler() {
        catItems.add(new SearchCategoryAdapter.SearchCategoryItem(
                getString(R.string.str_live_music),       R.drawable.event_live_non_song));
        catItems.add(new SearchCategoryAdapter.SearchCategoryItem(
                getString(R.string.str_stage_arts),       R.drawable.event_arts_private_fantasy));
        catItems.add(new SearchCategoryAdapter.SearchCategoryItem(
                getString(R.string.str_workshop),         R.drawable.event_featured_bang_kieu));
        catItems.add(new SearchCategoryAdapter.SearchCategoryItem(
                getString(R.string.str_tour_experience),  R.drawable.event_featured_vi_ly_doi));
        catItems.add(new SearchCategoryAdapter.SearchCategoryItem(
                getString(R.string.str_sports),           R.drawable.event_trending_fws_sea));
        catItems.add(new SearchCategoryAdapter.SearchCategoryItem(
                getString(R.string.str_cat_festival),     R.drawable.event_trending_rising_fest));

        categoryAdapter = new SearchCategoryAdapter(requireContext(), catItems,
                item -> Toast.makeText(requireContext(), item.name, Toast.LENGTH_SHORT).show());
        rvCategories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);
    }

    // ── City horizontal scroll ────────────────────────────────────────────────────
    private void setupCityRecycler() {
        // Tham số thứ 4 là eventCount (int) — ảnh phải set riêng qua setLocalImageResId()
        // Thứ tự đồng bộ với HomeFragment "Điểm đến nổi bật"
        Destination c1 = new Destination("d1", "TP. Hồ Chí Minh", null, 120);
        c1.setLocalImageResId(R.drawable.destination_hcmc);
        cityItems.add(c1);

        Destination c2 = new Destination("d2", "Hà Nội", null, 89);
        c2.setLocalImageResId(R.drawable.destination_hanoi);
        cityItems.add(c2);

        Destination c3 = new Destination("d3", "Đà Nẵng", null, 45);
        c3.setLocalImageResId(R.drawable.destination_da_nang);
        cityItems.add(c3);

        Destination c4 = new Destination("d4", "Hội An", null, 32);
        c4.setLocalImageResId(R.drawable.destination_hoi_an);
        cityItems.add(c4);

        Destination c5 = new Destination("d5", "Đà Lạt", null, 28);
        c5.setLocalImageResId(R.drawable.destination_da_lat);
        cityItems.add(c5);

        destinationAdapter = new DestinationAdapter(requireContext(), cityItems,
                dest -> Toast.makeText(requireContext(), dest.getName(), Toast.LENGTH_SHORT).show());
        rvCities.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCities.setAdapter(destinationAdapter);
    }

    // ── Suggestions 2-column vertical grid ───────────────────────────────────────
    private void loadMockSuggestions() {
        int[][] data = {
            {R.drawable.event_featured_vi_ly_doi,      600000},
            {R.drawable.event_trending_fws_sea,        1200000},
            {R.drawable.event_trending_rising_fest,    900000},
            {R.drawable.event_live_non_song,           350000},
            {R.drawable.event_arts_private_fantasy,    800000},
            {R.drawable.event_featured_bang_kieu,      350000},
            {R.drawable.event_featured_vi_ly_doi,      500000},
            {R.drawable.event_trending_fws_sea,        1000000},
            {R.drawable.event_trending_rising_fest,    750000},
            {R.drawable.event_live_non_song,           400000},
            {R.drawable.event_arts_private_fantasy,    650000},
            {R.drawable.event_featured_bang_kieu,      300000},
            {R.drawable.event_featured_vi_ly_doi,      700000},
            {R.drawable.event_trending_fws_sea,        1100000},
            {R.drawable.event_trending_rising_fest,    850000},
            {R.drawable.event_live_non_song,           450000},
            {R.drawable.event_arts_private_fantasy,    750000},
            {R.drawable.event_featured_bang_kieu,      320000},
            {R.drawable.event_featured_vi_ly_doi,      580000},
            {R.drawable.event_trending_fws_sea,        950000},
        };
        String[] titles = {
            "Vì Lý Do Đời - Mr. Siro", "FWS SEA 2026", "RISING FEST 2026",
            "Hòa nhạc Non Sông", "Private Show in Fantasy", "Bằng Kiều - Còn Mưa Ngang Qua",
            "Mừng Ngày Non Sông", "ESL Pro League 2026", "Sun Fest 2026",
            "Đêm nhạc Acoustic", "Sân khấu nghệ thuật", "Rock Concert 2026",
            "Mr. Siro Live Tour", "FFWS SEA Finals", "Music Carnival",
            "Hòa âm ánh sáng", "Ballet Đương đại", "Indie Vibes Festival",
            "Acoustic Night", "Gaming Championship 2026"
        };
        String[] cities = {
            "TP.Hồ Chí Minh","TP.Hồ Chí Minh","Hà Nội","Đà Nẵng","Hà Nội",
            "TP.Hồ Chí Minh","TP.Hồ Chí Minh","TP.Hồ Chí Minh","Hà Nội","Đà Lạt",
            "Hà Nội","TP.Hồ Chí Minh","TP.Hồ Chí Minh","TP.Hồ Chí Minh","Hà Nội",
            "Đà Nẵng","Hà Nội","TP.Hồ Chí Minh","Đà Lạt","TP.Hồ Chí Minh"
        };
        String[] dates = {
            "01/06/2026","15/06/2026","22/06/2026","01/06/2026","16/05/2026",
            "19/05/2026","01/05/2026","10/07/2026","05/07/2026","20/06/2026",
            "25/06/2026","12/07/2026","08/06/2026","20/06/2026","28/06/2026",
            "15/06/2026","30/06/2026","18/07/2026","22/06/2026","05/08/2026"
        };
        for (int i = 0; i < data.length; i++) {
            Event e = new Event("sg" + i, titles[i], null, dates[i], cities[i],
                    Constants.EVENT_STATUS_PUBLISHED, data[i][1]);
            e.setLocalImageResId(data[i][0]);
            e.setVenueCity(cities[i]);
            danhSachSuggested.add(e);
        }
    }

    private void loadSuggestionsFromFirestore() {
        FirestoreHelper.loadEvents(new FirestoreHelper.OnEventsLoaded() {
            @Override public void onSuccess(java.util.List<Event> events) {
                if (!isAdded() || events.isEmpty()) return;
                danhSachSuggested.clear();
                danhSachSuggested.addAll(events);
                if (suggestedAdapter != null) suggestedAdapter.notifyDataSetChanged();
            }
            @Override public void onFailure(Exception e) { /* mock vẫn hiển thị */ }
        });
    }

    private void setupSuggestedRecycler() {
        suggestedAdapter = new EventAdapter(requireContext(), danhSachSuggested,
                event -> Toast.makeText(requireContext(), event.getTitle(), Toast.LENGTH_SHORT).show(),
                R.layout.item_event_card_grid);
        rvSuggested.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvSuggested.setAdapter(suggestedAdapter);
    }

    // ── Recent searches ──────────────────────────────────────────────────────────
    private void loadRecentSearches() {
        ArrayList<String> recent = getRecentSearches();
        if (sectionRecentSearch == null || containerRecentItems == null) return;
        containerRecentItems.removeAllViews();
        if (recent.isEmpty()) {
            sectionRecentSearch.setVisibility(View.GONE);
            return;
        }
        sectionRecentSearch.setVisibility(View.VISIBLE);
        for (String q : recent) addRecentSearchItem(q);
    }

    private void addRecentSearchItem(String query) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        int dp4  = (int)(4  * density);
        int dp8  = (int)(8  * density);
        int dp18 = (int)(18 * density);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp4, 0, dp4);
        row.setClickable(true);
        row.setFocusable(true);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(R.drawable.ic_history);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp18, dp18);
        iconLp.setMarginEnd(dp8);
        icon.setLayoutParams(iconLp);

        TextView txt = new TextView(requireContext());
        txt.setText(query);
        txt.setTextColor(requireContext().getColor(R.color.clr_text_black));
        txt.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        LinearLayout.LayoutParams txtLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        txt.setLayoutParams(txtLp);

        row.addView(icon);
        row.addView(txt);
        row.setOnClickListener(v -> {
            etSearch.setText(query);
            etSearch.setSelection(query.length());
        });
        containerRecentItems.addView(row);

        View divider = new View(requireContext());
        divider.setBackgroundColor(requireContext().getColor(R.color.clr_divider));
        containerRecentItems.addView(divider,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private ArrayList<String> getRecentSearches() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE);
        ArrayList<String> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_RECENT, "[]"));
            for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
        } catch (JSONException ignored) {}
        return list;
    }

    private void saveRecentSearch(String query) {
        ArrayList<String> list = getRecentSearches();
        list.remove(query);
        list.add(0, query);
        if (list.size() > MAX_RECENT) list = new ArrayList<>(list.subList(0, MAX_RECENT));
        requireContext().getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
                .edit().putString(KEY_RECENT, new JSONArray(list).toString()).apply();
    }

    private void clearRecentSearches() {
        requireContext().getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
                .edit().remove(KEY_RECENT).apply();
    }
}
