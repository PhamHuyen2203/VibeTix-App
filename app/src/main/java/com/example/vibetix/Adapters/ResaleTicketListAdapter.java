package com.example.vibetix.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Models.TicketTransfer;
import com.example.vibetix.R;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Adapter cho danh sách vé bán lại của 1 sự kiện (card có nút "Mua lại").
 */
public class ResaleTicketListAdapter extends RecyclerView.Adapter<ResaleTicketListAdapter.ListingVH> {

    public interface OnBuyClickListener {
        void onBuyClick(TicketTransfer transfer);
    }

    private final Context context;
    private final List<TicketTransfer> listings;
    private final OnBuyClickListener listener;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public ResaleTicketListAdapter(Context context, List<TicketTransfer> listings, OnBuyClickListener listener) {
        this.context = context;
        this.listings = listings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ListingVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_resale_ticket_listing, parent, false);
        return new ListingVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingVH h, int position) {
        TicketTransfer t = listings.get(position);

        String typeName = t.getTicketTypeName();
        h.txtType.setText(typeName != null && !typeName.isEmpty() ? typeName : "Vé bán lại");
        h.txtMessage.setText(t.getMessage() != null && !t.getMessage().isEmpty()
                ? t.getMessage() : "Không có lời nhắn");
        h.txtPrice.setText(t.getPrice() > 0 ? formatter.format(t.getPrice()) + " đ" : "Thương lượng");
        h.txtQty.setText("Số lượng: 1");

        // Mã người bán rút gọn (4 ký tự cuối UID)
        String sid = t.getSenderId();
        String shortId = (sid != null && sid.length() >= 4) ? sid.substring(sid.length() - 4) : "****";
        h.txtSeller.setText("Người bán: " + shortId);

        h.btnBuy.setOnClickListener(v -> {
            if (listener != null) listener.onBuyClick(t);
        });
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    static class ListingVH extends RecyclerView.ViewHolder {
        TextView txtType, txtMessage, txtSeller, txtQty, txtPrice;
        Button btnBuy;

        ListingVH(@NonNull View itemView) {
            super(itemView);
            txtType = itemView.findViewById(R.id.txtListingType);
            txtMessage = itemView.findViewById(R.id.txtListingMessage);
            txtSeller = itemView.findViewById(R.id.txtListingSeller);
            txtQty = itemView.findViewById(R.id.txtListingQty);
            txtPrice = itemView.findViewById(R.id.txtListingPrice);
            btnBuy = itemView.findViewById(R.id.btnListingBuy);
        }
    }
}
