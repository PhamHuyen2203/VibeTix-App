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
import com.example.vibetix.Models.Destination;
import com.example.vibetix.R;

import java.util.ArrayList;

public class DestinationAdapter extends RecyclerView.Adapter<DestinationAdapter.DestinationViewHolder> {

    Context context;
    ArrayList<Destination> danhSachDestination;
    OnDestinationClickListener listener;

    public interface OnDestinationClickListener {
        void onDestinationClick(Destination destination);
    }

    public DestinationAdapter(Context context, ArrayList<Destination> danhSachDestination, OnDestinationClickListener listener) {
        this.context = context;
        this.danhSachDestination = danhSachDestination;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DestinationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_destination, parent, false);
        return new DestinationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DestinationViewHolder holder, int position) {
        Destination destination = danhSachDestination.get(position);
        holder.txtDestinationName.setText(destination.getName());
        Object imageSource = destination.getLocalImageResId() != 0
                ? destination.getLocalImageResId()
                : destination.getImageUrl();

        Glide.with(context)
                .load(imageSource)
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imvDestinationImage);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onDestinationClick(destination);
            }
        });
    }

    @Override
    public int getItemCount() {
        return danhSachDestination.size();
    }

    static class DestinationViewHolder extends RecyclerView.ViewHolder {
        ImageView imvDestinationImage;
        TextView txtDestinationName;

        DestinationViewHolder(@NonNull View itemView) {
            super(itemView);
            imvDestinationImage = itemView.findViewById(R.id.imvDestinationImage);
            txtDestinationName = itemView.findViewById(R.id.txtDestinationName);
        }
    }
}
