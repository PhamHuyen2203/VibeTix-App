package com.example.vibetix.Models;

import com.google.firebase.Timestamp;

/**
 * Ticket — vé của người dùng (USER_TICKET trong ERD).
 * Đại diện cho một vé cụ thể thuộc sở hữu của một user.
 */
public class Ticket {
    private String id;
    private String orderId;
    private String userEmail;
    private String eventId;
    private String eventTitle;
    private String eventDate;
    private String eventLocation;
    private String eventImageUrl;
    private String ticketTypeName;
    private long purchasePrice;
    private long resalePrice;
    private String status; // "ACTIVE", "RESELLING", "USED", "EXPIRED"
    private String attendeeName;
    private String ticketCode;
    private String displayCode;
    private Timestamp issuedAt;
    private String eventStatus; // transient — status của event (approved/ongoing/completed...)
    // Transient display field — not persisted to Firestore
    // Format: "qty:typeName:unitPrice" entries joined by "|"
    // e.g. "1:Vé VIP:150000|2:Vé Thường:200000"
    private String itemBreakdown;

    public Ticket() {}

    public Ticket(String id, String orderId, String userEmail, String eventId,
                  String eventTitle, String eventDate, String eventLocation,
                  String eventImageUrl, String ticketTypeName, long purchasePrice,
                  long resalePrice, String status, String attendeeName) {
        this.id = id;
        this.orderId = orderId;
        this.userEmail = userEmail;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.eventDate = eventDate;
        this.eventLocation = eventLocation;
        this.eventImageUrl = eventImageUrl;
        this.ticketTypeName = ticketTypeName;
        this.purchasePrice = purchasePrice;
        this.resalePrice = resalePrice;
        this.status = status;
        this.attendeeName = attendeeName;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    public String getEventLocation() { return eventLocation; }
    public void setEventLocation(String eventLocation) { this.eventLocation = eventLocation; }

    public String getEventImageUrl() { return eventImageUrl; }
    public void setEventImageUrl(String eventImageUrl) { this.eventImageUrl = eventImageUrl; }

    public String getTicketTypeName() { return ticketTypeName; }
    public void setTicketTypeName(String ticketTypeName) { this.ticketTypeName = ticketTypeName; }

    public long getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(long purchasePrice) { this.purchasePrice = purchasePrice; }

    public long getResalePrice() { return resalePrice; }
    public void setResalePrice(long resalePrice) { this.resalePrice = resalePrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAttendeeName() { return attendeeName; }
    public void setAttendeeName(String attendeeName) { this.attendeeName = attendeeName; }

    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }

    public String getDisplayCode() { return displayCode; }
    public void setDisplayCode(String displayCode) { this.displayCode = displayCode; }

    public Timestamp getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Timestamp issuedAt) { this.issuedAt = issuedAt; }

    public String getItemBreakdown() { return itemBreakdown; }
    public void setItemBreakdown(String itemBreakdown) { this.itemBreakdown = itemBreakdown; }

    public String getEventStatus() { return eventStatus; }
    public void setEventStatus(String eventStatus) { this.eventStatus = eventStatus; }
}
