package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Destination;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QuerySnapshot;

public class DestinationRepository extends BaseRepository {

    public Task<QuerySnapshot> getAllDestinations() {
        return db.collection(FirebaseCollections.DESTINATIONS).get();
    }

    public Task<DocumentReference> addDestination(Destination destination) {
        return db.collection(FirebaseCollections.DESTINATIONS).add(destination);
    }

    public Task<Void> updateDestination(String id, Destination destination) {
        return db.collection(FirebaseCollections.DESTINATIONS).document(id).set(destination);
    }

    public Task<Void> deleteDestination(String id) {
        return db.collection(FirebaseCollections.DESTINATIONS).document(id).delete();
    }
}
