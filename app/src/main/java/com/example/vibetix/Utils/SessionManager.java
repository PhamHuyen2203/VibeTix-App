package com.example.vibetix.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.vibetix.Models.User;
import com.google.gson.Gson;

/**
 * SessionManager — Quản lý phiên đăng nhập và trạng thái organizer đang active.
 *
 * Lưu trữ:
 *  - Thông tin User đăng nhập
 *  - Organizer ID đang active (user có thể có nhiều)
 *  - Brand name của organizer đang active
 *  - Staff role nếu user được assign vào event_staff
 */
public class SessionManager {
    private static final String PREF_NAME             = "VibeTixSession";
    private static final String KEY_USER              = "user_data";
    private static final String KEY_IS_LOGGED_IN      = "is_logged_in";
    private static final String KEY_ACTIVE_ORG_ID     = "active_organizer_id";
    private static final String KEY_ACTIVE_ORG_NAME   = "active_organizer_name";
    private static final String KEY_ACTIVE_ORG_LOGO   = "active_organizer_logo";
    private static final String KEY_STAFF_ROLE        = "staff_role";    // "owner" | "manager" | "check_in_staff"
    private static final String KEY_STAFF_EVENT_ID    = "staff_event_id"; // Event user được assign
    private static final String KEY_ACTIVE_EVENT_ID   = "active_event_id"; // Event đang được xem trong EventHub
    private static final String KEY_ACTIVE_EVENT_ROLE = "active_event_role"; // Role trong event đó

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Gson gson;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        gson = new Gson();
    }

    // ─── User Session ───────────────────────────────────────────────────────

    public void createLoginSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER, gson.toJson(user));
        editor.apply();
    }

    public User getUserDetails() {
        String userData = pref.getString(KEY_USER, null);
        if (userData != null) {
            return gson.fromJson(userData, User.class);
        }
        return null;
    }

    public void updateUser(User user) {
        editor.putString(KEY_USER, gson.toJson(user));
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void logoutUser() {
        editor.clear();
        editor.apply();
    }

    // ─── Organizer Session ──────────────────────────────────────────────────

    /**
     * Lưu organizer đang active (khi user chuyển sang Organizer Mode).
     */
    public void setActiveOrganizer(String organizerId, String brandName, String logoUrl) {
        editor.putString(KEY_ACTIVE_ORG_ID, organizerId);
        editor.putString(KEY_ACTIVE_ORG_NAME, brandName != null ? brandName : "");
        editor.putString(KEY_ACTIVE_ORG_LOGO, logoUrl != null ? logoUrl : "");
        editor.apply();
    }

    public String getActiveOrganizerId() {
        return pref.getString(KEY_ACTIVE_ORG_ID, null);
    }

    /** Shortcut: chỉ cập nhật organizer ID (dùng khi switch organizer từ Hub). */
    public void setActiveOrganizerId(String organizerId) {
        editor.putString(KEY_ACTIVE_ORG_ID, organizerId);
        editor.apply();
    }

    public String getActiveOrganizerName() {
        return pref.getString(KEY_ACTIVE_ORG_NAME, "");
    }

    public String getActiveOrganizerLogoUrl() {
        return pref.getString(KEY_ACTIVE_ORG_LOGO, "");
    }

    public void clearActiveOrganizer() {
        editor.remove(KEY_ACTIVE_ORG_ID);
        editor.remove(KEY_ACTIVE_ORG_NAME);
        editor.remove(KEY_ACTIVE_ORG_LOGO);
        editor.apply();
    }

    // ─── Staff Role ─────────────────────────────────────────────────────────

    /**
     * Lưu role của user khi được assign vào event_staff.
     * @param role "owner" | "manager" | "check_in_staff"
     * @param eventId Event mà user được assign (null nếu là owner)
     */
    public void setStaffRole(String role, String eventId) {
        editor.putString(KEY_STAFF_ROLE, role);
        editor.putString(KEY_STAFF_EVENT_ID, eventId != null ? eventId : "");
        editor.apply();
    }

    /**
     * @return "owner" | "manager" | "check_in_staff" | null
     */
    public String getStaffRole() {
        return pref.getString(KEY_STAFF_ROLE, "owner"); // Default: owner
    }

    public String getStaffEventId() {
        return pref.getString(KEY_STAFF_EVENT_ID, null);
    }

    public void clearStaffRole() {
        editor.remove(KEY_STAFF_ROLE);
        editor.remove(KEY_STAFF_EVENT_ID);
        editor.apply();
    }

    // ─── Active Event Context (dùng bởi EventHubActivity và các fragment con) ──

    /**
     * Lưu context sự kiện đang xem trong EventHub.
     * @param eventId ID sự kiện
     * @param role    "owner" | "manager" | "check_in_staff"
     */
    public void setActiveEvent(String eventId, String role) {
        editor.putString(KEY_ACTIVE_EVENT_ID, eventId != null ? eventId : "");
        editor.putString(KEY_ACTIVE_EVENT_ROLE, role != null ? role : "");
        // Sync with staff role keys for backward compatibility
        editor.putString(KEY_STAFF_ROLE, role != null ? role : "");
        editor.putString(KEY_STAFF_EVENT_ID, eventId != null ? eventId : "");
        editor.apply();
    }

    /** @return ID sự kiện đang được xem trong EventHub, hoặc null */
    public String getActiveEventId() {
        String id = pref.getString(KEY_ACTIVE_EVENT_ID, null);
        return (id != null && !id.isEmpty()) ? id : null;
    }

    /** @return Role trong sự kiện đang active, hoặc null */
    public String getActiveEventRole() {
        String r = pref.getString(KEY_ACTIVE_EVENT_ROLE, null);
        return (r != null && !r.isEmpty()) ? r : null;
    }

    public void clearActiveEvent() {
        editor.remove(KEY_ACTIVE_EVENT_ID);
        editor.remove(KEY_ACTIVE_EVENT_ROLE);
        editor.apply();
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    /** @deprecated dùng getStaffRole() thay thế */
    @Deprecated
    public String getUserRole() {
        User user = getUserDetails();
        return user != null ? user.getRole() : null;
    }

    public boolean isOwner() {
        return "owner".equals(getStaffRole());
    }

    public boolean isManager() {
        return "manager".equals(getStaffRole());
    }

    public boolean isCheckInStaff() {
        return "check_in_staff".equals(getStaffRole());
    }
}
