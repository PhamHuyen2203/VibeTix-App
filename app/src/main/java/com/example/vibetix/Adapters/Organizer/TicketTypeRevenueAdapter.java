package com.example.vibetix.Adapters.Organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TicketTypeRevenueAdapter extends RecyclerView.Adapter<TicketTypeRevenueAdapter.VH> {

    public static class TicketTypeStat {
        public String name;
        public long pricePerTicket;
        public long soldQuantity;
        public long revenue;

        public TicketTypeStat(String name, long pricePerTicket, long soldQuantity, long revenue) {
            this.name = name;
            this.pricePerTicket = pricePerTicket;
            this.soldQuantity = soldQuantity;
            this.revenue = revenue;
        }
    }

    private final List<TicketTypeStat> items;
    private final NumberFormat vndFmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    public TicketTypeRevenueAdapter(List<TicketTypeStat> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket_type_revenue, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TicketTypeStat stat = items.get(position);
        h.tvName.setText(stat.name != null ? stat.name : "—");
        h.tvPrice.setText(vndFmt.format(stat.pricePerTicket) + " ₫ / vé");
        h.tvSold.setText(stat.soldQuantity + " vé");
        h.tvRevenue.setText(formatRevenue(stat.revenue));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void updateData(List<TicketTypeStat> newItems) {
        if (items != newItems) {
            items.clear();
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    private String formatRevenue(long amount) {
        if (amount == 0) return "0đ";
        if (amount >= 1_000_000_000) return String.format(Locale.getDefault(), "%.1fB₫", amount / 1_000_000_000.0);
        if (amount >= 1_000_000) return String.format(Locale.getDefault(), "%.1fM₫", amount / 1_000_000.0);
        if (amount >= 1_000) return String.format(Locale.getDefault(), "%.0fK₫", amount / 1_000.0);
        return new DecimalFormat("#,###").format(amount) + "đ";
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvSold, tvRevenue;

        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvTicketTypeName);
            tvPrice = v.findViewById(R.id.tvTicketTypePrice);
            tvSold = v.findViewById(R.id.tvTicketTypeSold);
            tvRevenue = v.findViewById(R.id.tvTicketTypeRevenue);
        }
    }
}
