package com.example.vibetix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibetix.Activities.Auth.AuthActivity;
import com.example.vibetix.Activities.Admin.AdminMainActivity;
import com.example.vibetix.Activities.User.UserMainActivity;
import com.example.vibetix.Utils.Constants;
import com.example.vibetix.Utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        
        Intent intent;
        if (sessionManager.isLoggedIn()) {
            String role = sessionManager.getUserRole();
            if (Constants.ROLE_ADMIN.equalsIgnoreCase(role)) {
                intent = new Intent(this, AdminMainActivity.class);
            } else {
                // Cả user thường và organizer đều vào UserMainActivity
                // (Organizer sẽ thấy tab OrganizerHubFragment khi nhấn nút +)
                intent = new Intent(this, UserMainActivity.class);
            }
        } else {
            intent = new Intent(this, AuthActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
