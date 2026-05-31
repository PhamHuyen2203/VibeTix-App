package com.example.vibetix.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Models.Ticket;
import com.example.vibetix.R;

import java.util.List;

/**
 * TicketAdapter — hiển thị danh sách vé trong RecyclerView.
 * Dùng item_purchased_ticket layout.
 */
public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    public interface OnTicketClickListener {
        void onQrClick(Ticket ticket);
        void onResellClick(Ticket ticket);
    }

    private final Context               context;
    private final List<Ticket>          tickets;
    private final OnTicketClickListener listener;

    public TicketAdapter(Context context, List<Ticket> tickets, OnTicketClickListener listener) {
        this.context  = context;
        this.tickets  = tickets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_purchased_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);
        if (ticket == null) return;

        if (holder.txtTicketEventName != null) {
            holder.txtTicketEventName.setText(ticket.getEventTitle() != null ? ticket.getEventTitle() : "");
        }
        if (holder.txtTicketDate != null) {
            holder.txtTicketDate.setText(ticket.getEventDate() != null ? ticket.getEventDate() : "");
        }
        if (holder.txtTicketLocation != null) {
            holder.txtTicketLocation.setText(ticket.getEventLocation() != null ? ticket.getEventLocation() : "");
        }

        if (holder.btnShowQr != null) {
            holder.btnShowQr.setOnClickListener(v -> {
                if (listener != null) listener.onQrClick(ticket);
            });
        }
        if (holder.btnResell != null) {
            boolean isReselling = "RESELLING".equalsIgnoreCase(ticket.getStatus());
            holder.btnResell.setText(isReselling ? "Hủy bán" : "Bán lại vé");
            holder.btnResell.setOnClickListener(v -> {
                if (listener != null) listener.onResellClick(ticket);
            });
        }
    }

    @Override
    public int getItemCount() {
        return tickets == null ? 0 : tickets.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView txtTicketEventName;
        TextView txtTicketDate;
        TextView txtTicketLocation;
        Button   btnShowQr;
        Button   btnResell;

        TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTicketEventName = itemView.findViewById(R.id.txtTicketEventTitle);
            txtTicketDate      = itemView.findViewById(R.id.txtTicketEventDate);
            txtTicketLocation  = itemView.findViewById(R.id.txtTicketEventLocation);
            btnShowQr          = itemView.findViewById(R.id.btnTicketQrAction);
            btnResell          = itemView.findViewById(R.id.btnTicketResellAction);
        }
    }
}
