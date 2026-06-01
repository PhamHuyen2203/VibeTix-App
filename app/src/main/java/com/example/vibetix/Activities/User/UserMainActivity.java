package com.example.vibetix.Activities.User;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.vibetix.Activities.BaseActivity;
import com.example.vibetix.Fragments.User.AllEventsFragment;
import com.example.vibetix.Fragments.User.HomeFragment;
import com.example.vibetix.Fragments.User.MyTicketsFragment;
import com.example.vibetix.Fragments.User.OrganizerHubFragment;
import com.example.vibetix.Fragments.User.ProfileFragment;
import com.example.vibetix.Fragments.User.SearchFragment;
import com.example.vibetix.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * UserMainActivity — Màn hình chính, dùng cho tất cả user (mua vé & tổ chức).
 *
 * 5 tabs:
 *   Home        → HomeFragment        (khám phá sự kiện)
 *   Events      → AllEventsFragment   (tất cả sự kiện, lọc được)
 *   My Tickets  → MyTicketsFragment   (vé đã mua)
 *   Organizer   → OrganizerHubFragment (hub tổ chức — có điều kiện)
 *   Profile     → ProfileFragment     (hồ sơ + quản lý BTC)
 */
public class UserMainActivity extends BaseActivity {

    BottomNavigationView bottomNavCustomer;

    // Giữ instance fragment để tránh recreate khi switch tab
    private HomeFragment homeFragment;
    private AllEventsFragment allEventsFragment;
    private MyTicketsFragment myTicketsFragment;
    private OrganizerHubFragment organizerHubFragment;
    private ProfileFragment profileFragment;

    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_main);

        checkLoginStatus();

        addViews();
        initFragments();
        addEvents();

        // Tab mặc định: Home
        if (savedInstanceState == null) {
            showTab(homeFragment);
            bottomNavCustomer.setSelectedItemId(R.id.nav_home);
        }
    }

    private void addViews() {
        bottomNavCustomer = findViewById(R.id.bottomNavCustomer);
    }

    /**
     * Tạo tất cả fragment instances và add vào FragmentManager (hidden).
     * Cách này giữ trạng thái fragment khi switch tab (không recreate).
     */
    private void initFragments() {
        homeFragment        = new HomeFragment();
        allEventsFragment   = new AllEventsFragment();
        myTicketsFragment   = new MyTicketsFragment();
        organizerHubFragment = new OrganizerHubFragment();
        profileFragment     = new ProfileFragment();

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .add(R.id.frameContainerMain, homeFragment)
                .add(R.id.frameContainerMain, allEventsFragment).hide(allEventsFragment)
                .add(R.id.frameContainerMain, myTicketsFragment).hide(myTicketsFragment)
                .add(R.id.frameContainerMain, organizerHubFragment).hide(organizerHubFragment)
                .add(R.id.frameContainerMain, profileFragment).hide(profileFragment)
                .commit();

        activeFragment = homeFragment;
    }

    private void addEvents() {
        bottomNavCustomer.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showTab(homeFragment);
                return true;
            } else if (id == R.id.nav_events) {
                showTab(allEventsFragment);
                return true;
            } else if (id == R.id.nav_my_tickets) {
                showTab(myTicketsFragment);
                return true;
            } else if (id == R.id.nav_organizer) {
                showTab(organizerHubFragment);
                return true;
            } else if (id == R.id.nav_profile) {
                showTab(profileFragment);
                return true;
            }
            return false;
        });
    }

    /**
     * Show/hide fragments để giữ trạng thái (không dùng replace).
     */
    private void showTab(Fragment target) {
        if (activeFragment == target) return;
        getSupportFragmentManager()
                .beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;
    }

    /** Được gọi từ HomeFragment khi tap search bar. */
    public void openSearchFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameContainerMain, new SearchFragment())
                .addToBackStack("search")
                .commit();
    }

    /** Được gọi từ OrganizerHubFragment khi muốn về tab Events. */
    public void switchToEventsTab() {
        bottomNavCustomer.setSelectedItemId(R.id.nav_events);
    }

    /** Được gọi từ bất kỳ đâu để switch sang tab cụ thể. */
    public void switchToTab(int navItemId) {
        bottomNavCustomer.setSelectedItemId(navItemId);
    }
}
