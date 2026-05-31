package com.example.vibetix.Models;

/**
 * Ticket — vé vật lý (USER_TICKET trong ERD).
 * Mỗi đối tượng đại diện cho 1 vé vật lý đã được phát hành.
 * TODO: Thay bằng Firestore document khi backend sẵn sàng.
 */
public class Ticket {
    private String id;           // user_ticket_id
    private String ticketCode;   // UUID v4 — dùng tạo QR
    private String displayCode;  // 8 ký tự in-upper (VD: A3K9-M2PQ)
    private String eventId;
    private String eventTitle;
    private String eventDate;
    private String eventLocation;
    private String status;        // "ACTIVE" | "USED" | "EXPIRED" | "RESELLING" | "TRANSFERRED"
    private long   resalePrice;   // 0 nếu không bán lại
    private String orderItemId;

    public Ticket() {}

    public Ticket(String id, String ticketCode, String displayCode,
                  String eventId, String eventTitle, String eventDate,
                  String eventLocation, String status, long resalePrice) {
        this.id            = id;
        this.ticketCode    = ticketCode;
        this.displayCode   = displayCode;
        this.eventId       = eventId;
        this.eventTitle    = eventTitle;
        this.eventDate     = eventDate;
        this.eventLocation = eventLocation;
        this.status        = status;
        this.resalePrice   = resalePrice;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public String getId()            { return id; }
    public void   setId(String v)    { this.id = v; }

    public String getTicketCode()         { return ticketCode; }
    public void   setTicketCode(String v) { this.ticketCode = v; }

    public String getDisplayCode()         { return displayCode; }
    public void   setDisplayCode(String v) { this.displayCode = v; }

    public String getEventId()         { return eventId; }
    public void   setEventId(String v) { this.eventId = v; }

    public String getEventTitle()         { return eventTitle; }
    public void   setEventTitle(String v) { this.eventTitle = v; }

    public String getEventDate()         { return eventDate; }
    public void   setEventDate(String v) { this.eventDate = v; }

    public String getEventLocation()         { return eventLocation; }
    public void   setEventLocation(String v) { this.eventLocation = v; }

    public String getStatus()         { return status; }
    public void   setStatus(String v) { this.status = v; }

    public long getResalePrice()      { return resalePrice; }
    public void setResalePrice(long v){ this.resalePrice = v; }

    public String getOrderItemId()         { return orderItemId; }
    public void   setOrderItemId(String v) { this.orderItemId = v; }
}
