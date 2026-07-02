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
import com.example.vibetix.R;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Adapter cho danh sách sự kiện có vé bán lại (group theo sự kiện) — màn marketplace.
 */
public class ResaleEventGroupAdapter extends RecyclerView.Adapter<ResaleEventGroupAdapter.GroupVH> {

    /** Một nhóm vé bán lại theo sự kiện. */
    public static class ResaleGroup {
        public String eventId;
        public String title;
        public String date;
        public String imageUrl;
        public String location;
        public int count;
        public long minPrice;
    }

    public interface OnGroupClickListener {
        void onGroupClick(ResaleGroup group);
    }

    private final Context context;
    private final List<ResaleGroup> groups;
    private final OnGroupClickListener listener;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public ResaleEventGroupAdapter(Context context, List<ResaleGroup> groups, OnGroupClickListener listener) {
        this.context = context;
        this.groups = groups;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_resale_event_group, parent, false);
        return new GroupVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupVH h, int position) {
        ResaleGroup g = groups.get(position);
        h.txtTitle.setText(g.title);
        h.txtDate.setText(g.date);
        h.txtCount.setText(g.count + " vé");
        h.txtFromPrice.setText(g.minPrice > 0 ? "Từ " + formatter.format(g.minPrice) + " đ" : "Thương lượng");

        if (g.imageUrl != null && !g.imageUrl.isEmpty()) {
            Glide.with(context).load(g.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(h.imv);
        } else {
            h.imv.setImageResource(R.drawable.ic_launcher_background);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onGroupClick(g);
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class GroupVH extends RecyclerView.ViewHolder {
        ImageView imv;
        TextView txtTitle, txtDate, txtCount, txtFromPrice;

        GroupVH(@NonNull View itemView) {
            super(itemView);
            imv = itemView.findViewById(R.id.imvGroupImage);
            txtTitle = itemView.findViewById(R.id.txtGroupTitle);
            txtDate = itemView.findViewById(R.id.txtGroupDate);
            txtCount = itemView.findViewById(R.id.txtGroupCount);
            txtFromPrice = itemView.findViewById(R.id.txtGroupFromPrice);
        }
    }
}
