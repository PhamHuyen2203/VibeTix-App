package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Ticket;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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
    public void getActiveTickets(String userEmail, OnTicketsLoadedListener listener) {
        ticketsRef.whereEqualTo("userEmail", userEmail)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ticket> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Ticket t = doc.toObject(Ticket.class);
                        if (t.getId() == null) t.setId(doc.getId());
                        list.add(t);
                    }
                    listener.onSuccess(list);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Get ended (used/expired) tickets bought by the user
    public void getEndedTickets(String userEmail, OnTicketsLoadedListener listener) {
        ticketsRef.whereEqualTo("userEmail", userEmail)
                .whereIn("status", List.of("USED", "EXPIRED"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ticket> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Ticket t = doc.toObject(Ticket.class);
                        if (t.getId() == null) t.setId(doc.getId());
                        list.add(t);
                    }
                    listener.onSuccess(list);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Get reselling tickets posted by the user
    public void getResellingTicketsByUser(String userEmail, OnTicketsLoadedListener listener) {
        ticketsRef.whereEqualTo("userEmail", userEmail)
                .whereEqualTo("status", "RESELLING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ticket> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Ticket t = doc.toObject(Ticket.class);
                        if (t.getId() == null) t.setId(doc.getId());
                        list.add(t);
                    }
                    listener.onSuccess(list);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Get ALL active reselling tickets on the platform (for Home / Search listing)
    public void getAllResellingTickets(OnTicketsLoadedListener listener) {
        ticketsRef.whereEqualTo("status", "RESELLING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ticket> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Ticket t = doc.toObject(Ticket.class);
                        if (t.getId() == null) t.setId(doc.getId());
                        list.add(t);
                    }
                    listener.onSuccess(list);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Add a purchased ticket to Firestore
    public void saveTicket(Ticket ticket, OnTicketActionListener listener) {
        ticketsRef.document(ticket.getId())
                .set(ticket)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    /** Đăng bán lại vé. */
    public void resellTicket(String ticketId, long resalePrice, OnTicketActionListener listener) {
        ticketsRef.document(ticketId)
                .update("status", "RESELLING", "resalePrice", resalePrice)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    // Buy a resale ticket (Transfer ownership)
    public void buyResaleTicket(String ticketId, String newOwnerEmail, String newAttendeeName, OnTicketActionListener listener) {
        ticketsRef.document(ticketId)
                .update("userEmail", newOwnerEmail, "attendeeName", newAttendeeName, "status", "ACTIVE", "resalePrice", 0L)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    // Cancel resale listing (take ticket off resale back to active)
    public void cancelResale(String ticketId, OnTicketActionListener listener) {
        ticketsRef.document(ticketId)
                .update("status", "ACTIVE", "resalePrice", 0L)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }
}
