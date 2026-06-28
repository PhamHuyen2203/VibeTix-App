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

        // Date
        String dateStr = event.getStartTime();
        holder.tvDate.setText(dateStr != null ? "📅 " + dateStr : holder.itemView.getContext().getString(R.string.txt_coming_soon));

        // Venue
        if (holder.tvVenue != null) {
            String city = event.getVenueCity();
            String venue = event.getVenueName();
            if (city != null && !city.isEmpty()) {
                holder.tvVenue.setText("📍 " + (venue != null && !venue.isEmpty() ? venue + ", " + city : city));
                holder.tvVenue.setVisibility(View.VISIBLE);
            } else if (venue != null && !venue.isEmpty()) {
                holder.tvVenue.setText("📍 " + venue);
                holder.tvVenue.setVisibility(View.VISIBLE);
            } else {
                holder.tvVenue.setVisibility(View.GONE);
            }
        }

        // Binding Role Badge — màu theo role
        if (event.getUserRole() != null && !event.getUserRole().isEmpty()) {
            holder.tvRoleBadge.setVisibility(View.VISIBLE);
            String roleStr = event.getUserRole().toUpperCase();
            switch (roleStr) {
                case "OWNER":
                    holder.tvRoleBadge.setText(holder.itemView.getContext().getString(R.string.role_owner));
                    holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_role_owner);
                    holder.tvRoleBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_text_white));
                    break;
                case "MANAGER":
                    holder.tvRoleBadge.setText(holder.itemView.getContext().getString(R.string.role_manager));
                    holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_role_manager);
                    holder.tvRoleBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_text_white));
                    break;
                default:
                    holder.tvRoleBadge.setText(holder.itemView.getContext().getString(R.string.role_staff));
                    holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_role_staff);
                    holder.tvRoleBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_text_black));
                    
                    // Hide stats for staff
                    if (holder.layoutStats != null) {
                        holder.layoutStats.setVisibility(View.GONE);
                    }
                    break;
            }
            
            // Explicitly show stats for Owner/Manager if previously hidden by view recycling
            if (roleStr.equals("OWNER") || roleStr.equals("MANAGER")) {
                if (holder.layoutStats != null) {
                    holder.layoutStats.setVisibility(View.VISIBLE);
                }
            }
        } else {
            holder.tvRoleBadge.setVisibility(View.GONE);
        }

        // Binding Status Badge — đúng màu theo spec
        if (event.getStatusStr() != null) {
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            String s = event.getStatusStr().toLowerCase();
            switch (s) {
                case "approved":
                case "ongoing":
                    holder.tvStatusBadge.setText(String.format(holder.itemView.getContext().getString(R.string.status_dot_prefix), s.equals("approved") ? holder.itemView.getContext().getString(R.string.status_approved_caps) : holder.itemView.getContext().getString(R.string.status_ongoing_caps)));
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_active); // xanh
                    holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_text_white));
                    break;
                case "pending":
                    holder.tvStatusBadge.setText(String.format(holder.itemView.getContext().getString(R.string.status_dot_prefix), holder.itemView.getContext().getString(R.string.status_pending_caps)));
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_draft);
                    holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_warning));
                    break;
                case "cancelled":
                    holder.tvStatusBadge.setText(String.format(holder.itemView.getContext().getString(R.string.status_dot_prefix), holder.itemView.getContext().getString(R.string.status_cancelled_caps)));
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_draft);
                    holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_error));
                    break;
                case "completed":
                    holder.tvStatusBadge.setText(String.format(holder.itemView.getContext().getString(R.string.status_dot_prefix), holder.itemView.getContext().getString(R.string.status_completed_caps)));
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_active);
                    holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_text_white));
                    break;
                default: // draft
                    holder.tvStatusBadge.setText(String.format(holder.itemView.getContext().getString(R.string.status_dot_prefix), holder.itemView.getContext().getString(R.string.status_draft_caps)));
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_draft);
                    holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_grey_1));
                    break;
            }
        } else if (event.getStatus() != null) {
            // fallback: dùng enum
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            switch (event.getStatusEnum()) {
                case APPROVED: case ONGOING:
                    holder.tvStatusBadge.setText(String.format(holder.itemView.getContext().getString(R.string.status_dot_prefix), event.getStatusEnum() == Event.Status.APPROVED ? holder.itemView.getContext().getString(R.string.status_approved_caps) : holder.itemView.getContext().getString(R.string.status_ongoing_caps)));
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_active);
                    holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_text_white));
                    break;
                case PENDING:
                    holder.tvStatusBadge.setText(String.format(holder.itemView.getContext().getString(R.string.status_dot_prefix), holder.itemView.getContext().getString(R.string.status_pending_caps)));
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_draft);
                    holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_warning));
                    break;
                case CANCELLED:
                    holder.tvStatusBadge.setText(String.format(holder.itemView.getContext().getString(R.string.status_dot_prefix), holder.itemView.getContext().getString(R.string.status_cancelled_caps)));
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_draft);
                    holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_error));
                    break;
                default:
                    holder.tvStatusBadge.setText(String.format(holder.itemView.getContext().getString(R.string.status_dot_prefix), holder.itemView.getContext().getString(R.string.status_draft_caps)));
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_draft);
                    holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_grey_1));
                    break;
            }
        } else {
            holder.tvStatusBadge.setVisibility(View.GONE);
        }

        holder.tvStatTickets.setText("...");
        holder.tvStatRevenue.setText("...");

        String finalRoleStr = (event.getUserRole() != null) ? event.getUserRole().toUpperCase() : "";
        boolean isDraftOrPending = false;
        if (event.getStatusStr() != null) {
            String s = event.getStatusStr().toLowerCase();
            isDraftOrPending = s.equals("draft") || s.equals("pending");
        } else if (event.getStatusEnum() != null) {
            isDraftOrPending = (event.getStatusEnum() == Event.Status.DRAFT || event.getStatusEnum() == Event.Status.PENDING);
        }

        // Hide stats if draft or pending
        if (isDraftOrPending) {
            if (holder.layoutStats != null) holder.layoutStats.setVisibility(View.GONE);
        } else if (finalRoleStr.equals("OWNER") || finalRoleStr.equals("MANAGER")) {
            if (holder.layoutStats != null) holder.layoutStats.setVisibility(View.VISIBLE);
            com.example.vibetix.Firebase.FirestoreHelper.calculateEventStats(event.getId(), (totalTickets, totalRevenue) -> {
                java.text.NumberFormat vndFmt = java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));
                holder.tvStatTickets.setText(String.valueOf(totalTickets));
                holder.tvStatRevenue.setText(holder.itemView.getContext().getString(R.string.dash_price_vnd, vndFmt.format((long) totalRevenue)));
            });
        }
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
        TextView tvTitle, tvDate, tvVenue, tvRoleBadge, tvStatusBadge, tvStatTickets, tvStatRevenue;
        View layoutStats;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster       = itemView.findViewById(R.id.ivEventPoster);
            tvTitle        = itemView.findViewById(R.id.tvEventTitle);
            tvDate         = itemView.findViewById(R.id.tvEventDate);
            tvVenue        = itemView.findViewById(R.id.tvEventVenue);
            tvRoleBadge    = itemView.findViewById(R.id.tvRoleBadge);
            tvStatusBadge  = itemView.findViewById(R.id.tvStatusBadge);
            tvStatTickets  = itemView.findViewById(R.id.tvStatTickets);
            tvStatRevenue  = itemView.findViewById(R.id.tvStatRevenue);
            btnEventOptions = itemView.findViewById(R.id.btnEventOptions);
            layoutStats    = itemView.findViewById(R.id.layoutStats);
        }
    }
}
