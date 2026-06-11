package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.Exclude;

public class Category {
    private String categoryId;
    private String name;
    private String slug;
    private String iconUrl;
    private boolean isActive = true;

    public Category() {}

    @PropertyName("category_id")
    public String getCategoryId() { return categoryId; }
    @PropertyName("category_id")
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    @PropertyName("icon_url")
    public String getIconUrl() { return iconUrl; }
    @PropertyName("icon_url")
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    @PropertyName("is_active")
    public boolean isActive() { return isActive; }
    @PropertyName("is_active")
    public void setActive(boolean active) { isActive = active; }

    // Mock fields
    private int iconResId;
    private int iconBgColorResId;

    public Category(String id, String name, String slug, int iconResId, int iconBgColorResId) {
        this.categoryId = id;
        this.name = name;
        this.slug = slug;
        this.iconResId = iconResId;
        this.iconBgColorResId = iconBgColorResId;
    }

    @Exclude
    public int getIconResId() { return iconResId; }
    @Exclude
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }

    @Exclude
    public int getIconBgColorResId() { return iconBgColorResId; }
    @Exclude
    public void setIconBgColorResId(int iconBgColorResId) { this.iconBgColorResId = iconBgColorResId; }
}
