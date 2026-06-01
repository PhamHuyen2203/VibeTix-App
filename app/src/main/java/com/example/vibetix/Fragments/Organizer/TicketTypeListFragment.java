package com.example.vibetix.Fragments.Organizer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Adapters.Organizer.TicketTypeAdapter;
import com.example.vibetix.Models.TicketType;
import com.example.vibetix.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TicketTypeListFragment extends Fragment {

    public static final String ARG_EVENT_ID = "event_id";
    private String eventId;

    private RecyclerView rvTicketTypes;
    private LinearLayout layoutEmpty;
    private ExtendedFloatingActionButton fabAddTicketType;

    private TicketTypeAdapter adapter;
    private List<TicketType> ticketTypes;
    private FirebaseFirestore db;
    private ItemTouchHelper itemTouchHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ticket_type_list, container, false);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }

        db = FirebaseFirestore.getInstance();
        ticketTypes = new ArrayList<>();

        bindViews(view);
        setupRecyclerView();
        setupFab();

        if (eventId != null) {
            loadTicketTypes();
        } else {
            showEmptyState(true);
        }

        return view;
    }

    private void bindViews(View view) {
        rvTicketTypes = view.findViewById(R.id.rvTicketTypes);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        fabAddTicketType = view.findViewById(R.id.fabAddTicketType);
    }

    private void setupRecyclerView() {
        adapter = new TicketTypeAdapter(ticketTypes, new TicketTypeAdapter.OnTicketTypeInteractionListener() {
            @Override
            public void onEdit(TicketType ticketType) {
                showCreateEditBottomSheet(ticketType);
            }

            @Override
            public void onDelete(TicketType ticketType) {
                deleteTicketType(ticketType);
            }

            @Override
            public void onStatusChanged(TicketType ticketType, boolean isActive) {
                updateTicketTypeStatus(ticketType, isActive);
            }

            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                if (itemTouchHelper != null) {
                    itemTouchHelper.startDrag(viewHolder);
                }
            }
        });

        rvTicketTypes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTicketTypes.setAdapter(adapter);

        setupItemTouchHelper();
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                Collections.swap(ticketTypes, fromPosition, toPosition);
                adapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                saveOrder(); // Cập nhật sort_order lên Firestore sau khi drag drop xong
            }
        };

        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(rvTicketTypes);
    }

    private void setupFab() {
        fabAddTicketType.setOnClickListener(v -> showCreateEditBottomSheet(null));
    }

    private void loadTicketTypes() {
        db.collection("events").document(eventId).collection("ticket_types")
                .orderBy("sort_order", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    ticketTypes.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            TicketType tt = doc.toObject(TicketType.class);
                            tt.setTicketTypeId(doc.getId());
                            ticketTypes.add(tt);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    showEmptyState(ticketTypes.isEmpty());
                });
    }

    private void showEmptyState(boolean isEmpty) {
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvTicketTypes.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showCreateEditBottomSheet(@Nullable TicketType ticketType) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext(), R.style.Theme_Design_BottomSheetDialog);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_create_ticket_type, null);
        sheet.setContentView(view);

        TextInputEditText etTicketName = view.findViewById(R.id.etTicketName);
        TextInputEditText etTicketPrice = view.findViewById(R.id.etTicketPrice);
        TextInputEditText etTicketQuantity = view.findViewById(R.id.etTicketQuantity);
        MaterialSwitch swTransferable = view.findViewById(R.id.swTransferable);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveTicketType);

        if (ticketType != null) {
            etTicketName.setText(ticketType.getName());
            etTicketPrice.setText(String.valueOf(ticketType.getPrice()));
            etTicketQuantity.setText(String.valueOf(ticketType.getTotalQuantity()));
            swTransferable.setChecked(ticketType.isTransferable());
        }

        btnSave.setOnClickListener(v -> {
            String name = etTicketName.getText().toString().trim();
            String priceStr = etTicketPrice.getText().toString().trim();
            String qtyStr = etTicketQuantity.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(qtyStr)) {
                Toast.makeText(getContext(), "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);
            int qty = Integer.parseInt(qtyStr);

            TicketType tt = ticketType != null ? ticketType : new TicketType();
            tt.setName(name);
            tt.setPrice(price);
            tt.setTotalQuantity(qty);
            tt.setAvailableQuantity(qty - tt.getSoldCount()); // Tạm tính
            tt.setTransferable(swTransferable.isChecked());
            tt.setEventId(eventId);

            if (ticketType == null) {
                tt.setTicketTypeId(UUID.randomUUID().toString());
                tt.setSortOrder(ticketTypes.size());
            }

            db.collection("events").document(eventId)
                    .collection("ticket_types").document(tt.getTicketTypeId())
                    .set(tt)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Đã lưu loại vé", Toast.LENGTH_SHORT).show();
                        sheet.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        sheet.show();
    }

    private void deleteTicketType(TicketType ticketType) {
        db.collection("events").document(eventId)
                .collection("ticket_types").document(ticketType.getTicketTypeId())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Đã xóa loại vé", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Xóa thất bại", Toast.LENGTH_SHORT).show());
    }

    private void updateTicketTypeStatus(TicketType ticketType, boolean isActive) {
        db.collection("events").document(eventId)
                .collection("ticket_types").document(ticketType.getTicketTypeId())
                .update("is_active", isActive)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Cập nhật trạng thái thất bại", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                });
    }

    private void saveOrder() {
        for (int i = 0; i < ticketTypes.size(); i++) {
            TicketType tt = ticketTypes.get(i);
            if (tt.getSortOrder() != i) {
                tt.setSortOrder(i);
                db.collection("events").document(eventId)
                        .collection("ticket_types").document(tt.getTicketTypeId())
                        .update("sort_order", i);
            }
        }
    }
}
