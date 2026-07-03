package com.example.vibetix.Adapters.Organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Star;
import com.example.vibetix.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class DraftStarAdapter extends RecyclerView.Adapter<DraftStarAdapter.VH> {

    public interface OnRemoveListener {
        void onRemove(int position, Star star);
    }

    private final List<Star> items;
    private final OnRemoveListener listener;

    public DraftStarAdapter(List<Star> items, OnRemoveListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_draft_star, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Star star = items.get(position);
        h.tvName.setText(star.getStageName() != null ? star.getStageName() : "");
        h.tvGenre.setText(star.getGenre() != null ? star.getGenre() : "");

        if (star.getAvatarUrl() != null && !star.getAvatarUrl().isEmpty()) {
            Glide.with(h.ivAvatar.getContext())
                    .load(star.getAvatarUrl())
                    .placeholder(R.drawable.img_placeholder)
                    .circleCrop()
                    .into(h.ivAvatar);
        } else {
            h.ivAvatar.setImageResource(R.drawable.img_placeholder);
        }

        h.btnRemove.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID && listener != null)
                listener.onRemove(pos, items.get(pos));
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvName, tvGenre;
        MaterialButton btnRemove;
        VH(View v) {
            super(v);
            ivAvatar  = v.findViewById(R.id.ivDraftStarAvatar);
            tvName    = v.findViewById(R.id.tvDraftStarName);
            tvGenre   = v.findViewById(R.id.tvDraftStarGenre);
            btnRemove = v.findViewById(R.id.btnDraftStarRemove);
        }
    }
}
