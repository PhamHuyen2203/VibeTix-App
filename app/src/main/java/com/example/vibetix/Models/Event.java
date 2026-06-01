package com.example.vibetix.Models;

public class Event {
    private String id;
    private String title;
    private String imageUrl;
    private String date;
    private String location;
    private String category;
    private long minPrice;
    private long maxPrice;
    private boolean isFree;
    private boolean isSoldOut;
    private String status;
    private int localImageResId;          // landscape / banner image
    private int localPortraitImageResId;  // portrait poster image (for Featured section)
    private String venueCity;
    private String venueName;
    private String venueAddress;
    private String organizerName;
    private String endDate;
    private int interestCount;
    private boolean isFeatured;
    private String description;

    public Event() {}

    public Event(String id, String title, String imageUrl, String date,
                 String location, String category, long minPrice) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.date = date;
        this.location = location;
        this.category = category;
        this.minPrice = minPrice;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getMinPrice() { return minPrice; }
    public void setMinPrice(long minPrice) { this.minPrice = minPrice; }

    public long getMaxPrice() { return maxPrice; }
    public void setMaxPrice(long maxPrice) { this.maxPrice = maxPrice; }

    public boolean isFree() { return isFree; }
    public void setFree(boolean free) { isFree = free; }

    public boolean isSoldOut() { return isSoldOut; }
    public void setSoldOut(boolean soldOut) { isSoldOut = soldOut; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getLocalImageResId() { return localImageResId; }
    public void setLocalImageResId(int localImageResId) { this.localImageResId = localImageResId; }

    public int getLocalPortraitImageResId() { return localPortraitImageResId; }
    public void setLocalPortraitImageResId(int localPortraitImageResId) { this.localPortraitImageResId = localPortraitImageResId; }

    public String getVenueCity() { return venueCity; }
    public void setVenueCity(String venueCity) { this.venueCity = venueCity; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public String getVenueAddress() { return venueAddress; }
    public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public int getInterestCount() { return interestCount; }
    public void setInterestCount(int interestCount) { this.interestCount = interestCount; }

    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean featured) { isFeatured = featured; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
