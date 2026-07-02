package com.example.vibetix.Models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@IgnoreExtraProperties
public class EventSession {
    private String sessionId;
    private Timestamp startTime;
    private Timestamp endTime;
    private int availableQuantity = -1; // -1 = not tracked

    public EventSession() {}

    @Exclude public String getSessionId() { return sessionId; }
    @Exclude public void setSessionId(String id) { this.sessionId = id; }

    @PropertyName("start_time")
    public Timestamp getStartTime() { return startTime; }
    @PropertyName("start_time")
    public void setStartTime(Timestamp t) { this.startTime = t; }

    @PropertyName("end_time")
    public Timestamp getEndTime() { return endTime; }
    @PropertyName("end_time")
    public void setEndTime(Timestamp t) { this.endTime = t; }

    @PropertyName("available_quantity")
    public int getAvailableQuantity() { return availableQuantity; }
    @PropertyName("available_quantity")
    public void setAvailableQuantity(int q) { this.availableQuantity = q; }

    /** "20:00 - 22:00, T6" */
    @Exclude
    public String getDisplayTime() {
        if (startTime == null) return "--:--";
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String start = tf.format(startTime.toDate());
        String end = endTime != null ? tf.format(endTime.toDate()) : "";

        // Day-of-week abbreviation
        SimpleDateFormat dowFmt = new SimpleDateFormat("EE", new Locale("vi", "VN"));
        String dow = dowFmt.format(startTime.toDate());

        return start + (end.isEmpty() ? "" : " - " + end) + ", " + dow;
    }

    /** "03 Tháng 07, 2026" */
    @Exclude
    public String getDisplayDate() {
        if (startTime == null) return "--";
        Date d = startTime.toDate();
        SimpleDateFormat fmt = new SimpleDateFormat("dd 'Tháng' MM, yyyy", new Locale("vi", "VN"));
        return fmt.format(d);
    }
}
