package com.example.vibetix.Models;

public class Category {
    private String id;
    private String name;
    private String slug;
    private int iconResId;
    private int iconBgColorResId;

    public Category() {}

    public Category(String id, String name, String slug, int iconResId, int iconBgColorResId) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.iconResId = iconResId;
        this.iconBgColorResId = iconBgColorResId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public int getIconResId() { return iconResId; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }

    public int getIconBgColorResId() { return iconBgColorResId; }
    public void setIconBgColorResId(int iconBgColorResId) { this.iconBgColorResId = iconBgColorResId; }
}
