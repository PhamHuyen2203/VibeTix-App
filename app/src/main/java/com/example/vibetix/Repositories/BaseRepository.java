package com.example.vibetix.Repositories;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

public abstract class BaseRepository {
    protected FirebaseFirestore db;

    public BaseRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Cache-first helper: gọi callback ngay từ cache (nếu có), rồi gọi lại từ server.
     * Dùng cho các query cần hiển thị nhanh + dữ liệu mới nhất.
     */
    public interface OnQueryResult {
        void onResult(QuerySnapshot snapshot);
        default void onError(Exception e) {}
    }

    protected void queryCacheFirst(Query query, OnQueryResult callback) {
        // Bước 1: cache — hiển thị ngay
        query.get(Source.CACHE)
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) callback.onResult(snap);
                });

        // Bước 2: server — cập nhật dữ liệu mới
        query.get(Source.SERVER)
                .addOnSuccessListener(callback::onResult)
                .addOnFailureListener(e -> callback.onError(e));
    }
}
