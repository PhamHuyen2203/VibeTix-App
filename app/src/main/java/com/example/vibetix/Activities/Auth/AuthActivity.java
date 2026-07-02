package com.example.vibetix.Activities.Auth;

import android.os.Bundle;
import android.view.Window;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vibetix.Fragments.Auth.LoginFragment;
import com.example.vibetix.R;

/**
 * AuthActivity — container for LoginFragment and RegisterFragment.
 *
 * Navigation flow:
 *   MainActivity → (not logged in) → AuthActivity (LoginFragment)
 *   LoginFragment  → [Đăng ký]          → RegisterFragment
 *   RegisterFragment → [Back] / [Đã có TK] → LoginFragment
 *   LoginFragment  → [Đăng nhập OK]     → UserMainActivity / OrganizerMainActivity
 *   UserMainActivity → [Đăng xuất]      → AuthActivity (clear back stack)
 */
public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Show LoginFragment on first launch (avoid re-adding on config change)
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.authContainer, new LoginFragment())
                    .commit();
        }
    }

    /**
     * Navigate to RegisterFragment, adding LoginFragment to back stack
     * so the system Back button returns to login.
     */
    public void showRegisterFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right)
                .replace(R.id.authContainer, new com.example.vibetix.Fragments.Auth.RegisterFragment())
                .addToBackStack("register")
                .commit();
    }

    /**
     * Pop RegisterFragment and return to LoginFragment.
     */
    public void showLoginFragment() {
        getSupportFragmentManager().popBackStack();
    }

    /** Hiển thị bất kỳ Fragment nào (dùng cho Google Sign-In mode). */
    public void showFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right)
                .replace(R.id.authContainer, fragment)
                .addToBackStack("google_register")
                .commit();
    }
}
