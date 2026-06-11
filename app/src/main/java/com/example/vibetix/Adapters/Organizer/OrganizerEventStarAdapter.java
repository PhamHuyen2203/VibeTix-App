package com.example.vibetix.Adapters.Organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.vibetix.Models.EventStar;
import com.example.vibetix.Models.Star;
import com.example.vibetix.R;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class OrganizerEventStarAdapter extends RecyclerView.Adapter<OrganizerEventStarAdapter.ViewHolder> {

    public static class EventStarWrapper {
        public EventStar eventStar;
        public Star star;
        
        public EventStarWrapper(EventStar eventStar, Star star) {
            this.eventStar = eventStar;
            this.star = star;
        }
    }

    public interface OnEventStarActionListener {
        void onEdit(EventStarWrapper wrapper);
        void onDelete(EventStarWrapper wrapper);
        void onConfirmedToggle(EventStarWrapper wrapper, boolean isConfirmed);
    }

    private List<EventStarWrapper> list;
    private OnEventStarActionListener listener;

    public OrganizerEventStarAdapter(List<EventStarWrapper> list, OnEventStarActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_star_organizer, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventStarWrapper wrapper = list.get(position);
        
        if (wrapper.star != null) {
            holder.tvStarName.setText(wrapper.star.getStageName() != null ? wrapper.star.getStageName() : "Không xác định");
            
            if (wrapper.star.getAvatarUrl() != null && !wrapper.star.getAvatarUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(wrapper.star.getAvatarUrl())
                        .placeholder(R.drawable.ic_organizer_placeholder)
                        .circleCrop()
                        .into(holder.ivStarAvatar);
            } else {
                holder.ivStarAvatar.setImageResource(R.drawable.ic_organizer_placeholder);
            }
        } else {
            holder.tvStarName.setText("Nghệ sĩ không khả dụng");
            holder.ivStarAvatar.setImageResource(R.drawable.ic_organizer_placeholder);
        }

        if (wrapper.eventStar != null) {
            String role = wrapper.eventStar.getRole();
            if (role != null && !role.isEmpty()) {
                holder.tvStarRole.setText(role);
                holder.tvStarRole.setVisibility(View.VISIBLE);
            } else {
                holder.tvStarRole.setVisibility(View.GONE);
            }
            
            holder.switchConfirmed.setOnCheckedChangeListener(null);
            holder.switchConfirmed.setChecked(wrapper.eventStar.isConfirmed());
            holder.switchConfirmed.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onConfirmedToggle(wrapper, isChecked);
                }
            });
        }

        holder.btnOptions.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(holder.itemView.getContext(), holder.btnOptions);
            popup.getMenu().add("Chỉnh sửa");
            popup.getMenu().add("Xoá khỏi sự kiện");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Chỉnh sửa")) {
                    if (listener != null) listener.onEdit(wrapper);
                } else if (item.getTitle().equals("Xoá khỏi sự kiện")) {
                    if (listener != null) listener.onDelete(wrapper);
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void updateData(List<EventStarWrapper> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public List<EventStarWrapper> getList() {
        return list;
    }

    public void moveItem(int fromPosition, int toPosition) {
        if (list == null || fromPosition < 0 || toPosition < 0 || fromPosition >= list.size() || toPosition >= list.size()) return;
        EventStarWrapper item = list.remove(fromPosition);
        list.add(toPosition, item);
        notifyItemMoved(fromPosition, toPosition);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivStarAvatar;
        TextView tvStarName, tvStarRole;
        ImageButton btnOptions, btnDragHandle;
        SwitchMaterial switchConfirmed;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStarAvatar = itemView.findViewById(R.id.ivStarAvatar);
            tvStarName   = itemView.findViewById(R.id.tvStarName);
            tvStarRole   = itemView.findViewById(R.id.tvStarRole);
            btnOptions   = itemView.findViewById(R.id.btnOptions);
            btnDragHandle = itemView.findViewById(R.id.btnDragHandle);
            switchConfirmed = itemView.findViewById(R.id.switchConfirmed);
        }
    }
}
