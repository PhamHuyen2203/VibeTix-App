package com.example.vibetix.Firebase;

/**
 * Hằng số tên collection Firestore — lowercase snake_case, đồng bộ với Firebase Console.
 */
public class FirebaseCollections {
    // Core entities
    public static final String USERS         = "users";
    public static final String ORGANIZERS    = "organizers";
    public static final String EVENTS        = "events";

    // Ticket & Order
    public static final String TICKET_TYPES     = "ticket_types";
    public static final String ORDERS           = "orders";
    public static final String ORDER_ITEMS      = "order_items";
    public static final String USER_TICKETS     = "user_tickets";
    public static final String TICKET_TRANSFERS = "ticket_transfers";

    // Promotion & Access
    public static final String DISCOUNTS     = "discounts";
    public static final String EVENT_STAFF   = "event_staff";

    // Communication
    public static final String NOTIFICATIONS = "notifications";

    // Legacy (kept for Admin screens — to be migrated later)
    public static final String CATEGORIES    = "categories";
    public static final String VENUES        = "venues";
    public static final String DESTINATIONS  = "destinations"; // legacy

    private FirebaseCollections() {}
}
