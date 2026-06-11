package com.example.vibetix.Activities.Organizer;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.TicketType;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.example.vibetix.databinding.ActivityTicketTypeManagementBinding;
import com.example.vibetix.databinding.BottomSheetTicketTypeBinding;
import com.example.vibetix.databinding.ItemTicketTypeRowBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TicketTypeManagementActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID    = "extra_event_id";
    public static final String EXTRA_EVENT_TITLE = "extra_event_title";

    private ActivityTicketTypeManagementBinding binding;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    private String eventId;
    private String eventTitle;

    private final List<TicketType> ticketTypes = new ArrayList<>();
    private TicketTypeAdapter adapter;

    // NumberFormat for VNĐ currency
    private final NumberFormat vndFmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTicketTypeManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db             = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Read intent extras
        eventId    = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        setupToolbar();
        setupRecyclerView();
        setupFab();

        if (eventId != null && !eventId.isEmpty()) {
            loadTicketTypes();
        } else {
            showError("Không tìm thấy thông tin sự kiện.");
        }
    }

    // ── Toolbar ──────────────────────────────────────────────────────────────
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý loại vé");
            if (eventTitle != null && !eventTitle.isEmpty()) {
                getSupportActionBar().setSubtitle(eventTitle);
                binding.tvEventName.setText(eventTitle);
            }
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    // ── RecyclerView ─────────────────────────────────────────────────────────
    private void setupRecyclerView() {
        adapter = new TicketTypeAdapter(this, ticketTypes,
                this::onEditTicketType,
                this::onDeleteTicketType,
                this::onToggleActive);
        binding.rvTicketTypes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTicketTypes.setAdapter(adapter);
        binding.rvTicketTypes.setNestedScrollingEnabled(false);
    }

    // ── FAB ──────────────────────────────────────────────────────────────────
    private void setupFab() {
        String role = sessionManager.getStaffRole();
        if ("check_in_staff".equals(role)) {
            // Check-in staff cannot manage ticket types
            binding.fabAddTicketType.setVisibility(View.GONE);
            return;
        }
        binding.fabAddTicketType.setOnClickListener(v -> openTicketTypeSheet(null));
    }

    // ── Load from Firestore ───────────────────────────────────────────────────
    private void loadTicketTypes() {
        binding.pbLoading.setVisibility(View.VISIBLE);
        binding.layoutEmpty.setVisibility(View.GONE);

        db.collection(FirebaseCollections.TICKET_TYPES)
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    ticketTypes.clear();

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    double totalRevenue = 0;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            TicketType tt = doc.toObject(TicketType.class);
                            if (tt != null) {
                                if (tt.getTicketTypeId() == null) {
                                    tt.setTicketTypeId(doc.getId());
                                }
                                ticketTypes.add(tt);
                                totalRevenue += tt.getPrice() * tt.getSoldCount();
                            }
                        } catch (Exception e) {
                            // Bỏ qua document bị lỗi parse data
                            e.printStackTrace();
                        }
                    }
                    
                    // Sort locally to avoid Firestore composite index requirement
                    java.util.Collections.sort(ticketTypes, (t1, t2) -> Long.compare(t1.getSortOrder(), t2.getSortOrder()));
                    
                    adapter.notifyDataSetChanged();

                    // Update header stats
                    binding.tvTicketTypeCount.setText(ticketTypes.size() + " loại vé");
                    binding.tvTotalRevenue.setText("Tổng doanh thu: " + vndFmt.format((long) totalRevenue) + " ₫");
                    
                    if (ticketTypes.isEmpty()) {
                        showEmptyState();
                    } else {
                        binding.layoutEmpty.setVisibility(View.GONE);
                        binding.rvTicketTypes.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.pbLoading.setVisibility(View.GONE);
                    showError("Không thể tải dữ liệu: " + e.getMessage());
                    showEmptyState();
                });
    }

    private void showEmptyState() {
        binding.layoutEmpty.setVisibility(View.VISIBLE);
        binding.rvTicketTypes.setVisibility(View.GONE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADD / EDIT bottom sheet
    // ─────────────────────────────────────────────────────────────────────────
    private void openTicketTypeSheet(TicketType existing) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog);
        BottomSheetTicketTypeBinding sheetBinding =
                BottomSheetTicketTypeBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());
        dialog.getBehavior().setPeekHeight(700);

        boolean isEdit = (existing != null);
        sheetBinding.tvBottomSheetTitle.setText(isEdit ? "Chỉnh sửa loại vé" : "Thêm loại vé");

        // Pre-fill for edit
        if (isEdit) {
            sheetBinding.etTicketName.setText(existing.getName());
            sheetBinding.etTicketPrice.setText(String.valueOf((long) existing.getPrice()));
            sheetBinding.etTotalQty.setText(String.valueOf(existing.getTotalQuantity()));
            sheetBinding.etTicketDesc.setText(existing.getDescription());
            if (existing.getSaleStart() != null) sheetBinding.etSaleStart.setText(displayDate(existing.getSaleStart()));
            if (existing.getSaleEnd() != null)   sheetBinding.etSaleEnd.setText(displayDate(existing.getSaleEnd()));
            sheetBinding.switchTransferable.setChecked(existing.isTransferable());
            sheetBinding.switchIsActive.setChecked(existing.isActive());

            // Lock price if there are confirmed orders
            checkHasConfirmedOrders(existing.getTicketTypeId(), hasOrders -> {
                if (hasOrders) {
                    sheetBinding.etTicketPrice.setEnabled(false);
                    sheetBinding.etTicketPrice.setFocusable(false);
                    sheetBinding.tvPriceLockWarning.setVisibility(View.VISIBLE);
                }
            });
        }

        // Date pickers
        sheetBinding.etSaleStart.setOnClickListener(v ->
                showDatePicker(sheetBinding.etSaleStart));
        sheetBinding.etSaleEnd.setOnClickListener(v ->
                showDatePicker(sheetBinding.etSaleEnd));

        // Save
        sheetBinding.btnSaveTicketType.setOnClickListener(v -> {
            String name        = getText(sheetBinding.etTicketName);
            String priceStr    = getText(sheetBinding.etTicketPrice);
            String qtyStr      = getText(sheetBinding.etTotalQty);
            String desc        = getText(sheetBinding.etTicketDesc);
            String saleStart   = getText(sheetBinding.etSaleStart);
            String saleEnd     = getText(sheetBinding.etSaleEnd);
            boolean transferable = sheetBinding.switchTransferable.isChecked();
            boolean isActive   = sheetBinding.switchIsActive.isChecked();

            // Validate
            if (TextUtils.isEmpty(name)) {
                sheetBinding.etTicketName.setError("Vui lòng nhập tên loại vé");
                return;
            }
            if (TextUtils.isEmpty(priceStr)) {
                sheetBinding.etTicketPrice.setError("Vui lòng nhập giá vé");
                return;
            }
            if (TextUtils.isEmpty(qtyStr)) {
                sheetBinding.etTotalQty.setError("Vui lòng nhập số lượng vé");
                return;
            }

            long price;
            long qty;
            try {
                price = Long.parseLong(priceStr);
                qty   = Long.parseLong(qtyStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Giá hoặc số lượng không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            if (price < 0) { sheetBinding.etTicketPrice.setError("Giá không hợp lệ"); return; }
            if (qty <= 0)  { sheetBinding.etTotalQty.setError("Số lượng phải lớn hơn 0"); return; }

            sheetBinding.btnSaveTicketType.setEnabled(false);
            sheetBinding.btnSaveTicketType.setText("Đang lưu…");

            if (isEdit) {
                if (qty < existing.getSoldCount()) {
                    sheetBinding.etTotalQty.setError("Không thể giảm số lượng xuống dưới số vé đã bán (" + existing.getSoldCount() + " vé)!");
                    sheetBinding.etTotalQty.requestFocus();
                    sheetBinding.btnSaveTicketType.setEnabled(true);
                    sheetBinding.btnSaveTicketType.setText("LƯU THÔNG TIN");
                    return;
                }
                updateTicketType(existing, name, price, qty, desc, saleStart, saleEnd,
                        transferable, isActive, dialog);
            } else {
                createTicketType(name, price, qty, desc, saleStart, saleEnd,
                        transferable, isActive, dialog);
            }
        });

        dialog.show();
    }

    private void createTicketType(String name, long price, long qty, String desc,
                                   String saleStart, String saleEnd,
                                   boolean transferable, boolean isActive,
                                   BottomSheetDialog dialog) {
        String id = UUID.randomUUID().toString();
        com.google.firebase.Timestamp tsSaleStart = toTimestamp(saleStart);
        com.google.firebase.Timestamp tsSaleEnd   = toTimestamp(saleEnd);
        Map<String, Object> data = buildTicketTypeMap(id, name, price, qty, desc,
                tsSaleStart, tsSaleEnd, transferable, isActive);

        db.collection(FirebaseCollections.TICKET_TYPES).document(id)
                .set(data)
                .addOnSuccessListener(v -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Đã tạo loại vé thành công ✓", Toast.LENGTH_SHORT).show();
                    loadTicketTypes();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    showError("Lỗi tạo loại vé: " + e.getMessage());
                });
    }

    private void updateTicketType(TicketType existing, String name, long price, long qty,
                                   String desc, String saleStart, String saleEnd,
                                   boolean transferable, boolean isActive,
                                   BottomSheetDialog dialog) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        // Only update price if field is enabled (no confirmed orders)
        updates.put("price", price);
        // Update available_quantity relative to how many were already sold
        long sold = existing.getSoldCount();
        updates.put("total_quantity", qty);
        updates.put("available_quantity", Math.max(0, qty - sold));
        updates.put("description", desc);
        updates.put("sale_start", toTimestamp(saleStart));
        updates.put("sale_end", toTimestamp(saleEnd));
        updates.put("is_transferable", transferable);
        updates.put("is_active", isActive);

        db.collection(FirebaseCollections.TICKET_TYPES).document(existing.getTicketTypeId())
                .update(updates)
                .addOnSuccessListener(v -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Đã cập nhật loại vé ✓", Toast.LENGTH_SHORT).show();
                    loadTicketTypes();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    showError("Lỗi cập nhật: " + e.getMessage());
                });
    }

    private Map<String, Object> buildTicketTypeMap(String id, String name, long price,
                                                    long qty, String desc,
                                                    com.google.firebase.Timestamp saleStart, com.google.firebase.Timestamp saleEnd,
                                                    boolean transferable, boolean isActive) {
        Map<String, Object> data = new HashMap<>();
        data.put("ticket_type_id", id);
        data.put("event_id", eventId);
        data.put("name", name);
        data.put("price", price);
        data.put("total_quantity", qty);
        data.put("available_quantity", qty);
        data.put("sold_quantity", 0L);
        data.put("description", TextUtils.isEmpty(desc) ? "" : desc);
        data.put("sale_start", saleStart);
        data.put("sale_end", saleEnd);
        data.put("is_transferable", transferable);
        data.put("is_active", isActive);
        data.put("sort_order", ticketTypes.size());
        return data;
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    private void onDeleteTicketType(TicketType tt) {
        // Check if any order_items exist for this ticket type
        db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereEqualTo("ticket_type_id", tt.getTicketTypeId())
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        // Has orders — cannot delete
                        new AlertDialog.Builder(this)
                                .setTitle("Không thể xóa")
                                .setMessage("Loại vé \"" + tt.getName() + "\" đã có đơn hàng. " +
                                        "Hãy tắt kích hoạt thay vì xóa.")
                                .setPositiveButton("Đã hiểu", null)
                                .show();
                    } else {
                        // Safe to delete
                        confirmDelete(tt);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không kiểm tra được đơn hàng", Toast.LENGTH_SHORT).show());
    }

    private void confirmDelete(TicketType tt) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa loại vé?")
                .setMessage("Bạn có chắc muốn xóa loại vé \"" + tt.getName() + "\"?\nHành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (d, w) -> {
                    db.collection(FirebaseCollections.TICKET_TYPES)
                            .document(tt.getTicketTypeId())
                            .delete()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Đã xóa loại vé", Toast.LENGTH_SHORT).show();
                                loadTicketTypes();
                            })
                            .addOnFailureListener(e ->
                                    showError("Xóa thất bại: " + e.getMessage()));
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ── Toggle active ─────────────────────────────────────────────────────────
    private void onToggleActive(TicketType tt, boolean newValue) {
        db.collection(FirebaseCollections.TICKET_TYPES)
                .document(tt.getTicketTypeId())
                .update("is_active", newValue)
                .addOnSuccessListener(v -> {
                    tt.setActive(newValue);
                    String msg = newValue ? "Đã kích hoạt loại vé" : "Đã tắt loại vé";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Revert switch on failure
                    Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                    int idx = ticketTypes.indexOf(tt);
                    if (idx >= 0) adapter.notifyItemChanged(idx);
                });
    }

    private void onEditTicketType(TicketType tt) {
        openTicketTypeSheet(tt);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    interface HasOrdersCallback {
        void onResult(boolean hasOrders);
    }

    private void checkHasConfirmedOrders(String ticketTypeId, HasOrdersCallback callback) {
        db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereEqualTo("ticket_type_id", ticketTypeId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> callback.onResult(snap != null && !snap.isEmpty()))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    private String displayDate(com.google.firebase.Timestamp ts) {
        if (ts == null) return "";
        try {
            SimpleDateFormat dispFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return dispFmt.format(ts.toDate());
        } catch (Exception e) {
            return "";
        }
    }

    private com.google.firebase.Timestamp toTimestamp(String displayDate) {
        if (displayDate == null || displayDate.isEmpty()) return null;
        try {
            SimpleDateFormat dispFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return new com.google.firebase.Timestamp(dispFmt.parse(displayDate));
        } catch (Exception e) {
            return null;
        }
    }

    private void showDatePicker(com.google.android.material.textfield.TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this,
                (datePicker, y, m, d) -> {
                    String formatted = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y);
                    target.setText(formatted);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        Editable e = et.getText();
        return e == null ? "" : e.toString().trim();
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    // =========================================================================
    // Inner RecyclerView Adapter
    // =========================================================================
    public static class TicketTypeAdapter
            extends RecyclerView.Adapter<TicketTypeAdapter.ViewHolder> {

        interface OnEdit   { void onEdit(TicketType tt); }
        interface OnDelete { void onDelete(TicketType tt); }
        interface OnToggle { void onToggle(TicketType tt, boolean checked); }

        private final Context context;
        private final List<TicketType> items;
        private final OnEdit onEdit;
        private final OnDelete onDelete;
        private final OnToggle onToggle;
        private final NumberFormat vndFmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

        public TicketTypeAdapter(Context ctx, List<TicketType> items,
                                  OnEdit onEdit, OnDelete onDelete, OnToggle onToggle) {
            this.context  = ctx;
            this.items    = items;
            this.onEdit   = onEdit;
            this.onDelete = onDelete;
            this.onToggle = onToggle;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemTicketTypeRowBinding b = ItemTicketTypeRowBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(b);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder vh, int position) {
            TicketType tt = items.get(position);
            ItemTicketTypeRowBinding b = vh.binding;

            // Name
            b.tvTicketName.setText(tt.getName());

            // Price
            long priceVal = (long) tt.getPrice();
            if (priceVal == 0) {
                b.tvTicketPrice.setText("Miễn phí");
            } else {
                b.tvTicketPrice.setText(vndFmt.format(priceVal) + " ₫");
            }

            // Sold / total progress
            long sold  = tt.getSoldCount();
            long total = tt.getTotalQuantity();
            b.tvSoldCount.setText(sold + " / " + total + " vé");
            int progress = (total > 0) ? (int) ((sold * 100f) / total) : 0;
            b.pbTicketSales.setMax(100);
            b.pbTicketSales.setProgress(progress);

            // Status badge
            if (tt.isActive()) {
                b.tvStatusBadge.setText("Hoạt động");
                b.tvStatusBadge.setTextColor(context.getColor(R.color.clr_success));
                b.tvStatusBadge.setBackgroundResource(R.drawable.bg_ticket_type_active);
            } else {
                b.tvStatusBadge.setText("Tạm tắt");
                b.tvStatusBadge.setTextColor(context.getColor(R.color.clr_error));
                b.tvStatusBadge.setBackgroundResource(R.drawable.bg_ticket_type_inactive);
            }

            // Active switch — suppress listener while setting programmatically
            b.switchActive.setOnCheckedChangeListener(null);
            b.switchActive.setChecked(tt.isActive());
            b.switchActive.setOnCheckedChangeListener((v, checked) ->
                    onToggle.onToggle(tt, checked));

            // Edit / Delete
            b.btnEditTicket.setOnClickListener(v -> onEdit.onEdit(tt));
            b.btnDeleteTicket.setOnClickListener(v -> onDelete.onDelete(tt));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ItemTicketTypeRowBinding binding;
            ViewHolder(ItemTicketTypeRowBinding b) {
                super(b.getRoot());
                binding = b;
            }
        }
    }
}
