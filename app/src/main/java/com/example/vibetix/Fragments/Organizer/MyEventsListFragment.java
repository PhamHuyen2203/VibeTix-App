package com.example.vibetix.Fragments.Organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Activities.Organizer.CreateEditEventActivity;
import com.example.vibetix.Activities.Organizer.EventHubActivity;
import com.example.vibetix.Adapters.Organizer.EventDashboardAdapter;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyEventsListFragment extends Fragment {

    private RecyclerView rvMyEvents;
    private LinearLayout layoutEmptyEvents;
    private ProgressBar pbLoading;
    private ExtendedFloatingActionButton fabCreateEvent;
    private View btnCreateEventList;
    private android.widget.EditText etSearchEvents;

    private EventDashboardAdapter adapter;
    private List<Event> eventList;       // full list from Firestore
    private List<Event> filteredList;    // list being displayed

    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private String currentUserId;

    private ActivityResultLauncher<Intent> createEventLauncher;

    public MyEventsListFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register launcher before view creation (must be in onCreate or earlier)
        createEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    // Reload list when event creation/edit is successful
                    loadEvents();
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_events_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());
        
        if (sessionManager.getUserDetails() != null) {
            currentUserId = sessionManager.getUserDetails().getUserId();
        } else {
            currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        }

        bindViews(view);
        setupRecyclerView();
        setupClickListeners();
        loadEvents();
    }

    private void bindViews(View view) {
        rvMyEvents         = view.findViewById(R.id.rvMyEvents);
        layoutEmptyEvents  = view.findViewById(R.id.layoutEmptyEvents);
        pbLoading          = view.findViewById(R.id.pbLoading);
        fabCreateEvent     = view.findViewById(R.id.fabCreateEvent);
        btnCreateEventList = view.findViewById(R.id.btnCreateEventList);
        etSearchEvents     = view.findViewById(R.id.etSearchEvents);
    }

    private void setupRecyclerView() {
        eventList    = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new EventDashboardAdapter(filteredList, new EventDashboardAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                handleEventClick(event);
            }
            @Override
            public void onEventOptionClick(Event event, View anchor) {}
        });
        rvMyEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMyEvents.setAdapter(adapter);
    }

    private void setupClickListeners() {
        View.OnClickListener createListener = v -> {
            Intent intent = new Intent(getActivity(), CreateEditEventActivity.class);
            createEventLauncher.launch(intent);
        };

        if (fabCreateEvent != null) fabCreateEvent.setOnClickListener(createListener);
        if (btnCreateEventList != null) btnCreateEventList.setOnClickListener(createListener);

        // Search filter
        if (etSearchEvents != null) {
            etSearchEvents.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterEvents(s.toString().trim());
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    private void filterEvents(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(eventList);
        } else {
            String lower = query.toLowerCase();
            for (Event e : eventList) {
                if ((e.getTitle() != null && e.getTitle().toLowerCase().contains(lower))
                        || (e.getVenueCity() != null && e.getVenueCity().toLowerCase().contains(lower))) {
                    filteredList.add(e);
                }
            }
        }
        adapter.notifyDataSetChanged();
        layoutEmptyEvents.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadEvents() {
        if (currentUserId == null) return;
        pbLoading.setVisibility(View.VISIBLE);

        // 1. Lấy tất cả quyền từ event_staff
        db.collection("event_staff")
                .whereEqualTo("user_id", currentUserId)
                .get()
                .addOnSuccessListener(staffSnap -> {
                    if (!isAdded()) return;
                    if (staffSnap == null || staffSnap.isEmpty()) {
                        pbLoading.setVisibility(View.GONE);
                        showEmptyState(true);
                        return;
                    }

                    Map<String, String> eventRoleMap = new HashMap<>();
                    for (DocumentSnapshot doc : staffSnap.getDocuments()) {
                        String eId = doc.getString("event_id");
                        String role = doc.getString("role");
                        if (eId != null && role != null) {
                            eventRoleMap.put(eId, role);
                        }
                    }

                    List<String> eventIds = new ArrayList<>(eventRoleMap.keySet());
                    if (eventIds.isEmpty()) {
                        pbLoading.setVisibility(View.GONE);
                        showEmptyState(true);
                        return;
                    }

                    fetchEventDetailsChunked(eventIds, eventRoleMap);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(getContext(), getString(R.string.err_load_data), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchEventDetailsChunked(List<String> allEventIds, Map<String, String> roleMap) {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        
        // Chia chunk 10 items (giới hạn của whereIn)
        for (int i = 0; i < allEventIds.size(); i += 10) {
            int end = Math.min(i + 10, allEventIds.size());
            List<String> chunk = allEventIds.subList(i, end);
            
            Task<QuerySnapshot> task = db.collection(FirebaseCollections.EVENTS)
                    .whereIn(FieldPath.documentId(), chunk)
                    .get();
            tasks.add(task);
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            if (!isAdded()) return;
            pbLoading.setVisibility(View.GONE);
            eventList.clear();

            for (Object result : results) {
                QuerySnapshot snap = (QuerySnapshot) result;
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Event e = doc.toObject(Event.class);
                    if (e != null) {
                        e.setId(doc.getId());
                        // Gán role vào Event model
                        e.setUserRole(roleMap.get(doc.getId()));
                        eventList.add(e);
                    }
                }
            }

            // Sort by startTime descending
            java.util.Collections.sort(eventList, (e1, e2) -> {
                String st1 = e1.getStartTime();
                String st2 = e2.getStartTime();
                if (st1 == null && st2 == null) return 0;
                if (st1 == null) return 1;
                if (st2 == null) return -1;
                return st2.compareTo(st1);
            });

            // Sync filtered list với search query hiện tại
            String currentQuery = etSearchEvents != null ? etSearchEvents.getText().toString().trim() : "";
            filterEvents(currentQuery);
            showEmptyState(filteredList.isEmpty());
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(getContext(), getString(R.string.err_load_event_details), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState(boolean isEmpty) {
        layoutEmptyEvents.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvMyEvents.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void handleEventClick(Event event) {
        String role = event.getUserRole();
        if (role == null || role.isEmpty()) role = "owner";

        sessionManager.setActiveEvent(event.getEventId(), role);

        String status = event.getStatusStr();

        if ("draft".equals(status) || "rejected".equals(status)) {
            // Draft case 1: user tự lưu, chưa gửi
            // Draft case 2: admin từ chối pending → có rejection_reason / status = rejected
            Intent intent = new Intent(getActivity(), CreateEditEventActivity.class);
            intent.putExtra(CreateEditEventActivity.EXTRA_EVENT_ID, event.getEventId());
            // Nếu có lý do từ chối: truyền sang để hiển banner
            if (event.getRejectionReason() != null) {
                intent.putExtra(CreateEditEventActivity.EXTRA_REJECTION_REASON, event.getRejectionReason());
            }
            createEventLauncher.launch(intent);

        } else if ("pending".equals(status)) {
            // Pending: mở dưới dạng read-only — user chờ admin duyệt
            Intent intent = new Intent(getActivity(), CreateEditEventActivity.class);
            intent.putExtra(CreateEditEventActivity.EXTRA_EVENT_ID, event.getEventId());
            intent.putExtra(CreateEditEventActivity.EXTRA_IS_READ_ONLY, true);
            createEventLauncher.launch(intent);

        } else {
            // Approved / Ongoing / Completed / Cancelled → EventHub
            Intent intent = new Intent(getActivity(), EventHubActivity.class);
            intent.putExtra(EventHubActivity.EXTRA_EVENT_ID, event.getEventId());
            intent.putExtra(EventHubActivity.EXTRA_ROLE, role);
            startActivity(intent);
        }
    }
}
