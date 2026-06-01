package com.example.vibetix.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnEventActionListener listener;

    private int itemLayoutId = R.layout.item_event_admin;

    public interface OnEventActionListener {
        default void onFeaturedChanged(String eventId, boolean isFeatured) {}
        void onEventClick(Event event);
    }

    public EventAdapter(android.content.Context context, List<Event> eventList, OnEventActionListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    public EventAdapter(android.content.Context context, List<Event> eventList, OnEventActionListener listener, int itemLayoutId) {
        this.eventList = eventList;
        this.listener = listener;
        this.itemLayoutId = itemLayoutId;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(itemLayoutId, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return eventList != null ? eventList.size() : 0;
    }

    public void updateList(List<Event> newList) {
        this.eventList = newList;
        notifyDataSetChanged();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivPoster;
        private TextView tvTitle, tvTime, tvStatus;
        private SwitchCompat switchFeatured;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_event_poster);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvTime = itemView.findViewById(R.id.tv_event_time);
            tvStatus = itemView.findViewById(R.id.tv_event_status);
            switchFeatured = itemView.findViewById(R.id.switch_featured);
        }

        public void bind(final Event event, final OnEventActionListener listener) {
            if (tvTitle != null) tvTitle.setText(event.getTitle());
            if (tvTime != null) tvTime.setText(event.getStartTime());
            if (tvStatus != null) {
                if (event.getStatus() != null) {
                    tvStatus.setText(event.getStatus().name());
                } else {
                    tvStatus.setText(event.getStatusStr());
                }
            }

            // Load image with Glide
            if (ivPoster != null) {
                Glide.with(itemView.getContext())
                        .load(event.getPosterUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(ivPoster);
            }

            // Set switch state without triggering listener
            if (switchFeatured != null) {
                switchFeatured.setOnCheckedChangeListener(null);
                switchFeatured.setChecked(event.isFeatured());

                // Handle switch toggle
                switchFeatured.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (listener != null) {
                        listener.onFeaturedChanged(event.getId(), isChecked);
                    }
                });
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onEventClick(event);
            });
        }
    }
}
