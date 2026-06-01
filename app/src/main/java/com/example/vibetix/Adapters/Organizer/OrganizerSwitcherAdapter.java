package com.example.vibetix.Adapters.Organizer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.R;

import java.util.List;

public class OrganizerSwitcherAdapter extends RecyclerView.Adapter<OrganizerSwitcherAdapter.ViewHolder> {

    private final Context context;
    private final List<Organizer> organizers;
    private final String currentActiveId;
    private final OnOrganizerSelectedListener listener;

    public interface OnOrganizerSelectedListener {
        void onSelected(Organizer organizer);
    }

    public OrganizerSwitcherAdapter(Context context, List<Organizer> organizers, String currentActiveId, OnOrganizerSelectedListener listener) {
        this.context = context;
        this.organizers = organizers;
        this.currentActiveId = currentActiveId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_organizer_switcher, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Organizer org = organizers.get(position);

        holder.tvOrgName.setText(org.getBrandName());
        
        if (org.getLogoUrl() != null && !org.getLogoUrl().isEmpty()) {
            Glide.with(context).load(org.getLogoUrl())
                    .placeholder(R.drawable.ic_organizer_placeholder)
                    .circleCrop()
                    .into(holder.ivLogo);
        } else {
            holder.ivLogo.setImageResource(R.drawable.ic_organizer_placeholder);
        }

        if (org.getOrganizerId() != null && org.getOrganizerId().equals(currentActiveId)) {
            holder.ivActiveCheck.setVisibility(View.VISIBLE);
            holder.ivActiveCheck.setImageResource(R.drawable.ic_check);
            holder.tvSub.setText("Đang chọn");
            holder.tvSub.setTextColor(context.getResources().getColor(R.color.clr_primary));
        } else {
            holder.ivActiveCheck.setVisibility(View.GONE);
            holder.tvSub.setText("Nhấn để chọn");
            holder.tvSub.setTextColor(context.getResources().getColor(R.color.clr_text_secondary));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSelected(org);
        });
    }

    @Override
    public int getItemCount() {
        return organizers != null ? organizers.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivLogo, ivActiveCheck;
        TextView tvOrgName, tvSub;

        ViewHolder(View itemView) {
            super(itemView);
            ivLogo = itemView.findViewById(R.id.ivOrgSwitchLogo);
            ivActiveCheck = itemView.findViewById(R.id.ivActiveCheck);
            tvOrgName = itemView.findViewById(R.id.tvOrgSwitchName);
            tvSub = itemView.findViewById(R.id.tvOrgSwitchSub);
        }
    }
}
