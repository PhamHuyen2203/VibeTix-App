package com.example.vibetix.Adapters.Organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Models.UserTicket;
import com.example.vibetix.R;

import java.util.List;

public class TicketSaleAdapter extends RecyclerView.Adapter<TicketSaleAdapter.VH> {

    public interface OnTicketClickListener {
        void onTicketClick(UserTicket ticket);
    }

    private final List<UserTicket> items;
    private final OnTicketClickListener listener;

    public TicketSaleAdapter(List<UserTicket> items, OnTicketClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket_sale, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        UserTicket t = items.get(position);

        // Attendee name or fallback
        String name = t.getFullName();
        if (name == null || name.isEmpty()) name = t.getEmail();
        if (name == null || name.isEmpty()) name = "Khách hàng #" + (position + 1);
        h.tvName.setText(name);

        // Ticket type + display code
        String typeName = t.getTicketTypeName() != null ? t.getTicketTypeName() : "—";
        String code = t.getDisplayCode() != null ? t.getDisplayCode()
                : (t.getUserTicketId() != null && t.getUserTicketId().length() >= 8
                ? t.getUserTicketId().substring(0, 8).toUpperCase() : "—");
        h.tvType.setText(typeName + " · #" + code);

        boolean isUsed = t.isUsed();
        boolean expired = false;
        boolean cancelled = false;
        if (isUsed) {
            h.tvStatus.setText("Đã check-in");
            h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.clr_success));
        } else if (expired) {
            h.tvStatus.setText("Hết hạn");
            h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.clr_text_secondary));
        } else if (cancelled) {
            h.tvStatus.setText("Đã hủy");
            h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.clr_error));
        } else {
            h.tvStatus.setText("Hợp lệ");
            h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.clr_primary_blue));
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTicketClick(t);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void updateData(List<UserTicket> newItems) {
        if (items != newItems) {
            items.clear();
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvStatus;

        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvAttendeeName);
            tvType = v.findViewById(R.id.tvTicketType);
            tvStatus = v.findViewById(R.id.tvTicketStatus);
        }
    }
}
