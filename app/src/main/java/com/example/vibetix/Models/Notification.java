package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;

/**
 * Notification — Thông báo gửi từ Organizer đến người mua vé.
 * Collection: notifications
 */
public class Notification {
    private String notificationId;
    private String userId;       // FK → người nhận
    private String eventId;      // FK → sự kiện liên quan
    private String organizerId;  // FK → BTC gửi
    private String title;
    private String body;
    private String sentAt;       // ISO 8601
    private boolean isRead = false;

    public Notification() {}

    public Notification(String notificationId, String userId, String eventId,
                        String organizerId, String title, String body, String sentAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.eventId = eventId;
        this.organizerId = organizerId;
        this.title = title;
        this.body = body;
        this.sentAt = sentAt;
        this.isRead = false;
    }

    @PropertyName("notification_id")
    public String getNotificationId() { return notificationId; }
    @PropertyName("notification_id")
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    @PropertyName("user_id")
    public String getUserId() { return userId; }
    @PropertyName("user_id")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("event_id")
    public String getEventId() { return eventId; }
    @PropertyName("event_id")
    public void setEventId(String eventId) { this.eventId = eventId; }

    @PropertyName("organizer_id")
    public String getOrganizerId() { return organizerId; }
    @PropertyName("organizer_id")
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    @PropertyName("sent_at")
    public String getSentAt() { return sentAt; }
    @PropertyName("sent_at")
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }

    @PropertyName("is_read")
    public boolean isRead() { return isRead; }
    @PropertyName("is_read")
    public void setRead(boolean read) { isRead = read; }
}
