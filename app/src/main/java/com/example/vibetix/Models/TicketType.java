package com.example.vibetix.Models;

import java.util.List;

public class TicketType {
    private String id;
    private String eventId;
    private String name;
    private long price;
    private int remainingQuantity;
    private List<String> benefits;

    public TicketType() {}

    public TicketType(String id, String eventId, String name, long price, int remainingQuantity, List<String> benefits) {
        this.id = id;
        this.eventId = eventId;
        this.name = name;
        this.price = price;
        this.remainingQuantity = remainingQuantity;
        this.benefits = benefits;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }

    public int getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(int remainingQuantity) { this.remainingQuantity = remainingQuantity; }

    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits) { this.benefits = benefits; }
}
