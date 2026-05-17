package com.example.vibetix.Utils;

public class Constants {
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ORGANIZER = "ORGANIZER";
    public static final String ROLE_ADMIN = "ADMIN";

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