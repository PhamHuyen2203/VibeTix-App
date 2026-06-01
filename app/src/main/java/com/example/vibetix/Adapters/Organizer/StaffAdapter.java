package com.example.vibetix.Adapters.Organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Models.EventStaff;
import com.example.vibetix.R;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.StaffViewHolder> {

    private final List<EventStaff> staffList;
    private final OnStaffInteractionListener listener;

    public interface OnStaffInteractionListener {
        void onOptionsClick(EventStaff staff, View anchor);
        void onStatusChange(EventStaff staff, boolean isActive);
    }

    public StaffAdapter(List<EventStaff> staffList, OnStaffInteractionListener listener) {
        this.staffList = staffList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_staff_row, parent, false);
        return new StaffViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StaffViewHolder holder, int position) {
        EventStaff staff = staffList.get(position);

        holder.tvStaffName.setText(staff.getStaffName() != null ? staff.getStaffName() : "Unknown User");
        holder.tvStaffEmail.setText(staff.getStaffEmail() != null ? staff.getStaffEmail() : "No email");
        
        holder.tvRoleBadge.setText(staff.getRole() == EventStaff.Role.MANAGER ? "Quản lý" : "Soát vé");
        
        // Cập nhật giao diện theo role
        if (staff.getRole() == EventStaff.Role.MANAGER) {
            holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_status_badge_draft);
            holder.tvRoleBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_primary));
        } else {
            holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_status_badge_draft);
            holder.tvRoleBadge.setTextColor(holder.itemView.getContext().getColor(R.color.clr_warning));
        }

        holder.btnStaffOptions.setOnClickListener(v -> listener.onOptionsClick(staff, v));
        
        // Hiện tại EventStaff chưa có field isActive, ta giả sử luôn true hoặc thêm logic sau
        holder.swStaffActive.setChecked(true);
        holder.swStaffActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            listener.onStatusChange(staff, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return staffList.size();
    }

    static class StaffViewHolder extends RecyclerView.ViewHolder {
        TextView tvStaffName, tvStaffEmail, tvRoleBadge;
        ImageView btnStaffOptions;
        MaterialSwitch swStaffActive;

        public StaffViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStaffName = itemView.findViewById(R.id.tvStaffName);
            tvStaffEmail = itemView.findViewById(R.id.tvStaffEmail);
            tvRoleBadge = itemView.findViewById(R.id.tvRoleBadge);
            btnStaffOptions = itemView.findViewById(R.id.btnStaffOptions);
            swStaffActive = itemView.findViewById(R.id.swStaffActive);
        }
    }
}
