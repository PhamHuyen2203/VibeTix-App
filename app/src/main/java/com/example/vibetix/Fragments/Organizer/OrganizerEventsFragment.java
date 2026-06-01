package com.example.vibetix.Fragments.Organizer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;
import android.content.Intent;
import com.example.vibetix.Activities.Organizer.TicketTypeActivity;
import com.example.vibetix.Activities.Organizer.CreateEditEventActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Adapters.Organizer.EventDashboardAdapter;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * OrganizerEventsFragment — Danh sách sự kiện của organizer hiện tại.
 *
 * Tính năng:
 *  - Load events từ Firestore collection "EVENT" theo organizer_id
 *  - Filter theo status: All / Approved / Pending / Ongoing
 *  - Search theo tên event (local filter)
 *  - FAB tạo sự kiện mới
 *  - Empty state khi không có events
 */
public class OrganizerEventsFragment extends Fragment {

    private RecyclerView rvOrganizerEvents;
    private ProgressBar pbEventsLoading;
    private LinearLayout layoutEventsEmpty;
    private EditText etEventSearch;
    private ExtendedFloatingActionButton fabCreateEvent;

    // Filter chips
    private com.google.android.material.chip.ChipGroup chipGroupStatus;

    private EventDashboardAdapter eventAdapter;
    private List<Event> allEvents;        // toàn bộ list từ Firestore
    private List<Event> filteredEvents;   // list sau filter + search

    private FirebaseFirestore db;
    private SessionManager sessionManager;

    private String activeFilter = "ALL"; // ALL | APPROVED | PENDING | ONGOING

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_organizer_events, container, false);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());

        bindViews(view);
        setupRecyclerView();
        setupFilterChips();
        setupSearch();
        setupFab();
        loadEvents();

        return view;
    }

    private void bindViews(View view) {
        rvOrganizerEvents = view.findViewById(R.id.rvOrganizerEvents);
        pbEventsLoading   = view.findViewById(R.id.pbEventsLoading);
        layoutEventsEmpty = view.findViewById(R.id.layoutEventsEmpty);
        etEventSearch     = view.findViewById(R.id.etEventSearch);
        fabCreateEvent    = view.findViewById(R.id.fabCreateEvent);

        chipGroupStatus = view.findViewById(R.id.chipGroupStatus);
    }

    private void setupRecyclerView() {
        allEvents      = new ArrayList<>();
        filteredEvents = new ArrayList<>();

        eventAdapter = new EventDashboardAdapter(filteredEvents, new EventDashboardAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                Toast.makeText(getContext(), "Event: " + event.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEventOptionClick(Event event, View anchor) {
                showEventOptionsMenu(event, anchor);
            }
        });

        rvOrganizerEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOrganizerEvents.setAdapter(eventAdapter);
    }

    private void showEventOptionsMenu(Event event, View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, "Chỉnh sửa");
        popup.getMenu().add(0, 2, 0, "Quản lý vé");
        popup.getMenu().add(0, 3, 0, "Gửi duyệt");
        popup.getMenu().add(0, 4, 0, "Huỷ sự kiện");
        popup.getMenu().add(0, 5, 0, "Quản lý nhân sự");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    Intent editIntent = new Intent(getActivity(), CreateEditEventActivity.class);
                    editIntent.putExtra(CreateEditEventActivity.EXTRA_EVENT_ID, event.getEventId());
                    startActivity(editIntent);
                    return true;
                case 2:
                    Intent ticketIntent = new Intent(getActivity(), TicketTypeActivity.class);
                    ticketIntent.putExtra(TicketTypeActivity.EXTRA_EVENT_ID, event.getEventId());
                    startActivity(ticketIntent);
                    return true;
                case 3:
                    Toast.makeText(getContext(), "Đã gửi duyệt sự kiện", Toast.LENGTH_SHORT).show();
                    return true;
                case 4:
                    Toast.makeText(getContext(), "Đã huỷ sự kiện", Toast.LENGTH_SHORT).show();
                    return true;
                case 5:
                    Intent staffIntent = new Intent(getActivity(), com.example.vibetix.Activities.Organizer.StaffManagementActivity.class);
                    staffIntent.putExtra(com.example.vibetix.Activities.Organizer.StaffManagementActivity.EXTRA_EVENT_ID, event.getEventId());
                    startActivity(staffIntent);
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void setupFilterChips() {
        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            
            if (checkedId == R.id.chipAll) {
                activeFilter = "ALL";
            } else if (checkedId == R.id.chipApproved) {
                activeFilter = "APPROVED";
            } else if (checkedId == R.id.chipPending) {
                activeFilter = "PENDING";
            } else if (checkedId == R.id.chipOngoing) {
                activeFilter = "ONGOING";
            } else if (checkedId == R.id.chipDraft) {
                activeFilter = "DRAFT";
            } else if (checkedId == R.id.chipCompleted) {
                activeFilter = "COMPLETED";
            } else if (checkedId == R.id.chipCancelled) {
                activeFilter = "CANCELLED";
            }
            
            applyFilters();
        });
    }

    private void setupSearch() {
        etEventSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFab() {
        fabCreateEvent.setOnClickListener(v ->
                Toast.makeText(getContext(), "Tạo sự kiện mới (Coming soon)", Toast.LENGTH_SHORT).show()
        );

        // Cũng xử lý btn trong empty state
        View btnCreateFirstEvent = requireView().findViewById(R.id.btnCreateFirstEvent);
        if (btnCreateFirstEvent != null) {
            btnCreateFirstEvent.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Tạo sự kiện mới (Coming soon)", Toast.LENGTH_SHORT).show()
            );
        }
    }

    /**
     * Load events từ Firestore theo organizer_id.
     * organizer_id lấy từ SessionManager (userId của user đang đăng nhập).
     */
    private void loadEvents() {
        String userId = null;
        if (sessionManager.getUserDetails() != null) {
            userId = sessionManager.getUserDetails().getUserId();
        }

        if (userId == null) {
            showEmptyState(true);
            return;
        }

        showLoading(true);

        // Tìm ORGANIZER document của user hiện tại trước
        final String finalUserId = userId;
        db.collection("organizers")
                .whereEqualTo("user_id", finalUserId)
                .limit(1)
                .get()
                .addOnSuccessListener(orgSnapshot -> {
                    if (orgSnapshot == null || orgSnapshot.isEmpty()) {
                        // Thử query event trực tiếp bằng user_id (một số schema dùng user_id)
                        fetchEventsByField("organizer_id", finalUserId); // Fallback: try to query by user_id directly as organizer_id
                    } else {
                        com.example.vibetix.Models.Organizer organizer = orgSnapshot.getDocuments().get(0).toObject(com.example.vibetix.Models.Organizer.class);
                        String organizerId = organizer.getOrganizerId();
                        if (organizerId == null) organizerId = orgSnapshot.getDocuments().get(0).getId();
                        fetchEventsByField("organizer_id", organizerId);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showEmptyState(true);
                });
    }

    private void fetchEventsByField(String field, String value) {
        db.collection("events")
                .whereEqualTo(field, value)
                .get()
                .addOnSuccessListener(snapshot -> {
                    showLoading(false);
                    allEvents.clear();

                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                allEvents.add(event);
                            }
                        }
                    }

                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showEmptyState(true);
                });
    }

    /**
     * Áp dụng bộ lọc status + search query lên allEvents → filteredEvents.
     */
    private void applyFilters() {
        String searchQuery = etEventSearch.getText().toString().trim().toLowerCase();
        filteredEvents.clear();

        for (Event event : allEvents) {
            // Filter by status
            boolean statusMatch = true;
            if (!"ALL".equals(activeFilter)) {
                Event.Status targetStatus;
                switch (activeFilter) {
                    case "APPROVED": targetStatus = Event.Status.APPROVED; break;
                    case "PENDING":  targetStatus = Event.Status.PENDING;  break;
                    case "ONGOING":  targetStatus = Event.Status.ONGOING;  break;
                    case "DRAFT":    targetStatus = Event.Status.DRAFT;    break;
                    case "COMPLETED":targetStatus = Event.Status.COMPLETED;break;
                    case "CANCELLED":targetStatus = Event.Status.CANCELLED;break;
                    default:         targetStatus = null;
                }
                statusMatch = (targetStatus == null || event.getStatus() == targetStatus);
            }

            // Filter by search query
            boolean searchMatch = searchQuery.isEmpty() ||
                    (event.getTitle() != null &&
                     event.getTitle().toLowerCase().contains(searchQuery));

            if (statusMatch && searchMatch) {
                filteredEvents.add(event);
            }
        }

        eventAdapter.notifyDataSetChanged();
        showEmptyState(filteredEvents.isEmpty());
    }

    private void showLoading(boolean show) {
        pbEventsLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        rvOrganizerEvents.setVisibility(show ? View.GONE : View.VISIBLE);
        layoutEventsEmpty.setVisibility(View.GONE);
    }

    private void showEmptyState(boolean show) {
        layoutEventsEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        rvOrganizerEvents.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
