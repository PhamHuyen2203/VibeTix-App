package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Ticket;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TicketRepository {

    private final FirebaseFirestore db;
    private final CollectionReference ticketsRef;

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
    }

    // Get active (upcoming) tickets bought by the user
    public void getActiveTickets(String userId, OnTicketsLoadedListener listener) {
        ticketsRef.whereEqualTo("owner_id", userId)
                .whereEqualTo("status", "valid")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ticket> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Ticket t = mapDocToTicket(doc);
                        list.add(t);
                    }
                    listener.onSuccess(list);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Get ended (used/expired) tickets bought by the user
    public void getEndedTickets(String userId, OnTicketsLoadedListener listener) {
        ticketsRef.whereEqualTo("owner_id", userId)
                .whereIn("status", List.of("used", "expired"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ticket> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Ticket t = mapDocToTicket(doc);
                        list.add(t);
                    }
                    listener.onSuccess(list);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Get reselling tickets posted by the user
    public void getResellingTicketsByUser(String userId, OnTicketsLoadedListener listener) {
        ticketsRef.whereEqualTo("owner_id", userId)
                .whereEqualTo("status", "reselling")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ticket> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Ticket t = mapDocToTicket(doc);
                        list.add(t);
                    }
                    listener.onSuccess(list);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Get ALL active reselling tickets on the platform
    public void getAllResellingTickets(OnTicketsLoadedListener listener) {
        ticketsRef.whereEqualTo("status", "reselling")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ticket> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Ticket t = mapDocToTicket(doc);
                        list.add(t);
                    }
                    listener.onSuccess(list);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Add a purchased ticket to Firestore
    public void saveTicket(Ticket ticket, OnTicketActionListener listener) {
        Map<String, Object> data = mapTicketToMap(ticket);
        
        ticketsRef.document(ticket.getId())
                .set(data)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    /** Đăng bán lại vé. */
    public void resellTicket(String ticketId, String sellerId, long resalePrice, OnTicketActionListener listener) {
        // 1. Thêm thông tin vào ticket_transfers
        Map<String, Object> transfer = new HashMap<>();
        transfer.put("transfer_id", UUID.randomUUID().toString());
        transfer.put("user_ticket_id", ticketId);
        transfer.put("sender_id", sellerId);
        transfer.put("resale_price", resalePrice);
        transfer.put("status", "PENDING");
        transfer.put("created_at", com.google.firebase.Timestamp.now());

        db.collection(FirebaseCollections.TICKET_TRANSFERS)
                .add(transfer)
                .addOnSuccessListener(documentReference -> {
                    // 2. Cập nhật trạng thái vé cũ thành "reselling"
                    ticketsRef.document(ticketId)
                            .update("status", "reselling", "resalePrice", resalePrice)
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(listener::onFailure);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Cancel resale listing
    public void cancelResale(String ticketId, OnTicketActionListener listener) {
        ticketsRef.document(ticketId)
                .update("status", "valid", "resalePrice", 0L)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    // Helper to map Firestore doc to Ticket model
    private Ticket mapDocToTicket(QueryDocumentSnapshot doc) {
        Ticket t = new Ticket();
        t.setId(doc.getId());
        t.setOrderId(doc.getString("order_item_id"));
        t.setUserEmail(doc.getString("owner_id")); // Mapping owner_id to userEmail for UI consistency
        t.setEventId(doc.getString("event_id"));
        t.setEventTitle(doc.getString("eventTitle"));
        t.setEventDate(doc.getString("eventDate"));
        t.setEventLocation(doc.getString("eventLocation"));
        t.setEventImageUrl(doc.getString("eventImageUrl"));
        t.setTicketTypeName(doc.getString("ticketTypeName"));
        
        Long pPrice = doc.getLong("purchasePrice");
        t.setPurchasePrice(pPrice != null ? pPrice : 0L);
        
        Long rPrice = doc.getLong("resalePrice");
        t.setResalePrice(rPrice != null ? rPrice : 0L);
        
        String status = doc.getString("status");
        if ("valid".equals(status)) t.setStatus("ACTIVE");
        else if ("reselling".equals(status)) t.setStatus("RESELLING");
        else if ("used".equals(status)) t.setStatus("USED");
        else if ("expired".equals(status)) t.setStatus("EXPIRED");
        else t.setStatus(status != null ? status.toUpperCase() : "ACTIVE");
        
        t.setAttendeeName(doc.getString("attendeeName"));
        return t;
    }

    // Helper to map Ticket model to Firestore map
    private Map<String, Object> mapTicketToMap(Ticket t) {
        Map<String, Object> map = new HashMap<>();
        map.put("user_ticket_id", t.getId());
        map.put("order_item_id", t.getOrderId());
        map.put("owner_id", t.getUserEmail());
        map.put("event_id", t.getEventId());
        map.put("eventTitle", t.getEventTitle()); // Denormalized for UI
        map.put("eventDate", t.getEventDate());
        map.put("eventLocation", t.getEventLocation());
        map.put("eventImageUrl", t.getEventImageUrl());
        map.put("ticketTypeName", t.getTicketTypeName());
        map.put("purchasePrice", t.getPurchasePrice());
        map.put("resalePrice", t.getResalePrice());
        
        String status = t.getStatus();
        if ("ACTIVE".equalsIgnoreCase(status)) map.put("status", "valid");
        else if ("RESELLING".equalsIgnoreCase(status)) map.put("status", "reselling");
        else map.put("status", status.toLowerCase());
        
        map.put("attendeeName", t.getAttendeeName());
        map.put("issued_at", com.google.firebase.Timestamp.now());
        return map;
    }
}
