package com.example.vibetix.Adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibetix.Models.Category;
import com.example.vibetix.R;

import java.util.ArrayList;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    Context context;
    ArrayList<Category> danhSachCategory;
    OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(Context context, ArrayList<Category> danhSachCategory, OnCategoryClickListener listener) {
        this.context = context;
        this.danhSachCategory = danhSachCategory;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_chip, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = danhSachCategory.get(position);
        holder.txtCategoryName.setText(category.getName());

        if (category.getIconResId() != 0) {
            holder.imvCategoryIcon.setImageResource(category.getIconResId());
        }

        if (category.getIconBgColorResId() != 0) {
            GradientDrawable bg = (GradientDrawable) ContextCompat
                    .getDrawable(context, R.drawable.bg_category_icon).mutate();
            bg.setColor(ContextCompat.getColor(context, category.getIconBgColorResId()));
            holder.imvCategoryIcon.setBackground(bg);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return danhSachCategory.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imvCategoryIcon;
        TextView txtCategoryName;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imvCategoryIcon = itemView.findViewById(R.id.imvCategoryIcon);
            txtCategoryName = itemView.findViewById(R.id.txtCategoryName);
        }
    }
}
