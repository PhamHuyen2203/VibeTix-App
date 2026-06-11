package com.example.vibetix.Models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

@IgnoreExtraProperties
public class TicketTransfer implements Serializable {
    public enum Status {
        PENDING, ACCEPTED, REJECTED, EXPIRED, CANCELLED
    }

    @PropertyName("transfer_id")
    private String transferId;

    @PropertyName("user_ticket_id")
    private String userTicketId;

    @PropertyName("sender_id")
    private String senderId;

    @PropertyName("receiver_id")
    private String receiverId;

    @PropertyName("receiver_email")
    private String receiverEmail;

    @PropertyName("status")
    private String statusStr = "pending";

    @PropertyName("transfer_token")
    private String transferToken;

    @PropertyName("message")
    private String message;

    @PropertyName("created_at")
    private Object createdAt;

    @PropertyName("expires_at")
    private Object expiresAt;

    @PropertyName("completed_at")
    private Object completedAt;

    public TicketTransfer() {}

    @PropertyName("transfer_id")
    public String getTransferId() { return transferId; }
    @PropertyName("transfer_id")
    public void setTransferId(String transferId) { this.transferId = transferId; }

    @PropertyName("user_ticket_id")
    public String getUserTicketId() { return userTicketId; }
    @PropertyName("user_ticket_id")
    public void setUserTicketId(String userTicketId) { this.userTicketId = userTicketId; }

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

    @PropertyName("status")
    public String getStatusStr() { return statusStr; }
    @PropertyName("status")
    public void setStatusStr(String statusStr) { this.statusStr = statusStr; }

    @com.google.firebase.firestore.Exclude
    public Status getStatus() {
        if (statusStr == null) return Status.PENDING;
        try {
            return Status.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Status.PENDING;
        }
    }

    @com.google.firebase.firestore.Exclude
    public void setStatus(Status status) {
        this.statusStr = status != null ? status.name().toLowerCase() : Status.PENDING.name().toLowerCase();
    }

    @PropertyName("transfer_token")
    public String getTransferToken() { return transferToken; }
    @PropertyName("transfer_token")
    public void setTransferToken(String transferToken) { this.transferToken = transferToken; }

    @PropertyName("message")
    public String getMessage() { return message; }
    @PropertyName("message")
    public void setMessage(String message) { this.message = message; }

    @PropertyName("created_at")
    public Object getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }

    @PropertyName("expires_at")
    public Object getExpiresAt() { return expiresAt; }
    @PropertyName("expires_at")
    public void setExpiresAt(Object expiresAt) { this.expiresAt = expiresAt; }

    @PropertyName("completed_at")
    public Object getCompletedAt() { return completedAt; }
    @PropertyName("completed_at")
    public void setCompletedAt(Object completedAt) { this.completedAt = completedAt; }
}
