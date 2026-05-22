package com.example.vibetix.Activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Fragments.User.HomeFragment;
import com.example.vibetix.Fragments.User.MyTicketsFragment;
import com.example.vibetix.Fragments.User.ProfileFragment;
import com.example.vibetix.Fragments.User.SearchFragment;
import com.example.vibetix.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class UserMainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_main);

        addViews();
        addEvents();

        // Mở HomeFragment mặc định khi vào app
        if (savedInstanceState == null) {
            openFragment(new HomeFragment());
        }
    }

    private void addViews() {
        bottomNavCustomer = findViewById(R.id.bottomNavCustomer);
    }

    private void addEvents() {
        bottomNavCustomer.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                openFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_my_tickets) {
                openFragment(new MyTicketsFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                openFragment(new ProfileFragment());
                return true;
            }
            return false;
        });
    }

    private void openFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameContainerMain, fragment)
                .commit();
    }

    public void openSearchFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameContainerMain, new SearchFragment())
                .addToBackStack("search")
                .commit();
    }
}
