package com.example.vibetix.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.R;

import java.util.List;

/**
 * Adapter gợi ý nhanh (autocomplete dropdown) — hiện title sự kiện khi đang gõ.
 */
public class AutoSuggestAdapter extends RecyclerView.Adapter<AutoSuggestAdapter.VH> {

    public interface OnSuggestionClick {
        void onClick(String title, String eventId);
    }

    private final List<String[]> items; // [title, eventId]
    private final OnSuggestionClick listener;

    public AutoSuggestAdapter(List<String[]> items, OnSuggestionClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String[] item = items.get(position);
        h.text.setText(item[0]);
        h.text.setTextSize(14);
        h.text.setTextColor(0xFF808E92);
        h.text.setPadding(48, 28, 48, 28);
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item[0], item[1]);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView text;
        VH(@NonNull View v) {
            super(v);
            text = v.findViewById(android.R.id.text1);
        }
    }
}
