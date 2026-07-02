package com.example.vibetix;

import android.app.Application;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;

public class VibeTixApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                            PersistentCacheSettings.newBuilder()
                                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                                    .build())
                    .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        } catch (Exception e) {
            // Firestore đã khởi tạo trước → persistence đã bật mặc định, bỏ qua
            Log.w("VibeTix", "Firestore settings already configured: " + e.getMessage());
        }
    }
}
