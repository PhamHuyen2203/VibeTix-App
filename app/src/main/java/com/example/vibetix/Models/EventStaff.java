package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;

/**
 * EventStaff — Phân quyền nhân viên cho từng sự kiện.
 *
 * Collection: event_staff
 * Roles:
 *   - "manager"        : Xem orders/revenue, CRUD ticket_types, discounts, gửi thông báo
 *   - "check_in_staff" : Chỉ xem attendees, quét QR check-in
 *
 * Owner (user sở hữu organizer) không cần entry trong collection này.
 */
public class EventStaff {
    public enum Role {
        MANAGER, CHECK_IN_STAFF;

        public String toValue() {
            switch (this) {
                case MANAGER: return "manager";
                case CHECK_IN_STAFF: return "check_in_staff";
                default: return "check_in_staff";
            }
        }

        public static Role fromValue(String value) {
            if ("manager".equalsIgnoreCase(value)) return MANAGER;
            return CHECK_IN_STAFF;
        }
    }

    private String staffId;       // UUID v4
    private String userId;        // FK → users/{user_id} (người được assign)
    private String eventId;       // FK → events/{event_id}
    private String organizerId;   // FK → organizers/{organizer_id}
    private String roleStr;       // "manager" | "check_in_staff"
    private String assignedBy;    // user_id của owner gán
    private String assignedAt;    // ISO 8601 timestamp

    // Transient fields cho UI (không lưu Firestore)
    private String staffName;
    private String staffEmail;
    private String staffAvatarUrl;

    public EventStaff() {}

    public EventStaff(String staffId, String userId, String eventId,
                      String organizerId, Role role, String assignedBy, String assignedAt) {
        this.staffId = staffId;
        this.userId = userId;
        this.eventId = eventId;
        this.organizerId = organizerId;
        this.roleStr = role.toValue();
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    @PropertyName("staff_id")
    public String getStaffId() { return staffId; }
    @PropertyName("staff_id")
    public void setStaffId(String staffId) { this.staffId = staffId; }

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

    @PropertyName("role")
    public String getRoleStr() { return roleStr; }
    @PropertyName("role")
    public void setRoleStr(String roleStr) { this.roleStr = roleStr; }

    public Role getRole() { return Role.fromValue(roleStr); }
    public void setRole(Role role) { this.roleStr = role.toValue(); }

    @PropertyName("assigned_by")
    public String getAssignedBy() { return assignedBy; }
    @PropertyName("assigned_by")
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    @PropertyName("assigned_at")
    public String getAssignedAt() { return assignedAt; }
    @PropertyName("assigned_at")
    public void setAssignedAt(String assignedAt) { this.assignedAt = assignedAt; }

    // UI-only transient fields
    @com.google.firebase.firestore.Exclude
    public String getStaffName() { return staffName; }
    @com.google.firebase.firestore.Exclude
    public void setStaffName(String staffName) { this.staffName = staffName; }

    @com.google.firebase.firestore.Exclude
    public String getStaffEmail() { return staffEmail; }
    @com.google.firebase.firestore.Exclude
    public void setStaffEmail(String staffEmail) { this.staffEmail = staffEmail; }

    @com.google.firebase.firestore.Exclude
    public String getStaffAvatarUrl() { return staffAvatarUrl; }
    @com.google.firebase.firestore.Exclude
    public void setStaffAvatarUrl(String staffAvatarUrl) { this.staffAvatarUrl = staffAvatarUrl; }
}
