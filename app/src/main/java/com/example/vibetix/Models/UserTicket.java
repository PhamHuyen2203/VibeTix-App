package com.example.vibetix.Models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
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

    @PropertyName("user_id")
    public String getOwnerId() { return ownerId; }
    @PropertyName("user_id")
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

    @Exclude
    public Status getStatus() {
        if (statusStr == null) return Status.VALID;
        try {
            return Status.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Status.VALID;
        }
    }

    @Exclude
    public void setStatus(Status status) {
        this.statusStr = status != null ? status.name().toLowerCase() : Status.VALID.name().toLowerCase();
    }

    @Exclude
    public boolean isUsed() {
        return getStatus() == Status.USED;
    }

    @Exclude
    public boolean isExpired() {
        return getStatus() == Status.EXPIRED;
    }

    @Exclude
    public boolean isCancelled() {
        return getStatus() == Status.CANCELLED;
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
    @Exclude
    public String getUserId() { return ownerId; }
    @Exclude
    public void setUserId(String userId) { this.ownerId = userId; }

    /** Alias for userTicketId. */
    @Exclude
    public String getTicketId() { return userTicketId; }
    @Exclude
    public void setTicketId(String id) { this.userTicketId = id; }



    /** created_at field (for ordering). */
    private Object createdAt;
    @Exclude
    public Object getCreatedAt() { return createdAt; }
    @Exclude
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }

    // UI-only denormalized fields (NOT in DB schema — loaded separately from users collection)
    private String fullName;
    private String email;
    private String ticketTypeName;

    @com.google.firebase.firestore.Exclude
    public String getFullName() { return fullName; }
    @com.google.firebase.firestore.Exclude
    public void setFullName(String fullName) { this.fullName = fullName; }

    @com.google.firebase.firestore.Exclude
    public String getEmail() { return email; }
    @com.google.firebase.firestore.Exclude
    public void setEmail(String email) { this.email = email; }

    @com.google.firebase.firestore.Exclude
    public String getTicketTypeName() { return ticketTypeName; }
    @com.google.firebase.firestore.Exclude
    public void setTicketTypeName(String ticketTypeName) { this.ticketTypeName = ticketTypeName; }


}
