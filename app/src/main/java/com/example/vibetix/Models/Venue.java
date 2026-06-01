package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;

public class Venue {
    private String venueId; // UUID v4
    private String name;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private Integer capacity;
    private String imageUrl;

    public Venue() {}

    @PropertyName("venue_id")
    public String getVenueId() { return venueId; }
    @PropertyName("venue_id")
    public void setVenueId(String venueId) { this.venueId = venueId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    @PropertyName("image_url")
    public String getImageUrl() { return imageUrl; }
    @PropertyName("image_url")
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
