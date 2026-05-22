package com.example.vibetix.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Models.Event;
import com.example.vibetix.R;

import java.util.ArrayList;

/**
 * Adapter for the Resale Ticket section cards (item_resale_card.xml).
 * Each card shows the event image with a fixed RE-SALE badge, the event title, and date.
 * Follows the same addViews / addEvents + listener pattern as the project convention.
 */
public class ResaleEventAdapter extends RecyclerView.Adapter<ResaleEventAdapter.ResaleViewHolder> {

    Context context;
    ArrayList<Event> danhSachResale;
    OnResaleClickListener listener;

    public interface OnResaleClickListener {
        void onResaleClick(Event event);
    }

    public ResaleEventAdapter(Context context,
                               ArrayList<Event> danhSachResale,
                               OnResaleClickListener listener) {
        this.context = context;
        this.danhSachResale = danhSachResale;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_resale_card, parent, false);
        return new ResaleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResaleViewHolder holder, int position) {
        Event event = danhSachResale.get(position);

        // Title
        holder.txtResaleTitle.setText(event.getTitle());

        // Date
        holder.txtResaleDate.setText(event.getDate());

        // Event image (local drawable preferred over URL)
        if (event.getLocalImageResId() != 0) {
            holder.imvResaleImage.setImageResource(event.getLocalImageResId());
        } else {
            holder.imvResaleImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // Click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onResaleClick(event);
        });
    }

    @Override
    public int getItemCount() {
        return danhSachResale.size();
    }

    public void updateData(ArrayList<Event> data) {
        danhSachResale.clear();
        danhSachResale.addAll(data);
        notifyDataSetChanged();
    }

    static class ResaleViewHolder extends RecyclerView.ViewHolder {
        ImageView imvResaleImage;
        TextView txtResaleTitle;
        TextView txtResaleDate;

        ResaleViewHolder(@NonNull View itemView) {
            super(itemView);
            imvResaleImage = itemView.findViewById(R.id.imvResaleImage);
            txtResaleTitle = itemView.findViewById(R.id.txtResaleTitle);
            txtResaleDate = itemView.findViewById(R.id.txtResaleDate);
        }
    }
}
