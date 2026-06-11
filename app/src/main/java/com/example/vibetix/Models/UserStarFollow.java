package com.example.vibetix.Models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

@IgnoreExtraProperties
public class UserStarFollow implements Serializable {
    @PropertyName("user_id")
    private String userId;

    @PropertyName("star_id")
    private String starId;

    @PropertyName("followed_at")
    private com.google.firebase.Timestamp followedAt;

    @PropertyName("notify_new_event")
    private boolean notifyNewEvent = true;

    @PropertyName("notify_presale")
    private boolean notifyPresale = true;

    public UserStarFollow() {}

    @PropertyName("user_id")
    public String getUserId() { return userId; }
    @PropertyName("user_id")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("star_id")
    public String getStarId() { return starId; }
    @PropertyName("star_id")
    public void setStarId(String starId) { this.starId = starId; }

    @PropertyName("followed_at")
    public com.google.firebase.Timestamp getFollowedAt() { return followedAt; }
    @PropertyName("followed_at")
    public void setFollowedAt(com.google.firebase.Timestamp followedAt) { this.followedAt = followedAt; }

    @PropertyName("notify_new_event")
    public boolean isNotifyNewEvent() { return notifyNewEvent; }
    @PropertyName("notify_new_event")
    public void setNotifyNewEvent(boolean notifyNewEvent) { this.notifyNewEvent = notifyNewEvent; }

    @PropertyName("notify_presale")
    public boolean isNotifyPresale() { return notifyPresale; }
    @PropertyName("notify_presale")
    public void setNotifyPresale(boolean notifyPresale) { this.notifyPresale = notifyPresale; }
}
