package com.example.vibetix.Adapters.Organizer;

import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vibetix.Models.OrderItem;
import com.example.vibetix.Models.Order;
import com.example.vibetix.R;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrganizerOrderAdapter extends RecyclerView.Adapter<OrganizerOrderAdapter.ViewHolder> {

    public static class OrderWrapper {
        public Order order;
        public List<OrderItem> items = new ArrayList<>();
        public boolean isExpanded = false;
        
        public long getTotalAmount() {
            long total = 0;
            for (OrderItem i : items) {
                total += i.getQuantity() * i.getPricePerTicket();
            }
            return total;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(OrderWrapper orderWrapper);
    }

    private List<OrderWrapper> list;
    private final SimpleDateFormat dispFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private OnItemClickListener listener;

    public OrganizerOrderAdapter(List<OrderWrapper> list) {
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
        OrderWrapper wrapper = list.get(position);
        Order o = wrapper.order;
        
        String id = o.getOrderId();
        if (id != null && id.length() > 8) {
            id = id.substring(0, 8).toUpperCase();
        }
        holder.tvOrderId.setText("#" + id);
        
        if (o.getOrderDate() != null) {
            holder.tvOrderDate.setText(formatDate(o.getOrderDate()));
        } else {
            holder.tvOrderDate.setText("");
        }

        holder.tvTotalAmount.setText(new DecimalFormat("#,###").format(wrapper.getTotalAmount()) + " ₫");

        String status = o.getStatusStr() != null ? o.getStatusStr() : "pending";
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
        
        holder.vDivider.setVisibility(wrapper.isExpanded ? View.VISIBLE : View.GONE);
        holder.llOrderItemsContainer.setVisibility(wrapper.isExpanded ? View.VISIBLE : View.GONE);
        holder.ivExpand.setRotation(wrapper.isExpanded ? 180 : 0);
        
        holder.llOrderHeader.setOnClickListener(v -> {
            wrapper.isExpanded = !wrapper.isExpanded;
            notifyItemChanged(position);
        });

        holder.llOrderItemsContainer.removeAllViews();
        for (OrderItem item : wrapper.items) {
            TextView tv = new TextView(holder.itemView.getContext());
            String ticketName = item.getTicketTypeName() != null ? item.getTicketTypeName() : "Vé không rõ";
            tv.setText(item.getQuantity() + "x " + ticketName + " - " + new DecimalFormat("#,###").format(item.getPricePerTicket()) + " ₫");
            tv.setTextColor(0xFF333333);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tv.setPadding(0, 8, 0, 8);
            holder.llOrderItemsContainer.addView(tv);
        }

        if (holder.btnConfirmOrder != null) {
            if ("pending".equalsIgnoreCase(status) && "transfer".equalsIgnoreCase(o.getPaymentMethod())) {
                holder.btnConfirmOrder.setVisibility(View.VISIBLE);
                holder.llOrderItemsContainer.addView(holder.btnConfirmOrder);
                holder.btnConfirmOrder.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClick(wrapper);
                    }
                });
            } else {
                holder.btnConfirmOrder.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void updateData(List<OrderWrapper> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    private String formatDate(Object d) {
        if (d instanceof java.util.Date) {
            return dispFmt.format((java.util.Date) d);
        } else if (d instanceof com.google.firebase.Timestamp) {
            return dispFmt.format(((com.google.firebase.Timestamp) d).toDate());
        }
        return d.toString();
    }

    private void setRoundedBadgeBg(TextView tv, int colorARGB) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(colorARGB);
        gd.setCornerRadius(tv.getContext().getResources().getDisplayMetrics().density * 6);
        tv.setBackground(gd);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llOrderHeader, llOrderItemsContainer;
        TextView tvOrderId, tvOrderDate, tvStatus, tvTotalAmount;
        ImageView ivExpand;
        View vDivider;
        com.google.android.material.button.MaterialButton btnConfirmOrder;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            llOrderHeader = itemView.findViewById(R.id.llOrderHeader);
            llOrderItemsContainer = itemView.findViewById(R.id.llOrderItemsContainer);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            ivExpand = itemView.findViewById(R.id.ivExpand);
            vDivider = itemView.findViewById(R.id.vDivider);
            btnConfirmOrder = itemView.findViewById(R.id.btnConfirmOrder);
        }
    }
}
