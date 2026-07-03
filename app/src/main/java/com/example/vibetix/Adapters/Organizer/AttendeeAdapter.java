package com.example.vibetix.Adapters.Organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vibetix.Models.UserTicket;
import com.example.vibetix.R;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import java.util.ArrayList;

public class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.ViewHolder> {

    public interface OnCheckInListener {
        void onCheckIn(UserTicket ticket);
    }

    public static class AttendeeGroup {
        public String userId;
        public String userName;
        public String userEmail;
        public List<UserTicket> tickets = new ArrayList<>();
        public boolean isExpanded = false;
    }

    private List<AttendeeGroup> list;
    private OnCheckInListener checkInListener;

    public AttendeeAdapter(List<AttendeeGroup> list, OnCheckInListener listener) {
        this.list = list;
        this.checkInListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendee, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendeeGroup group = list.get(position);
        
        holder.tvAttendeeName.setText(group.userName != null && !group.userName.isEmpty() ? group.userName : "Khách chưa rõ tên");
        holder.tvAttendeeEmail.setText(group.userEmail != null ? group.userEmail : "Không có email");
        holder.tvTicketCount.setText(group.tickets.size() + " vé");
        
        // Expand/Collapse logic
        holder.llTicketsContainer.setVisibility(group.isExpanded ? View.VISIBLE : View.GONE);
        holder.ivExpand.setRotation(group.isExpanded ? 180 : 0);
        
        holder.llHeader.setOnClickListener(v -> {
            group.isExpanded = !group.isExpanded;
            notifyItemChanged(position);
        });

        // Populate tickets
        holder.llTicketsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
        for (UserTicket t : group.tickets) {
            View ticketView = inflater.inflate(R.layout.item_attendee_ticket, holder.llTicketsContainer, false);
            
            TextView tvTicketType = ticketView.findViewById(R.id.tvTicketType);
            TextView tvTicketCode = ticketView.findViewById(R.id.tvTicketCode);
            TextView tvTicketStatus = ticketView.findViewById(R.id.tvTicketStatus);
            MaterialButton btnCheckIn = ticketView.findViewById(R.id.btnCheckIn);
            
            tvTicketType.setText(t.getTicketTypeName() != null ? t.getTicketTypeName() : "Vé mặc định");
            
            String displayCode = t.getDisplayCode();
            if (displayCode == null || displayCode.isEmpty()) {
                String id = t.getUserTicketId();
                displayCode = (id != null && id.length() > 8) ? id.substring(0, 8).toUpperCase() : id;
            }
            tvTicketCode.setText("#" + displayCode);

            boolean isUsed = UserTicket.Status.USED.equals(t.getStatus());
            if (isUsed) {
                tvTicketStatus.setText("Đã check-in ✓");
                tvTicketStatus.setTextColor(ticketView.getContext().getColor(R.color.clr_success));
                btnCheckIn.setVisibility(View.GONE);
            } else {
                tvTicketStatus.setText("Chưa check-in");
                tvTicketStatus.setTextColor(ticketView.getContext().getColor(R.color.clr_warning));
                btnCheckIn.setVisibility(View.VISIBLE);
                btnCheckIn.setOnClickListener(v -> {
                    if (checkInListener != null) {
                        checkInListener.onCheckIn(t);
                    }
                });
            }
            
            holder.llTicketsContainer.addView(ticketView);
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void updateData(List<AttendeeGroup> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llHeader, llTicketsContainer;
        TextView tvAttendeeName, tvAttendeeEmail, tvTicketCount;
        ImageView ivExpand;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            llHeader = itemView.findViewById(R.id.llHeader);
            llTicketsContainer = itemView.findViewById(R.id.llTicketsContainer);
            tvAttendeeName = itemView.findViewById(R.id.tvAttendeeName);
            tvAttendeeEmail = itemView.findViewById(R.id.tvAttendeeEmail);
            tvTicketCount = itemView.findViewById(R.id.tvTicketCount);
            ivExpand = itemView.findViewById(R.id.ivExpand);
        }
    }
}
