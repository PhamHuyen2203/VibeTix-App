package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Discount {
    @PropertyName("discount_id")
    private String id;
    
    @PropertyName("code")
    private String code;
    
    @PropertyName("title")
    private String title;
    
    @PropertyName("description")
    private String description;
    
    @PropertyName("type")
    private String type;         // "percentage" or "fixed"
    
    @PropertyName("value")
    private double value;
    
    @PropertyName("max_discount")
    private double maxDiscount;
    
    @PropertyName("min_order_value")
    private double minOrderValue;
    
    @PropertyName("start_date")
    private com.google.firebase.Timestamp startDate;
    
    @PropertyName("expiry_date")
    private com.google.firebase.Timestamp expiryDate;
    
    @PropertyName("creator_type")
    private String creatorType;  // "admin" or "organizer"
    
    @PropertyName("scope")
    private String scope;        // "global" or "event"
    
    @PropertyName("event_id")
    private String eventId;      // Null for global
    
    @PropertyName("is_active")
    private boolean isActive;
    
    @PropertyName("usage_limit")
    private long usageLimit;
    
    @PropertyName("usage_per_user")
    private long usagePerUser;
    
    @PropertyName("used_count")
    private long usedCount;
    
    @PropertyName("created_by")
    private String createdBy;

    public Discount() {
        this.id = java.util.UUID.randomUUID().toString();
    }

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

    public com.google.firebase.Timestamp getStartDate() { return startDate; }
    public void setStartDate(com.google.firebase.Timestamp startDate) { this.startDate = startDate; }

    public com.google.firebase.Timestamp getExpiryDate() { return expiryDate; }
    public void setExpiryDate(com.google.firebase.Timestamp expiryDate) { this.expiryDate = expiryDate; }

    public String getCreatorType() { return creatorType; }
    public void setCreatorType(String creatorType) { this.creatorType = creatorType; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getUsageLimit() { return usageLimit; }
    public void setUsageLimit(long usageLimit) { this.usageLimit = usageLimit; }

    public long getUsagePerUser() { return usagePerUser; }
    public void setUsagePerUser(long usagePerUser) { this.usagePerUser = usagePerUser; }

    public long getUsedCount() { return usedCount; }
    public void setUsedCount(long usedCount) { this.usedCount = usedCount; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
