package com.example.vibetix.Adapters.Organizer;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.R;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;
// TODO: Use actual TicketType model when available, creating a stub for now.
import com.example.vibetix.Models.TicketType; 

public class TicketTypeAdapter extends RecyclerView.Adapter<TicketTypeAdapter.TicketTypeViewHolder> {

    private final List<TicketType> ticketTypes;
    private final OnTicketTypeInteractionListener listener;

    public interface OnTicketTypeInteractionListener {
        void onEdit(TicketType ticketType);
        void onDelete(TicketType ticketType);
        void onStatusChanged(TicketType ticketType, boolean isActive);
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public TicketTypeAdapter(List<TicketType> ticketTypes, OnTicketTypeInteractionListener listener) {
        this.ticketTypes = ticketTypes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TicketTypeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket_type, parent, false);
        return new TicketTypeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketTypeViewHolder holder, int position) {
        TicketType ticketType = ticketTypes.get(position);

        holder.tvTicketName.setText(ticketType.getName());
        holder.tvTicketPrice.setText(String.format("%,.0fđ", ticketType.getPrice()));
        
        int sold = ticketType.getSoldCount();
        holder.tvTicketQuantity.setText(sold + " / " + ticketType.getTotalQuantity());
        
        holder.swTicketActive.setOnCheckedChangeListener(null);
        holder.swTicketActive.setChecked(ticketType.isActive());
        
        holder.swTicketActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onStatusChanged(ticketType, isChecked);
        });

        holder.btnEditTicket.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(ticketType);
        });

        holder.btnDeleteTicket.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(ticketType);
        });

        holder.ivDragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (listener != null) listener.onStartDrag(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return ticketTypes != null ? ticketTypes.size() : 0;
    }

    static class TicketTypeViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDragHandle;
        TextView tvTicketName, tvTicketPrice, tvTicketQuantity;
        MaterialSwitch swTicketActive;
        TextView btnEditTicket, btnDeleteTicket;

        public TicketTypeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDragHandle = itemView.findViewById(R.id.ivDragHandle);
            tvTicketName = itemView.findViewById(R.id.tvTicketName);
            tvTicketPrice = itemView.findViewById(R.id.tvTicketPrice);
            tvTicketQuantity = itemView.findViewById(R.id.tvTicketQuantity);
            swTicketActive = itemView.findViewById(R.id.swTicketActive);
            btnEditTicket = itemView.findViewById(R.id.btnEditTicket);
            btnDeleteTicket = itemView.findViewById(R.id.btnDeleteTicket);
        }
    }
}
