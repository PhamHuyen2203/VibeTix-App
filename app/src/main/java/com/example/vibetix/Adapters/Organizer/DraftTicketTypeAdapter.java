package com.example.vibetix.Adapters.Organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Models.TicketType;
import com.example.vibetix.R;
import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;
import java.util.List;

public class DraftTicketTypeAdapter extends RecyclerView.Adapter<DraftTicketTypeAdapter.VH> {

    public interface Listener {
        void onEdit(int position, TicketType tt);
        void onDelete(int position, TicketType tt);
    }

    private final List<TicketType> items;
    private final Listener listener;
    private final DecimalFormat fmt = new DecimalFormat("#,###");

    public DraftTicketTypeAdapter(List<TicketType> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_draft_ticket_type, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TicketType tt = items.get(position);
        h.tvName.setText(tt.getName() != null ? tt.getName() : "");
        h.tvDetails.setText(fmt.format(tt.getPrice()) + " đ · " + tt.getTotalQuantity() + " vé");
        h.btnEdit.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID && listener != null)
                listener.onEdit(pos, items.get(pos));
        });
        h.btnDelete.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID && listener != null)
                listener.onDelete(pos, items.get(pos));
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails;
        MaterialButton btnEdit, btnDelete;
        VH(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvDraftTtName);
            tvDetails = v.findViewById(R.id.tvDraftTtDetails);
            btnEdit   = v.findViewById(R.id.btnDraftTtEdit);
            btnDelete = v.findViewById(R.id.btnDraftTtDelete);
        }
    }
}
