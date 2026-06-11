package com.example.vibetix.Adapters.Organizer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.example.vibetix.R;

import java.util.List;

/**
 * FeatureAdapter — Adapter cho Feature Grid trong EventHubActivity.
 * Layout dọc: icon trên, tên dưới — kiểu iOS App grid hiện đại.
 */
public class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.ViewHolder> {

    public static class FeatureItem {
        public final int iconRes;
        public final String title;
        public final boolean isEnabled;
        public final boolean isHighlighted;   // Giữ field nhưng không dùng để đổi màu card nữa
        public final Runnable onClick;

        public FeatureItem(int iconRes, String title, boolean isEnabled,
                           boolean isHighlighted, Runnable onClick) {
            this.iconRes = iconRes;
            this.title = title;
            this.isEnabled = isEnabled;
            this.isHighlighted = isHighlighted;
            this.onClick = onClick;
        }
    }

    private final Context context;
    private final List<FeatureItem> items;

    public FeatureAdapter(Context context, List<FeatureItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_feature, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        FeatureItem item = items.get(position);

        // Icon & Title — luôn rõ nét
        h.ivIcon.setImageResource(item.iconRes);
        h.tvTitle.setText(item.title);

        // TẤT CẢ card đều nền trắng, không phân biệt highlighted
        // => Giao diện nhất quán 100%
        h.card.setCardBackgroundColor(context.getColor(R.color.clr_bg_surface));
        h.card.setAlpha(1.0f);  // Không làm mờ card nữa

        if (item.isEnabled) {
            // Enabled: icon xanh đậm, tên đen, chevron hiện
            h.iconContainer.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(context.getColor(R.color.clr_primary_light)));
            h.ivIcon.setImageTintList(
                    android.content.res.ColorStateList.valueOf(context.getColor(R.color.clr_primary)));
            h.tvTitle.setTextColor(context.getColor(R.color.clr_text_primary));
            h.tvTitle.setAlpha(1.0f);
            h.ivChevron.setVisibility(View.VISIBLE);
            h.tvNoPerm.setVisibility(View.GONE);
            h.card.setClickable(true);
            h.card.setFocusable(true);
        } else {
            // Disabled: icon xám, tên xám, hiện nhãn phụ "Cần quyền Owner"
            h.iconContainer.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFF1F5F9));
            h.ivIcon.setImageTintList(
                    android.content.res.ColorStateList.valueOf(context.getColor(R.color.clr_text_secondary)));
            h.tvTitle.setTextColor(context.getColor(R.color.clr_text_secondary));
            h.tvTitle.setAlpha(1.0f);  // Text vẫn rõ ràng, không bị mờ
            h.ivChevron.setVisibility(View.GONE);
            h.tvNoPerm.setVisibility(View.VISIBLE);
            h.card.setClickable(false);
            h.card.setFocusable(false);
        }

        // Click listener
        h.card.setOnClickListener(v -> {
            if (item.isEnabled && item.onClick != null) {
                item.onClick.run();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final FrameLayout iconContainer;
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvNoPerm;
        final ImageView ivChevron;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card          = itemView.findViewById(R.id.cardFeature);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            ivIcon        = itemView.findViewById(R.id.ivFeatureIcon);
            tvTitle       = itemView.findViewById(R.id.tvFeatureTitle);
            tvNoPerm      = itemView.findViewById(R.id.tvFeatureNoPerm);
            ivChevron     = itemView.findViewById(R.id.ivChevron);
        }
    }
}
