package com.example.vibetix.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.vibetix.Models.FeaturedStar;
import com.example.vibetix.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;

public class FeaturedStarAdapter extends RecyclerView.Adapter<FeaturedStarAdapter.StarViewHolder> {

    Context context;
    ArrayList<FeaturedStar> danhSachStar;
    OnStarClickListener listener;

    private static final int[] AVATAR_COLORS = {
            0xFF2563EB, 0xFF06B6D4, 0xFF8B5CF6,
            0xFFEC4899, 0xFFF59E0B, 0xFF10B981,
            0xFFEF4444, 0xFF6366F1
    };

    public interface OnStarClickListener {
        void onStarClick(FeaturedStar star);
    }

    public FeaturedStarAdapter(Context context, ArrayList<FeaturedStar> danhSachStar, OnStarClickListener listener) {
        this.context = context;
        this.danhSachStar = danhSachStar;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_featured_star, parent, false);
        return new StarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StarViewHolder holder, int position) {
        FeaturedStar star = danhSachStar.get(position);
        holder.txtStarName.setText(star.getName());

        boolean hasUrl = star.getAvatarUrl() != null && !star.getAvatarUrl().isEmpty();
        boolean hasLocal = star.getLocalImageResId() != 0;

        if (hasLocal) {
            Glide.with(context)
                    .load(star.getLocalImageResId())
                    .circleCrop()
                    .into(holder.imvStarAvatar);
        } else if (hasUrl) {
            int color = AVATAR_COLORS[position % AVATAR_COLORS.length];
            Glide.with(context)
                    .load(star.getAvatarUrl())
                    .circleCrop()
                    .override(120, 120)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(new ColorDrawable(color))
                    .error(new ColorDrawable(color))
                    .into(holder.imvStarAvatar);
        } else {
            int color = AVATAR_COLORS[position % AVATAR_COLORS.length];
            holder.imvStarAvatar.setImageDrawable(new ColorDrawable(color));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onStarClick(star);
        });
    }

    @Override
    public int getItemCount() {
        return danhSachStar.size();
    }

    public void updateData(ArrayList<FeaturedStar> data) {
        danhSachStar.clear();
        danhSachStar.addAll(data);
        notifyDataSetChanged();
    }

    static class StarViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imvStarAvatar;
        TextView txtStarName;

        StarViewHolder(@NonNull View itemView) {
            super(itemView);
            imvStarAvatar = itemView.findViewById(R.id.imvStarAvatar);
            txtStarName = itemView.findViewById(R.id.txtStarName);
        }
    }
}
