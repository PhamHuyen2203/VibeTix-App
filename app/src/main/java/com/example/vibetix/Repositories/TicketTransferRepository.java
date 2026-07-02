package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.TicketTransfer;
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

/**
 * Repository cho ticket_transfers collection.
 * Mỗi query tự động join với user_tickets → events để populate display fields.
 */
public class TicketTransferRepository {

    private final FirebaseFirestore db;
    private final CollectionReference transfersRef;
    private final CollectionReference userTicketsRef;
    private final CollectionReference eventsRef;
    private final CollectionReference orderItemsRef;

    public interface OnTransfersLoadedListener {
        void onSuccess(List<TicketTransfer> transfers);
        void onFailure(Exception e);
    }

    public interface OnTransferActionListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public TicketTransferRepository() {
        db = FirebaseFirestore.getInstance();
        transfersRef = db.collection(FirebaseCollections.TICKET_TRANSFERS);
        userTicketsRef = db.collection(FirebaseCollections.USER_TICKETS);
        eventsRef = db.collection(FirebaseCollections.EVENTS);
        orderItemsRef = db.collection(FirebaseCollections.ORDER_ITEMS);
    }

    // ── Query methods ───────────────────────────────────────────────────────────

    /** ACTIVE tab: vé đang đăng chuyển nhượng bởi user này */
    public void getPendingTransfersBySender(String userId, OnTransfersLoadedListener listener) {
        transfersRef.whereEqualTo("sender_id", userId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    List<TicketTransfer> transfers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        TicketTransfer t = doc.toObject(TicketTransfer.class);
                        if (t.getTransferId() == null) t.setTransferId(doc.getId());
                        transfers.add(t);
                    }
                    enrichWithEventData(transfers, listener);
                })
                .addOnFailureListener(listener::onFailure);
    }

    /** PAID tab: chuyển nhượng đã được chấp nhận */
    public void getAcceptedTransfersBySender(String userId, OnTransfersLoadedListener listener) {
        transfersRef.whereEqualTo("sender_id", userId)
                .whereEqualTo("status", "accepted")
                .get()
                .addOnSuccessListener(snap -> {
                    List<TicketTransfer> transfers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        TicketTransfer t = doc.toObject(TicketTransfer.class);
                        if (t.getTransferId() == null) t.setTransferId(doc.getId());
                        transfers.add(t);
                    }
                    enrichWithEventData(transfers, listener);
                })
                .addOnFailureListener(listener::onFailure);
    }

    /** CANCELLED tab */
    public void getCancelledTransfersBySender(String userId, OnTransfersLoadedListener listener) {
        transfersRef.whereEqualTo("sender_id", userId)
                .whereIn("status", List.of("cancelled", "rejected", "expired"))
                .get()
                .addOnSuccessListener(snap -> {
                    List<TicketTransfer> transfers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        TicketTransfer t = doc.toObject(TicketTransfer.class);
                        if (t.getTransferId() == null) t.setTransferId(doc.getId());
                        transfers.add(t);
                    }
                    enrichWithEventData(transfers, listener);
                })
                .addOnFailureListener(listener::onFailure);
    }

    /** PENDING tab: transfers chờ user này chấp nhận */
    public void getPendingTransfersForReceiver(String userId, OnTransfersLoadedListener listener) {
        transfersRef.whereEqualTo("receiver_id", userId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    List<TicketTransfer> transfers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        TicketTransfer t = doc.toObject(TicketTransfer.class);
                        if (t.getTransferId() == null) t.setTransferId(doc.getId());
                        transfers.add(t);
                    }
                    enrichWithEventData(transfers, listener);
                })
                .addOnFailureListener(listener::onFailure);
    }

    /** Homepage: tất cả pending transfers (vé đang được chuyển nhượng trên sàn).
     *  Chỉ hiện sự kiện approved/ongoing — ẩn completed (đã qua). */
    public void getAllPendingTransfers(OnTransfersLoadedListener listener) {
        transfersRef.whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    List<TicketTransfer> transfers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        TicketTransfer t = doc.toObject(TicketTransfer.class);
                        if (t.getTransferId() == null) t.setTransferId(doc.getId());
                        transfers.add(t);
                    }
                    enrichWithEventData(transfers, new OnTransfersLoadedListener() {
                        @Override
                        public void onSuccess(List<TicketTransfer> enriched) {
                            // Lọc bỏ transfer của event đã completed/cancelled
                            List<TicketTransfer> filtered = new ArrayList<>();
                            for (TicketTransfer t : enriched) {
                                String evStatus = t.getEventStatus();
                                if (evStatus != null &&
                                        ("completed".equals(evStatus) || "cancelled".equals(evStatus))) continue;
                                filtered.add(t);
                            }
                            listener.onSuccess(filtered);
                        }
                        @Override
                        public void onFailure(Exception e) { listener.onFailure(e); }
                    });
                })
                .addOnFailureListener(listener::onFailure);
    }

    // ── Action methods ──────────────────────────────────────────────────────────

    /** Tạo một transfer mới */
    public void createTransfer(TicketTransfer transfer, OnTransferActionListener listener) {
        String docId = transfer.getTransferId();
        transfersRef.document(docId)
                .set(transfer)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    /** Chấp nhận/hoàn tất transfer khi người mua thanh toán xong (mua lại vé). */
    public void acceptTransfer(String transferId, String buyerId, OnTransferActionListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "accepted");
        updates.put("receiver_id", buyerId);
        updates.put("completed_at", Timestamp.now());
        transfersRef.document(transferId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    /** Hủy một transfer đang pending */
    public void cancelTransfer(String transferId, OnTransferActionListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");
        updates.put("completed_at", Timestamp.now());
        transfersRef.document(transferId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    // ── Join logic: ticket_transfers → user_tickets → events ────────────────────

    /**
     * Enrich transfers với event data bằng cách:
     * 1. Lấy tất cả user_ticket_id unique → query user_tickets để lấy event_id
     * 2. Lấy tất cả event_id unique → query events để lấy title, image, date, location
     * 3. Map ngược lại vào từng TicketTransfer
     */
    private void enrichWithEventData(List<TicketTransfer> transfers, OnTransfersLoadedListener listener) {
        if (transfers.isEmpty()) {
            listener.onSuccess(transfers);
            return;
        }

        // Tách: transfers có event_id denormalized sẵn (chỉ cần lấy status event)
        //       vs transfers thiếu data (cần join qua user_tickets → events)
        List<TicketTransfer> needEnrich = new ArrayList<>();
        Set<String> knownEventIds = new HashSet<>();
        for (TicketTransfer t : transfers) {
            boolean hasDenormalized = t.getEventTitle() != null && !t.getEventTitle().isEmpty()
                    && t.getEventId() != null && !t.getEventId().isEmpty();
            if (hasDenormalized) {
                knownEventIds.add(t.getEventId());
            } else if (t.getUserTicketId() != null) {
                needEnrich.add(t);
            }
        }

        // Nếu tất cả đều đã có event_id → chỉ cần fetch status cho filter
        if (needEnrich.isEmpty()) {
            if (knownEventIds.isEmpty()) {
                listener.onSuccess(transfers);
                return;
            }
            fetchEvents(new ArrayList<>(knownEventIds), 0, new HashMap<>(), (eventDataMap) -> {
                for (TicketTransfer t : transfers) {
                    if (t.getEventId() == null) continue;
                    Event ev = eventDataMap.get(t.getEventId());
                    if (ev != null) t.setEventStatus(ev.getStatus());
                }
                listener.onSuccess(transfers);
            });
            return;
        }

        // Collect unique user_ticket_ids (chỉ những cái cần enrich)
        Set<String> ticketIds = new HashSet<>();
        for (TicketTransfer t : needEnrich) {
            if (t.getUserTicketId() != null) ticketIds.add(t.getUserTicketId());
        }

        if (ticketIds.isEmpty()) {
            listener.onSuccess(transfers);
            return;
        }

        // Bước 1: Fetch user_tickets docs → map user_ticket_id → event_id VÀ → order_item_id
        List<String> ticketIdList = new ArrayList<>(ticketIds);
        Map<String, String> ticketToOrderItem = new HashMap<>();
        // Firestore whereIn limit is 30; batch if needed
        fetchUserTickets(ticketIdList, 0, new HashMap<>(), ticketToOrderItem, (ticketToEventMap) -> {
            // ticketToEventMap: user_ticket_id → event_id
            Set<String> eventIds = new HashSet<>(ticketToEventMap.values());
            if (eventIds.isEmpty()) {
                listener.onSuccess(transfers);
                return;
            }

            // Bước 2: Fetch giá vé từ order_items (price_per_ticket) theo order_item_id
            List<String> orderItemIds = new ArrayList<>(new HashSet<>(ticketToOrderItem.values()));
            fetchOrderItemPrices(orderItemIds, 0, new HashMap<>(), (priceMap) -> {
                // priceMap: order_item_id → price_per_ticket

                // Bước 3: Fetch events docs (bao gồm cả event_id từ transfers đã denormalized)
                Set<String> allEventIds = new HashSet<>(eventIds);
                allEventIds.addAll(knownEventIds);
                List<String> eventIdList = new ArrayList<>(allEventIds);
                fetchEvents(eventIdList, 0, new HashMap<>(), (eventDataMap) -> {
                    // Set eventStatus cho transfers đã denormalized
                    for (TicketTransfer t : transfers) {
                        if (t.getEventId() != null && !t.getEventId().isEmpty()
                                && t.getEventTitle() != null && !t.getEventTitle().isEmpty()) {
                            Event ev = eventDataMap.get(t.getEventId());
                            if (ev != null) t.setEventStatus(ev.getStatus());
                        }
                    }
                    for (TicketTransfer t : transfers) {
                        String eventId = ticketToEventMap.get(t.getUserTicketId());
                        if (eventId == null) continue;
                        t.setEventId(eventId);

                        // Giá bán lại: nếu transfer chưa có giá riêng → lấy giá vé gốc (price_per_ticket)
                        if (t.getPrice() <= 0) {
                            String orderItemId = ticketToOrderItem.get(t.getUserTicketId());
                            Long price = orderItemId != null ? priceMap.get(orderItemId) : null;
                            if (price != null) {
                                t.setPrice(price);
                                if (t.getOriginalPrice() <= 0) t.setOriginalPrice(price);
                            }
                        }

                        Event ev = eventDataMap.get(eventId);
                        if (ev == null) continue;
                        t.setEventTitle(ev.getTitle());
                        t.setEventImageUrl(ev.getPosterUrl() != null ? ev.getPosterUrl() : ev.getImageUrl());
                        t.setEventDate(ev.getDate());
                        t.setEventLocation(ev.getLocation());
                        t.setEventStatus(ev.getStatus());
                    }
                    listener.onSuccess(transfers);
                });
            });
        });
    }

    /** Fetch user_tickets in batches of 30 — map user_ticket_id → event_id và → order_item_id */
    private void fetchUserTickets(List<String> ids, int offset,
                                   Map<String, String> ticketToEvent,
                                   Map<String, String> ticketToOrderItem,
                                   OnMapReady<String, String> callback) {
        if (offset >= ids.size()) {
            callback.onReady(ticketToEvent);
            return;
        }
        int end = Math.min(offset + 30, ids.size());
        List<String> batch = ids.subList(offset, end);

        userTicketsRef.whereIn("user_ticket_id", batch)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String userTicketId = doc.getString("user_ticket_id");
                        String eventId = doc.getString("event_id");
                        String orderItemId = doc.getString("order_item_id");
                        if (userTicketId != null && eventId != null) {
                            ticketToEvent.put(userTicketId, eventId);
                        }
                        if (userTicketId != null && orderItemId != null) {
                            ticketToOrderItem.put(userTicketId, orderItemId);
                        }
                    }
                    fetchUserTickets(ids, end, ticketToEvent, ticketToOrderItem, callback);
                })
                .addOnFailureListener(e -> callback.onReady(ticketToEvent));
    }

    /** Fetch giá vé từ order_items.price_per_ticket theo order_item_id (batch 30) */
    private void fetchOrderItemPrices(List<String> ids, int offset,
                                      Map<String, Long> accumulator,
                                      OnMapReady<String, Long> callback) {
        if (offset >= ids.size()) {
            callback.onReady(accumulator);
            return;
        }
        int end = Math.min(offset + 30, ids.size());
        List<String> batch = ids.subList(offset, end);

        orderItemsRef.whereIn("order_item_id", batch)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String orderItemId = doc.getString("order_item_id");
                        Long price = doc.getLong("price_per_ticket");
                        if (orderItemId != null && price != null) {
                            accumulator.put(orderItemId, price);
                        }
                    }
                    fetchOrderItemPrices(ids, end, accumulator, callback);
                })
                .addOnFailureListener(e -> callback.onReady(accumulator));
    }

    /** Fetch events by doc IDs using individual get() calls batched */
    private void fetchEvents(List<String> ids, int offset,
                              Map<String, Event> accumulator,
                              OnMapReady<String, Event> callback) {
        if (offset >= ids.size()) {
            callback.onReady(accumulator);
            return;
        }
        int end = Math.min(offset + 10, ids.size());
        List<String> batch = ids.subList(offset, end);

        // Use com.google.android.gms.tasks.Tasks to fan out individual doc gets
        List<com.google.android.gms.tasks.Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : batch) {
            tasks.add(eventsRef.document(id).get());
        }

        com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    for (Object obj : results) {
                        DocumentSnapshot doc = (DocumentSnapshot) obj;
                        if (doc.exists()) {
                            // Dùng FirestoreHelper.docToEvent() để map đúng snake_case fields
                            // (banner_url, poster_url, start_time, venue_city...) thay vì toObject()
                            Event ev = com.example.vibetix.Firebase.FirestoreHelper.docToEvent(doc);
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

    private interface OnMapReady<K, V> {
        void onReady(Map<K, V> map);
    }
}
