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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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

    // ── Public API ──────────────────────────────────────────────────────────────

    /** Upcoming tab: valid + used-but-event-not-yet-ended */
    public void getActiveTickets(String userId, OnTicketsLoadedListener listener) {
        loadTicketsCacheFirst(userId, Arrays.asList("valid", "used"), true, listener);
    }

    /** Ended tab: used (event ended) + expired */
    public void getEndedTickets(String userId, OnTicketsLoadedListener listener) {
        loadTicketsCacheFirst(userId, Arrays.asList("used", "expired"), false, listener);
    }

    /**
     * Cache-first: hiển thị dữ liệu cache ngay lập tức, sau đó cập nhật từ server.
     * Nếu không có cache (lần đầu) thì chỉ load từ server.
     */
    private void loadTicketsCacheFirst(String userId, List<String> statuses,
                                        boolean isActiveTab, OnTicketsLoadedListener listener) {
        com.google.firebase.firestore.Query query = ticketsRef
                .whereEqualTo("user_id", userId)
                .whereIn("status", statuses);

        // Bước 1: thử cache trước — hiển thị ngay nếu có
        query.get(com.google.firebase.firestore.Source.CACHE)
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        List<RawUserTicket> raw = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snap) raw.add(parseRawTicket(doc));
                        joinAndBuild(raw, isActiveTab, listener);
                    }
                });

        // Bước 2: luôn fetch từ server để cập nhật dữ liệu mới nhất
        query.get(com.google.firebase.firestore.Source.SERVER)
                .addOnSuccessListener(snap -> {
                    List<RawUserTicket> raw = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) raw.add(parseRawTicket(doc));
                    joinAndBuild(raw, isActiveTab, listener);
                })
                .addOnFailureListener(e -> {
                    // Server fail nhưng đã hiển thị cache ở trên → không cần báo lỗi
                });
    }

    public void saveTicket(Ticket ticket, OnTicketActionListener listener) {
        ticketsRef.document(ticket.getId())
                .set(ticket)
                .addOnSuccessListener(v -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    // ── Data model ──────────────────────────────────────────────────────────────

    private static class RawUserTicket {
        String userTicketId;
        String eventId;
        String orderItemId;   // null for legacy tickets
        String ticketCode;
        String displayCode;
        String status;
        Timestamp issuedAt;
        // Legacy fields (SimulatedPaymentFragment schema — Ticket model saved directly)
        String legacyTypeName;
        long   legacyPrice;
        String legacyOrderId;
        String legacyEventTitle;
        String legacyEventDate;
        String legacyEventLocation;
        String legacyEventImageUrl;
    }

    private RawUserTicket parseRawTicket(QueryDocumentSnapshot doc) {
        RawUserTicket r = new RawUserTicket();
        r.userTicketId = doc.getString("user_ticket_id");
        if (r.userTicketId == null || r.userTicketId.isEmpty()) r.userTicketId = doc.getId();
        r.eventId       = doc.getString("event_id");
        r.orderItemId   = doc.getString("order_item_id");
        r.ticketCode    = doc.getString("ticket_code");
        r.displayCode   = doc.getString("display_code");
        r.status        = doc.getString("status");
        r.issuedAt      = doc.getTimestamp("issued_at");
        // Legacy fields (present when SimulatedPaymentFragment saved a Ticket object)
        r.legacyTypeName      = doc.getString("ticketTypeName");
        Long lp = doc.getLong("purchasePrice");
        r.legacyPrice         = lp != null ? lp : 0;
        r.legacyOrderId       = doc.getString("orderId");
        r.legacyEventTitle    = doc.getString("eventTitle");
        r.legacyEventDate     = doc.getString("eventDate");
        r.legacyEventLocation = doc.getString("eventLocation");
        r.legacyEventImageUrl = doc.getString("eventImageUrl");
        return r;
    }

    // ── Core join + build ───────────────────────────────────────────────────────

    private void joinAndBuild(List<RawUserTicket> rawTickets, boolean isActiveTab,
                               OnTicketsLoadedListener listener) {
        if (rawTickets.isEmpty()) { listener.onSuccess(new ArrayList<>()); return; }

        // Separate legacy (no order_item_id) vs new tickets
        List<RawUserTicket> legacyList = new ArrayList<>();
        List<RawUserTicket> newList    = new ArrayList<>();
        Set<String> orderItemIds       = new HashSet<>();

        for (RawUserTicket r : rawTickets) {
            if (r.orderItemId == null || r.orderItemId.isEmpty()) {
                legacyList.add(r);
            } else {
                newList.add(r);
                orderItemIds.add(r.orderItemId);
            }
        }

        // Fetch order_items for new tickets to get order_id, type, price
        fetchOrderItems(new ArrayList<>(orderItemIds), 0, new ArrayList<>(), allOi -> {

            Map<String, String> oiToOrderId   = new HashMap<>();
            Map<String, long[]> oiToPriceQty  = new HashMap<>();
            Map<String, String> oiToTypeId    = new HashMap<>();

            for (Map<String, Object> oi : allOi) {
                String oiId   = str(oi, "order_item_id");
                String oId    = str(oi, "order_id");
                Long price    = (Long) oi.get("price_per_ticket");
                Long qty      = (Long) oi.get("quantity");
                String typeId = str(oi, "ticket_type_id");
                if (oiId == null || oiId.isEmpty()) continue;
                if (oId != null && !oId.isEmpty()) oiToOrderId.put(oiId, oId);
                oiToPriceQty.put(oiId, new long[]{price != null ? price : 0, qty != null ? qty : 1});
                if (typeId != null && !typeId.isEmpty()) oiToTypeId.put(oiId, typeId);
            }

            // Group new tickets by (event_id + order_id)
            LinkedHashMap<String, List<RawUserTicket>> grouped = new LinkedHashMap<>();
            for (RawUserTicket r : newList) {
                String orderId = oiToOrderId.get(r.orderItemId);
                String key = (r.eventId != null ? r.eventId : "")
                           + "_" + (orderId != null ? orderId : r.orderItemId);
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
            }

            // Collect all event IDs + ticket type IDs needed
            Set<String> eventIds = new HashSet<>();
            for (RawUserTicket r : rawTickets) if (r.eventId != null && !r.eventId.isEmpty()) eventIds.add(r.eventId);
            Set<String> typeIds = new HashSet<>(oiToTypeId.values());

            // Collect all individual IDs for pending transfer check
            List<String> allIds = new ArrayList<>();
            for (RawUserTicket r : rawTickets) if (r.userTicketId != null) allIds.add(r.userTicketId);

            // ── Parallel fetch: events + ticketTypes + pendingTransfers ──────────
            // Dùng mảng giữ kết quả để biết khi nào cả 3 xong
            @SuppressWarnings("unchecked")
            Map<String, Event>[] eventMapHolder    = new Map[]{null};
            Map<String, String>[] nameMapHolder    = new Map[]{null};
            Map<String, Boolean>[] transferHolder  = new Map[]{null};
            Set<String>[] pendingHolder            = new Set[]{null};
            int[] doneCount = {0};

            Runnable tryBuild = () -> {
                synchronized (doneCount) {
                    doneCount[0]++;
                    if (doneCount[0] < 3) return; // chờ cả 3 xong
                }
                Map<String, Event>   eventMap       = eventMapHolder[0];
                Map<String, String>  typeNameMap    = nameMapHolder[0];
                Map<String, Boolean> typeTransferMap = transferHolder[0];
                Set<String>          pendingIds      = pendingHolder[0];

                        Date now = new Date();
                        List<Ticket> result = new ArrayList<>();

                        // ── Process new (grouped) tickets ─────────────────────
                        for (Map.Entry<String, List<RawUserTicket>> entry : grouped.entrySet()) {
                            List<RawUserTicket> group = entry.getValue();
                            RawUserTicket first = group.get(0);
                            Event ev = first.eventId != null ? eventMap.get(first.eventId) : null;
                            boolean eventEnded = isEventEnded(ev, now);
                            String rawStatus   = first.status != null ? first.status.toLowerCase() : "valid";

                            // A1: filter by tab + event endtime
                            if (isActiveTab  && "used".equals(rawStatus) && eventEnded)  continue;
                            if (!isActiveTab && "used".equals(rawStatus) && !eventEnded) continue;

                            // Split by reselling status
                            List<RawUserTicket> resellingGrp = new ArrayList<>();
                            List<RawUserTicket> activeGrp    = new ArrayList<>();
                            for (RawUserTicket r : group) {
                                if (pendingIds.contains(r.userTicketId)) resellingGrp.add(r);
                                else                                      activeGrp.add(r);
                            }

                            if (!resellingGrp.isEmpty()) {
                                Ticket c = buildGroupCard(resellingGrp, ev, "RESELLING",
                                        oiToOrderId, oiToPriceQty, oiToTypeId, typeNameMap, typeTransferMap);
                                if (c != null) result.add(c);
                            }
                            if (!activeGrp.isEmpty()) {
                                Ticket c = buildGroupCard(activeGrp, ev, mapRawStatus(rawStatus),
                                        oiToOrderId, oiToPriceQty, oiToTypeId, typeNameMap, typeTransferMap);
                                if (c != null) result.add(c);
                            }
                        }

                        // ── Process legacy tickets (1 ticket = 1 card) ────────
                        for (RawUserTicket r : legacyList) {
                            Event ev = r.eventId != null ? eventMap.get(r.eventId) : null;
                            boolean eventEnded = isEventEnded(ev, now);
                            String rawStatus = r.status != null ? r.status.toLowerCase() : "valid";

                            if (isActiveTab  && "used".equals(rawStatus) && eventEnded)  continue;
                            if (!isActiveTab && "used".equals(rawStatus) && !eventEnded) continue;

                            boolean reselling  = pendingIds.contains(r.userTicketId);
                            String displayStatus = reselling ? "RESELLING" : mapRawStatus(rawStatus);

                            Ticket t = buildLegacyCard(r, ev, displayStatus);
                            result.add(t);
                        }

                finishSort(result, listener);
            };

            // Khởi động 3 fetch song song
            fetchEvents(new ArrayList<>(eventIds), 0, new HashMap<>(), evMap -> {
                eventMapHolder[0] = evMap;
                tryBuild.run();
            });

            fetchTicketTypeNames(new ArrayList<>(typeIds), 0, new HashMap<>(), new HashMap<>(),
                    (nameMap, trMap) -> {
                nameMapHolder[0] = nameMap;
                transferHolder[0] = trMap;
                tryBuild.run();
            });

            checkPendingTransfers(allIds, pending -> {
                pendingHolder[0] = pending;
                tryBuild.run();
            });
        });
    }

    // ── Card builders ───────────────────────────────────────────────────────────

    /** Build a card from a group of new tickets (all from same order, same split bucket) */
    private Ticket buildGroupCard(List<RawUserTicket> group, Event ev, String displayStatus,
                                   Map<String, String> oiToOrderId,
                                   Map<String, long[]> oiToPriceQty,
                                   Map<String, String> oiToTypeId,
                                   Map<String, String> typeNameMap,
                                   Map<String, Boolean> typeTransferMap) {
        if (group.isEmpty()) return null;
        RawUserTicket first = group.get(0);

        Ticket ticket = new Ticket();
        ticket.setId(first.userTicketId);
        ticket.setEventId(first.eventId);
        ticket.setTicketCode(first.ticketCode);
        ticket.setDisplayCode(first.displayCode);
        ticket.setIssuedAt(first.issuedAt);
        ticket.setStatus(displayStatus);

        if (ev != null) {
            ticket.setEventTitle(ev.getTitle());
            ticket.setEventDate(ev.getDate());
            ticket.setEventLocation(ev.getLocation() != null ? ev.getLocation() : ev.getVenueCity());
            ticket.setEventImageUrl(ev.getPosterUrl() != null ? ev.getPosterUrl() : ev.getImageUrl());
            ticket.setEventStatus(ev.getStatus());
            Object et = ev.getEndTimeObject();
            if (et instanceof Timestamp) ticket.setEventEndTime((Timestamp) et);
        }

        // Build ticket type info and price from order_items
        // Count per order_item_id within this sub-group
        LinkedHashMap<String, Integer> oiCountMap = new LinkedHashMap<>();
        for (RawUserTicket r : group) {
            if (r.orderItemId != null && !r.orderItemId.isEmpty())
                oiCountMap.merge(r.orderItemId, 1, Integer::sum);
        }

        StringBuilder typeBuilder      = new StringBuilder();
        StringBuilder breakdownBuilder = new StringBuilder();
        long totalPrice = 0;
        String orderId  = null;

        for (Map.Entry<String, Integer> e : oiCountMap.entrySet()) {
            String oiId  = e.getKey();
            int    count = e.getValue();
            long[] pq    = oiToPriceQty.getOrDefault(oiId, new long[]{0, 1});
            long   price = pq[0];
            String typeId = oiToTypeId.get(oiId);
            String tName  = typeId != null ? typeNameMap.get(typeId) : null;
            if (tName == null || tName.isEmpty()) tName = "Vé";
            totalPrice += price * count;
            if (typeBuilder.length()      > 0) typeBuilder.append(", ");
            if (breakdownBuilder.length() > 0) breakdownBuilder.append("|");
            typeBuilder.append(count).append("x ").append(tName);
            breakdownBuilder.append(count).append(":").append(tName).append(":").append(price);
            if (orderId == null) orderId = oiToOrderId.get(oiId);
        }

        if (typeBuilder.length() == 0) typeBuilder.append("Vé");
        ticket.setTicketTypeName(typeBuilder.toString());
        ticket.setPurchasePrice(totalPrice);
        // RESELLING card in "Vé đã mua" shows original purchase price of the tickets being sold
        if ("RESELLING".equals(displayStatus)) ticket.setResalePrice(totalPrice);
        ticket.setItemBreakdown(breakdownBuilder.toString());
        if (orderId != null) ticket.setOrderId(orderId);

        // Build parallel lists for ticket type selection (B2) — only transferable tickets
        List<String> ids             = new ArrayList<>();
        List<String> typeNames       = new ArrayList<>();
        boolean anyTransferable      = false;
        for (RawUserTicket r : group) {
            String oiId   = r.orderItemId;
            String typeId = oiId != null ? oiToTypeId.get(oiId) : null;
            String tName  = typeId != null ? typeNameMap.get(typeId) : null;
            if (tName == null || tName.isEmpty()) tName = "Vé";
            Boolean isTransferable = typeId != null ? typeTransferMap.get(typeId) : null;
            boolean canTransfer    = isTransferable == null || isTransferable; // default true if field missing
            if (canTransfer) {
                ids.add(r.userTicketId);
                typeNames.add(tName);
                anyTransferable = true;
            }
        }
        ticket.setIndividualTicketIds(ids);
        ticket.setIndividualTicketTypeNames(typeNames);
        ticket.setTransferable(anyTransferable);

        return ticket;
    }

    /** Build a card from a single legacy ticket */
    private Ticket buildLegacyCard(RawUserTicket r, Event ev, String displayStatus) {
        Ticket ticket = new Ticket();
        ticket.setId(r.userTicketId);
        ticket.setEventId(r.eventId);
        ticket.setOrderId(r.legacyOrderId);
        ticket.setTicketCode(r.ticketCode);
        ticket.setDisplayCode(r.displayCode);
        ticket.setIssuedAt(r.issuedAt);
        ticket.setStatus(displayStatus);

        // Prefer Firebase event data; fall back to legacy denormalized fields
        if (ev != null) {
            ticket.setEventTitle(ev.getTitle());
            ticket.setEventDate(ev.getDate());
            ticket.setEventLocation(ev.getLocation() != null ? ev.getLocation() : ev.getVenueCity());
            ticket.setEventImageUrl(ev.getPosterUrl() != null ? ev.getPosterUrl() : ev.getImageUrl());
            ticket.setEventStatus(ev.getStatus());
            Object et = ev.getEndTimeObject();
            if (et instanceof Timestamp) ticket.setEventEndTime((Timestamp) et);
        } else {
            ticket.setEventTitle(r.legacyEventTitle);
            ticket.setEventDate(r.legacyEventDate);
            ticket.setEventLocation(r.legacyEventLocation);
            ticket.setEventImageUrl(r.legacyEventImageUrl);
        }

        String typeName = r.legacyTypeName != null && !r.legacyTypeName.isEmpty() ? r.legacyTypeName : "Vé";
        ticket.setTicketTypeName(typeName);
        ticket.setPurchasePrice(r.legacyPrice);

        List<String> ids       = new ArrayList<>();
        List<String> typeNames = new ArrayList<>();
        ids.add(r.userTicketId);
        typeNames.add(typeName);
        ticket.setIndividualTicketIds(ids);
        ticket.setIndividualTicketTypeNames(typeNames);

        return ticket;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private boolean isEventEnded(Event ev, Date now) {
        if (ev == null) return false;
        Object et = ev.getEndTimeObject();
        if (et instanceof Timestamp) return ((Timestamp) et).toDate().before(now);
        String st = ev.getStatus();
        return "completed".equalsIgnoreCase(st) || "cancelled".equalsIgnoreCase(st);
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

    private static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof String ? (String) v : null;
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

    // ── Firebase fetchers ────────────────────────────────────────────────────────

    private interface OnOrderItemsReady { void onReady(List<Map<String, Object>> items); }
    private interface OnMapReady        { void onReady(Map<String, Event> map); }
    private interface OnNameMapReady    { void onReady(Map<String, String> nameMap, Map<String, Boolean> transferMap); }
    private interface OnSetReady        { void onReady(Set<String> set); }

    private void fetchOrderItems(List<String> ids, int offset,
                                  List<Map<String, Object>> acc, OnOrderItemsReady cb) {
        if (ids.isEmpty() || offset >= ids.size()) { cb.onReady(acc); return; }
        int end = Math.min(offset + 30, ids.size());
        List<String> batch = new ArrayList<>();
        for (String id : ids.subList(offset, end)) if (id != null && !id.isEmpty()) batch.add(id);
        if (batch.isEmpty()) { fetchOrderItems(ids, end, acc, cb); return; }

        db.collection(FirebaseCollections.ORDER_ITEMS)
                .whereIn("order_item_id", batch)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("order_item_id",   doc.getString("order_item_id"));
                        item.put("order_id",        doc.getString("order_id"));
                        item.put("price_per_ticket",doc.getLong("price_per_ticket"));
                        item.put("quantity",        doc.getLong("quantity"));
                        item.put("ticket_type_id",  doc.getString("ticket_type_id"));
                        acc.add(item);
                    }
                    fetchOrderItems(ids, end, acc, cb);
                })
                .addOnFailureListener(e -> cb.onReady(acc));
    }

    private void fetchEvents(List<String> ids, int offset,
                              Map<String, Event> acc, OnMapReady cb) {
        if (ids.isEmpty() || offset >= ids.size()) { cb.onReady(acc); return; }
        int end = Math.min(offset + 10, ids.size());
        List<com.google.android.gms.tasks.Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : ids.subList(offset, end))
            if (id != null && !id.isEmpty()) tasks.add(eventsRef.document(id).get());
        if (tasks.isEmpty()) { fetchEvents(ids, end, acc, cb); return; }

        com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    for (Object obj : results) {
                        DocumentSnapshot doc = (DocumentSnapshot) obj;
                        if (doc.exists()) {
                            Event ev = FirestoreHelper.docToEvent(doc);
                            if (ev != null) {
                                if (ev.getEventId() == null) ev.setEventId(doc.getId());
                                acc.put(doc.getId(), ev);
                            }
                        }
                    }
                    fetchEvents(ids, end, acc, cb);
                })
                .addOnFailureListener(e -> cb.onReady(acc));
    }

    private void fetchTicketTypeNames(List<String> ids, int offset,
                                       Map<String, String> nameAcc, Map<String, Boolean> transferAcc,
                                       OnNameMapReady cb) {
        if (ids.isEmpty() || offset >= ids.size()) { cb.onReady(nameAcc, transferAcc); return; }
        int end = Math.min(offset + 10, ids.size());
        List<com.google.android.gms.tasks.Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : ids.subList(offset, end))
            if (id != null && !id.isEmpty())
                tasks.add(db.collection(FirebaseCollections.TICKET_TYPES).document(id).get());
        if (tasks.isEmpty()) { fetchTicketTypeNames(ids, end, nameAcc, transferAcc, cb); return; }

        com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    for (Object obj : results) {
                        DocumentSnapshot doc = (DocumentSnapshot) obj;
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            if (name != null) nameAcc.put(doc.getId(), name);
                            Boolean isTransferable = doc.getBoolean("is_transferable");
                            transferAcc.put(doc.getId(), isTransferable != null && isTransferable);
                        }
                    }
                    fetchTicketTypeNames(ids, end, nameAcc, transferAcc, cb);
                })
                .addOnFailureListener(e -> cb.onReady(nameAcc, transferAcc));
    }

    private void checkPendingTransfers(List<String> ticketIds, OnSetReady cb) {
        if (ticketIds.isEmpty()) { cb.onReady(new HashSet<>()); return; }
        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        checkPendingBatch(ticketIds, 0, new HashSet<>(), currentUid, cb);
    }

    private void checkPendingBatch(List<String> ids, int offset, Set<String> acc,
                                    String senderUid, OnSetReady cb) {
        if (offset >= ids.size()) { cb.onReady(acc); return; }
        int end = Math.min(offset + 30, ids.size());
        List<String> batch = new ArrayList<>();
        for (String id : ids.subList(offset, end)) if (id != null && !id.isEmpty()) batch.add(id);
        if (batch.isEmpty()) { checkPendingBatch(ids, end, acc, senderUid, cb); return; }

        com.google.firebase.firestore.Query q = db.collection(FirebaseCollections.TICKET_TRANSFERS)
                .whereIn("user_ticket_id", batch)
                .whereEqualTo("status", "pending");
        // Filter by sender to avoid false positives from other users' transfers
        if (senderUid != null && !senderUid.isEmpty()) q = q.whereEqualTo("sender_id", senderUid);

        q.get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String utId = doc.getString("user_ticket_id");
                        if (utId != null) acc.add(utId);
                    }
                    checkPendingBatch(ids, end, acc, senderUid, cb);
                })
                .addOnFailureListener(e -> cb.onReady(acc));
    }
}
