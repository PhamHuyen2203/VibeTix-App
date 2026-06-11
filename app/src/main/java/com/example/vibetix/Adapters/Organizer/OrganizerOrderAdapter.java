package com.example.vibetix.Adapters.Organizer;

import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vibetix.Models.OrderItem;
import com.example.vibetix.Models.Order;
import com.example.vibetix.R;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrganizerOrderAdapter extends RecyclerView.Adapter<OrganizerOrderAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(OrderItem orderItem);
    }

    private List<OrderItem> list;
    private final SimpleDateFormat dispFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private OnItemClickListener listener;

    public OrganizerOrderAdapter(List<OrderItem> list) {
        this.list = list;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_organizer, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = list.get(position);
        Order o = item.getParentOrder();
        
        String id = item.getOrderId();
        if (id != null && id.length() > 8) {
            id = id.substring(0, 8).toUpperCase();
        }
        holder.tvOrderId.setText("#" + id);
        
        if (o != null && o.getOrderDate() != null) {
            holder.tvOrderDate.setText(formatDate(o.getOrderDate()));
        } else {
            holder.tvOrderDate.setText("");
        }

        long totalAmount = item.getQuantity() * item.getPricePerTicket();
        holder.tvTotalAmount.setText(new DecimalFormat("#,###").format(totalAmount) + " ₫");

        String ticketName = item.getTicketTypeName() != null ? item.getTicketTypeName() : "Vé không rõ";
        holder.tvTicketInfo.setText(item.getQuantity() + "x " + ticketName);

        String status = (o != null && o.getStatusStr() != null) ? o.getStatusStr() : "pending";
        
        switch (status.toLowerCase()) {
            case "completed":
            case "confirmed":
            case "paid":
                holder.tvStatus.setText("Thành công");
                holder.tvStatus.setTextColor(0xFF27AE60);
                setRoundedBadgeBg(holder.tvStatus, 0x1A27AE60);
                break;
            case "cancelled":
                holder.tvStatus.setText("Đã hủy");
                holder.tvStatus.setTextColor(0xFFEB5757);
                setRoundedBadgeBg(holder.tvStatus, 0x1AEB5757);
                break;
            case "refunded":
                holder.tvStatus.setText("Hoàn tiền");
                holder.tvStatus.setTextColor(0xFFE6B93E);
                setRoundedBadgeBg(holder.tvStatus, 0x1AE6B93E);
                break;
            default:
                holder.tvStatus.setText("Chờ xử lý");
                holder.tvStatus.setTextColor(0xFF226CEB);
                setRoundedBadgeBg(holder.tvStatus, 0x1A226CEB);
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    private String formatDate(com.google.firebase.Timestamp ts) {
        if (ts == null) return "";
        try {
            return dispFmt.format(ts.toDate());
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    /**
     * Tạo badge nền bo tròn cho TextView trạng thái đơn hàng.
     * Dùng GradientDrawable thay vì setBackgroundColor để tạo corner radius.
     */
    private void setRoundedBadgeBg(TextView view, int bgColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8f,
                view.getContext().getResources().getDisplayMetrics()));
        bg.setColor(bgColor);
        view.setBackground(bg);
    }

    public void updateData(List<OrderItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvStatus, tvOrderDate, tvTotalAmount, tvTicketInfo;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId     = itemView.findViewById(R.id.tvOrderId);
            tvStatus      = itemView.findViewById(R.id.tvStatus);
            tvOrderDate   = itemView.findViewById(R.id.tvOrderDate);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvTicketInfo  = itemView.findViewById(R.id.tvTicketInfo);
        }
    }
}
