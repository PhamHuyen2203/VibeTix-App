package com.example.vibetix.Adapters.Organizer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * OrganizerEventCardAdapter — Adapter cho danh sách sự kiện của organizer.
 *
 * Features:
 *  - Loads event poster with Glide
 *  - Shows status badge with color-coded pill background
 *  - Shows formatted date, venue, ticket stats and min price
 *  - Overflow PopupMenu with context-aware actions
 *  - Role-based: "Huỷ sự kiện" only for owner role
 */
public class OrganizerEventCardAdapter extends RecyclerView.Adapter<OrganizerEventCardAdapter.ViewHolder> {

    // ─── Listener interface ─────────────────────────────────────────────────

    public interface OnEventActionListener {
        void onViewDetail(Event event);
        void onEdit(Event event);
        void onManageTickets(Event event);
        void onSubmit(Event event);   // Gửi duyệt (only for draft)
        void onCancel(Event event);   // Huỷ sự kiện (only for owner role)
    }

    // ─── Fields ─────────────────────────────────────────────────────────────

    private final List<Event> events;
    private final OnEventActionListener listener;
    private final String staffRole; // "owner" | "manager" | "check_in_staff"

    private static final NumberFormat VND_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // ─── Constructor ─────────────────────────────────────────────────────────

    public OrganizerEventCardAdapter(List<Event> events,
                                     OnEventActionListener listener,
                                     String staffRole) {
        this.events = events;
        this.listener = listener;
        this.staffRole = staffRole != null ? staffRole : "owner";
    }

    // ─── RecyclerView.Adapter ────────────────────────────────────────────────

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_organizer_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        Context ctx = holder.itemView.getContext();

        // — Poster image ———————————————————————————————————————————————————
        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            Glide.with(ctx)
                    .load(event.getPosterUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_cat_music)
                    .error(R.drawable.ic_cat_music)
                    .into(holder.ivEventPoster);
        } else {
            holder.ivEventPoster.setImageResource(R.drawable.ic_cat_music);
        }

        // — Title ————————————————————————————————————————————————————————
        holder.tvEventTitle.setText(event.getTitle() != null ? event.getTitle() : "—");

        // — Status badge —————————————————————————————————————————————————
        bindStatusBadge(ctx, holder.tvEventStatus, event.getStatusStr());

        // — Date ——————————————————————————————————————————————————————————
        String dateStr = event.getStartTime();
        holder.tvEventDate.setText(dateStr != null && !dateStr.isEmpty() ? dateStr : "—");

        // — Venue —————————————————————————————————————————————————————————
        String venue = buildVenueString(event);
        holder.tvEventVenue.setText(venue);

        // — Mini stats: ticket count + price ———————————————————————————
        holder.tvEventTicketStats.setText("🎫 Xem vé");  // Placeholder; real count loaded lazily if needed
        if (event.getMinPrice() > 0) {
            String priceStr = "💰 " + VND_FORMAT.format((long) event.getMinPrice()) + "đ";
            holder.tvEventMinPrice.setText(priceStr);
            holder.tvEventMinPrice.setVisibility(View.VISIBLE);
        } else {
            holder.tvEventMinPrice.setText("💰 Miễn phí");
            holder.tvEventMinPrice.setVisibility(View.VISIBLE);
        }

        // — Card click → view detail —————————————————————————————————————
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onViewDetail(event);
        });

        // — Overflow popup menu ——————————————————————————————————————————
        holder.ivEventMenu.setOnClickListener(v -> showPopupMenu(ctx, v, event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Bind status badge: pill background + text + text color.
     * Status colors:
     *   draft     → grey   (#808E92 text, #1A808E92 bg)
     *   pending   → amber  (#E2B93B text, #1AE2B93B bg)
     *   approved  → green  (#27AE60 text, #1A27AE60 bg)
     *   ongoing   → blue   (#226CEB text, #1A226CEB bg)
     *   completed → dark   (#1E252D text, #1A1E252D bg)
     *   cancelled → red    (#EB5757 text, #1AEB5757 bg)
     */
    private void bindStatusBadge(Context ctx, TextView badge, String statusStr) {
        String status = statusStr != null ? statusStr.toLowerCase() : "draft";
        String label;
        int textColor;
        int bgColor;

        switch (status) {
            case "pending":
                label    = "Chờ duyệt";
                textColor = Color.parseColor("#E2B93B");
                bgColor   = Color.parseColor("#1AE2B93B");
                break;
            case "approved":
                label    = "Đã duyệt";
                textColor = Color.parseColor("#27AE60");
                bgColor   = Color.parseColor("#1A27AE60");
                break;
            case "ongoing":
                label    = "Đang diễn";
                textColor = Color.parseColor("#226CEB");
                bgColor   = Color.parseColor("#1A226CEB");
                break;
            case "completed":
                label    = "Hoàn thành";
                textColor = Color.parseColor("#1E252D");
                bgColor   = Color.parseColor("#1A1E252D");
                break;
            case "cancelled":
                label    = "Đã huỷ";
                textColor = Color.parseColor("#EB5757");
                bgColor   = Color.parseColor("#1AEB5757");
                break;
            case "draft":
            default:
                label    = "Nháp";
                textColor = Color.parseColor("#808E92");
                bgColor   = Color.parseColor("#1A808E92");
                break;
        }

        badge.setText(label);
        badge.setTextColor(textColor);

        // Build pill drawable programmatically
        GradientDrawable pill = new GradientDrawable();
        pill.setShape(GradientDrawable.RECTANGLE);
        pill.setCornerRadius(dpToPx(ctx, 50));
        pill.setColor(bgColor);
        badge.setBackground(pill);
    }

    private String buildVenueString(Event event) {
        StringBuilder sb = new StringBuilder();
        if (event.getVenueName() != null && !event.getVenueName().isEmpty()) {
            sb.append(event.getVenueName());
        }
        if (event.getVenueCity() != null && !event.getVenueCity().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(event.getVenueCity());
        }
        return sb.length() > 0 ? sb.toString() : "—";
    }

    private float dpToPx(Context ctx, float dp) {
        return dp * ctx.getResources().getDisplayMetrics().density;
    }

    /**
     * Show a context popup menu with role-aware actions.
     */
    private void showPopupMenu(Context ctx, View anchor, Event event) {
        PopupMenu popup = new PopupMenu(ctx, anchor);

        // Core actions always visible
        popup.getMenu().add(0, R.id.menu_view_detail, 0, "Xem chi tiết");
        popup.getMenu().add(0, R.id.menu_edit, 1, "Chỉnh sửa");
        popup.getMenu().add(0, R.id.menu_manage_tickets, 2, "Quản lý vé");

        // "Gửi duyệt" only for draft events
        String statusStr = event.getStatusStr();
        if ("draft".equalsIgnoreCase(statusStr)) {
            popup.getMenu().add(0, R.id.menu_submit, 3, "Gửi duyệt");
        }

        // "Huỷ sự kiện" only for owner role and events not already cancelled/completed
        boolean canCancel = "owner".equals(staffRole) &&
                !"cancelled".equalsIgnoreCase(statusStr) &&
                !"completed".equalsIgnoreCase(statusStr);
        if (canCancel) {
            popup.getMenu().add(0, R.id.menu_cancel_event, 4, "Huỷ sự kiện");
        }

        popup.setOnMenuItemClickListener(item -> {
            if (listener == null) return false;
            int id = item.getItemId();
            if (id == R.id.menu_view_detail) {
                listener.onViewDetail(event);
                return true;
            } else if (id == R.id.menu_edit) {
                listener.onEdit(event);
                return true;
            } else if (id == R.id.menu_manage_tickets) {
                listener.onManageTickets(event);
                return true;
            } else if (id == R.id.menu_submit) {
                listener.onSubmit(event);
                return true;
            } else if (id == R.id.menu_cancel_event) {
                listener.onCancel(event);
                return true;
            }
            return false;
        });

        popup.show();
    }

    // ─── ViewHolder ──────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEventPoster;
        ImageView ivEventMenu;
        TextView  tvEventStatus;
        TextView  tvEventTitle;
        TextView  tvEventDate;
        TextView  tvEventVenue;
        TextView  tvEventTicketStats;
        TextView  tvEventMinPrice;

        ViewHolder(View itemView) {
            super(itemView);
            ivEventPoster      = itemView.findViewById(R.id.ivEventPoster);
            ivEventMenu        = itemView.findViewById(R.id.ivEventMenu);
            tvEventStatus      = itemView.findViewById(R.id.tvEventStatus);
            tvEventTitle       = itemView.findViewById(R.id.tvEventTitle);
            tvEventDate        = itemView.findViewById(R.id.tvEventDate);
            tvEventVenue       = itemView.findViewById(R.id.tvEventVenue);
            tvEventTicketStats = itemView.findViewById(R.id.tvEventTicketStats);
            tvEventMinPrice    = itemView.findViewById(R.id.tvEventMinPrice);
        }
    }
}
