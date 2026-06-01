package com.example.vibetix.Models;

public class Destination {
    private String id;
    private String name;
    private String imageUrl;
    private String address;
    private String city;
    private int capacity;
    private int eventCount;
    private int localImageResId;

    public Destination() {}

    public Destination(String id, String name, String imageUrl, int eventCount) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.eventCount = eventCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getEventCount() { return eventCount; }
    public void setEventCount(int eventCount) { this.eventCount = eventCount; }

    public int getLocalImageResId() { return localImageResId; }
    public void setLocalImageResId(int localImageResId) { this.localImageResId = localImageResId; }
}
