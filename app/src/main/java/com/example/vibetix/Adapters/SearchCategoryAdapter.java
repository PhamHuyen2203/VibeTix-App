package com.example.vibetix.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.R;

import java.util.List;

public class SearchCategoryAdapter extends RecyclerView.Adapter<SearchCategoryAdapter.ViewHolder> {

    public static class SearchCategoryItem {
        public String name;
        public String categoryId; // Firestore category_id UUID, "all" = tất cả
        public int    imageResId;

        public SearchCategoryItem(String name, int imageResId) {
            this.name       = name;
            this.categoryId = "all";
            this.imageResId = imageResId;
        }

        public SearchCategoryItem(String name, String categoryId, int imageResId) {
            this.name       = name;
            this.categoryId = categoryId;
            this.imageResId = imageResId;
        }
    }

    public interface OnCategoryClickListener {
        void onClick(SearchCategoryItem item);
    }

    private final Context context;
    private final List<SearchCategoryItem> items;
    private final OnCategoryClickListener listener;

    public SearchCategoryAdapter(Context context, List<SearchCategoryItem> items,
                                  OnCategoryClickListener listener) {
        this.context  = context;
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_search_category_banner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchCategoryItem item = items.get(position);
        holder.imvBanner.setImageResource(item.imageResId);
        holder.txtName.setText(item.name);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imvBanner;
        final TextView  txtName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imvBanner = itemView.findViewById(R.id.imvCategoryBanner);
            txtName   = itemView.findViewById(R.id.txtCategoryName);
        }
    }
}
