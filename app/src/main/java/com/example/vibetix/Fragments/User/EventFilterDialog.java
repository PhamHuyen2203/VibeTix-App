package com.example.vibetix.Fragments.User;

import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.vibetix.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class EventFilterDialog extends BottomSheetDialogFragment {

    /** Tất cả tiêu chí lọc được trả về khi Apply */
    public static class FilterCriteria {
        public int     count;        // số điều kiện active
        public String  cityFilter;   // "" = toàn quốc | "Hồ Chí Minh" | "Hà Nội" | "Đà Lạt" | "Đà Nẵng"
        public boolean freeOnly;     // chỉ sự kiện miễn phí
        public long    minPrice;     // 0 nếu không set
        public long    maxPrice;     // Long.MAX_VALUE nếu không set
        public boolean[] categories; // [music, arts, workshop, tour, sports, festival]
    }

    public interface OnFilterApplied {
        void onApplied(FilterCriteria criteria);
    }

    private static final float PRICE_MIN = 0f;
    private static final float PRICE_MAX = 5_000_000f;

    private OnFilterApplied listener;
    private RadioGroup rgLocation;
    private SwitchMaterial switchFreeOnly;
    private LinearLayout layoutPriceRange;
    private RangeSlider priceRangeSlider;
    private TextView txtMinPrice, txtMaxPrice;
    private TextView btnClose, btnReset, btnApply;

    private float currentMin = PRICE_MIN;
    private float currentMax = PRICE_MAX;

    private final int[] catChipIds = {
        R.id.chipCatMusic, R.id.chipCatArts, R.id.chipCatWorkshop,
        R.id.chipCatTour,  R.id.chipCatSports, R.id.chipCatFestival
    };
    private final boolean[] catSelected = new boolean[6];
    private final TextView[] catChipViews = new TextView[6];

    public void setOnFilterApplied(OnFilterApplied listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_event_filter, container, false);

        rgLocation       = view.findViewById(R.id.rgLocation);
        switchFreeOnly   = view.findViewById(R.id.switchFreeOnly);
        layoutPriceRange = view.findViewById(R.id.layoutPriceRange);
        priceRangeSlider = view.findViewById(R.id.priceRangeSlider);
        txtMinPrice      = view.findViewById(R.id.txtMinPrice);
        txtMaxPrice      = view.findViewById(R.id.txtMaxPrice);
        btnClose         = view.findViewById(R.id.btnFilterClose);
        btnReset         = view.findViewById(R.id.btnFilterReset);
        btnApply         = view.findViewById(R.id.btnFilterApply);

        // ── Setup RangeSlider ──────────────────────────────────────────────
        if (priceRangeSlider != null) {
            priceRangeSlider.setValueFrom(PRICE_MIN);
            priceRangeSlider.setValueTo(PRICE_MAX);
            priceRangeSlider.setValues(PRICE_MIN, PRICE_MAX);

            priceRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
                List<Float> vals = slider.getValues();
                currentMin = vals.get(0);
                currentMax = vals.get(1);
                updatePriceLabels();
            });

            // Single tap on a thumb → open text input dialog
            priceRangeSlider.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
                private List<Float> valuesAtTouchDown;

                @Override
                public void onStartTrackingTouch(@NonNull RangeSlider slider) {
                    valuesAtTouchDown = slider.getValues();
                }

                @Override
                public void onStopTrackingTouch(@NonNull RangeSlider slider) {
                    // If thumb didn't move (single tap), open input dialog
                    List<Float> current = slider.getValues();
                    if (valuesAtTouchDown != null
                            && current.get(0).equals(valuesAtTouchDown.get(0))
                            && current.get(1).equals(valuesAtTouchDown.get(1))) {
                        // Determine which thumb was tapped by proximity to active index
                        // — open min dialog; user can still tap max label directly
                    }
                }
            });
        }

        // Tap min label → enter min price
        if (txtMinPrice != null) {
            txtMinPrice.setOnClickListener(v -> showPriceInputDialog(true));
        }
        // Tap max label → enter max price
        if (txtMaxPrice != null) {
            txtMaxPrice.setOnClickListener(v -> showPriceInputDialog(false));
        }

        updatePriceLabels();

        // ── Free-only switch hides/shows slider ───────────────────────────
        if (switchFreeOnly != null) {
            switchFreeOnly.setOnCheckedChangeListener((btn, checked) -> {
                if (layoutPriceRange != null) {
                    layoutPriceRange.setVisibility(checked ? View.GONE : View.VISIBLE);
                }
            });
        }

        // ── Category chips ────────────────────────────────────────────────
        for (int i = 0; i < catChipIds.length; i++) {
            TextView chip = view.findViewById(catChipIds[i]);
            catChipViews[i] = chip;
            final int idx = i;
            if (chip != null) {
                chip.setOnClickListener(v -> {
                    catSelected[idx] = !catSelected[idx];
                    updateCatChip(chip, catSelected[idx]);
                });
            }
        }

        // ── Close ─────────────────────────────────────────────────────────
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        // ── Reset ─────────────────────────────────────────────────────────
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                if (rgLocation != null) rgLocation.check(R.id.rbNational);
                if (switchFreeOnly != null) switchFreeOnly.setChecked(false);
                currentMin = PRICE_MIN;
                currentMax = PRICE_MAX;
                if (priceRangeSlider != null) priceRangeSlider.setValues(PRICE_MIN, PRICE_MAX);
                if (layoutPriceRange != null) layoutPriceRange.setVisibility(View.VISIBLE);
                updatePriceLabels();
                for (int i = 0; i < catSelected.length; i++) catSelected[i] = false;
                for (int i = 0; i < catChipViews.length; i++) {
                    if (catChipViews[i] != null) updateCatChip(catChipViews[i], false);
                }
            });
        }

        // ── Apply — build FilterCriteria và trả về ────────────────────────
        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                if (listener != null) {
                    FilterCriteria c = new FilterCriteria();
                    // Địa điểm
                    c.cityFilter = "";
                    if (rgLocation != null) {
                        int loc = rgLocation.getCheckedRadioButtonId();
                        if (loc == R.id.rbHCMC)   c.cityFilter = "Hồ Chí Minh";
                        else if (loc == R.id.rbHanoi)  c.cityFilter = "Hà Nội";
                        else if (loc == R.id.rbDaLat)  c.cityFilter = "Đà Lạt";
                        else if (loc == R.id.rbDaNang) c.cityFilter = "Đà Nẵng";
                    }
                    // Giá
                    c.freeOnly = switchFreeOnly != null && switchFreeOnly.isChecked();
                    c.minPrice = c.freeOnly ? 0 : (long) currentMin;
                    c.maxPrice = c.freeOnly ? 0 : (currentMax >= PRICE_MAX ? Long.MAX_VALUE : (long) currentMax);
                    // Thể loại trong dialog (index: 0=music,1=arts,2=workshop,3=tour,4=sports,5=festival)
                    c.categories = catSelected.clone();
                    // Đếm số filter active
                    if (!c.cityFilter.isEmpty()) c.count++;
                    if (c.freeOnly) c.count++;
                    if (c.minPrice > 0 || c.maxPrice < Long.MAX_VALUE) c.count++;
                    for (boolean b : c.categories) if (b) c.count++;
                    listener.onApplied(c);
                }
                dismiss();
            });
        }

        return view;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updatePriceLabels() {
        if (txtMinPrice != null) {
            txtMinPrice.setText(formatPrice(currentMin, false));
        }
        if (txtMaxPrice != null) {
            txtMaxPrice.setText(formatPrice(currentMax, currentMax >= PRICE_MAX));
        }
    }

    private String formatPrice(float value, boolean showPlus) {
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return fmt.format((long) value) + (showPlus ? "+đ" : "đ");
    }

    private void showPriceInputDialog(boolean isMin) {
        if (getContext() == null) return;

        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(isMin ? "Giá tối thiểu (VND)" : "Giá tối đa (VND)");
        input.setText(String.valueOf((long)(isMin ? currentMin : currentMax)));
        input.selectAll();

        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(isMin ? "Nhập giá tối thiểu" : "Nhập giá tối đa")
                .setView(input)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        try {
                            float entered = Float.parseFloat(text);
                            entered = Math.max(PRICE_MIN, Math.min(entered, PRICE_MAX));
                            if (isMin) {
                                currentMin = Math.min(entered, currentMax);
                            } else {
                                currentMax = Math.max(entered, currentMin);
                            }
                            if (priceRangeSlider != null) {
                                priceRangeSlider.setValues(currentMin, currentMax);
                            }
                            updatePriceLabels();
                        } catch (NumberFormatException ignored) {}
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateCatChip(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_quick_chip_active);
            chip.setTextColor(requireContext().getColor(R.color.clr_primary_blue));
        } else {
            chip.setBackgroundResource(R.drawable.bg_quick_chip);
            chip.setTextColor(requireContext().getColor(R.color.clr_text_black));
        }
    }
}
