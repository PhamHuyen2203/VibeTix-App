package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Category;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QuerySnapshot;

public class CategoryRepository extends BaseRepository {

    public Task<QuerySnapshot> getAllCategories() {
        return db.collection(FirebaseCollections.CATEGORIES).get();
    }

    public Task<DocumentReference> addCategory(Category category) {
        return db.collection(FirebaseCollections.CATEGORIES).add(category);
    }

    public Task<Void> updateCategory(String id, Category category) {
        return db.collection(FirebaseCollections.CATEGORIES).document(id).set(category);
    }

    public Task<Void> deleteCategory(String id) {
        return db.collection(FirebaseCollections.CATEGORIES).document(id).delete();
    }
}
