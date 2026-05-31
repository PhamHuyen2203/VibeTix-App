package com.example.vibetix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibetix.Activities.AuthActivity;
import com.example.vibetix.Activities.UserMainActivity;
import com.example.vibetix.Utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Splash / router activity.
 *
 * Checks SharedPreferences for an existing login session:
 *   • Logged in  → UserMainActivity (or OrganizerMainActivity when built)
 *   • Not logged in → AuthActivity (Login screen)
 *
 * TODO: when Firebase Auth is integrated, replace the SharedPreferences check
 *       with FirebaseAuth.getInstance().getCurrentUser() != null.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dùng Firebase Auth làm nguồn sự thật — nếu user đã đăng nhập trước đó
        // Firebase tự phục hồi session mà không cần SharedPreferences
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        Intent intent;
        if (currentUser != null) {
            intent = new Intent(this, UserMainActivity.class);
        } else {
            intent = new Intent(this, AuthActivity.class);
        }

        startActivity(intent);
        finish(); // remove MainActivity from back stack
    }
}
