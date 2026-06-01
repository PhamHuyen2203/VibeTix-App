package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.Exclude;

/**
 * Event — Sự kiện.
 * Persisted fields: event_id, organizer_id, user_id, category_id,
 *   title, description, poster_url, banner_url, start_time, end_time,
 *   status, is_featured, interest_count, min_price, max_price, created_at,
 *   venue_name, venue_address, venue_city.
 * Transient (@Exclude) fields: localImageResId, localPortraitImageResId, isSoldOut, date, price.
 */
public class Event {
    public enum Status {
        DRAFT, PENDING, APPROVED, ONGOING, COMPLETED, CANCELLED
    }

    // ─── Firestore-persisted fields ─────────────────────────────────────────
    private String eventId;       // UUID v4
    private String organizerId;   // FK → organizers
    private String userId;        // Creator FK → users
    private String categoryId;    // FK → categories
    private String venueId;       // FK → venues (optional, nullable)
    private String venueName;
    private String venueAddress;
    private String venueCity;
    private String title;
    private String description;
    private String posterUrl;
    private String bannerUrl;
    private Object startTime;
    private Object endTime;
    private String statusStr;     // "draft" | "pending" | "approved" | "ongoing" | "completed" | "cancelled"
    private boolean isFeatured = false;
    private int interestCount = 0;
    private double minPrice = 0;
    private Double maxPrice = null;
    private Object createdAt;

    // ─── Transient UI fields (@Exclude → not saved to Firestore) ────────────
    private int localImageResId;
    private int localPortraitImageResId;
    private boolean isSoldOut;
    private String date;
    private int price;

    public Event() {}

    // Convenience constructor for legacy list adapters
    public Event(String id, String title, String posterUrl, String date,
                 String venueCity, String statusStr, int price) {
        this.eventId = id;
        this.title = title;
        this.posterUrl = posterUrl;
        this.date = date;
        this.startTime = date;
        this.venueCity = venueCity;
        this.statusStr = statusStr;
        this.price = price;
        this.minPrice = price;
    }

    public Event(String id, String title, String posterUrl, String startTimeStr,
                 Status status, boolean isFeatured) {
        this.eventId = id;
        this.title = title;
        this.posterUrl = posterUrl;
        this.startTime = startTimeStr;
        this.date = startTimeStr;
        setStatus(status);
        this.isFeatured = isFeatured;
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────

    @Exclude public String getId()          { return eventId; }
    @Exclude public void setId(String id)   { this.eventId = id; }

    @PropertyName("event_id")   public String getEventId()               { return eventId; }
    @PropertyName("event_id")   public void setEventId(String v)         { eventId = v; }

    @PropertyName("organizer_id") public String getOrganizerId()         { return organizerId; }
    @PropertyName("organizer_id") public void setOrganizerId(String v)   { organizerId = v; }

    @PropertyName("user_id")    public String getUserId()                 { return userId; }
    @PropertyName("user_id")    public void setUserId(String v)           { userId = v; }

    @PropertyName("category_id") public String getCategoryId()           { return categoryId; }
    @PropertyName("category_id") public void setCategoryId(String v)     { categoryId = v; }

    @PropertyName("venue_id")   public String getVenueId()               { return venueId; }
    @PropertyName("venue_id")   public void setVenueId(String v)         { venueId = v; }

    @PropertyName("venue_name") public String getVenueName()             { return venueName; }
    @PropertyName("venue_name") public void setVenueName(String v)       { venueName = v; }

    @PropertyName("venue_address") public String getVenueAddress()       { return venueAddress; }
    @PropertyName("venue_address") public void setVenueAddress(String v) { venueAddress = v; }

    @PropertyName("venue_city") public String getVenueCity()             { return venueCity; }
    @PropertyName("venue_city") public void setVenueCity(String v)       { venueCity = v; }

    public String getTitle()               { return title; }
    public void setTitle(String v)         { title = v; }

    public String getDescription()         { return description; }
    public void setDescription(String v)   { description = v; }

    @PropertyName("poster_url") public String getPosterUrl()             { return posterUrl; }
    @PropertyName("poster_url") public void setPosterUrl(String v)       { posterUrl = v; }

    @PropertyName("banner_url") public String getBannerUrl()             { return bannerUrl; }
    @PropertyName("banner_url") public void setBannerUrl(String v)       { bannerUrl = v; }

    @PropertyName("start_time") public Object getStartTimeRaw()             { return startTime; }
    @PropertyName("start_time") public void setStartTimeRaw(Object v)       { startTime = v; }

    @PropertyName("end_time")   public Object getEndTimeRaw()               { return endTime; }
    @PropertyName("end_time")   public void setEndTimeRaw(Object v)         { endTime = v; }

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

    @PropertyName("status")     public String getStatusStr()             { return statusStr; }
    @PropertyName("status")     public void setStatusStr(String v)       { statusStr = v; }

    public Status getStatus() {
        if (statusStr == null) return Status.DRAFT;
        try { return Status.valueOf(statusStr.toUpperCase()); }
        catch (IllegalArgumentException e) { return Status.DRAFT; }
    }
    public void setStatus(Status status) {
        statusStr = status != null ? status.name().toLowerCase() : "draft";
    }

    @PropertyName("is_featured")    public boolean isFeatured()                 { return isFeatured; }
    @PropertyName("is_featured")    public void setFeatured(boolean v)          { isFeatured = v; }

    @PropertyName("interest_count") public int getInterestCount()               { return interestCount; }
    @PropertyName("interest_count") public void setInterestCount(int v)         { interestCount = v; }

    @PropertyName("min_price")      public double getMinPrice()                 { return minPrice; }
    @PropertyName("min_price")      public void setMinPrice(double v)           { minPrice = v; }

    @PropertyName("max_price")      public Double getMaxPrice()                 { return maxPrice; }
    @PropertyName("max_price")      public void setMaxPrice(Double v)           { maxPrice = v; }

    @PropertyName("created_at")     public Object getCreatedAt()                { return createdAt; }
    @PropertyName("created_at")     public void setCreatedAt(Object v)          { createdAt = v; }

    // ─── Transient UI helpers ────────────────────────────────────────────────

    @Exclude public int  getLocalImageResId()                                   { return localImageResId; }
    @Exclude public void setLocalImageResId(int v)                              { localImageResId = v; }

    @Exclude public int  getLocalPortraitImageResId()                           { return localPortraitImageResId; }
    @Exclude public void setLocalPortraitImageResId(int v)                      { localPortraitImageResId = v; }

    @Exclude public boolean isSoldOut()                                         { return isSoldOut; }
    @Exclude public void    setSoldOut(boolean v)                               { isSoldOut = v; }

    @Exclude public String getDate()                                            { return date != null ? date : getStartTime(); }
    @Exclude public void   setDate(String v)                                    { date = v; }

    @Exclude public int  getPrice()                                             { return price; }
    @Exclude public void setPrice(int v)                                        { price = v; }

    /** Alias for posterUrl — used by legacy adapters. */
    @Exclude public String getImageUrl()                                        { return posterUrl; }
    @Exclude public void   setImageUrl(String v)                               { posterUrl = v; }
}
