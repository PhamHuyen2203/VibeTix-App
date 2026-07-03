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
     * Enrich transfers: luôn join user_tickets → order_items → ticket_types để lấy tên loại vé.
     * Với transfers đã có event data denormalized: giữ event data, chỉ bổ sung ticketTypeName + eventStatus.
     * Với transfers chưa có: join đầy đủ qua user_tickets → events.
     */
    private void enrichWithEventData(List<TicketTransfer> transfers, OnTransfersLoadedListener listener) {
        if (transfers.isEmpty()) { listener.onSuccess(transfers); return; }

        // Collect ALL user_ticket_ids để join ticket type name cho tất cả
        Set<String> allTicketIds = new HashSet<>();
        Set<String> knownEventIds = new HashSet<>();
        List<TicketTransfer> needEventEnrich = new ArrayList<>();

        for (TicketTransfer t : transfers) {
            if (t.getUserTicketId() != null) allTicketIds.add(t.getUserTicketId());
            boolean hasDenormalized = t.getEventTitle() != null && !t.getEventTitle().isEmpty()
                    && t.getEventId() != null && !t.getEventId().isEmpty();
            if (hasDenormalized) knownEventIds.add(t.getEventId());
            else if (t.getUserTicketId() != null) needEventEnrich.add(t);
        }

        if (allTicketIds.isEmpty()) { listener.onSuccess(transfers); return; }

        // Bước 1: Fetch TẤT CẢ user_tickets → order_item_id, event_id, legacy ticketTypeName
        Map<String, String> ticketToOrderItem      = new HashMap<>();
        Map<String, String> ticketToLegacyTypeName = new HashMap<>();
        fetchUserTickets(new ArrayList<>(allTicketIds), 0, new HashMap<>(),
                ticketToOrderItem, ticketToLegacyTypeName, (ticketToEventMap) -> {

            // Bước 2: Fetch order_items → ticket_type_id + price
            List<String> orderItemIds = new ArrayList<>(new HashSet<>(ticketToOrderItem.values()));
            Map<String, String> orderItemToTypeId = new HashMap<>();
            fetchOrderItemPrices(orderItemIds, 0, new HashMap<>(), orderItemToTypeId, (priceMap) -> {

                // Bước 3: Fetch ticket_types → name
                List<String> typeIds = new ArrayList<>(new HashSet<>(orderItemToTypeId.values()));
                fetchTicketTypeNames(typeIds, 0, new HashMap<>(), (typeNameMap) -> {

                    // Bước 4: Fetch events
                    Set<String> allEventIds = new HashSet<>(knownEventIds);
                    allEventIds.addAll(ticketToEventMap.values());
                    fetchEvents(new ArrayList<>(allEventIds), 0, new HashMap<>(), (eventDataMap) -> {

                        for (TicketTransfer t : transfers) {
                            String utId        = t.getUserTicketId();
                            String orderItemId = utId != null ? ticketToOrderItem.get(utId) : null;

                            // Ticket type name: join chain ưu tiên, fallback legacy field
                            if (t.getTicketTypeName() == null || t.getTicketTypeName().isEmpty()) {
                                String typeName = null;
                                if (orderItemId != null) {
                                    String typeId = orderItemToTypeId.get(orderItemId);
                                    typeName = typeId != null ? typeNameMap.get(typeId) : null;
                                }
                                if (typeName == null && utId != null)
                                    typeName = ticketToLegacyTypeName.get(utId);
                                if (typeName != null) t.setTicketTypeName(typeName);
                            }

                            boolean hasDenormalized = t.getEventTitle() != null && !t.getEventTitle().isEmpty()
                                    && t.getEventId() != null && !t.getEventId().isEmpty();

                            if (hasDenormalized) {
                                Event ev = eventDataMap.get(t.getEventId());
                                if (ev != null) t.setEventStatus(ev.getStatus());
                            } else {
                                String eventId = utId != null ? ticketToEventMap.get(utId) : null;
                                if (eventId == null) continue;
                                t.setEventId(eventId);

                                if (t.getPrice() <= 0 && orderItemId != null) {
                                    Long price = priceMap.get(orderItemId);
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
                        }
                        listener.onSuccess(transfers);
                    });
                });
            });
        });
    }

    /** Fetch user_tickets in batches of 30.
     *  Populates: ticketToEvent (id→eventId), ticketToOrderItem (id→orderItemId),
     *             ticketToLegacyTypeName (id→ticketTypeName for legacy docs without order_item_id). */
    private void fetchUserTickets(List<String> ids, int offset,
                                   Map<String, String> ticketToEvent,
                                   Map<String, String> ticketToOrderItem,
                                   Map<String, String> ticketToLegacyTypeName,
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
                        if (userTicketId == null) userTicketId = doc.getId();
                        String eventId     = doc.getString("event_id");
                        String orderItemId = doc.getString("order_item_id");
                        // Legacy field written by SimulatedPaymentFragment
                        String legacyName  = doc.getString("ticketTypeName");

                        if (eventId != null) ticketToEvent.put(userTicketId, eventId);
                        if (orderItemId != null && !orderItemId.isEmpty())
                            ticketToOrderItem.put(userTicketId, orderItemId);
                        else if (legacyName != null && !legacyName.isEmpty())
                            ticketToLegacyTypeName.put(userTicketId, legacyName);
                    }
                    fetchUserTickets(ids, end, ticketToEvent, ticketToOrderItem,
                            ticketToLegacyTypeName, callback);
                })
                .addOnFailureListener(e -> callback.onReady(ticketToEvent));
    }

    /** Fetch giá vé + ticket_type_id từ order_items theo order_item_id (batch 30) */
    private void fetchOrderItemPrices(List<String> ids, int offset,
                                      Map<String, Long> priceAcc,
                                      Map<String, String> typeIdAcc,
                                      OnMapReady<String, Long> callback) {
        if (offset >= ids.size()) {
            callback.onReady(priceAcc);
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
                        String typeId = doc.getString("ticket_type_id");
                        if (orderItemId != null) {
                            if (price != null) priceAcc.put(orderItemId, price);
                            if (typeId != null && !typeId.isEmpty()) typeIdAcc.put(orderItemId, typeId);
                        }
                    }
                    fetchOrderItemPrices(ids, end, priceAcc, typeIdAcc, callback);
                })
                .addOnFailureListener(e -> callback.onReady(priceAcc));
    }

    /** Fetch tên loại vé từ ticket_types collection (batch 10 doc gets) */
    private void fetchTicketTypeNames(List<String> typeIds, int offset,
                                      Map<String, String> nameAcc,
                                      OnMapReady<String, String> callback) {
        if (offset >= typeIds.size()) { callback.onReady(nameAcc); return; }
        int end = Math.min(offset + 10, typeIds.size());
        List<com.google.android.gms.tasks.Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : typeIds.subList(offset, end)) {
            if (id != null && !id.isEmpty())
                tasks.add(db.collection(FirebaseCollections.TICKET_TYPES).document(id).get());
        }
        if (tasks.isEmpty()) { fetchTicketTypeNames(typeIds, end, nameAcc, callback); return; }

        com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    for (Object obj : results) {
                        DocumentSnapshot doc = (DocumentSnapshot) obj;
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            if (name != null) nameAcc.put(doc.getId(), name);
                        }
                    }
                    fetchTicketTypeNames(typeIds, end, nameAcc, callback);
                })
                .addOnFailureListener(e -> callback.onReady(nameAcc));
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
