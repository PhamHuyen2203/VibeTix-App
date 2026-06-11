package com.example.vibetix.Models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

@IgnoreExtraProperties
public class EventStar implements Serializable {
    @PropertyName("event_id")
    private String eventId;

    @PropertyName("star_id")
    private String starId;

    @PropertyName("role")
    private String role; // Headliner, Special Guest...

    @PropertyName("performance_order")
    private Integer performanceOrder;

    @PropertyName("is_confirmed")
    private boolean isConfirmed = true;

    @PropertyName("added_at")
    private com.google.firebase.Timestamp addedAt;

    public EventStar() {}

    @PropertyName("event_id")
    public String getEventId() { return eventId; }
    @PropertyName("event_id")
    public void setEventId(String eventId) { this.eventId = eventId; }

    @PropertyName("star_id")
    public String getStarId() { return starId; }
    @PropertyName("star_id")
    public void setStarId(String starId) { this.starId = starId; }

    @PropertyName("role")
    public String getRole() { return role; }
    @PropertyName("role")
    public void setRole(String role) { this.role = role; }

    @PropertyName("performance_order")
    public Integer getPerformanceOrder() { return performanceOrder; }
    @PropertyName("performance_order")
    public void setPerformanceOrder(Integer performanceOrder) { this.performanceOrder = performanceOrder; }

    @PropertyName("is_confirmed")
    public boolean isConfirmed() { return isConfirmed; }
    @PropertyName("is_confirmed")
    public void setConfirmed(boolean confirmed) { isConfirmed = confirmed; }

    @PropertyName("added_at")
    public com.google.firebase.Timestamp getAddedAt() { return addedAt; }
    @PropertyName("added_at")
    public void setAddedAt(com.google.firebase.Timestamp addedAt) { this.addedAt = addedAt; }
}
