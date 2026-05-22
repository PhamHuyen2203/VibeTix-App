package com.example.vibetix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibetix.Activities.AuthActivity;
import com.example.vibetix.Activities.UserMainActivity;
import com.example.vibetix.Utils.Constants;

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

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_AUTH, MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
        String role = prefs.getString(Constants.KEY_USER_ROLE, Constants.ROLE_CUSTOMER);

        Intent intent;
        if (isLoggedIn) {
            // Route to appropriate main screen based on role
            if (Constants.ROLE_ORGANIZER.equals(role)) {
                // OrganizerMainActivity placeholder — redirect to user main for now
                intent = new Intent(this, UserMainActivity.class);
            } else {
                intent = new Intent(this, UserMainActivity.class);
            }
        } else {
            intent = new Intent(this, AuthActivity.class);
        }

        startActivity(intent);
        finish(); // remove MainActivity from back stack
    }
}
