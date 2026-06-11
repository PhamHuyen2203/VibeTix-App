package com.example.vibetix.Models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

@IgnoreExtraProperties
public class Notification implements Serializable {
    @PropertyName("notification_id")
    private String notificationId;

    @PropertyName("user_id")
    private String userId;

    @PropertyName("type")
    private String type;

    @PropertyName("title")
    private String title;

    @PropertyName("body")
    private String body;

    @PropertyName("ref_type")
    private String refType;

    @PropertyName("ref_id")
    private String refId;

    @PropertyName("channel")
    private String channel;

    @PropertyName("is_read")
    private boolean isRead = false;

    @PropertyName("read_at")
    private com.google.firebase.Timestamp readAt;

    @PropertyName("sent_at")
    private com.google.firebase.Timestamp sentAt;

    @PropertyName("status")
    private String status = "pending";

    @PropertyName("created_at")
    private com.google.firebase.Timestamp createdAt;

    public Notification() {}

    @PropertyName("notification_id")
    public String getNotificationId() { return notificationId; }
    @PropertyName("notification_id")
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    @PropertyName("user_id")
    public String getUserId() { return userId; }
    @PropertyName("user_id")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }

    @PropertyName("title")
    public String getTitle() { return title; }
    @PropertyName("title")
    public void setTitle(String title) { this.title = title; }

    @PropertyName("body")
    public String getBody() { return body; }
    @PropertyName("body")
    public void setBody(String body) { this.body = body; }

    @PropertyName("ref_type")
    public String getRefType() { return refType; }
    @PropertyName("ref_type")
    public void setRefType(String refType) { this.refType = refType; }

    @PropertyName("ref_id")
    public String getRefId() { return refId; }
    @PropertyName("ref_id")
    public void setRefId(String refId) { this.refId = refId; }

    @PropertyName("channel")
    public String getChannel() { return channel; }
    @PropertyName("channel")
    public void setChannel(String channel) { this.channel = channel; }

    @PropertyName("is_read")
    public boolean isRead() { return isRead; }
    @PropertyName("is_read")
    public void setRead(boolean read) { isRead = read; }

    @PropertyName("read_at")
    public com.google.firebase.Timestamp getReadAt() { return readAt; }
    @PropertyName("read_at")
    public void setReadAt(com.google.firebase.Timestamp readAt) { this.readAt = readAt; }

    @PropertyName("sent_at")
    public com.google.firebase.Timestamp getSentAt() { return sentAt; }
    @PropertyName("sent_at")
    public void setSentAt(com.google.firebase.Timestamp sentAt) { this.sentAt = sentAt; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("created_at")
    public com.google.firebase.Timestamp getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(com.google.firebase.Timestamp createdAt) { this.createdAt = createdAt; }
}
