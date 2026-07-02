package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Firebase.FirestoreHelper;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.Ticket;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TicketRepository {

    private final FirebaseFirestore db;
    private final CollectionReference ticketsRef;
    private final CollectionReference eventsRef;

    public interface OnTicketsLoadedListener {
        void onSuccess(List<Ticket> tickets);
        void onFailure(Exception e);
    }

    public interface OnTicketActionListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public TicketRepository() {
        db = FirebaseFirestore.getInstance();
        ticketsRef = db.collection(FirebaseCollections.USER_TICKETS);
        eventsRef = db.collection(FirebaseCollections.EVENTS);
    }

    /**
     * Get active (upcoming) tickets for user — queries real schema:
     * user_tickets WHERE user_id == userId AND status == "valid"
     * Then joins with events collection to populate display fields.
     */
    public void getActiveTickets(String userId, OnTicketsLoadedListener listener) {
        ticketsRef.whereEqualTo("user_id", userId)
                .whereEqualTo("status", "valid")
                .get()
                .addOnSuccessListener(snap -> {
                    List<RawUserTicket> rawTickets = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        rawTickets.add(parseRawTicket(doc));
                    }
                    joinWithEvents(rawTickets, "ACTIVE", listener);
                })
                .addOnFailureListener(listener::onFailure);
    }

    /**
     * Get ended (used/expired) tickets for user — queries real schema:
     * user_tickets WHERE user_id == userId AND status IN ["used", "expired"]
     */
    public void getEndedTickets(String userId, OnTicketsLoadedListener listener) {
        ticketsRef.whereEqualTo("user_id", userId)
                .whereIn("status", List.of("used", "expired"))
                .get()
                .addOnSuccessListener(snap -> {
                    List<RawUserTicket> rawTickets = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        rawTickets.add(parseRawTicket(doc));
                    }
                    joinWithEvents(rawTickets, null, listener);
                })
                .addOnFailureListener(listener::onFailure);
    }

    /**
     * Add a purchased ticket to Firestore (legacy — used by SimulatedPaymentFragment).
     */
    public void saveTicket(Ticket ticket, OnTicketActionListener listener) {
        ticketsRef.document(ticket.getId())
                .set(ticket)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    // ── Internal helpers ────────────────────────────────────────────────────────

    /** Lightweight struct for raw user_ticket doc fields */
    private static class RawUserTicket {
        String userTicketId;
        String eventId;
        String orderItemId;
        String ticketCode;
        String displayCode;
        String status;
        boolean isUsed;
        Timestamp issuedAt;
    }

    private RawUserTicket parseRawTicket(QueryDocumentSnapshot doc) {
        RawUserTicket raw = new RawUserTicket();
        raw.userTicketId = doc.getString("user_ticket_id");
        if (raw.userTicketId == null) raw.userTicketId = doc.getId();
        raw.eventId = doc.getString("event_id");
        raw.orderItemId = doc.getString("order_item_id");
        raw.ticketCode = doc.getString("ticket_code");
        raw.displayCode = doc.getString("display_code");
        raw.status = doc.getString("status");
        Boolean used = doc.getBoolean("is_used");
        raw.isUsed = used != null && used;
        raw.issuedAt = doc.getTimestamp("issued_at");
        return raw;
    }

    /**
     * Join raw user_tickets with events collection to build display Ticket objects.
     * @param displayStatus override status for adapter (e.g. "ACTIVE"), or null to map from raw status.
     */
    private void joinWithEvents(List<RawUserTicket> rawTickets, String displayStatus, OnTicketsLoadedListener listener) {
        if (rawTickets.isEmpty()) {
            listener.onSuccess(new ArrayList<>());
            return;
        }

        // Collect unique event_ids
        Set<String> eventIds = new HashSet<>();
        for (RawUserTicket raw : rawTickets) {
            if (raw.eventId != null) eventIds.add(raw.eventId);
        }

        if (eventIds.isEmpty()) {
            // No event_ids — return tickets without event info
            List<Ticket> result = new ArrayList<>();
            for (RawUserTicket raw : rawTickets) {
                result.add(buildTicket(raw, null, displayStatus));
            }
            listener.onSuccess(result);
            return;
        }

        // Group tickets by event_id → 1 card per sự kiện (gộp mọi loại vé cùng event)
        java.util.LinkedHashMap<String, List<RawUserTicket>> grouped = new java.util.LinkedHashMap<>();
        for (RawUserTicket raw : rawTickets) {
            String key = raw.eventId != null ? raw.eventId : raw.userTicketId;
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(raw);
        }

        // Collect order_item_ids to fetch price + ticket_type info
        Set<String> orderItemIds = new HashSet<>();
        for (RawUserTicket raw : rawTickets) {
            if (raw.orderItemId != null) orderItemIds.add(raw.orderItemId);
        }

        // Batch fetch events
        List<String> eventIdList = new ArrayList<>(eventIds);
        fetchEvents(eventIdList, 0, new HashMap<>(), eventMap -> {
            // Fetch order_items for price + quantity per type
            fetchOrderItems(new ArrayList<>(orderItemIds), 0, new ArrayList<>(), allOrderItems -> {
                // Build order_item_id → {price, quantity, ticket_type_id, order_id} map
                Map<String, long[]> oiMap = new HashMap<>(); // order_item_id → [price, qty]
                Map<String, String> oiTypeMap = new HashMap<>(); // order_item_id → ticket_type_id
                Map<String, String> oiOrderMap = new HashMap<>(); // order_item_id → order_id
                for (Map<String, Object> oi : allOrderItems) {
                    String oiId = (String) oi.get("order_item_id");
                    Long price = (Long) oi.get("price_per_ticket");
                    Long qty = (Long) oi.get("quantity");
                    String typeId = (String) oi.get("ticket_type_id");
                    String orderId = (String) oi.get("order_id");
                    if (oiId != null) {
                        oiMap.put(oiId, new long[]{price != null ? price : 0, qty != null ? qty : 1});
                        if (typeId != null) oiTypeMap.put(oiId, typeId);
                        if (orderId != null) oiOrderMap.put(oiId, orderId);
                    }
                }

                // Fetch ticket_type names
                Set<String> typeIds = new HashSet<>(oiTypeMap.values());
                typeIds.remove("");
                typeIds.remove(null);
                fetchTicketTypeNames(new ArrayList<>(typeIds), 0, new HashMap<>(), typeNameMap -> {
                    List<Ticket> result = new ArrayList<>();
                    for (Map.Entry<String, List<RawUserTicket>> entry : grouped.entrySet()) {
                        List<RawUserTicket> group = entry.getValue();
                        RawUserTicket first = group.get(0);
                        Event ev = first.eventId != null ? eventMap.get(first.eventId) : null;

                        Ticket ticket = buildTicket(first, ev, displayStatus);

                        // Build type name + total price từ order_items
                        Set<String> seenOi = new HashSet<>();
                        StringBuilder typeBuilder = new StringBuilder();
                        StringBuilder breakdownBuilder = new StringBuilder();
                        long totalPrice = 0;
                        String realOrderId = null;
                        for (RawUserTicket raw : group) {
                            if (raw.orderItemId != null && !seenOi.contains(raw.orderItemId)) {
                                seenOi.add(raw.orderItemId);
                                long[] pq = oiMap.getOrDefault(raw.orderItemId, new long[]{0, 1});
                                long price = pq[0];
                                long qty = pq[1];
                                totalPrice += price * qty;

                                // Tên loại vé
                                String typeId = oiTypeMap.get(raw.orderItemId);
                                String tName = typeId != null ? typeNameMap.get(typeId) : null;
                                if (tName == null) tName = "Vé";
                                if (typeBuilder.length() > 0) typeBuilder.append(", ");
                                typeBuilder.append(qty + "x ").append(tName);

                                // Breakdown "qty:name:price"
                                if (breakdownBuilder.length() > 0) breakdownBuilder.append("|");
                                breakdownBuilder.append(qty).append(":").append(tName).append(":").append(price);

                                // Lấy orderId thật từ order_item
                                if (realOrderId == null) realOrderId = oiOrderMap.get(raw.orderItemId);
                            }
                        }
                        if (typeBuilder.length() == 0) typeBuilder.append("Vé");
                        ticket.setTicketTypeName(typeBuilder.toString());
                        ticket.setPurchasePrice(totalPrice); // tạm — sẽ override bằng order.total_amount
                        ticket.setItemBreakdown(breakdownBuilder.toString());
                        if (realOrderId != null) ticket.setOrderId(realOrderId);

                        result.add(ticket);
                    }

                    // Fetch orders để lấy total_amount (đã trừ discount) → giá đúng trên card
                    Set<String> orderIds = new HashSet<>();
                    for (Ticket t : result) {
                        if (t.getOrderId() != null) orderIds.add(t.getOrderId());
                    }
                    if (orderIds.isEmpty()) {
                        finishSort(result, listener);
                        return;
                    }
                    fetchOrderTotals(new ArrayList<>(orderIds), 0, new HashMap<>(), orderTotalMap -> {
                        for (Ticket t : result) {
                            Long paidAmount = orderTotalMap.get(t.getOrderId());
                            if (paidAmount != null && paidAmount > 0) {
                                t.setPurchasePrice(paidAmount);
                            }
                        }
                        finishSort(result, listener);
                    });
                });
            });
        });
    }

    private Ticket buildTicket(RawUserTicket raw, Event ev, String displayStatus) {
        Ticket ticket = new Ticket();
        ticket.setId(raw.userTicketId);
        ticket.setOrderId(raw.orderItemId);
        ticket.setEventId(raw.eventId);
        ticket.setTicketCode(raw.ticketCode);
        ticket.setDisplayCode(raw.displayCode);
        ticket.setIssuedAt(raw.issuedAt);

        if (displayStatus != null) {
            ticket.setStatus(displayStatus);
        } else {
            ticket.setStatus(mapRawStatus(raw.status));
        }

        if (ev != null) {
            ticket.setEventTitle(ev.getTitle());
            ticket.setEventDate(ev.getDate());
            ticket.setEventLocation(ev.getLocation() != null ? ev.getLocation() : ev.getVenueCity());
            String imageUrl = ev.getPosterUrl() != null ? ev.getPosterUrl() : ev.getImageUrl();
            ticket.setEventImageUrl(imageUrl);
            ticket.setPurchasePrice(ev.getPrice());
            ticket.setEventStatus(ev.getStatus());
        }

        return ticket;
    }

    private String mapRawStatus(String rawStatus) {
        if (rawStatus == null) return "EXPIRED";
        switch (rawStatus.toLowerCase()) {
            case "valid":       return "ACTIVE";
            case "used":        return "USED";
            case "expired":     return "EXPIRED";
            case "transferred": return "RESOLD";
            case "cancelled":   return "RESALE_CANCELLED";
            default:            return "EXPIRED";
        }
    }

    /** Fetch events by doc IDs using individual get() calls, batched 10 at a time */
    private void fetchEvents(List<String> ids, int offset,
                             Map<String, Event> accumulator,
                             OnMapReady callback) {
        if (offset >= ids.size()) {
            callback.onReady(accumulator);
            return;
        }
        int end = Math.min(offset + 10, ids.size());
        List<String> batch = ids.subList(offset, end);

        List<com.google.android.gms.tasks.Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : batch) {
            tasks.add(eventsRef.document(id).get());
        }

        com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    for (Object obj : results) {
                        DocumentSnapshot doc = (DocumentSnapshot) obj;
                        if (doc.exists()) {
                            Event ev = FirestoreHelper.docToEvent(doc);
                            if (ev != null) {
                                if (ev.getEventId() == null) ev.setEventId(doc.getId());
                                accumulator.put(doc.getId(), ev);
                            }
                        }
                    }
                    fetchEvents(ids, end, accumulator, callback);
                })
                .addOnFailureListener(e -> callback.onReady(accumulator));
    }

    private interface OnMapReady {
        void onReady(Map<String, Event> map);
    }

    private interface OnOrderItemsReady {
        void onReady(List<Map<String, Object>> items);
    }

    /** Fetch full order_items by order_item_id (batch 30) */
    private void fetchOrderItems(List<String> ids, int offset,
                                 List<Map<String, Object>> acc, OnOrderItemsReady callback) {
        if (ids.isEmpty() || offset >= ids.size()) { callback.onReady(acc); return; }
        int end = Math.min(offset + 30, ids.size());
        List<String> batch = ids.subList(offset, end);

        db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereIn("order_item_id", batch)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("order_item_id", doc.getString("order_item_id"));
                        item.put("order_id", doc.getString("order_id"));
                        item.put("price_per_ticket", doc.getLong("price_per_ticket"));
                        item.put("quantity", doc.getLong("quantity"));
                        item.put("ticket_type_id", doc.getString("ticket_type_id"));
                        acc.add(item);
                    }
                    fetchOrderItems(ids, end, acc, callback);
                })
                .addOnFailureListener(e -> callback.onReady(acc));
    }

    private interface OnNameMapReady {
        void onReady(Map<String, String> map);
    }

    private void finishSort(List<Ticket> result, OnTicketsLoadedListener listener) {
        result.sort((a, b) -> {
            if (a.getIssuedAt() == null && b.getIssuedAt() == null) return 0;
            if (a.getIssuedAt() == null) return 1;
            if (b.getIssuedAt() == null) return -1;
            return b.getIssuedAt().compareTo(a.getIssuedAt());
        });
        listener.onSuccess(result);
    }

    private interface OnLongMapReady {
        void onReady(Map<String, Long> map);
    }

    /** Fetch orders by order_id (batch 10) → map order_id → total_amount */
    private void fetchOrderTotals(List<String> ids, int offset,
                                  Map<String, Long> acc, OnLongMapReady callback) {
        if (ids.isEmpty() || offset >= ids.size()) { callback.onReady(acc); return; }
        int end = Math.min(offset + 10, ids.size());
        List<String> batch = ids.subList(offset, end);

        List<com.google.android.gms.tasks.Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : batch) {
            tasks.add(db.collection(FirebaseCollections.ORDERS).document(id).get());
        }

        com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    for (Object obj : results) {
                        DocumentSnapshot doc = (DocumentSnapshot) obj;
                        if (doc.exists()) {
                            Long total = doc.getLong("total_amount");
                            if (total != null) acc.put(doc.getId(), total);
                        }
                    }
                    fetchOrderTotals(ids, end, acc, callback);
                })
                .addOnFailureListener(e -> callback.onReady(acc));
    }

    /** Fetch ticket type names by ticket_type_id (batch 10) */
    private void fetchTicketTypeNames(List<String> ids, int offset,
                                      Map<String, String> acc, OnNameMapReady callback) {
        if (ids.isEmpty() || offset >= ids.size()) { callback.onReady(acc); return; }
        int end = Math.min(offset + 10, ids.size());
        List<String> batch = ids.subList(offset, end);

        List<com.google.android.gms.tasks.Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : batch) {
            tasks.add(db.collection(FirebaseCollections.TICKET_TYPES).document(id).get());
        }

        com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    for (Object obj : results) {
                        DocumentSnapshot doc = (DocumentSnapshot) obj;
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            if (name != null) acc.put(doc.getId(), name);
                        }
                    }
                    fetchTicketTypeNames(ids, end, acc, callback);
                })
                .addOnFailureListener(e -> callback.onReady(acc));
    }
}
