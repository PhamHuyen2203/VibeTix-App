package com.example.vibetix.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;

import java.util.ArrayList;

/**
 * Adapter for the Featured Events section.
 * Shows ONLY the portrait poster image — no title, date, or price text.
 * Uses item_featured_poster_card.xml.
 */
public class FeaturedEventAdapter extends RecyclerView.Adapter<FeaturedEventAdapter.FeaturedViewHolder> {

    private final Context context;
    private final ArrayList<Event> danhSachFeatured;
    private OnFeaturedClickListener listener;

    public interface OnFeaturedClickListener {
        void onFeaturedClick(Event event);
    }

    public FeaturedEventAdapter(Context context,
                                ArrayList<Event> danhSachFeatured,
                                OnFeaturedClickListener listener) {
        this.context = context;
        this.danhSachFeatured = danhSachFeatured;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeaturedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_featured_poster_card, parent, false);
        return new FeaturedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeaturedViewHolder holder, int position) {
        Event event = danhSachFeatured.get(position);

        // Portrait section: ưu tiên poster_url, fallback banner_url, rồi local
        Object imageSource;
        if (event.getLocalPortraitImageResId() != 0) {
            imageSource = event.getLocalPortraitImageResId();        // local portrait
        } else if (event.getPortraitImageUrl() != null && !event.getPortraitImageUrl().isEmpty()) {
            imageSource = event.getPortraitImageUrl();               // poster_url từ Firestore
        } else if (event.getLocalImageResId() != 0) {
            imageSource = event.getLocalImageResId();                // local landscape fallback
        } else if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            imageSource = event.getImageUrl();                       // banner_url fallback cuối
        } else {
            imageSource = R.drawable.ic_launcher_background;
        }

        Glide.with(context)
                .load(imageSource)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(holder.imvFeaturedPoster);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFeaturedClick(event);
        });
    }

    @Override
    public int getItemCount() {
        return danhSachFeatured.size();
    }

    public void updateData(ArrayList<Event> data) {
        danhSachFeatured.clear();
        danhSachFeatured.addAll(data);
        notifyDataSetChanged();
    }

    static class FeaturedViewHolder extends RecyclerView.ViewHolder {
        ImageView imvFeaturedPoster;

        FeaturedViewHolder(@NonNull View itemView) {
            super(itemView);
            imvFeaturedPoster = itemView.findViewById(R.id.imvFeaturedPoster);
        }
    }
}
