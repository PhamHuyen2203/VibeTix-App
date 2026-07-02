package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Discount {
    private String id;
    private String code;
    private String title;
    private String description;
    private String type;         // "percentage" or "fixed"
    private double value;
    private double maxDiscount;
    private double minOrderValue;
    private com.google.firebase.Timestamp startDate;
    private com.google.firebase.Timestamp expiryDate;
    private String creatorType;  // "admin" or "organizer"
    private String scope;        // "global" or "event"
    private String eventId;      // Null for global
    private boolean isActive;
    private long usageLimit;
    private long usagePerUser;
    private long usedCount;
    private String createdBy;

    public Discount() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    @PropertyName("discount_id")
    public String getId() { return id; }
    @PropertyName("discount_id")
    public void setId(String id) { this.id = id; }

    @PropertyName("code")
    public String getCode() { return code; }
    @PropertyName("code")
    public void setCode(String code) { this.code = code; }

    @PropertyName("title")
    public String getTitle() { return title; }
    @PropertyName("title")
    public void setTitle(String title) { this.title = title; }

    @PropertyName("description")
    public String getDescription() { return description; }
    @PropertyName("description")
    public void setDescription(String description) { this.description = description; }

    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }

    @PropertyName("value")
    public double getValue() { return value; }
    @PropertyName("value")
    public void setValue(double value) { this.value = value; }

    @PropertyName("max_discount")
    public double getMaxDiscount() { return maxDiscount; }
    @PropertyName("max_discount")
    public void setMaxDiscount(double maxDiscount) { this.maxDiscount = maxDiscount; }

    @PropertyName("min_order_value")
    public double getMinOrderValue() { return minOrderValue; }
    @PropertyName("min_order_value")
    public void setMinOrderValue(double minOrderValue) { this.minOrderValue = minOrderValue; }

    @PropertyName("start_date")
    public com.google.firebase.Timestamp getStartDate() { return startDate; }
    @PropertyName("start_date")
    public void setStartDate(com.google.firebase.Timestamp startDate) { this.startDate = startDate; }

    @PropertyName("expiry_date")
    public com.google.firebase.Timestamp getExpiryDate() { return expiryDate; }
    @PropertyName("expiry_date")
    public void setExpiryDate(com.google.firebase.Timestamp expiryDate) { this.expiryDate = expiryDate; }

    @PropertyName("creator_type")
    public String getCreatorType() { return creatorType; }
    @PropertyName("creator_type")
    public void setCreatorType(String creatorType) { this.creatorType = creatorType; }

    @PropertyName("scope")
    public String getScope() { return scope; }
    @PropertyName("scope")
    public void setScope(String scope) { this.scope = scope; }

    @PropertyName("event_id")
    public String getEventId() { return eventId; }
    @PropertyName("event_id")
    public void setEventId(String eventId) { this.eventId = eventId; }

    @PropertyName("is_active")
    public boolean isActive() { return isActive; }
    @PropertyName("is_active")
    public void setActive(boolean active) { isActive = active; }

    @PropertyName("usage_limit")
    public long getUsageLimit() { return usageLimit; }
    @PropertyName("usage_limit")
    public void setUsageLimit(long usageLimit) { this.usageLimit = usageLimit; }

    @PropertyName("usage_per_user")
    public long getUsagePerUser() { return usagePerUser; }
    @PropertyName("usage_per_user")
    public void setUsagePerUser(long usagePerUser) { this.usagePerUser = usagePerUser; }

    @PropertyName("used_count")
    public long getUsedCount() { return usedCount; }
    @PropertyName("used_count")
    public void setUsedCount(long usedCount) { this.usedCount = usedCount; }

    @PropertyName("created_by")
    public String getCreatedBy() { return createdBy; }
    @PropertyName("created_by")
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
