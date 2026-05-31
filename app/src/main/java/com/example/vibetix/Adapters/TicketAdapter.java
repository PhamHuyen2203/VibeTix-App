package com.example.vibetix.Adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Ticket;
import com.example.vibetix.R;

import java.text.DecimalFormat;
import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private final Context context;
    private final List<Ticket> ticketsList;
    private final OnTicketClickListener listener;

    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public interface OnTicketClickListener {
        void onQrClick(Ticket ticket);
        void onResellClick(Ticket ticket);
    }

    public TicketAdapter(Context context, List<Ticket> ticketsList, OnTicketClickListener listener) {
        this.context = context;
        this.ticketsList = ticketsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_purchased_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = ticketsList.get(position);

        holder.txtTicketEventTitle.setText(ticket.getEventTitle());
        holder.txtTicketEventDate.setText(ticket.getEventDate());
        holder.txtTicketEventLocation.setText(ticket.getEventLocation());
        holder.txtTicketTypeName.setText(ticket.getTicketTypeName());

        // Display correct image based on event ID
        int coverResId = R.drawable.event_live_non_song;
        if (!"b1".equals(ticket.getEventId()) && !"e1".equals(ticket.getEventId()) && !"rs1".equals(ticket.getEventId())) {
            coverResId = R.drawable.event_arts_private_fantasy;
        }

        Glide.with(context)
                .load(coverResId)
                .into(holder.imvTicketThumb);

        // Bind status and pricing details
        String status = ticket.getStatus();
        holder.btnTicketResellAction.setVisibility(View.VISIBLE);
        holder.btnTicketQrAction.setVisibility(View.VISIBLE);

        if ("ACTIVE".equalsIgnoreCase(status)) {
            holder.txtTicketStatusBadge.setText("Còn hiệu lực");
            holder.txtTicketStatusBadge.setTextColor(context.getResources().getColor(R.color.clr_success));
            holder.txtTicketStatusBadge.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.clr_bg_section)));
            holder.txtTicketPrice.setText(formatter.format(ticket.getPurchasePrice()) + " đ");

            holder.btnTicketResellAction.setText("Bán lại vé");
            holder.btnTicketResellAction.setEnabled(true);
        } else if ("RESELLING".equalsIgnoreCase(status)) {
            holder.txtTicketStatusBadge.setText("Đang bán lại");
            holder.txtTicketStatusBadge.setTextColor(context.getResources().getColor(R.color.clr_primary_blue));
            holder.txtTicketStatusBadge.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.clr_bg_section)));
            holder.txtTicketPrice.setText(formatter.format(ticket.getResalePrice()) + " đ");

            holder.btnTicketResellAction.setText("Hủy bán");
            holder.btnTicketResellAction.setEnabled(true);
        } else if ("USED".equalsIgnoreCase(status)) {
            holder.txtTicketStatusBadge.setText("Đã sử dụng");
            holder.txtTicketStatusBadge.setTextColor(context.getResources().getColor(R.color.clr_grey_1));
            holder.txtTicketStatusBadge.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.clr_divider)));
            holder.txtTicketPrice.setText(formatter.format(ticket.getPurchasePrice()) + " đ");

            holder.btnTicketResellAction.setVisibility(View.GONE);
            holder.btnTicketQrAction.setVisibility(View.GONE);
        } else if ("EXPIRED".equalsIgnoreCase(status)) {
            holder.txtTicketStatusBadge.setText("Hết hạn");
            holder.txtTicketStatusBadge.setTextColor(context.getResources().getColor(R.color.clr_error));
            holder.txtTicketStatusBadge.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.clr_divider)));
            holder.txtTicketPrice.setText(formatter.format(ticket.getPurchasePrice()) + " đ");

            holder.btnTicketResellAction.setVisibility(View.GONE);
            holder.btnTicketQrAction.setVisibility(View.GONE);
        }

        // Set action triggers
        holder.btnTicketQrAction.setOnClickListener(v -> listener.onQrClick(ticket));
        holder.btnTicketResellAction.setOnClickListener(v -> listener.onResellClick(ticket));
    }

    @Override
    public int getItemCount() {
        return ticketsList.size();
    }

    public static class TicketViewHolder extends RecyclerView.ViewHolder {
        ImageView imvTicketThumb;
        TextView txtTicketEventTitle;
        TextView txtTicketEventDate;
        TextView txtTicketEventLocation;
        TextView txtTicketTypeName;
        TextView txtTicketPrice;
        TextView txtTicketStatusBadge;
        Button btnTicketResellAction;
        Button btnTicketQrAction;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            imvTicketThumb = itemView.findViewById(R.id.imvTicketThumb);
            txtTicketEventTitle = itemView.findViewById(R.id.txtTicketEventTitle);
            txtTicketEventDate = itemView.findViewById(R.id.txtTicketEventDate);
            txtTicketEventLocation = itemView.findViewById(R.id.txtTicketEventLocation);
            txtTicketTypeName = itemView.findViewById(R.id.txtTicketTypeName);
            txtTicketPrice = itemView.findViewById(R.id.txtTicketPrice);
            txtTicketStatusBadge = itemView.findViewById(R.id.txtTicketStatusBadge);
            btnTicketResellAction = itemView.findViewById(R.id.btnTicketResellAction);
            btnTicketQrAction = itemView.findViewById(R.id.btnTicketQrAction);
        }
    }
}
