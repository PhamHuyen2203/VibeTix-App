package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Order;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrderRepository {

    private final FirebaseFirestore db;
    private final CollectionReference ordersRef;

    public interface OnOrderActionListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public OrderRepository() {
        db = FirebaseFirestore.getInstance();
        ordersRef = db.collection(FirebaseCollections.ORDERS);
    }

    public void saveOrder(Order order, OnOrderActionListener listener) {
        ordersRef.document(order.getId())
                .set(order)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }
}
