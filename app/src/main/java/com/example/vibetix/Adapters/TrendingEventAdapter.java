package com.example.vibetix.Adapters;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Shader;
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
 * Adapter for the Trending Events section.
 * Shows ONLY the landscape banner image + a large rank number overlay (1, 2, 3…) at bottom-left.
 * Rank number uses a top→bottom blue gradient (#48C5E9 → #226CEB → #0C419D).
 * Uses item_trending_ranked_card.xml.
 */
public class TrendingEventAdapter extends RecyclerView.Adapter<TrendingEventAdapter.TrendingViewHolder> {

    // Blue gradient stops: light cyan → brand blue → dark navy
    private static final int COLOR_RANK_TOP    = 0xFF48C5E9; // clr_primary_pest
    private static final int COLOR_RANK_MID    = 0xFF226CEB; // clr_primary_blue
    private static final int COLOR_RANK_BOTTOM = 0xFF0C419D; // clr_dark_blue_3

    private final Context context;
    private final ArrayList<Event> danhSachTrending;
    private OnTrendingClickListener listener;

    public interface OnTrendingClickListener {
        void onTrendingClick(Event event);
    }

    public TrendingEventAdapter(Context context,
                                ArrayList<Event> danhSachTrending,
                                OnTrendingClickListener listener) {
        this.context = context;
        this.danhSachTrending = danhSachTrending;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_trending_ranked_card, parent, false);
        return new TrendingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrendingViewHolder holder, int position) {
        Event event = danhSachTrending.get(position);

        // Landscape image
        int resId = event.getLocalImageResId();
        if (resId != 0) {
            holder.imvTrendingImage.setImageResource(resId);
        } else {
            holder.imvTrendingImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // Rank number (1-based)
        holder.txtTrendingRank.setText(String.valueOf(position + 1));

        // Apply blue gradient shader once the TextView is measured (post guarantees layout pass)
        holder.txtTrendingRank.post(() -> {
            float h = holder.txtTrendingRank.getHeight();
            if (h <= 0) return;
            LinearGradient shader = new LinearGradient(
                    0f, 0f,             // startX, startY (top)
                    0f, h,              // endX,   endY   (bottom)
                    new int[]{COLOR_RANK_TOP, COLOR_RANK_MID, COLOR_RANK_BOTTOM},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            );
            holder.txtTrendingRank.getPaint().setShader(shader);
            holder.txtTrendingRank.invalidate();
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTrendingClick(event);
        });
    }

    @Override
    public int getItemCount() {
        return danhSachTrending.size();
    }

    public void updateData(ArrayList<Event> data) {
        danhSachTrending.clear();
        danhSachTrending.addAll(data);
        notifyDataSetChanged();
    }

    static class TrendingViewHolder extends RecyclerView.ViewHolder {
        ImageView imvTrendingImage;
        TextView txtTrendingRank;

        TrendingViewHolder(@NonNull View itemView) {
            super(itemView);
            imvTrendingImage = itemView.findViewById(R.id.imvTrendingImage);
            txtTrendingRank  = itemView.findViewById(R.id.txtTrendingRank);
        }
    }
}
