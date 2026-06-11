package com.example.vibetix.Adapters.Organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vibetix.Models.Discount;
import com.example.vibetix.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiscountAdapter extends RecyclerView.Adapter<DiscountAdapter.ViewHolder> {

    private List<Discount> list;

    public DiscountAdapter(List<Discount> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_discount, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Discount d = list.get(position);
        holder.tvCode.setText(d.getCode());
        holder.tvTitle.setText(d.getTitle());
        
        if ("percentage".equals(d.getType())) {
            holder.tvValue.setText(d.getValue() + "%");
        } else {
            holder.tvValue.setText(String.format("%,d đ", d.getValue()));
        }
        
        String limit = d.getUsageLimit() > 0 ? String.valueOf(d.getUsageLimit()) : "∞";
        holder.tvUsage.setText(d.getUsedCount() + "/" + limit);

        long now = System.currentTimeMillis();
        long startTime = 0;
        long expiryTime = 0;
        
        if (d.getStartDate() != null) startTime = d.getStartDate().toDate().getTime();
        if (d.getExpiryDate() != null) expiryTime = d.getExpiryDate().toDate().getTime();

        if (!d.isActive()) {
            holder.tvStatus.setText("Đã tắt");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.clr_error));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_ticket_type_inactive);
        } else if (startTime > 0 && now < startTime) {
            holder.tvStatus.setText("Sắp tới");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.clr_warning));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_ticket_type_inactive);
        } else if (expiryTime > 0 && now > expiryTime) {
            holder.tvStatus.setText("Hết hạn");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.clr_error));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_ticket_type_inactive);
        } else {
            holder.tvStatus.setText("Đang chạy");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.clr_success));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_ticket_type_active);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvExpiry.setText("Hết hạn: " + (expiryTime > 0 ? sdf.format(new Date(expiryTime)) : "N/A"));
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void updateData(List<Discount> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvTitle, tvValue, tvUsage, tvStatus, tvExpiry;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvValue = itemView.findViewById(R.id.tvValue);
            tvUsage = itemView.findViewById(R.id.tvUsage);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvExpiry = itemView.findViewById(R.id.tvExpiry);
        }
    }
}
