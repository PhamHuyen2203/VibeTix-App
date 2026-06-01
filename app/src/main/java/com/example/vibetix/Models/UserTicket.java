package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;

public class UserTicket {
    public enum Status {
        VALID, USED, EXPIRED, TRANSFERRED, CANCELLED
    }

    private String userTicketId; // UUID v4
    private String orderItemId;
    private String eventId; // Thêm eventId để query nhanh thống kê dashboard (không có trong schema gốc nhưng cần thiết cho NoSQL)
    private String ownerId;
    private String ticketCode;
    private String displayCode;
    private String statusStr = "valid";
    private Object checkedInAt;
    private Object issuedAt;

    public UserTicket() {}

    @PropertyName("user_ticket_id")
    public String getUserTicketId() { return userTicketId; }
    @PropertyName("user_ticket_id")
    public void setUserTicketId(String userTicketId) { this.userTicketId = userTicketId; }

    @PropertyName("order_item_id")
    public String getOrderItemId() { return orderItemId; }
    @PropertyName("order_item_id")
    public void setOrderItemId(String orderItemId) { this.orderItemId = orderItemId; }

    @PropertyName("event_id")
    public String getEventId() { return eventId; }
    @PropertyName("event_id")
    public void setEventId(String eventId) { this.eventId = eventId; }

    @PropertyName("owner_id")
    public String getOwnerId() { return ownerId; }
    @PropertyName("owner_id")
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    @PropertyName("ticket_code")
    public String getTicketCode() { return ticketCode; }
    @PropertyName("ticket_code")
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }

    @PropertyName("display_code")
    public String getDisplayCode() { return displayCode; }
    @PropertyName("display_code")
    public void setDisplayCode(String displayCode) { this.displayCode = displayCode; }

    @PropertyName("status")
    public String getStatusStr() { return statusStr; }
    @PropertyName("status")
    public void setStatusStr(String statusStr) { this.statusStr = statusStr; }

    public Status getStatusEnum() {
        if (statusStr == null) return Status.VALID;
        try {
            return Status.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Status.VALID;
        }
    }

    public void setStatus(Status status) {
        this.statusStr = status != null ? status.name().toLowerCase() : Status.VALID.name().toLowerCase();
    }

    @PropertyName("checked_in_at")
    public Object getCheckedInAt() { return checkedInAt; }
    @PropertyName("checked_in_at")
    public void setCheckedInAt(Object checkedInAt) { this.checkedInAt = checkedInAt; }

    @PropertyName("issued_at")
    public Object getIssuedAt() { return issuedAt; }
    @PropertyName("issued_at")
    public void setIssuedAt(Object issuedAt) { this.issuedAt = issuedAt; }

    // ─── Convenience aliases ─────────────────────────────────────────────────
    /** Alias for ownerId — used when Firestore field is user_id. */
    public String getUserId() { return ownerId; }
    public void setUserId(String userId) { this.ownerId = userId; }

    /** Alias for userTicketId. */
    public String getTicketId() { return userTicketId; }
    public void setTicketId(String id) { this.userTicketId = id; }

    /** Alias for statusStr — plain string status. */
    public String getStatus() { return statusStr; }

    /** created_at field (for ordering). */
    private Object createdAt;
    @PropertyName("created_at")
    public Object getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }
}
