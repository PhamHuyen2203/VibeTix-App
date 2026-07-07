package com.example.vibetix.Adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Ticket;
import com.example.vibetix.R;

import java.text.DecimalFormat;
import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private final Context context;
    private final List<Ticket> ticketsList;
    private final OnTicketClickListener listener;

    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public interface OnTicketClickListener {
        void onQrClick(Ticket ticket);
        void onResellClick(Ticket ticket);
        default void onItemClick(Ticket ticket) {}
    }

    public TicketAdapter(Context context, List<Ticket> ticketsList, OnTicketClickListener listener) {
        this.context = context;
        this.ticketsList = ticketsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_purchased_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = ticketsList.get(position);
        if (ticket == null) return;

        holder.txtTicketEventTitle.setText(ticket.getEventTitle());
        holder.txtTicketEventDate.setText(ticket.getEventDate());
        holder.txtTicketEventLocation.setText(ticket.getEventLocation());
        // Hiện loại vé sổ dọc: "1x Vé VIP, 1x Vé Early bird" → mỗi loại 1 dòng
        String typeDisplay = ticket.getTicketTypeName();
        if (typeDisplay != null && typeDisplay.contains(", ")) {
            typeDisplay = typeDisplay.replace(", ", "\n");
        }
        holder.txtTicketTypeName.setText(typeDisplay);

        // Load event image from URL stored in ticket
        if (ticket.getEventImageUrl() != null && !ticket.getEventImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(ticket.getEventImageUrl())
                    .placeholder(R.drawable.event_live_non_song)
                    .error(R.drawable.event_live_non_song)
                    .into(holder.imvTicketThumb);
        } else {
            holder.imvTicketThumb.setImageResource(R.drawable.event_live_non_song);
        }

        // Bind status and pricing details
        String status = ticket.getStatus();
        holder.btnTicketResellAction.setVisibility(View.VISIBLE);
        holder.btnTicketQrAction.setVisibility(View.VISIBLE);

        if ("ACTIVE".equalsIgnoreCase(status)) {
            holder.txtTicketStatusBadge.setText(context.getString(R.string.str_ticket_valid_badge));
            holder.txtTicketStatusBadge.setTextColor(context.getResources().getColor(R.color.clr_success));
            holder.txtTicketStatusBadge.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.clr_bg_section)));
            holder.txtTicketPrice.setText(formatter.format(ticket.getPurchasePrice()) + " đ");

            // Cho bán lại nếu sự kiện còn hoạt động VÀ loại vé có is_transferable=true (B5)
            String evStatus = ticket.getEventStatus() != null ? ticket.getEventStatus().toLowerCase() : "";
            boolean eventActive = evStatus.isEmpty() || "approved".equals(evStatus) || "ongoing".equals(evStatus);
            boolean canResell = eventActive && ticket.isTransferable();
            if (canResell) {
                holder.btnTicketResellAction.setText(context.getString(R.string.str_resell_ticket_btn));
                holder.btnTicketResellAction.setEnabled(true);
                holder.btnTicketResellAction.setVisibility(View.VISIBLE);
            } else {
                holder.btnTicketResellAction.setVisibility(View.GONE);
            }
        } else if ("RESELLING".equalsIgnoreCase(status)) {
            holder.txtTicketStatusBadge.setText(context.getString(R.string.str_ticket_reselling_badge));
            holder.txtTicketStatusBadge.setTextColor(context.getResources().getColor(R.color.clr_primary_blue));
            holder.txtTicketStatusBadge.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.clr_bg_section)));
            holder.txtTicketPrice.setText(formatter.format(ticket.getResalePrice()) + " đ");

            holder.btnTicketResellAction.setText(context.getString(R.string.str_btn_cancel_resell));
            holder.btnTicketResellAction.setEnabled(true);
        } else if ("USED".equalsIgnoreCase(status)) {
            holder.txtTicketStatusBadge.setText(context.getString(R.string.str_ticket_used_badge));
            holder.txtTicketStatusBadge.setTextColor(context.getResources().getColor(R.color.clr_grey_1));
            holder.txtTicketStatusBadge.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.clr_divider)));
            holder.txtTicketPrice.setText(formatter.format(ticket.getPurchasePrice()) + " đ");

            holder.btnTicketResellAction.setVisibility(View.GONE);
            holder.btnTicketQrAction.setVisibility(View.GONE);
        } else if ("EXPIRED".equalsIgnoreCase(status)) {
            holder.txtTicketStatusBadge.setText(context.getString(R.string.str_ticket_expired_badge));
            holder.txtTicketStatusBadge.setTextColor(context.getResources().getColor(R.color.clr_error));
            holder.txtTicketStatusBadge.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.clr_divider)));
            holder.txtTicketPrice.setText(formatter.format(ticket.getPurchasePrice()) + " đ");

            holder.btnTicketResellAction.setVisibility(View.GONE);
            holder.btnTicketQrAction.setVisibility(View.GONE);
        } else if ("RESOLD".equalsIgnoreCase(status)) {
            holder.txtTicketStatusBadge.setText(context.getString(R.string.str_ticket_sold_badge));
            holder.txtTicketStatusBadge.setTextColor(context.getResources().getColor(R.color.clr_success));
            holder.txtTicketStatusBadge.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.clr_bg_section)));
            holder.txtTicketPrice.setText(formatter.format(ticket.getResalePrice()) + " đ");

            holder.btnTicketResellAction.setVisibility(View.GONE);
            holder.btnTicketQrAction.setVisibility(View.GONE);
        } else if ("RESALE_CANCELLED".equalsIgnoreCase(status)) {
            holder.txtTicketStatusBadge.setText(context.getString(R.string.str_ticket_cancelled_resell_badge));
            holder.txtTicketStatusBadge.setTextColor(context.getResources().getColor(R.color.clr_grey_1));
            holder.txtTicketStatusBadge.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.clr_divider)));
            holder.txtTicketPrice.setText(formatter.format(ticket.getResalePrice()) + " đ");

            holder.btnTicketResellAction.setText(context.getString(R.string.str_btn_relist));
            holder.btnTicketResellAction.setEnabled(true);
            holder.btnTicketResellAction.setVisibility(View.VISIBLE);
            holder.btnTicketQrAction.setVisibility(View.GONE);
        }

        // Set action triggers
        holder.btnTicketQrAction.setOnClickListener(v -> {
            if (listener != null) listener.onQrClick(ticket);
        });
        holder.btnTicketResellAction.setOnClickListener(v -> {
            if (listener != null) listener.onResellClick(ticket);
        });
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(ticket);
        });
    }

    @Override
    public int getItemCount() {
        return ticketsList == null ? 0 : ticketsList.size();
    }

    public static class TicketViewHolder extends RecyclerView.ViewHolder {
        ImageView imvTicketThumb;
        TextView txtTicketEventTitle;
        TextView txtTicketEventDate;
        TextView txtTicketEventLocation;
        TextView txtTicketTypeName;
        TextView txtTicketPrice;
        TextView txtTicketStatusBadge;
        Button btnTicketResellAction;
        Button btnTicketQrAction;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            imvTicketThumb = itemView.findViewById(R.id.imvTicketThumb);
            txtTicketEventTitle = itemView.findViewById(R.id.txtTicketEventTitle);
            txtTicketEventDate = itemView.findViewById(R.id.txtTicketEventDate);
            txtTicketEventLocation = itemView.findViewById(R.id.txtTicketEventLocation);
            txtTicketTypeName = itemView.findViewById(R.id.txtTicketTypeName);
            txtTicketPrice = itemView.findViewById(R.id.txtTicketPrice);
            txtTicketStatusBadge = itemView.findViewById(R.id.txtTicketStatusBadge);
            btnTicketResellAction = itemView.findViewById(R.id.btnTicketResellAction);
            btnTicketQrAction = itemView.findViewById(R.id.btnTicketQrAction);
        }
    }
}
