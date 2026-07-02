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

    // Extended profile info
    public static final String KEY_USER_AVATAR       = "user_avatar_url";
    public static final String KEY_USER_DOB          = "user_dob";
    public static final String KEY_USER_GENDER       = "user_gender";      // "male"/"female"/"other"
    public static final String KEY_TICKETS_BOUGHT    = "tickets_bought";
    public static final String KEY_EVENTS_INTERESTED = "events_interested";

    // Organizer profile (mock - replace with Firebase when ready)
    public static final String PREFS_PROFILE        = "vibetix_profile";
    public static final String KEY_ORG_BRAND_NAME   = "org_brand_name";
    public static final String KEY_ORG_DESCRIPTION  = "org_description";
    public static final String KEY_ORG_WEBSITE      = "org_website";
    public static final String KEY_ORG_STATUS       = "org_status";
    public static final String ORG_STATUS_NONE      = "none";
    public static final String ORG_STATUS_PENDING   = "pending";
    public static final String ORG_STATUS_APPROVED  = "approved";

    // Payment methods (JSON array in SharedPreferences)
    public static final String KEY_PAYMENT_METHODS  = "payment_methods";
    public static final String KEY_DEFAULT_PAYMENT  = "default_payment_id";
    public static final String PAYMENT_MOMO         = "momo";
    public static final String PAYMENT_ZALOPAY      = "zalopay";
    public static final String PAYMENT_VNPAY        = "vnpay";
    public static final String PAYMENT_VISA         = "visa";
    public static final String PAYMENT_ATM          = "atm";

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