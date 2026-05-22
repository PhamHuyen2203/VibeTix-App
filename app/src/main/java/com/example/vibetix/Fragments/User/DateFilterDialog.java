package com.example.vibetix.Fragments.User;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.vibetix.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateFilterDialog extends BottomSheetDialogFragment {

    public interface OnDateFilterApplied {
        void onApplied(String label, Calendar startDate, Calendar endDate);
    }

    private OnDateFilterApplied listener;
    private Calendar displayMonth;
    private Calendar selectedStart = null;
    private Calendar selectedEnd   = null;
    // selectedQuick: 0=All, 1=Today, 2=Tomorrow, 3=Weekend, 4=Month, -1=custom range
    private int selectedQuick = 0;

    private TextView txtMonthYear;
    private GridLayout calDayGrid;
    private TextView[] quickChips;
    private TextView btnClose, btnReset, btnApply;
    private View btnPrevMonth, btnNextMonth;

    public void setOnDateFilterApplied(OnDateFilterApplied listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_date_filter, container, false);

        displayMonth = Calendar.getInstance();
        displayMonth.set(Calendar.DAY_OF_MONTH, 1);

        txtMonthYear  = view.findViewById(R.id.txtMonthYear);
        calDayGrid    = view.findViewById(R.id.calDayGrid);
        btnPrevMonth  = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth  = view.findViewById(R.id.btnNextMonth);
        btnClose      = view.findViewById(R.id.btnDateClose);
        btnReset      = view.findViewById(R.id.btnDateReset);
        btnApply      = view.findViewById(R.id.btnDateApply);

        quickChips = new TextView[]{
            view.findViewById(R.id.chipAll),
            view.findViewById(R.id.chipToday),
            view.findViewById(R.id.chipTomorrow),
            view.findViewById(R.id.chipWeekend),
            view.findViewById(R.id.chipMonth)
        };

        renderMonth();
        updateQuickChips();

        // Quick chip clicks — auto-populate calendar selection
        for (int i = 0; i < quickChips.length; i++) {
            final int idx = i;
            if (quickChips[i] != null) {
                quickChips[i].setOnClickListener(v -> {
                    selectedQuick = idx;
                    applyQuickSelection(idx);
                    updateQuickChips();
                    renderMonth();
                });
            }
        }

        btnPrevMonth.setOnClickListener(v -> {
            displayMonth.add(Calendar.MONTH, -1);
            renderMonth();
        });

        btnNextMonth.setOnClickListener(v -> {
            displayMonth.add(Calendar.MONTH, 1);
            renderMonth();
        });

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                selectedStart = null;
                selectedEnd   = null;
                selectedQuick = 0;
                // Reset display to current month
                displayMonth = Calendar.getInstance();
                displayMonth.set(Calendar.DAY_OF_MONTH, 1);
                updateQuickChips();
                renderMonth();
            });
        }

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                if (listener != null) {
                    String label = buildLabel();
                    listener.onApplied(label, selectedStart, selectedEnd);
                }
                dismiss();
            });
        }

        return view;
    }

    // ── Quick selection logic ─────────────────────────────────────────────────

    private void applyQuickSelection(int idx) {
        Calendar today = Calendar.getInstance();
        clearTime(today);

        switch (idx) {
            case 0: // Tất cả các ngày
                selectedStart = null;
                selectedEnd   = null;
                break;

            case 1: // Hôm nay
                selectedStart = (Calendar) today.clone();
                selectedEnd   = (Calendar) today.clone();
                jumpDisplayToMonth(selectedStart);
                break;

            case 2: // Ngày mai
                Calendar tomorrow = (Calendar) today.clone();
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                selectedStart = tomorrow;
                selectedEnd   = (Calendar) tomorrow.clone();
                jumpDisplayToMonth(selectedStart);
                break;

            case 3: // Cuối tuần (thứ Bảy & Chủ Nhật của tuần hiện tại hoặc tuần tới)
                Calendar saturday = (Calendar) today.clone();
                // Advance to the nearest upcoming Saturday (inclusive of today if today is Sat)
                while (saturday.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                    saturday.add(Calendar.DAY_OF_MONTH, 1);
                }
                Calendar sunday = (Calendar) saturday.clone();
                sunday.add(Calendar.DAY_OF_MONTH, 1);
                selectedStart = saturday;
                selectedEnd   = sunday;
                jumpDisplayToMonth(selectedStart);
                break;

            case 4: // Tháng này — từ hôm nay đến cuối tháng
                Calendar endOfMonth = (Calendar) today.clone();
                endOfMonth.set(Calendar.DAY_OF_MONTH,
                        endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
                selectedStart = (Calendar) today.clone();
                selectedEnd   = endOfMonth;
                jumpDisplayToMonth(selectedStart);
                break;

            default:
                selectedStart = null;
                selectedEnd   = null;
                break;
        }
    }

    /** Move displayMonth to the month of the given calendar so selection is visible. */
    private void jumpDisplayToMonth(Calendar cal) {
        displayMonth = (Calendar) cal.clone();
        displayMonth.set(Calendar.DAY_OF_MONTH, 1);
        clearTime(displayMonth);
    }

    /** Zero out time fields so Calendar comparisons are date-only. */
    private void clearTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    // ── Label building ────────────────────────────────────────────────────────

    private String buildLabel() {
        switch (selectedQuick) {
            case 1: return "Hôm nay";
            case 2: return "Ngày mai";
            case 3: return "Cuối tuần";
            case 4: return "Tháng này";
        }
        if (selectedStart != null && selectedEnd != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
            return sdf.format(selectedStart.getTime()) + " – " + sdf.format(selectedEnd.getTime());
        }
        if (selectedStart != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(selectedStart.getTime());
        }
        return "Tất cả các ngày";
    }

    // ── Quick chip appearance ─────────────────────────────────────────────────

    private void updateQuickChips() {
        if (quickChips == null) return;
        for (int i = 0; i < quickChips.length; i++) {
            TextView chip = quickChips[i];
            if (chip == null) continue;
            // chip i is "active" when it matches selectedQuick AND not overridden by custom range
            boolean isActive = (i == selectedQuick) && (selectedQuick != -1);
            if (isActive) {
                chip.setBackgroundResource(R.drawable.bg_quick_chip_active);
                chip.setTextColor(requireContext().getColor(R.color.clr_primary_blue));
            } else {
                chip.setBackgroundResource(R.drawable.bg_quick_chip);
                chip.setTextColor(requireContext().getColor(R.color.clr_text_black));
            }
        }
    }

    // ── Month rendering ───────────────────────────────────────────────────────

    private void renderMonth() {
        if (calDayGrid == null || txtMonthYear == null) return;
        calDayGrid.removeAllViews();

        // Month title in Vietnamese
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));
        String monthStr = sdf.format(displayMonth.getTime());
        if (!monthStr.isEmpty()) {
            monthStr = Character.toUpperCase(monthStr.charAt(0)) + monthStr.substring(1);
        }
        txtMonthYear.setText(monthStr);

        Calendar cal = (Calendar) displayMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        // Offset: Monday=0 … Sunday=6
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int offset = (firstDayOfWeek == Calendar.SUNDAY) ? 6 : firstDayOfWeek - Calendar.MONDAY;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int totalCells  = offset + daysInMonth;
        int rows        = (int) Math.ceil(totalCells / 7.0);

        Calendar today = Calendar.getInstance();
        clearTime(today);

        float density  = requireContext().getResources().getDisplayMetrics().density;
        int cellHeight = (int) (42 * density);
        int cellMargin = (int) (2  * density);

        for (int i = 0; i < rows * 7; i++) {
            int dayNum = i - offset + 1;

            TextView dayTv = new TextView(requireContext());
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.columnSpec = GridLayout.spec(i % 7, 1, GridLayout.FILL, 1f);
            lp.rowSpec    = GridLayout.spec(i / 7, 1, GridLayout.FILL, 1f);
            lp.width      = 0;
            lp.height     = cellHeight;
            lp.setMargins(cellMargin, cellMargin, cellMargin, cellMargin);
            dayTv.setLayoutParams(lp);
            dayTv.setGravity(Gravity.CENTER);
            dayTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);

            if (dayNum < 1 || dayNum > daysInMonth) {
                dayTv.setText("");
                calDayGrid.addView(dayTv);
                continue;
            }

            dayTv.setText(String.valueOf(dayNum));

            Calendar dayCal = (Calendar) cal.clone();
            dayCal.set(Calendar.DAY_OF_MONTH, dayNum);
            clearTime(dayCal);

            // ── Past date: grey + not clickable ──────────────────────────
            boolean isPast = dayCal.before(today);
            if (isPast) {
                dayTv.setTextColor(0xFFCCCCCC);
                calDayGrid.addView(dayTv);
                continue;
            }

            boolean isToday = isSameDay(dayCal, today);
            boolean isStart = (selectedStart != null) && isSameDay(dayCal, selectedStart);
            boolean isEnd   = (selectedEnd   != null) && isSameDay(dayCal, selectedEnd);
            boolean inRange = isInRange(dayCal);

            if (isStart || isEnd) {
                dayTv.setBackgroundResource(R.drawable.bg_calendar_selected);
                dayTv.setTextColor(requireContext().getColor(R.color.clr_text_white));
                dayTv.setTypeface(null, Typeface.BOLD);
            } else if (inRange) {
                dayTv.setBackgroundResource(R.drawable.bg_calendar_range);
                dayTv.setTextColor(requireContext().getColor(R.color.clr_text_black));
            } else if (isToday) {
                dayTv.setTextColor(requireContext().getColor(R.color.clr_primary_blue));
                dayTv.setTypeface(null, Typeface.BOLD);
            } else {
                dayTv.setTextColor(requireContext().getColor(R.color.clr_text_black));
            }

            final Calendar clickedDay = (Calendar) dayCal.clone();
            dayTv.setOnClickListener(v -> onDayClicked(clickedDay));
            dayTv.setClickable(true);
            dayTv.setFocusable(true);

            calDayGrid.addView(dayTv);
        }
    }

    // ── Day click ─────────────────────────────────────────────────────────────

    private void onDayClicked(Calendar day) {
        // Deselect any quick option
        selectedQuick = -1;

        if (selectedStart == null || selectedEnd != null) {
            // Start a new range
            selectedStart = day;
            selectedEnd   = null;
        } else {
            // Set end of range
            if (day.before(selectedStart)) {
                selectedEnd   = selectedStart;
                selectedStart = day;
            } else {
                selectedEnd = day;
            }
        }

        updateQuickChips();
        renderMonth();
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR)         == b.get(Calendar.YEAR)
            && a.get(Calendar.MONTH)        == b.get(Calendar.MONTH)
            && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH);
    }

    private boolean isInRange(Calendar day) {
        if (selectedStart == null || selectedEnd == null) return false;
        return day.after(selectedStart) && day.before(selectedEnd);
    }
}
