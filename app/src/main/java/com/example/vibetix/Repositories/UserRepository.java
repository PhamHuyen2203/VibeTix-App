package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class UserRepository extends BaseRepository {
    
    public Task<Void> saveUser(User user) {
        return db.collection(FirebaseCollections.USERS).document(user.getUserId()).set(user);
    }

    public Task<DocumentSnapshot> getUserById(String userId) {
        return db.collection(FirebaseCollections.USERS).document(userId).get();
    }

    public Task<QuerySnapshot> getUserByEmail(String email) {
        return db.collection(FirebaseCollections.USERS).whereEqualTo("email", email).get();
    }
}
