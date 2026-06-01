package com.example.vibetix.Fragments.User;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.vibetix.Adapters.EventAdapter;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AllEventsFragment — Tab "Sự kiện": Hiển thị tất cả events từ Firestore.
 *
 * - Mặc định: approved/ongoing, end_time > now, sort by start_time ASC
 * - Filter chips: Tất cả | Hôm nay | Cuối tuần | Tháng này | Miễn phí
 * - Search bar: tìm theo tên (client-side filter)
 * - SwipeRefresh để reload
 */
public class AllEventsFragment extends Fragment {

    private RecyclerView rvEvents;
    private EventAdapter adapter;
    private List<Event> allEvents = new ArrayList<>();
    private List<Event> displayedEvents = new ArrayList<>();

    private SwipeRefreshLayout swipeRefresh;
    private ChipGroup chipGroup;
    private EditText etSearch;
    private TextView tvResultCount;
    private View emptyState;

    private FirebaseFirestore db;
    private String currentFilter = "active"; // active | today | weekend | month | free
    private String currentSearch = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_all_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        bindViews(view);
        setupRecyclerView();
        setupChips();
        setupSearch();
        setupSwipeRefresh();

        loadEvents();
    }

    private void bindViews(View view) {
        rvEvents     = view.findViewById(R.id.rvAllEvents);
        swipeRefresh = view.findViewById(R.id.swipeRefreshEvents);
        chipGroup    = view.findViewById(R.id.chipGroupFilter);
        etSearch     = view.findViewById(R.id.etSearchEvents);
        tvResultCount = view.findViewById(R.id.tvResultCount);
        emptyState   = view.findViewById(R.id.layoutEmptyEvents);
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(requireContext(), displayedEvents, event -> {
            // Navigate to EventDetailActivity
            if (getActivity() != null) {
                android.content.Intent intent = new android.content.Intent(
                        getActivity(),
                        com.example.vibetix.Activities.User.EventDetailActivity.class);
                intent.putExtra("event_id", event.getEventId());
                startActivity(intent);
            }
        });
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEvents.setAdapter(adapter);
    }

    private void setupChips() {
        if (chipGroup == null) return;
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int chipId = checkedIds.get(0);
            if (chipId == R.id.chipAll)      currentFilter = "active";
            else if (chipId == R.id.chipToday)   currentFilter = "today";
            else if (chipId == R.id.chipWeekend) currentFilter = "weekend";
            else if (chipId == R.id.chipMonth)   currentFilter = "month";
            else if (chipId == R.id.chipFree)    currentFilter = "free";
            applyFilter();
        });
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                currentSearch = s.toString().trim().toLowerCase();
                applyFilter();
            }
        });
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh == null) return;
        swipeRefresh.setColorSchemeResources(R.color.clr_primary);
        swipeRefresh.setOnRefreshListener(this::loadEvents);
    }

    /** Query Firestore: events có status approved/ongoing và chưa hết hạn. */
    private void loadEvents() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);

        String nowStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date());

        db.collection(FirebaseCollections.EVENTS)
                .whereIn("status", java.util.Arrays.asList("approved", "ongoing", "APPROVED", "ONGOING"))
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    allEvents.clear();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                if (event.getEventId() == null) event.setEventId(doc.getId());
                                // Lọc events chưa kết thúc (end_time > now)
                                String endTime = event.getEndTime();
                                if (endTime == null || endTime.compareTo(nowStr) >= 0) {
                                    allEvents.add(event);
                                }
                            }
                        }
                    }
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    applyFilter();
                })
                .addOnFailureListener(e -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    applyFilter(); // Show empty state
                });
    }

    /**
     * Áp dụng filter chip + search lên danh sách.
     */
    private void applyFilter() {
        displayedEvents.clear();
        String nowStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date());

        for (Event event : allEvents) {
            // Search filter
            if (!currentSearch.isEmpty()) {
                String title = event.getTitle() != null ? event.getTitle().toLowerCase() : "";
                if (!title.contains(currentSearch)) continue;
            }

            // Time filter
            String startTime = event.getStartTime() != null ? event.getStartTime() : "";
            switch (currentFilter) {
                case "today":
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    if (!startTime.startsWith(today)) continue;
                    break;
                case "weekend":
                    if (!isWeekend(startTime)) continue;
                    break;
                case "month":
                    String thisMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
                    if (!startTime.startsWith(thisMonth)) continue;
                    break;
                case "free":
                    if (event.getMinPrice() > 0) continue;
                    break;
                // "active" = mặc định, không filter thêm
            }

            displayedEvents.add(event);
        }

        // Update UI
        adapter.notifyDataSetChanged();
        updateResultCount();
        toggleEmptyState();
    }

    private boolean isWeekend(String startTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(startTime.substring(0, 10));
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);
            int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
            return dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateResultCount() {
        if (tvResultCount == null) return;
        tvResultCount.setText(displayedEvents.size() + " sự kiện");
    }

    private void toggleEmptyState() {
        if (emptyState == null) return;
        emptyState.setVisibility(displayedEvents.isEmpty() ? View.VISIBLE : View.GONE);
        rvEvents.setVisibility(displayedEvents.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload khi quay lại tab
        if (allEvents.isEmpty()) loadEvents();
    }
}
