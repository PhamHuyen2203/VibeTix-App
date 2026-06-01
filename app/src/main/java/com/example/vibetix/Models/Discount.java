package com.example.vibetix.Models;

import java.util.UUID;

public class Discount {
    private String id;
    private String code;
    private String title;
    private String description;
    private String type; // "percentage" or "fixed"
    private double value;
    private double maxDiscount;
    private double minOrderValue;
    private long startDate; // Timestamp
    private long expiryDate; // Timestamp
    private String creatorType; // "admin" or "organizer"
    private String scope; // "global" or "event"
    private String eventId; // Null for global
    private boolean isActive;

    public Discount() {
        this.id = UUID.randomUUID().toString();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public double getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(double maxDiscount) { this.maxDiscount = maxDiscount; }

    public double getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(double minOrderValue) { this.minOrderValue = minOrderValue; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getExpiryDate() { return expiryDate; }
    public void setExpiryDate(long expiryDate) { this.expiryDate = expiryDate; }

    public String getCreatorType() { return creatorType; }
    public void setCreatorType(String creatorType) { this.creatorType = creatorType; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
