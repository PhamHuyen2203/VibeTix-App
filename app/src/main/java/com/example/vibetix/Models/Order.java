package com.example.vibetix.Models;

public class Order {
    private String id;
    private String userEmail;
    private String eventId;
    private String eventTitle;
    private String ticketTypeName;
    private int quantity;
    private long basePrice;
    private long platformFee;
    private long totalPrice;
    private String paymentMethod;
    private String status;
    private long orderTimeMillis;

    // Attendee Info
    private String attendeeName;
    private String attendeeEmail;
    private String attendeePhone;
    private String attendeeCompany;
    private String attendeeRole;
    private String notes;

    public Order() {}

    public Order(String id, String userEmail, String eventId, String eventTitle, String ticketTypeName,
                 int quantity, long basePrice, long platformFee, long totalPrice, String paymentMethod,
                 String status, long orderTimeMillis, String attendeeName, String attendeeEmail,
                 String attendeePhone, String attendeeCompany, String attendeeRole, String notes) {
        this.id = id;
        this.userEmail = userEmail;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.ticketTypeName = ticketTypeName;
        this.quantity = quantity;
        this.basePrice = basePrice;
        this.platformFee = platformFee;
        this.totalPrice = totalPrice;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.orderTimeMillis = orderTimeMillis;
        this.attendeeName = attendeeName;
        this.attendeeEmail = attendeeEmail;
        this.attendeePhone = attendeePhone;
        this.attendeeCompany = attendeeCompany;
        this.attendeeRole = attendeeRole;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public String getTicketTypeName() { return ticketTypeName; }
    public void setTicketTypeName(String ticketTypeName) { this.ticketTypeName = ticketTypeName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public long getBasePrice() { return basePrice; }
    public void setBasePrice(long basePrice) { this.basePrice = basePrice; }

    public long getPlatformFee() { return platformFee; }
    public void setPlatformFee(long platformFee) { this.platformFee = platformFee; }

    public long getTotalPrice() { return totalPrice; }
    public void setTotalPrice(long totalPrice) { this.totalPrice = totalPrice; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getOrderTimeMillis() { return orderTimeMillis; }
    public void setOrderTimeMillis(long orderTimeMillis) { this.orderTimeMillis = orderTimeMillis; }

    public String getAttendeeName() { return attendeeName; }
    public void setAttendeeName(String attendeeName) { this.attendeeName = attendeeName; }

    public String getAttendeeEmail() { return attendeeEmail; }
    public void setAttendeeEmail(String attendeeEmail) { this.attendeeEmail = attendeeEmail; }

    public String getAttendeePhone() { return attendeePhone; }
    public void setAttendeePhone(String attendeePhone) { this.attendeePhone = attendeePhone; }

    public String getAttendeeCompany() { return attendeeCompany; }
    public void setAttendeeCompany(String attendeeCompany) { this.attendeeCompany = attendeeCompany; }

    public String getAttendeeRole() { return attendeeRole; }
    public void setAttendeeRole(String attendeeRole) { this.attendeeRole = attendeeRole; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
