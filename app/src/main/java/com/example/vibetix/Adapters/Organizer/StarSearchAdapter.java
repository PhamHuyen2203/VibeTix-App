package com.example.vibetix.Adapters.Organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Star;
import com.example.vibetix.R;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class StarSearchAdapter extends RecyclerView.Adapter<StarSearchAdapter.ViewHolder> {

    public interface OnStarAddListener {
        void onAdd(Star star);
    }

    private List<Star> list;
    private OnStarAddListener listener;

    public StarSearchAdapter(List<Star> list, OnStarAddListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_star_search, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Star star = list.get(position);

        holder.tvStarName.setText(star.getStageName() != null ? star.getStageName() : "Không xác định");
        holder.tvFollowers.setText(star.getFollowerCount() + " người theo dõi");

        if (star.getAvatarUrl() != null && !star.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(star.getAvatarUrl())
                    .placeholder(R.drawable.ic_organizer_placeholder)
                    .circleCrop()
                    .into(holder.ivStarAvatar);
        } else {
            holder.ivStarAvatar.setImageResource(R.drawable.ic_organizer_placeholder);
        }

        holder.btnAddStar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAdd(star);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void updateData(List<Star> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivStarAvatar;
        TextView tvStarName, tvFollowers;
        Button btnAddStar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStarAvatar = itemView.findViewById(R.id.ivStarAvatar);
            tvStarName   = itemView.findViewById(R.id.tvStarName);
            tvFollowers  = itemView.findViewById(R.id.tvFollowers);
            btnAddStar   = itemView.findViewById(R.id.btnAddStar);
        }
    }
}
