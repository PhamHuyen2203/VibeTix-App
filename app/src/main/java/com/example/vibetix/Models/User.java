package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;

/**
 * User — Người dùng cơ bản.
 * Tất cả tài khoản đều bắt đầu là User.
 * User trở thành "organizer" khi đã tạo ít nhất 1 sự kiện.
 * 1 User có thể có nhiều Organizer profiles (1:N).
 */
public class User {
    private String userId;           // UUID v4 — trùng với Firebase Auth UID
    private String email;
    private String passwordHash;     // Chỉ lưu trên backend, không dùng trực tiếp
    private String fullName;
    private String phone;
    private String avatarUrl;
    private boolean isActive = true;
    private Object createdAt;
    private String defaultOrganizerId; // UUID của organizer profile mặc định (nullable)

    // Role dùng nội bộ để điều hướng (không lưu trên Firestore, gán sau login)
    // Giá trị: "customer", "admin" — không có "organizer" cố định
    private String role;

    public User() {}

    // --- Getters & Setters ---

    @PropertyName("user_id")
    public String getUserId() { return userId; }
    @PropertyName("user_id")
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @PropertyName("password_hash")
    public String getPasswordHash() { return passwordHash; }
    @PropertyName("password_hash")
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    @PropertyName("full_name")
    public String getFullName() { return fullName; }
    @PropertyName("full_name")
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @PropertyName("avatar_url")
    public String getAvatarUrl() { return avatarUrl; }
    @PropertyName("avatar_url")
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    @PropertyName("is_active")
    public boolean isActive() { return isActive; }
    @PropertyName("is_active")
    public void setActive(boolean active) { isActive = active; }

    @PropertyName("created_at")
    public Object getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }

    @PropertyName("default_organizer_id")
    public String getDefaultOrganizerId() { return defaultOrganizerId; }
    @PropertyName("default_organizer_id")
    public void setDefaultOrganizerId(String defaultOrganizerId) { this.defaultOrganizerId = defaultOrganizerId; }

    // App-only role, not persisted to Firestore
    @com.google.firebase.firestore.Exclude
    public String getRole() { return role; }
    @com.google.firebase.firestore.Exclude
    public void setRole(String role) { this.role = role; }
}
