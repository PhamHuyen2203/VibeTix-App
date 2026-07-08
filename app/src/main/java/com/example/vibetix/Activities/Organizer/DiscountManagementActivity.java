package com.example.vibetix.Activities.Organizer;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.appcompat.widget.Toolbar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Adapters.Organizer.DiscountAdapter;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Discount;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.example.vibetix.databinding.BottomSheetDiscountBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DiscountManagementActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView rvDiscounts;
    private ExtendedFloatingActionButton fabAddDiscount;
    private android.widget.LinearLayout layoutEmpty;

    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private DiscountAdapter adapter;
    private List<Discount> allDiscounts = new ArrayList<>();
    
    private String eventId;
    private final java.text.SimpleDateFormat displayFmt = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
    private final java.text.SimpleDateFormat isoFmt    = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discount_management);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        rvDiscounts = findViewById(R.id.rvDiscounts);
        fabAddDiscount = (ExtendedFloatingActionButton) findViewById(R.id.fabAddDiscount);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        toolbar.setNavigationOnClickListener(v -> finish());
        
        rvDiscounts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DiscountAdapter(new ArrayList<>(), new DiscountAdapter.Listener() {
            @Override
            public void onItemClick(Discount discount) {
                openDiscountSheet(discount);
            }

            @Override
            public void onToggleActive(Discount discount) {
                toggleDiscountActiveState(discount);
            }
        });
        rvDiscounts.setAdapter(adapter);

        fabAddDiscount.setOnClickListener(v -> openDiscountSheet(null));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterDiscounts(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        if (eventId != null && !eventId.isEmpty()) {
            loadDiscounts();
        } else {
            Toast.makeText(this, getString(R.string.str_toast_event_info_not_found), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadDiscounts() {
        db.collection(FirebaseCollections.DISCOUNTS)
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allDiscounts.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Discount d = doc.toObject(Discount.class);
                            d.setId(doc.getId());
                            allDiscounts.add(d);
                        } catch (Exception ex) {
                            android.util.Log.e("DiscountManagement", "Parse error for doc " + doc.getId(), ex);
                        }
                    }
                    filterDiscounts(tabLayout.getSelectedTabPosition());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.str_toast_load_discount_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void filterDiscounts(int tabPosition) {
        List<Discount> filteredList = new ArrayList<>();
        java.util.Date now = new java.util.Date();

        for (Discount d : allDiscounts) {
            com.google.firebase.Timestamp expiryTs = d.getExpiryDate();
            boolean isExpired = (expiryTs != null && expiryTs.toDate().before(now));

            if (tabPosition == 0) { // Hiệu lực
                if (d.isActive() && !isExpired) {
                    filteredList.add(d);
                }
            } else if (tabPosition == 1) { // Hết hạn
                if (isExpired) {
                    filteredList.add(d);
                }
            } else if (tabPosition == 2) { // Đã tắt
                if (!d.isActive()) {
                    filteredList.add(d);
                }
            }
        }
        
        if (filteredList.isEmpty()) {
            layoutEmpty.setVisibility(android.view.View.VISIBLE);
            rvDiscounts.setVisibility(android.view.View.GONE);
        } else {
            layoutEmpty.setVisibility(android.view.View.GONE);
            rvDiscounts.setVisibility(android.view.View.VISIBLE);
        }
        
        adapter.updateData(filteredList);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // ADD / EDIT bottom sheet
    // ─────────────────────────────────────────────────────────────────────────
    private void openDiscountSheet(Discount existing) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog);
        BottomSheetDiscountBinding sheetBinding = BottomSheetDiscountBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());
        dialog.getBehavior().setPeekHeight(700);

        boolean isEdit = (existing != null);
        sheetBinding.tvBottomSheetTitle.setText(isEdit ? getString(R.string.str_edit_discount_title) : getString(R.string.str_add_discount_title));

        // Setup Dropdown for Type
        String[] types = new String[]{"Phần trăm (%)", "Số tiền (VNĐ)"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, types);
        sheetBinding.actvDiscountType.setAdapter(typeAdapter);
        sheetBinding.actvDiscountType.setText(types[0], false);

        // Pre-fill
        if (isEdit) {
            sheetBinding.etDiscountCode.setText(existing.getCode());
            sheetBinding.etDiscountTitle.setText(existing.getTitle());
            sheetBinding.etDiscountValue.setText(String.valueOf((long) existing.getValue()));
            sheetBinding.etUsageLimit.setText(String.valueOf(existing.getUsageLimit()));
            sheetBinding.etMinOrderValue.setText(String.valueOf((long) existing.getMinOrderValue()));
            sheetBinding.etMaxDiscount.setText(String.valueOf((long) existing.getMaxDiscount()));

            sheetBinding.etSaleStart.setText(timestampToDisplay(existing.getStartDate()));
            sheetBinding.etSaleEnd.setText(timestampToDisplay(existing.getExpiryDate()));

            sheetBinding.switchIsActive.setChecked(existing.isActive());

            if ("fixed".equals(existing.getType())) {
                sheetBinding.actvDiscountType.setText(types[1], false);
            } else {
                sheetBinding.actvDiscountType.setText(types[0], false);
            }
            sheetBinding.etDiscountCode.setEnabled(false);
        }

        // Date pickers
        sheetBinding.etSaleStart.setOnClickListener(v -> showDatePicker(sheetBinding.etSaleStart));
        sheetBinding.etSaleEnd.setOnClickListener(v -> showDatePicker(sheetBinding.etSaleEnd));

        // Save
        sheetBinding.btnSaveDiscount.setOnClickListener(v -> {
            String code        = getText(sheetBinding.etDiscountCode);
            String title       = getText(sheetBinding.etDiscountTitle);
            String valueStr    = getText(sheetBinding.etDiscountValue);
            String limitStr    = getText(sheetBinding.etUsageLimit);
            String minOrderStr = getText(sheetBinding.etMinOrderValue);
            String maxDiscStr  = getText(sheetBinding.etMaxDiscount);
            String saleStart   = getText(sheetBinding.etSaleStart);
            String saleEnd     = getText(sheetBinding.etSaleEnd);
            String typeStr     = sheetBinding.actvDiscountType.getText().toString();
            boolean isActive   = sheetBinding.switchIsActive.isChecked();

            if (TextUtils.isEmpty(code)) code = generateRandomCode();
            else code = code.toUpperCase();

            if (TextUtils.isEmpty(title)) {
                sheetBinding.etDiscountTitle.setError(getString(R.string.str_error_enter_name)); return;
            }
            if (TextUtils.isEmpty(valueStr)) {
                sheetBinding.etDiscountValue.setError(getString(R.string.str_error_enter_value)); return;
            }
            if (TextUtils.isEmpty(saleStart) || TextUtils.isEmpty(saleEnd)) {
                Toast.makeText(this, getString(R.string.str_toast_select_time), Toast.LENGTH_SHORT).show(); return;
            }

            long value;
            long limit = 0;
            long minOrder = 0;
            long maxDisc = 0;
            try {
                value = Long.parseLong(valueStr);
                if (!TextUtils.isEmpty(limitStr)) limit = Long.parseLong(limitStr);
                if (!TextUtils.isEmpty(minOrderStr)) minOrder = Long.parseLong(minOrderStr);
                if (!TextUtils.isEmpty(maxDiscStr)) maxDisc = Long.parseLong(maxDiscStr);
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.str_toast_invalid_value), Toast.LENGTH_SHORT).show(); return;
            }
            
            String discountType = typeStr.contains("%") ? "percentage" : "fixed";
            
            if ("percentage".equals(discountType) && value > 100) {
                Toast.makeText(this, getString(R.string.str_toast_percent_over_100), Toast.LENGTH_SHORT).show();
                return;
            }

            com.google.firebase.Timestamp tsStart = displayToTimestamp(saleStart, false);
            com.google.firebase.Timestamp tsEnd   = displayToTimestamp(saleEnd, true);
            if (tsStart == null || tsEnd == null) {
                Toast.makeText(this, getString(R.string.str_toast_invalid_date), Toast.LENGTH_SHORT).show();
                return;
            }

            if (tsEnd.compareTo(tsStart) < 0) {
                Toast.makeText(this, getString(R.string.str_toast_end_before_start), Toast.LENGTH_SHORT).show();
                return;
            }

            sheetBinding.btnSaveDiscount.setEnabled(false);
            sheetBinding.btnSaveDiscount.setText(getString(R.string.str_btn_saving));

            if (isEdit) {
                updateDiscount(existing, title, discountType, value, limit, minOrder, maxDisc, tsStart, tsEnd, isActive, dialog);
            } else {
                createDiscount(code, title, discountType, value, limit, minOrder, maxDisc, tsStart, tsEnd, isActive, dialog);
            }
        });

        dialog.show();
    }
    
    private void createDiscount(String code, String title, String type, long value, long limit,
                                long minOrder, long maxDisc,
                                com.google.firebase.Timestamp tsStart, com.google.firebase.Timestamp tsEnd, boolean isActive, BottomSheetDialog dialog) {
        Discount discount = new Discount();
        String id = UUID.randomUUID().toString();
        discount.setId(id);
        discount.setCode(code);
        discount.setTitle(title);
        discount.setType(type);
        discount.setValue(value);
        discount.setUsageLimit(limit);
        discount.setMinOrderValue(minOrder);
        discount.setMaxDiscount(maxDisc);
        discount.setStartDate(tsStart);
        discount.setExpiryDate(tsEnd);
        discount.setCreatorType("organizer");
        discount.setScope("event");
        discount.setEventId(eventId);
        discount.setActive(isActive);
        discount.setUsagePerUser(1); // Default value
        discount.setCreatedBy(sessionManager.getActiveOrganizerId());
        discount.setUsedCount(0);
        
        db.collection(FirebaseCollections.DISCOUNTS).document(discount.getId())
                .set(discount)
                .addOnSuccessListener(v -> {
                    dialog.dismiss();
                    Toast.makeText(this, getString(R.string.str_toast_create_success), Toast.LENGTH_SHORT).show();
                    loadDiscounts();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateDiscount(Discount existing, String title, String type, long value, long limit,
                                long minOrder, long maxDisc,
                                com.google.firebase.Timestamp tsStart, com.google.firebase.Timestamp tsEnd, boolean isActive, BottomSheetDialog dialog) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("type", type);
        updates.put("value", value);
        updates.put("usage_limit", limit);
        updates.put("min_order_value", minOrder);
        updates.put("max_discount", maxDisc);
        updates.put("start_date", tsStart);
        updates.put("expiry_date", tsEnd);
        updates.put("is_active", isActive);

        db.collection(FirebaseCollections.DISCOUNTS).document(existing.getId())
                .update(updates)
                .addOnSuccessListener(v -> {
                    dialog.dismiss();
                    Toast.makeText(this, getString(R.string.str_toast_update_success), Toast.LENGTH_SHORT).show();
                    loadDiscounts();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showDatePicker(TextInputEditText target) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        new DatePickerDialog(this,
                (datePicker, y, m, d) -> {
                    String formatted = String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y);
                    target.setText(formatted);
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH))
                .show();
    }

    private com.google.firebase.Timestamp displayToTimestamp(String displayDate) {
        return displayToTimestamp(displayDate, false);
    }

    private com.google.firebase.Timestamp displayToTimestamp(String displayDate, boolean endOfDay) {
        if (displayDate == null || displayDate.isEmpty()) return null;
        try {
            java.util.Date d = displayFmt.parse(displayDate);
            if (d == null) return null;
            if (endOfDay) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(d);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                cal.set(java.util.Calendar.MINUTE, 59);
                cal.set(java.util.Calendar.SECOND, 59);
                d = cal.getTime();
            }
            return new com.google.firebase.Timestamp(d);
        } catch (Exception e) {
            return null;
        }
    }

    private String timestampToDisplay(com.google.firebase.Timestamp ts) {
        if (ts == null) return "";
        try {
            return displayFmt.format(ts.toDate());
        } catch (Exception e) {
            return "";
        }
    }

    private String getText(TextInputEditText et) {
        Editable e = et.getText();
        return e == null ? "" : e.toString().trim();
    }
    
    private String generateRandomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void toggleDiscountActiveState(Discount discount) {
        boolean nextState = !discount.isActive();
        db.collection(FirebaseCollections.DISCOUNTS).document(discount.getId())
                .update("is_active", nextState)
                .addOnSuccessListener(v -> {
                    String msg = nextState ? "Đã kích hoạt mã" : "Đã tắt áp dụng mã";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    loadDiscounts();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
