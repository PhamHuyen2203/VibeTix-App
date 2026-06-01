package com.example.vibetix.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.R;
import java.util.List;

public class OrganizerAdapter extends RecyclerView.Adapter<OrganizerAdapter.OrganizerViewHolder> {

    private List<Organizer> organizerList;
    private OnOrganizerActionListener listener;

    public interface OnOrganizerActionListener {
        void onApprove(Organizer organizer);
        void onReject(Organizer organizer);
    }

    public OrganizerAdapter(List<Organizer> organizerList, OnOrganizerActionListener listener) {
        this.organizerList = organizerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrganizerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_organizer, parent, false);
        return new OrganizerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrganizerViewHolder holder, int position) {
        Organizer organizer = organizerList.get(position);
        holder.bind(organizer, listener);
    }

    @Override
    public int getItemCount() {
        return organizerList != null ? organizerList.size() : 0;
    }

    public void updateList(List<Organizer> newList) {
        this.organizerList = newList;
        notifyDataSetChanged();
    }

    public static class OrganizerViewHolder extends RecyclerView.ViewHolder {
        private TextView tvBrandName, tvEmail, tvPhone, tvWebsite;
        private Button btnApprove, btnReject;
        private View layoutActions;

        public OrganizerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBrandName = itemView.findViewById(R.id.tv_brand_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvPhone = itemView.findViewById(R.id.tv_phone);
            tvWebsite = itemView.findViewById(R.id.tv_website);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
            layoutActions = itemView.findViewById(R.id.layout_actions);
        }

        public void bind(final Organizer organizer, final OnOrganizerActionListener listener) {
            tvBrandName.setText(organizer.getBrandName());
            tvEmail.setText(organizer.getEmail());
            tvPhone.setText(organizer.getPhone());
            tvWebsite.setText(organizer.getWebsite());

            // Ẩn nút nếu đã xác minh
            if (organizer.isVerified()) {
                layoutActions.setVisibility(View.GONE);
            } else {
                layoutActions.setVisibility(View.VISIBLE);
            }

            btnApprove.setOnClickListener(v -> {
                if (listener != null) listener.onApprove(organizer);
            });

            btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(organizer);
            });
        }
    }
}
