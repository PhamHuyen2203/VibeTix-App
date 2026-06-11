package com.example.vibetix.Adapters.Organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Models.EventStaff;
import com.example.vibetix.R;
import com.example.vibetix.databinding.ItemStaffRowBinding;

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
        ItemStaffRowBinding binding = ItemStaffRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StaffViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StaffViewHolder holder, int position) {
        EventStaff staff = staffList.get(position);
        ItemStaffRowBinding b = holder.binding;

        b.tvStaffName.setText(staff.getStaffName() != null ? staff.getStaffName() : "Tài khoản VibeTix");
        b.tvStaffEmail.setText(staff.getStaffEmail() != null ? staff.getStaffEmail() : "No email");
        
        b.tvRoleBadge.setText(staff.getRole() == EventStaff.Role.MANAGER ? "Quản lý" : "Soát vé");
        
        // Cập nhật giao diện theo role
        if (staff.getRole() == EventStaff.Role.MANAGER) {
            b.tvRoleBadge.setBackgroundResource(R.drawable.bg_role_badge_manager);
            b.tvRoleBadge.setTextColor(b.getRoot().getContext().getColor(R.color.clr_primary));
        } else {
            b.tvRoleBadge.setBackgroundResource(R.drawable.bg_status_badge_draft);
            b.tvRoleBadge.setTextColor(b.getRoot().getContext().getColor(R.color.clr_warning));
        }

        b.btnStaffOptions.setVisibility(View.GONE); // Ẩn nút 3 chấm vì chỉ dùng switch bật/tắt
        
        // Remove listener temporarily to avoid triggering when setting state programmatically
        b.swStaffActive.setOnCheckedChangeListener(null);
        
        b.swStaffActive.setChecked(staff.isActive());
        b.swStaffActive.setText(staff.isActive() ? "Đang bật" : "Tạm ngưng");
        if (!staff.isActive()) {
            b.ivStaffAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(b.getRoot().getContext().getColor(R.color.clr_grey_1)));
            b.tvStaffName.setTextColor(b.getRoot().getContext().getColor(R.color.clr_grey_1));
            b.swStaffActive.setTextColor(b.getRoot().getContext().getColor(R.color.clr_error));
        } else {
            b.ivStaffAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(b.getRoot().getContext().getColor(R.color.clr_primary)));
            b.tvStaffName.setTextColor(b.getRoot().getContext().getColor(R.color.clr_text_black));
            b.swStaffActive.setTextColor(b.getRoot().getContext().getColor(R.color.clr_text_black));
        }

        b.swStaffActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            listener.onStatusChange(staff, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return staffList.size();
    }

    static class StaffViewHolder extends RecyclerView.ViewHolder {
        final ItemStaffRowBinding binding;

        public StaffViewHolder(@NonNull ItemStaffRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
