package com.example.vibetix.Activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Fragments.User.CreateEventFragment;
import com.example.vibetix.Fragments.User.EventsFragment;
import com.example.vibetix.Fragments.User.HomeFragment;
import com.example.vibetix.Fragments.User.MyTicketsFragment;
import com.example.vibetix.Fragments.User.ProfileFragment;
import com.example.vibetix.Fragments.User.SearchFragment;
import com.example.vibetix.R;

public class UserMainActivity extends AppCompatActivity {

    // Custom nav tabs
    private LinearLayout tabHome, tabEvents, tabCreate, tabTickets, tabProfile;
    private ImageView    icHome, icEvents, icTickets, icProfile;
    private TextView     txtHome, txtEvents, txtCreate, txtTickets, txtProfile;

    private int activeTabId = R.id.tabHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_main);

        bindViews();
        applyNavBarInset();
        setupNavListeners();

        if (savedInstanceState == null) {
            selectTab(R.id.tabHome);
        }
    }

    /** Chỉ apply navigation bar bottom inset — KHÔNG apply status bar top inset */
    private void applyNavBarInset() {
        LinearLayout nav = findViewById(R.id.customBottomNav);
        if (nav == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(nav, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(0, 0, 0, bottomInset);
            return insets;
        });
    }

    private void bindViews() {
        tabHome    = findViewById(R.id.tabHome);
        tabEvents  = findViewById(R.id.tabEvents);
        tabCreate  = findViewById(R.id.tabCreate);
        tabTickets = findViewById(R.id.tabTickets);
        tabProfile = findViewById(R.id.tabProfile);

        icHome    = findViewById(R.id.icHome);
        icEvents  = findViewById(R.id.icEvents);
        icTickets = findViewById(R.id.icTickets);
        icProfile = findViewById(R.id.icProfile);

        txtHome    = findViewById(R.id.txtHome);
        txtEvents  = findViewById(R.id.txtEvents);
        txtCreate  = findViewById(R.id.txtCreate);
        txtTickets = findViewById(R.id.txtTickets);
        txtProfile = findViewById(R.id.txtProfile);
    }

    private void setupNavListeners() {
        tabHome   .setOnClickListener(v -> selectTab(R.id.tabHome));
        tabEvents .setOnClickListener(v -> selectTab(R.id.tabEvents));
        tabCreate .setOnClickListener(v -> openSubFragment(new CreateEventFragment()));
        tabTickets.setOnClickListener(v -> selectTab(R.id.tabTickets));
        tabProfile.setOnClickListener(v -> selectTab(R.id.tabProfile));
    }

    private void selectTab(int tabId) {
        activeTabId = tabId;
        updateTabStyles();

        if (tabId == R.id.tabHome)    openFragment(new HomeFragment());
        else if (tabId == R.id.tabEvents)  openFragment(new EventsFragment());
        else if (tabId == R.id.tabTickets) openFragment(new MyTicketsFragment());
        else if (tabId == R.id.tabProfile) openFragment(new ProfileFragment());
    }

    private void updateTabStyles() {
        int blue = ContextCompat.getColor(this, R.color.clr_primary_blue);
        int grey = 0xFF808E92;

        // Reset all to grey
        setTabStyle(icHome,    txtHome,    R.drawable.ic_nav_home,    grey, false);
        setTabStyle(icEvents,  txtEvents,  R.drawable.ic_nav_events,  grey, false);
        setTabStyle(icTickets, txtTickets, R.drawable.ic_nav_ticket,  grey, false);
        setTabStyle(icProfile, txtProfile, R.drawable.ic_nav_profile, grey, false);

        // Highlight active tab
        if (activeTabId == R.id.tabHome)
            setTabStyle(icHome,    txtHome,    R.drawable.ic_nav_home,   blue, true);
        else if (activeTabId == R.id.tabEvents)
            setTabStyle(icEvents,  txtEvents,  R.drawable.ic_nav_events, blue, true);
        else if (activeTabId == R.id.tabTickets)
            setTabStyle(icTickets, txtTickets, R.drawable.ic_nav_ticket, blue, true);
        else if (activeTabId == R.id.tabProfile)
            setTabStyle(icProfile, txtProfile, R.drawable.ic_nav_profile,blue, true);

        // "Tạo sự kiện" tab text màu xám (không có active state)
        if (txtCreate != null) txtCreate.setTextColor(grey);
    }

    private void setTabStyle(ImageView icon, TextView label, int iconRes, int color, boolean bold) {
        if (icon  != null) { icon.setImageResource(iconRes); icon.setColorFilter(color); }
        if (label != null) {
            label.setTextColor(color);
            label.setTypeface(null, bold
                    ? android.graphics.Typeface.BOLD
                    : android.graphics.Typeface.NORMAL);
        }
    }

    // ── Fragment navigation ───────────────────────────────────────────────────

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

    public void openEventsFragment() {
        selectTab(R.id.tabEvents);
    }

    public void openSubFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameContainerMain, fragment)
                .addToBackStack(null)
                .commit();
    }
}
