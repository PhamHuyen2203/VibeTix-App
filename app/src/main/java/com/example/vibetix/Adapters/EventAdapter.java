package com.example.vibetix.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    Context context;
    ArrayList<Event> danhSachEvent;
    OnEventClickListener listener;
    private int layoutResId;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventAdapter(Context context, ArrayList<Event> danhSachEvent, OnEventClickListener listener) {
        this(context, danhSachEvent, listener, R.layout.item_event_card);
    }

    public EventAdapter(Context context, ArrayList<Event> danhSachEvent,
                        OnEventClickListener listener, int layoutResId) {
        this.context       = context;
        this.danhSachEvent = danhSachEvent;
        this.listener      = listener;
        this.layoutResId   = layoutResId;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = danhSachEvent.get(position);

        holder.txtEventTitle.setText(event.getTitle());
        holder.txtEventDate.setText(event.getDate());

        if (event.isFree()) {
            holder.txtEventPrice.setText(context.getString(R.string.str_free));
        } else {
            NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            String priceFormatted = fmt.format(event.getMinPrice()) + "đ";
            holder.txtEventPrice.setText(
                    context.getString(R.string.str_from_price, priceFormatted));
        }

        if (event.isSoldOut()) {
            holder.txtSoldOutBadge.setVisibility(View.VISIBLE);
        } else {
            holder.txtSoldOutBadge.setVisibility(View.GONE);
        }

        // Ongoing badge (ERD: EVENT.status = 'ongoing')
        if ("ongoing".equals(event.getStatus())) {
            holder.txtOngoingBadge.setVisibility(View.VISIBLE);
        } else {
            holder.txtOngoingBadge.setVisibility(View.GONE);
        }

        // Venue city (ERD: VENUE.city)
        if (event.getVenueCity() != null && !event.getVenueCity().isEmpty()) {
            holder.txtEventCity.setText(event.getVenueCity());
            holder.txtEventCity.setVisibility(View.VISIBLE);
        } else {
            holder.txtEventCity.setVisibility(View.GONE);
        }

        Object imageSource = event.getLocalImageResId() != 0
                ? event.getLocalImageResId()
                : event.getImageUrl();

        Glide.with(context)
                .load(imageSource)
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imvEventImage);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return danhSachEvent.size();
    }

    public void updateData(ArrayList<Event> data) {
        danhSachEvent.clear();
        danhSachEvent.addAll(data);
        notifyDataSetChanged();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView imvEventImage;
        TextView txtEventTitle;
        TextView txtEventDate;
        TextView txtEventCity;
        TextView txtEventPrice;
        TextView txtSoldOutBadge;
        TextView txtOngoingBadge;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            imvEventImage = itemView.findViewById(R.id.imvEventImage);
            txtEventTitle = itemView.findViewById(R.id.txtEventTitle);
            txtEventDate = itemView.findViewById(R.id.txtEventDate);
            txtEventCity = itemView.findViewById(R.id.txtEventCity);
            txtEventPrice = itemView.findViewById(R.id.txtEventPrice);
            txtSoldOutBadge = itemView.findViewById(R.id.txtSoldOutBadge);
            txtOngoingBadge = itemView.findViewById(R.id.txtOngoingBadge);
        }
    }
}
