package com.example.vibetix.Utils;

public class Constants {
    // User roles (Match Firestore values)
    public static final String ROLE_CUSTOMER  = "customer";
    public static final String ROLE_ORGANIZER = "organizer";
    public static final String ROLE_ADMIN     = "admin";

    // SharedPreferences — auth state (temporary until Firebase Auth is integrated)
    public static final String PREFS_AUTH         = "vibetix_auth";
    public static final String KEY_IS_LOGGED_IN   = "is_logged_in";
    public static final String KEY_USER_EMAIL     = "user_email";
    public static final String KEY_USER_NAME      = "user_name";
    public static final String KEY_USER_PHONE     = "user_phone";
    public static final String KEY_USER_ROLE      = "user_role";
    public static final String KEY_REMEMBER_ME    = "remember_me";
    public static final String KEY_USER_ORG_NAME  = "user_org_name";

    public static final String EVENT_STATUS_DRAFT = "DRAFT";
    public static final String EVENT_STATUS_PENDING = "PENDING";
    public static final String EVENT_STATUS_APPROVED = "APPROVED";
    public static final String EVENT_STATUS_REJECTED = "REJECTED";
    public static final String EVENT_STATUS_PUBLISHED = "PUBLISHED";
    public static final String EVENT_STATUS_CANCELLED = "CANCELLED";

    public static final String ORDER_STATUS_PENDING = "PENDING";
    public static final String ORDER_STATUS_PAID = "PAID";
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";

    public static final String PAYMENT_STATUS_PENDING = "PENDING";
    public static final String PAYMENT_STATUS_SUCCESS = "SUCCESS";
    public static final String PAYMENT_STATUS_FAILED = "FAILED";

    private Constants() {
        // Prevent initialization
    }
}