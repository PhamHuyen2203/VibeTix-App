package com.example.vibetix.Models;

public class User {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String role;

    public User() {
        // Required for Firebase
    }

    public User(String id, String fullName, String email, String phone, String avatarUrl, String role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setRole(String role) {
        this.role = role;
    }
}