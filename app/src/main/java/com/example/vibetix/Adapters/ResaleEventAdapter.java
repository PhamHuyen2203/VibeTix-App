package com.example.vibetix.Adapters;

import android.content.Context;
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

        // Giá bán lại
        if (holder.txtResalePrice != null) {
            long price = event.getMinPrice();
            if (price > 0) {
                holder.txtResalePrice.setText(new java.text.DecimalFormat("#,###").format(price) + " đ");
                holder.txtResalePrice.setVisibility(View.VISIBLE);
            } else {
                holder.txtResalePrice.setText("Thương lượng");
                holder.txtResalePrice.setVisibility(View.VISIBLE);
            }
        }

        Object imageSource;
        if (event.getLocalImageResId() != 0) {
            imageSource = event.getLocalImageResId();
        } else if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            imageSource = event.getImageUrl();
        } else {
            imageSource = R.drawable.ic_launcher_background;
        }

        Glide.with(context)
                .load(imageSource)
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imvResaleImage);

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
        TextView txtResalePrice;

        ResaleViewHolder(@NonNull View itemView) {
            super(itemView);
            imvResaleImage = itemView.findViewById(R.id.imvResaleImage);
            txtResaleTitle = itemView.findViewById(R.id.txtResaleTitle);
            txtResaleDate = itemView.findViewById(R.id.txtResaleDate);
            txtResalePrice = itemView.findViewById(R.id.txtResalePrice);
        }
    }
}
