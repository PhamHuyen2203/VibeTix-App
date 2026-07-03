package com.example.vibetix.Activities.Admin;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.vibetix.Fragments.Admin.DashboardFragment;
import com.example.vibetix.Fragments.Admin.EventApprovalFragment;
import com.example.vibetix.Fragments.Admin.GlobalDiscountFragment;
import com.example.vibetix.Fragments.Admin.MasterDataFragment;
import com.example.vibetix.Fragments.Admin.OrganizerApprovalFragment;
import com.example.vibetix.R;
import com.example.vibetix.Utils.LocaleHelper;
import com.google.android.material.navigation.NavigationView;

public class AdminMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.nav_open_drawer, R.string.nav_close_drawer);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            // Mặc định hiển thị Dashboard khi vào App
            loadFragment(new DashboardFragment(), "DASHBOARD", null);
            navigationView.setCheckedItem(R.id.nav_admin_dashboard);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        String tag = "";
        Bundle args = new Bundle();

        int id = item.getItemId();
        if (id == R.id.nav_admin_dashboard) {
            fragment = new DashboardFragment();
            tag = "DASHBOARD";
        } else if (id == R.id.nav_admin_organizers) {
            fragment = new OrganizerApprovalFragment();
            tag = "ORGANIZERS";
            // Ví dụ truyền UUID dưới dạng String
            args.putString("target_uuid", "550e8400-e29b-41d4-a716-446655440000");
        } else if (id == R.id.nav_admin_events) {
            fragment = new EventApprovalFragment();
            tag = "EVENTS";
        } else if (id == R.id.nav_admin_master_data) {
            fragment = new MasterDataFragment();
            tag = "MASTER_DATA";
        } else if (id == R.id.nav_admin_discounts) {
            fragment = new GlobalDiscountFragment();
            tag = "DISCOUNTS";
        }

        if (fragment != null) {
            loadFragment(fragment, tag, args);
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment, String tag, Bundle args) {
        if (args != null) {
            fragment.setArguments(args);
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.admin_fragment_container, fragment, tag)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
