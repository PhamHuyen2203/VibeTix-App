package com.example.vibetix.Models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

@IgnoreExtraProperties
public class EventInterest implements Serializable {
    @PropertyName("user_id")
    private String userId;

    @PropertyName("event_id")
    private String eventId;

    @PropertyName("created_at")
    private Object createdAt;

    @PropertyName("notify_on_sale")
    private boolean notifyOnSale = true;

    @PropertyName("notify_on_reminder")
    private boolean notifyOnReminder = true;

    public EventInterest() {}

    @PropertyName("user_id")
    public String getUserId() { return userId; }
    @PropertyName("user_id")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("event_id")
    public String getEventId() { return eventId; }
    @PropertyName("event_id")
    public void setEventId(String eventId) { this.eventId = eventId; }

    @PropertyName("created_at")
    public Object getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }

    @PropertyName("notify_on_sale")
    public boolean isNotifyOnSale() { return notifyOnSale; }
    @PropertyName("notify_on_sale")
    public void setNotifyOnSale(boolean notifyOnSale) { this.notifyOnSale = notifyOnSale; }

    @PropertyName("notify_on_reminder")
    public boolean isNotifyOnReminder() { return notifyOnReminder; }
    @PropertyName("notify_on_reminder")
    public void setNotifyOnReminder(boolean notifyOnReminder) { this.notifyOnReminder = notifyOnReminder; }
}
