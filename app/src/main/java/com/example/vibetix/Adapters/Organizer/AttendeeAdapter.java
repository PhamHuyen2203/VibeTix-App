package com.example.vibetix.Adapters.Organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vibetix.Models.UserTicket;
import com.example.vibetix.R;
import java.util.List;

public class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.ViewHolder> {

    private List<UserTicket> list;

    public AttendeeAdapter(List<UserTicket> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendee, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserTicket t = list.get(position);
        
        holder.tvAttendeeName.setText(t.getFullName() != null && !t.getFullName().isEmpty() ? t.getFullName() : "Khách chưa rõ tên");
        holder.tvAttendeeEmail.setText(t.getEmail() != null ? t.getEmail() : "Không có email");
        
        holder.tvTicketType.setText(t.getTicketTypeName() != null ? t.getTicketTypeName() : "Vé mặc định");
        
        String id = t.getUserTicketId();
        if (id != null && id.length() > 8) {
            id = id.substring(0, 8).toUpperCase();
        }
        holder.tvTicketCode.setText("Mã vé: #" + id);

        if (UserTicket.Status.USED.equals(t.getStatus())) {
            holder.tvTicketStatus.setText("Đã check-in");
            holder.tvTicketStatus.setTextColor(holder.itemView.getContext().getColor(R.color.clr_success));
            holder.tvTicketStatus.setBackgroundResource(R.drawable.bg_ticket_type_active);
        } else {
            holder.tvTicketStatus.setText("Chưa check-in");
            holder.tvTicketStatus.setTextColor(holder.itemView.getContext().getColor(R.color.clr_warning));
            holder.tvTicketStatus.setBackgroundResource(R.drawable.bg_ticket_type_inactive);
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void updateData(List<UserTicket> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAttendeeName, tvAttendeeEmail, tvTicketStatus, tvTicketType, tvTicketCode;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAttendeeName = itemView.findViewById(R.id.tvAttendeeName);
            tvAttendeeEmail = itemView.findViewById(R.id.tvAttendeeEmail);
            tvTicketStatus = itemView.findViewById(R.id.tvTicketStatus);
            tvTicketType = itemView.findViewById(R.id.tvTicketType);
            tvTicketCode = itemView.findViewById(R.id.tvTicketCode);
        }
    }
}
