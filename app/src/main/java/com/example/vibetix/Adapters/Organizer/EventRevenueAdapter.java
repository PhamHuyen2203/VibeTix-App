package com.example.vibetix.Adapters.Organizer;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.R;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

public class EventRevenueAdapter extends RecyclerView.Adapter<EventRevenueAdapter.VH> {

    public interface OnEventClickListener {
        void onEventClick(EventRevenueSummary summary);
    }

    public static class EventRevenueSummary {
        public String eventId;
        public String title;
        public String posterUrl;
        public String date;
        public String status;
        public long ticketsSold;
        public long revenue;
        public long totalOrders;

        public EventRevenueSummary(String eventId, String title, String posterUrl,
                                   String date, String status) {
            this.eventId = eventId;
            this.title = title;
            this.posterUrl = posterUrl;
            this.date = date;
            this.status = status;
        }
    }

    private final List<EventRevenueSummary> items;
    private final OnEventClickListener listener;

    public EventRevenueAdapter(List<EventRevenueSummary> items, OnEventClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_revenue, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        EventRevenueSummary s = items.get(position);
        h.tvTitle.setText(s.title != null ? s.title : "—");
        h.tvDate.setText(s.date != null ? s.date : "—");
        h.tvTicketsSold.setText(formatCompact(s.ticketsSold));
        h.tvRevenue.setText(formatRevenue(s.revenue));
        h.tvTotalOrders.setText(String.valueOf(s.totalOrders));

        // Status badge — solid colored pill
        h.tvStatus.setText(statusLabel(s.status));
        applyStatusBadge(h.tvStatus, s.status);

        // Poster
        Context ctx = h.ivPoster.getContext();
        if (s.posterUrl != null && !s.posterUrl.isEmpty()) {
            Glide.with(ctx).load(s.posterUrl)
                    .centerCrop()
                    .placeholder(R.color.clr_bg_section)
                    .into(h.ivPoster);
        } else {
            h.ivPoster.setBackgroundColor(ctx.getColor(R.color.clr_bg_section));
            h.ivPoster.setImageResource(0);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(s);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void updateData(List<EventRevenueSummary> newItems) {
        if (items != newItems) {
            items.clear();
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    private void applyStatusBadge(TextView tv, String status) {
        Context ctx = tv.getContext();
        int bgColor, textColor = 0xFFFFFFFF;
        if (status == null) {
            bgColor = 0xFF9E9E9E;
        } else {
            switch (status.toLowerCase()) {
                case "ongoing":   bgColor = 0xFF27AE60; break; // green
                case "completed": bgColor = 0xFF607D8B; break; // blue-grey
                case "cancelled": bgColor = 0xFFEB5757; break; // red
                case "pending":   bgColor = 0xFFE6A817; break; // amber
                case "draft":     bgColor = 0xFF9E9E9E; break; // grey
                default:          bgColor = 0xFF2563EB; break; // blue (approved)
            }
        }
        GradientDrawable pill = new GradientDrawable();
        pill.setShape(GradientDrawable.RECTANGLE);
        pill.setCornerRadius(100f);
        pill.setColor(bgColor);
        tv.setBackground(pill);
        tv.setTextColor(textColor);
        int h = (int) (tv.getResources().getDisplayMetrics().density * 4);
        int v = (int) (tv.getResources().getDisplayMetrics().density * 18);
        tv.setPadding(v, h, v, h);
    }

    private String statusLabel(String status) {
        if (status == null) return "—";
        switch (status.toLowerCase()) {
            case "approved": return "Đã duyệt";
            case "ongoing": return "Đang diễn ra";
            case "completed": return "Đã kết thúc";
            case "cancelled": return "Đã hủy";
            case "pending": return "Chờ duyệt";
            case "draft": return "Nháp";
            default: return status;
        }
    }

    private String formatCompact(long number) {
        if (number >= 1_000_000) return String.format(Locale.getDefault(), "%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format(Locale.getDefault(), "%.1fK", number / 1_000.0);
        return String.valueOf(number);
    }

    private String formatRevenue(long amount) {
        if (amount == 0) return "0đ";
        if (amount >= 1_000_000_000) return String.format(Locale.getDefault(), "%.1fB₫", amount / 1_000_000_000.0);
        if (amount >= 1_000_000) return String.format(Locale.getDefault(), "%.1fM₫", amount / 1_000_000.0);
        if (amount >= 1_000) return String.format(Locale.getDefault(), "%.0fK₫", amount / 1_000.0);
        return new DecimalFormat("#,###").format(amount) + "đ";
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvTitle, tvDate, tvStatus, tvTicketsSold, tvRevenue, tvTotalOrders;

        VH(@NonNull View v) {
            super(v);
            ivPoster = v.findViewById(R.id.ivEventPoster);
            tvTitle = v.findViewById(R.id.tvEventTitle);
            tvDate = v.findViewById(R.id.tvEventDate);
            tvStatus = v.findViewById(R.id.tvEventStatus);
            tvTicketsSold = v.findViewById(R.id.tvTicketsSold);
            tvRevenue = v.findViewById(R.id.tvRevenue);
            tvTotalOrders = v.findViewById(R.id.tvTotalOrders);
        }
    }
}
