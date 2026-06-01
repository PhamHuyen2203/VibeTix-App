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
import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;

import java.util.ArrayList;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    Context context;
    ArrayList<Event> danhSachBanner;
    OnBannerClickListener listener;

    public interface OnBannerClickListener {
        void onBannerClick(Event event);
    }

    public BannerAdapter(Context context, ArrayList<Event> danhSachBanner, OnBannerClickListener listener) {
        this.context = context;
        this.danhSachBanner = danhSachBanner;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Event event = danhSachBanner.get(position);
        holder.txtBannerTitle.setText(event.getTitle());
        holder.txtBannerDate.setText(event.getDate());

        // City (from ERD: VENUE.city)
        if (event.getVenueCity() != null && !event.getVenueCity().isEmpty()) {
            holder.txtBannerCity.setText(event.getVenueCity());
            holder.txtBannerCity.setVisibility(View.VISIBLE);
        } else {
            holder.txtBannerCity.setVisibility(View.GONE);
        }

        // Ongoing badge (from ERD: EVENT.status)
        if ("ongoing".equals(event.getStatus())) {
            holder.txtBannerStatusBadge.setVisibility(View.VISIBLE);
        } else {
            holder.txtBannerStatusBadge.setVisibility(View.GONE);
        }

        Object imageSource = event.getLocalImageResId() != 0
                ? event.getLocalImageResId()
                : event.getImageUrl();

        Glide.with(context)
                .load(imageSource)
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imvBanner);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onBannerClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return danhSachBanner.size();
    }

    public void updateData(ArrayList<Event> data) {
        danhSachBanner.clear();
        danhSachBanner.addAll(data);
        notifyDataSetChanged();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imvBanner;
        TextView txtBannerTitle;
        TextView txtBannerDate;
        TextView txtBannerCity;
        TextView txtBannerStatusBadge;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imvBanner = itemView.findViewById(R.id.imvBanner);
            txtBannerTitle = itemView.findViewById(R.id.txtBannerTitle);
            txtBannerDate = itemView.findViewById(R.id.txtBannerDate);
            txtBannerCity = itemView.findViewById(R.id.txtBannerCity);
            txtBannerStatusBadge = itemView.findViewById(R.id.txtBannerStatusBadge);
        }
    }
}
