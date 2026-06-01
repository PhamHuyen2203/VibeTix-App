package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;

/**
 * Organizer — Hồ sơ Ban tổ chức.
 *
 * Quan hệ: 1 User : N Organizers.
 * Mỗi Organizer document được tạo khi user thêm hồ sơ BTC mới.
 * user_id = Firebase Auth UID của người tạo.
 */
public class Organizer {
    private String organizerId;  // UUID v4 — document ID trong Firestore
    private String userId;       // FK → users/{user_id}
    private String brandName;
    private String logoUrl;
    private String description;
    private String websiteUrl;
    private String contactEmail; // Email liên hệ BTC (có thể khác email user)
    private String contactPhone; // SĐT liên hệ BTC
    private boolean isVerified = false;
    private Object createdAt;

    public Organizer() {}

    public Organizer(String organizerId, String userId, String brandName,
                     String logoUrl, String description, String websiteUrl,
                     String contactEmail, String contactPhone) {
        this.organizerId = organizerId;
        this.userId = userId;
        this.brandName = brandName;
        this.logoUrl = logoUrl;
        this.description = description;
        this.websiteUrl = websiteUrl;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
    }

    @PropertyName("organizer_id")
    public String getOrganizerId() { return organizerId; }
    @PropertyName("organizer_id")
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    @PropertyName("user_id")
    public String getUserId() { return userId; }
    @PropertyName("user_id")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("brand_name")
    public String getBrandName() { return brandName; }
    @PropertyName("brand_name")
    public void setBrandName(String brandName) { this.brandName = brandName; }

    @PropertyName("logo_url")
    public String getLogoUrl() { return logoUrl; }
    @PropertyName("logo_url")
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @PropertyName("website_url")
    public String getWebsiteUrl() { return websiteUrl; }
    @PropertyName("website_url")
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    @PropertyName("contact_email")
    public String getContactEmail() { return contactEmail; }
    @PropertyName("contact_email")
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    @PropertyName("contact_phone")
    public String getContactPhone() { return contactPhone; }
    @PropertyName("contact_phone")
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    @PropertyName("is_verified")
    public boolean isVerified() { return isVerified; }
    @PropertyName("is_verified")
    public void setVerified(boolean verified) { this.isVerified = verified; }

    @PropertyName("created_at")
    public Object getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }

    // Helpers for UI backward compat (exclude from Firestore serialization)
    @com.google.firebase.firestore.Exclude
    public String getEmail() { return contactEmail; }
    @com.google.firebase.firestore.Exclude
    public String getPhone() { return contactPhone; }
    @com.google.firebase.firestore.Exclude
    public String getWebsite() { return websiteUrl; }
}
