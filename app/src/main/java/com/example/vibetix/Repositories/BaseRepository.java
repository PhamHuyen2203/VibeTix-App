package com.example.vibetix.Repositories;

import com.google.firebase.firestore.FirebaseFirestore;

public abstract class BaseRepository {
    protected FirebaseFirestore db;

    public BaseRepository() {
        this.db = FirebaseFirestore.getInstance();
    }
}
