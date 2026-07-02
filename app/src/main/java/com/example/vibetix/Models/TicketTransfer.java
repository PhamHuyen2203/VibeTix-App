package com.example.vibetix.Models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

/**
 * TicketTransfer — đại diện cho một yêu cầu chuyển nhượng vé trong collection ticket_transfers.
 */
@IgnoreExtraProperties
public class TicketTransfer {

    private String transferId;
    private String senderId;
    private String receiverId;
    private String receiverEmail;
    private String userTicketId;
    private String status; // "pending", "accepted", "rejected", "expired", "cancelled"
    private long price = 0;      // Giá bán lại (VND) do người bán đặt
    private long originalPrice = 0; // Giá mua gốc (VND) — để hiển thị tham khảo
    private String message;
    private String transferToken;
    private Timestamp createdAt;
    private Timestamp completedAt;
    private Timestamp expiresAt;

    // Event display fields — denormalized (lưu thẳng khi tạo từ vé mua qua app).
    // Seed data cũ không có các field này → repository sẽ join từ user_tickets/events.
    private String eventTitle;
    private String eventDate;
    private String eventImageUrl;
    private String eventLocation;
    private String eventId;
    private String eventStatus; // transient — set sau khi join, không lưu Firestore

    public TicketTransfer() {}

    // ── Firestore-mapped getters/setters ──

    @PropertyName("transfer_id")
    public String getTransferId() { return transferId; }
    @PropertyName("transfer_id")
    public void setTransferId(String transferId) { this.transferId = transferId; }

    @PropertyName("sender_id")
    public String getSenderId() { return senderId; }
    @PropertyName("sender_id")
    public void setSenderId(String senderId) { this.senderId = senderId; }

    @PropertyName("receiver_id")
    public String getReceiverId() { return receiverId; }
    @PropertyName("receiver_id")
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    @PropertyName("receiver_email")
    public String getReceiverEmail() { return receiverEmail; }
    @PropertyName("receiver_email")
    public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }

    @PropertyName("user_ticket_id")
    public String getUserTicketId() { return userTicketId; }
    @PropertyName("user_ticket_id")
    public void setUserTicketId(String userTicketId) { this.userTicketId = userTicketId; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("price")
    public long getPrice() { return price; }
    @PropertyName("price")
    public void setPrice(long price) { this.price = price; }

    @PropertyName("original_price")
    public long getOriginalPrice() { return originalPrice; }
    @PropertyName("original_price")
    public void setOriginalPrice(long originalPrice) { this.originalPrice = originalPrice; }

    @PropertyName("message")
    public String getMessage() { return message; }
    @PropertyName("message")
    public void setMessage(String message) { this.message = message; }

    @PropertyName("transfer_token")
    public String getTransferToken() { return transferToken; }
    @PropertyName("transfer_token")
    public void setTransferToken(String transferToken) { this.transferToken = transferToken; }

    @PropertyName("created_at")
    public Timestamp getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @PropertyName("completed_at")
    public Timestamp getCompletedAt() { return completedAt; }
    @PropertyName("completed_at")
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }

    @PropertyName("expires_at")
    public Timestamp getExpiresAt() { return expiresAt; }
    @PropertyName("expires_at")
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }

    // ── Event display fields (denormalized, stored snake_case) ──

    @PropertyName("event_title")
    public String getEventTitle() { return eventTitle; }
    @PropertyName("event_title")
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    @PropertyName("event_date")
    public String getEventDate() { return eventDate; }
    @PropertyName("event_date")
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    @PropertyName("event_image_url")
    public String getEventImageUrl() { return eventImageUrl; }
    @PropertyName("event_image_url")
    public void setEventImageUrl(String eventImageUrl) { this.eventImageUrl = eventImageUrl; }

    @PropertyName("event_location")
    public String getEventLocation() { return eventLocation; }
    @PropertyName("event_location")
    public void setEventLocation(String eventLocation) { this.eventLocation = eventLocation; }

    @PropertyName("event_id")
    public String getEventId() { return eventId; }
    @PropertyName("event_id")
    public void setEventId(String eventId) { this.eventId = eventId; }

    @com.google.firebase.firestore.Exclude
    public String getEventStatus() { return eventStatus; }
    @com.google.firebase.firestore.Exclude
    public void setEventStatus(String eventStatus) { this.eventStatus = eventStatus; }
}
