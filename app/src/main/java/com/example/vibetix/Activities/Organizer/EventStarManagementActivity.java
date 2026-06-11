package com.example.vibetix.Activities.Organizer;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Adapters.Organizer.OrganizerEventStarAdapter;
import com.example.vibetix.Adapters.Organizer.StarSearchAdapter;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.EventStar;
import com.example.vibetix.Models.Notification;
import com.example.vibetix.Models.Star;
import com.example.vibetix.Models.UserStarFollow;
import com.example.vibetix.R;
import com.example.vibetix.databinding.ActivityEventStarManagementBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class EventStarManagementActivity extends AppCompatActivity {

    private ActivityEventStarManagementBinding binding;
    private FirebaseFirestore db;
    private String eventId;

    private OrganizerEventStarAdapter adapter;
    private List<OrganizerEventStarAdapter.EventStarWrapper> wrapperList = new ArrayList<>();

    // For bottom sheet search
    private List<Star> allActiveStars = new ArrayList<>();
    private StarSearchAdapter searchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventStarManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        eventId = getIntent().getStringExtra("EXTRA_EVENT_ID");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy sự kiện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.rvStars.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrganizerEventStarAdapter(wrapperList, new OrganizerEventStarAdapter.OnEventStarActionListener() {
            @Override
            public void onEdit(OrganizerEventStarAdapter.EventStarWrapper wrapper) {
                showEditDialog(wrapper);
            }

            @Override
            public void onDelete(OrganizerEventStarAdapter.EventStarWrapper wrapper) {
                showDeleteDialog(wrapper);
            }

            @Override
            public void onConfirmedToggle(OrganizerEventStarAdapter.EventStarWrapper wrapper, boolean isConfirmed) {
                String docId = wrapper.eventStar.getEventId() + "_" + wrapper.eventStar.getStarId();
                db.collection(FirebaseCollections.EVENT_STARS).document(docId)
                        .update("is_confirmed", isConfirmed)
                        .addOnFailureListener(e -> {
                            Toast.makeText(EventStarManagementActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            wrapper.eventStar.setConfirmed(!isConfirmed); // revert
                            adapter.notifyDataSetChanged();
                        });
                wrapper.eventStar.setConfirmed(isConfirmed);
            }
        });
        binding.rvStars.setAdapter(adapter);

        setupDragAndDrop();

        binding.fabAddStar.setOnClickListener(v -> showAddStarBottomSheet());

        loadEventStars();
    }

    private void setupDragAndDrop() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                adapter.moveItem(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // Not supported
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                savePerformanceOrder();
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(binding.rvStars);
    }

    private void savePerformanceOrder() {
        List<OrganizerEventStarAdapter.EventStarWrapper> list = adapter.getList();
        if (list == null || list.isEmpty()) return;

        WriteBatch batch = db.batch();
        for (int i = 0; i < list.size(); i++) {
            OrganizerEventStarAdapter.EventStarWrapper wrapper = list.get(i);
            wrapper.eventStar.setPerformanceOrder(i + 1);
            String docId = wrapper.eventStar.getEventId() + "_" + wrapper.eventStar.getStarId();
            batch.update(db.collection(FirebaseCollections.EVENT_STARS).document(docId), "performance_order", i + 1);
        }
        
        batch.commit().addOnFailureListener(e -> 
            Toast.makeText(this, "Lỗi cập nhật thứ tự: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void loadEventStars() {
        binding.pbLoading.setVisibility(View.VISIBLE);
        binding.rvStars.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);

        db.collection(FirebaseCollections.EVENT_STARS)
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    wrapperList.clear();
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        binding.pbLoading.setVisibility(View.GONE);
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    List<EventStar> eventStars = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        EventStar es = doc.toObject(EventStar.class);
                        if (es != null) {
                            eventStars.add(es);
                        }
                    }

                    // Fetch associated Stars
                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    for (EventStar es : eventStars) {
                        if (es.getStarId() != null) {
                            tasks.add(db.collection(FirebaseCollections.STARS).document(es.getStarId()).get());
                        }
                    }

                    if (tasks.isEmpty()) {
                        binding.pbLoading.setVisibility(View.GONE);
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                        for (EventStar es : eventStars) {
                            Star matchedStar = null;
                            for (Object res : results) {
                                DocumentSnapshot ds = (DocumentSnapshot) res;
                                if (ds.exists() && ds.getId().equals(es.getStarId())) {
                                    matchedStar = ds.toObject(Star.class);
                                    if (matchedStar != null) {
                                        matchedStar.setStarId(ds.getId());
                                    }
                                    break;
                                }
                            }
                            wrapperList.add(new OrganizerEventStarAdapter.EventStarWrapper(es, matchedStar));
                        }

                        // Sort by performance_order
                        Collections.sort(wrapperList, (w1, w2) -> {
                            Integer o1 = w1.eventStar.getPerformanceOrder();
                            Integer o2 = w2.eventStar.getPerformanceOrder();
                            if (o1 == null) o1 = 999;
                            if (o2 == null) o2 = 999;
                            return o1.compareTo(o2);
                        });

                        binding.pbLoading.setVisibility(View.GONE);
                        binding.rvStars.setVisibility(View.VISIBLE);
                        adapter.updateData(wrapperList);
                    });

                })
                .addOnFailureListener(e -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddStarBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog);
        com.example.vibetix.databinding.BottomSheetSearchStarsBinding sheetBinding = com.example.vibetix.databinding.BottomSheetSearchStarsBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        searchAdapter = new StarSearchAdapter(new ArrayList<>(), star -> {
            addStarToEvent(star);
            dialog.dismiss();
        });
        sheetBinding.rvSearchStars.setLayoutManager(new LinearLayoutManager(this));
        sheetBinding.rvSearchStars.setAdapter(searchAdapter);

        sheetBinding.btnCloseSheet.setOnClickListener(v -> dialog.dismiss());

        sheetBinding.pbSearchLoading.setVisibility(View.VISIBLE);
        
        // Fetch active stars for local filtering
        db.collection(FirebaseCollections.STARS)
                .whereEqualTo("is_active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allActiveStars.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Star s = doc.toObject(Star.class);
                        if (s != null) {
                            s.setStarId(doc.getId());
                            allActiveStars.add(s);
                        }
                    }
                    sheetBinding.pbSearchLoading.setVisibility(View.GONE);
                    searchAdapter.updateData(allActiveStars);
                    
                    if (allActiveStars.isEmpty()) {
                        sheetBinding.tvEmptySearch.setVisibility(View.VISIBLE);
                    }
                });

        sheetBinding.etSearchStar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().toLowerCase().trim();
                List<Star> filtered = new ArrayList<>();
                for (Star star : allActiveStars) {
                    if (star.getStageName() != null && star.getStageName().toLowerCase().contains(query)) {
                        filtered.add(star);
                    }
                }
                searchAdapter.updateData(filtered);
                sheetBinding.tvEmptySearch.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        dialog.show();
    }

    private void addStarToEvent(Star star) {
        // Check if already in list
        for (OrganizerEventStarAdapter.EventStarWrapper wrapper : wrapperList) {
            if (wrapper.star != null && wrapper.star.getStarId().equals(star.getStarId())) {
                Toast.makeText(this, "Nghệ sĩ này đã có trong danh sách!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        binding.pbLoading.setVisibility(View.VISIBLE);
        EventStar es = new EventStar();
        es.setEventId(eventId);
        es.setStarId(star.getStarId());
        es.setRole("Ca sĩ"); // Default
        es.setPerformanceOrder(wrapperList.size() + 1);
        es.setConfirmed(true);
        es.setAddedAt(Timestamp.now());

        // We use eventId_starId as document ID to enforce uniqueness
        String docId = eventId + "_" + star.getStarId();

        db.collection(FirebaseCollections.EVENT_STARS).document(docId)
                .set(es)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã thêm nghệ sĩ thành công", Toast.LENGTH_SHORT).show();
                    triggerNotificationForFollowers(star);
                    loadEventStars();
                })
                .addOnFailureListener(e -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void triggerNotificationForFollowers(Star star) {
        // Find followers and write notifications using batch
        db.collection(FirebaseCollections.USER_STAR_FOLLOWS)
                .whereEqualTo("star_id", star.getStarId())
                .whereEqualTo("notify_new_event", true)
                .get()
                .addOnSuccessListener(querySnap -> {
                    if (querySnap != null && !querySnap.isEmpty()) {
                        WriteBatch batch = db.batch();
                        int count = 0;
                        for (DocumentSnapshot doc : querySnap) {
                            UserStarFollow follow = doc.toObject(UserStarFollow.class);
                            if (follow != null && follow.getUserId() != null) {
                                String notificationId = UUID.randomUUID().toString();
                                DocumentReference notifRef = db.collection(FirebaseCollections.NOTIFICATIONS).document(notificationId);
                                
                                Notification notif = new Notification();
                                notif.setNotificationId(notificationId);
                                notif.setUserId(follow.getUserId());
                                notif.setType("star_new_event");
                                notif.setTitle("Nghệ sĩ bạn yêu thích có sự kiện mới!");
                                notif.setBody(star.getStageName() + " vừa xác nhận tham gia một sự kiện mới. Mua vé ngay!");
                                notif.setRefType("event");
                                notif.setRefId(eventId);
                                notif.setChannel("in-app");
                                notif.setRead(false);
                                notif.setStatus("sent");
                                notif.setCreatedAt(Timestamp.now());
                                notif.setSentAt(Timestamp.now());

                                batch.set(notifRef, notif);
                                count++;
                                
                                if (count >= 490) break; // Firestore limits 500 writes per batch
                            }
                        }
                        
                        if (count > 0) {
                            batch.commit(); // Fire and forget
                        }
                    }
                });
    }

    private void showEditDialog(OrganizerEventStarAdapter.EventStarWrapper wrapper) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chỉnh sửa: " + (wrapper.star != null ? wrapper.star.getStageName() : ""));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etRole = new EditText(this);
        etRole.setHint("Vai trò (VD: Headliner, MC...)");
        etRole.setText(wrapper.eventStar.getRole());
        layout.addView(etRole);

        final EditText etOrder = new EditText(this);
        etOrder.setHint("Thứ tự biểu diễn (Số)");
        etOrder.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etOrder.setText(String.valueOf(wrapper.eventStar.getPerformanceOrder()));
        layout.addView(etOrder);

        builder.setView(layout);
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String role = etRole.getText().toString().trim();
            String orderStr = etOrder.getText().toString().trim();
            int order = orderStr.isEmpty() ? 0 : Integer.parseInt(orderStr);

            binding.pbLoading.setVisibility(View.VISIBLE);
            String docId = wrapper.eventStar.getEventId() + "_" + wrapper.eventStar.getStarId();
            
            db.collection(FirebaseCollections.EVENT_STARS).document(docId)
                    .update("role", role, "performance_order", order)
                    .addOnSuccessListener(unused -> loadEventStars())
                    .addOnFailureListener(e -> {
                        binding.pbLoading.setVisibility(View.GONE);
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showDeleteDialog(OrganizerEventStarAdapter.EventStarWrapper wrapper) {
        new AlertDialog.Builder(this)
                .setTitle("Xoá Nghệ sĩ")
                .setMessage("Bạn có chắc chắn muốn xoá " + (wrapper.star != null ? wrapper.star.getStageName() : "nghệ sĩ này") + " khỏi sự kiện?")
                .setPositiveButton("Xoá", (dialog, which) -> {
                    binding.pbLoading.setVisibility(View.VISIBLE);
                    String docId = wrapper.eventStar.getEventId() + "_" + wrapper.eventStar.getStarId();
                    db.collection(FirebaseCollections.EVENT_STARS).document(docId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Đã xoá thành công", Toast.LENGTH_SHORT).show();
                                loadEventStars();
                            })
                            .addOnFailureListener(e -> {
                                binding.pbLoading.setVisibility(View.GONE);
                                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
