package com.example.vibetix.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Models.FeaturedStar;
import com.example.vibetix.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;

public class FeaturedStarAdapter extends RecyclerView.Adapter<FeaturedStarAdapter.StarViewHolder> {

    Context context;
    ArrayList<FeaturedStar> danhSachStar;
    OnStarClickListener listener;

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
        Object imageSource = star.getLocalImageResId() != 0
                ? star.getLocalImageResId()
                : star.getAvatarUrl();

        Glide.with(context)
                .load(imageSource)
                .circleCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(holder.imvStarAvatar);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onStarClick(star);
            }
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
