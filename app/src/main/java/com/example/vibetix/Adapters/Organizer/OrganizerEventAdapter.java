package com.example.vibetix.Adapters.Organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;

import java.util.List;

/**
 * OrganizerEventAdapter — Hiển thị danh sách events của organizer.
 * Dùng trong OrganizerHubFragment (recent events) và OrganizerEventsFragment.
 */
public class OrganizerEventAdapter extends RecyclerView.Adapter<OrganizerEventAdapter.ViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private final List<Event> events;
    private final OnEventClickListener listener;

    public OrganizerEventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_dashboard, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvTitle.setText(event.getTitle() != null ? event.getTitle() : "Untitled");
        holder.tvDate.setText(formatDate(event.getStartTime()));
        holder.tvStatus.setText(formatStatus(event.getStatusStr()));
        holder.tvStatus.setBackgroundResource(getStatusBg(event.getStatusStr()));

        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(event.getPosterUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_cat_music)
                    .into(holder.ivPoster);
        } else {
            holder.ivPoster.setImageResource(R.drawable.ic_cat_music);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(event);
        });
    }

    private String formatDate(String startTime) {
        if (startTime == null || startTime.length() < 10) return "—";
        try {
            // "2024-12-25T18:00:00" → "25/12/2024"
            String[] parts = startTime.substring(0, 10).split("-");
            return parts[2] + "/" + parts[1] + "/" + parts[0];
        } catch (Exception e) {
            return startTime;
        }
    }

    private String formatStatus(String status) {
        if (status == null) return "DRAFT";
        switch (status.toLowerCase()) {
            case "draft":     return "Nháp";
            case "pending":   return "Chờ duyệt";
            case "approved":  return "Đã duyệt";
            case "ongoing":   return "Đang diễn ra";
            case "completed": return "Đã kết thúc";
            case "cancelled": return "Đã huỷ";
            default:          return status.toUpperCase();
        }
    }

    private int getStatusBg(String status) {
        if (status == null) return R.drawable.bg_status_pending;
        switch (status.toLowerCase()) {
            case "approved":
            case "ongoing":   return R.drawable.bg_status_badge_success;
            case "cancelled": return R.drawable.bg_result_error;
            default:          return R.drawable.bg_status_pending;
        }
    }

    @Override
    public int getItemCount() { return events.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvTitle, tvDate, tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            ivPoster  = itemView.findViewById(R.id.ivEventPoster);
            tvTitle   = itemView.findViewById(R.id.tvEventTitle);
            tvDate    = itemView.findViewById(R.id.tvEventTime);
            tvStatus  = itemView.findViewById(R.id.tvEventStatus);
        }
    }
}
