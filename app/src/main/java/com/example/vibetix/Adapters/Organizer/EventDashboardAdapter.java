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

public class EventDashboardAdapter extends RecyclerView.Adapter<EventDashboardAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
        void onEventOptionClick(Event event, View anchor);
    }

    public EventDashboardAdapter(List<Event> eventList, OnEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_organizer_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.tvTitle.setText(event.getTitle());
        holder.tvDate.setText(event.getStartTime() != null ? event.getStartTime() : "Sắp diễn ra");

        // Binding Status Badge
        if (event.getStatus() != null) {
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(event.getStatus().name());
            
            // Tạm thời set màu nền cho draft/pending/approved
            switch (event.getStatus()) {
                case DRAFT:
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_draft);
                    holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.clr_text_black));
                    break;
                case APPROVED:
                case ONGOING:
                case COMPLETED:
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_active);
                    holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.clr_text_white));
                    break;
                case PENDING:
                case CANCELLED:
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_draft);
                    holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.clr_error));
                    break;
            }
        } else {
            holder.tvStatusBadge.setVisibility(View.GONE);
        }
        
        holder.tvStatTickets.setText("0/0");
        holder.tvStatRevenue.setText("0đ");

        // Sử dụng Glide để load ảnh poster
        Glide.with(holder.itemView.getContext())
                .load(event.getPosterUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.ivPoster);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });

        if (holder.btnEventOptions != null) {
            holder.btnEventOptions.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventOptionClick(event, v);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return eventList != null ? eventList.size() : 0;
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster, btnEventOptions;
        TextView tvTitle, tvDate, tvStatusBadge, tvStatTickets, tvStatRevenue;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivEventPoster);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvStatTickets = itemView.findViewById(R.id.tvStatTickets);
            tvStatRevenue = itemView.findViewById(R.id.tvStatRevenue);
            btnEventOptions = itemView.findViewById(R.id.btnEventOptions);
        }
    }
}
