package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Event {
    public enum Status {
        DRAFT, PENDING, APPROVED, ONGOING, COMPLETED, CANCELLED
    }

    public static final String APPROVED = "approved";
    public static final String ONGOING = "ongoing";
    public static final String PENDING = "pending";
    public static final String CANCELLED = "cancelled";

    private String eventId;
    private String organizerId;
    private String userId;
    private String categoryId;
    private String venueId;
    private String venueName;
    private String venueAddress;
    private String venueCity;
    private String title;
    private String description;
    private String posterUrl;
    private String bannerUrl;
    private Object startTime;
    private Object endTime;
    private String statusStr;
    private boolean isFeatured = false;
    private int interestCount = 0;
    private double minPrice = 0;
    private Double maxPrice = null;
    private Object createdAt;
    private Object updatedAt;
    private long ticketsSold = 0;

    private int localImageResId;
    private int localPortraitImageResId;
    @Exclude private String userRole;
    private boolean isSoldOut;
    private String date;
    private String location;
    private String category;
    private boolean isFree;
    private String portraitImageUrl;
    private String organizerName;
    private int price;
    private String status;

    public Event() {}

    public Event(String id, String title, String imageUrl, String date,
                 String location, String category, long minPrice) {
        this.eventId = id;
        this.title = title;
        this.posterUrl = imageUrl;
        this.date = date;
        this.location = location;
        this.category = category;
        this.minPrice = minPrice;
    }

    @Exclude public String getId() { return eventId; }
    @Exclude public void setId(String id) { this.eventId = id; }
    
    @PropertyName("event_id") public String getEventId() { return eventId; }
    @PropertyName("event_id") public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @Exclude public String getImageUrl() { return posterUrl; }
    @Exclude public void setImageUrl(String imageUrl) { this.posterUrl = imageUrl; }

    @PropertyName("poster_url") public String getPosterUrl() { return posterUrl; }
    @PropertyName("poster_url") public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getDate() { return date != null ? date : getStartTime(); }
    public void setDate(String date) { this.date = date; }

    public String getLocation() { return location != null ? location : venueAddress; }
    public void setLocation(String location) { this.location = location; }

    @PropertyName("organizer_id") public String getOrganizerId() { return organizerId; }
    @PropertyName("organizer_id") public void setOrganizerId(String v) { organizerId = v; }
    
    @PropertyName("user_id") public String getUserId() { return userId; }
    @PropertyName("user_id") public void setUserId(String userId) { this.userId = userId; }

    @Exclude public String getCategory() { return category != null ? category : categoryId; }
    @Exclude public void setCategory(String category) { this.category = category; }
    
    @PropertyName("category_id") public String getCategoryId() { return categoryId; }
    @PropertyName("category_id") public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    @PropertyName("venue_id") public String getVenueId() { return venueId; }
    @PropertyName("venue_id") public void setVenueId(String venueId) { this.venueId = venueId; }

    public long getMinPrice() { return (long) minPrice; }
    public void setMinPrice(long minPrice) { this.minPrice = minPrice; }
    @PropertyName("min_price") public void setMinPriceDouble(double v) { this.minPrice = v; }

    public boolean isFree() { return isFree || (price == 0 && minPrice == 0); }
    public void setFree(boolean free) { isFree = free; }

    public boolean isSoldOut() { return isSoldOut; }
    public void setSoldOut(boolean soldOut) { isSoldOut = soldOut; }

    @PropertyName("venue_name") public String getVenueName() { return venueName; }
    @PropertyName("venue_name") public void setVenueName(String v) { venueName = v; }

    @PropertyName("venue_address") public String getVenueAddress() { return venueAddress; }
    @PropertyName("venue_address") public void setVenueAddress(String v) { venueAddress = v; }

    @PropertyName("venue_city") public String getVenueCity() { return venueCity; }
    @PropertyName("venue_city") public void setVenueCity(String v) { venueCity = v; }

    public int getLocalImageResId() { return localImageResId; }
    public void setLocalImageResId(int localImageResId) { this.localImageResId = localImageResId; }

    public int getLocalPortraitImageResId() { return localPortraitImageResId; }
    public void setLocalPortraitImageResId(int localPortraitImageResId) { this.localPortraitImageResId = localPortraitImageResId; }

    @Exclude public String getPortraitImageUrl() { return posterUrl != null ? posterUrl : portraitImageUrl; }
    @Exclude public void setPortraitImageUrl(String v) { this.portraitImageUrl = v; }

    public String getOrganizerName() { return organizerName != null ? organizerName : organizerId; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    @PropertyName("interest_count") public int getInterestCount() { return interestCount; }
    @PropertyName("interest_count") public void setInterestCount(int interestCount) { this.interestCount = interestCount; }

    @PropertyName("is_featured") public boolean isFeatured() { return isFeatured; }
    @PropertyName("is_featured") public void setFeatured(boolean featured) { isFeatured = featured; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @PropertyName("start_time") public Object getStartTimeObject() { return startTime; }
    @PropertyName("start_time") public void setStartTimeObject(Object v) { startTime = v; }

    @Exclude public String getStartTime() {
        if (startTime instanceof String) return (String) startTime;
        if (startTime instanceof com.google.firebase.Timestamp) {
            return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                    .format(((com.google.firebase.Timestamp) startTime).toDate());
        }
        if (startTime != null) return startTime.toString();
        return null;
    }
    @Exclude public void setStartTime(String v) { startTime = v; }

    @PropertyName("end_time") public Object getEndTimeObject() { return endTime; }
    @PropertyName("end_time") public void setEndTimeObject(Object v) { endTime = v; }

    @Exclude public String getEndTime() {
        if (endTime instanceof String) return (String) endTime;
        if (endTime instanceof com.google.firebase.Timestamp) {
            return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                    .format(((com.google.firebase.Timestamp) endTime).toDate());
        }
        if (endTime != null) return endTime.toString();
        return null;
    }
    @Exclude public void setEndTime(String v) { endTime = v; }
    
    @Exclude public String getEndDate() { return getEndTime(); }
    @Exclude public void setEndDate(String v) { endTime = v; }

    @PropertyName("status") public String getStatusStr() { return statusStr != null ? statusStr : status; }
    @PropertyName("status") public void setStatusStr(String v) { statusStr = v; status = v; }

    @Exclude public Status getStatusEnum() {
        if (statusStr == null) return Status.DRAFT;
        try { return Status.valueOf(statusStr.toUpperCase()); }
        catch (IllegalArgumentException e) { return Status.DRAFT; }
    }
    @Exclude public void setStatus(Status s) {
        statusStr = s != null ? s.name().toLowerCase() : "draft";
        status = statusStr;
    }
    
    @Exclude public String getStatus() { return statusStr != null ? statusStr : status; }
    @Exclude public void setStatus(String s) { status = s; statusStr = s; }

    @PropertyName("max_price") public Double getMaxPrice() { return maxPrice; }
    @PropertyName("max_price") public void setMaxPrice(Double v) { maxPrice = v; }

    @PropertyName("created_at") public Object getCreatedAt() { return createdAt; }
    @PropertyName("created_at") public void setCreatedAt(Object v) { createdAt = v; }

    @PropertyName("updated_at") public Object getUpdatedAt() { return updatedAt; }
    @PropertyName("updated_at") public void setUpdatedAt(Object v) { updatedAt = v; }

    @PropertyName("tickets_sold") public long getTicketsSold() { return ticketsSold; }
    @PropertyName("tickets_sold") public void setTicketsSold(long v) { ticketsSold = v; }

    @Exclude public int getPrice() { return price; }
    @Exclude public void setPrice(int v) { price = v; }

    @Exclude public String getUserRole() { return userRole; }
    @Exclude public void setUserRole(String role) { this.userRole = role; }
}
