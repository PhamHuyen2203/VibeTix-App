package com.example.vibetix.Activities.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Fragments.User.CreateEventFragment;
import com.example.vibetix.Fragments.User.OrganizerHubFragment;
import com.example.vibetix.Fragments.User.EventsFragment;
import com.example.vibetix.Fragments.User.HomeFragment;
import com.example.vibetix.Fragments.User.MyTicketsFragment;
import com.example.vibetix.Fragments.User.ProfileFragment;
import com.example.vibetix.Fragments.User.SearchFragment;
import com.example.vibetix.Fragments.User.SecurityFragment;
import com.example.vibetix.R;
import com.example.vibetix.Utils.Constants;
import com.example.vibetix.Utils.LocaleHelper;

public class UserMainActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

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
            // C1: Nếu được mở từ notification order_confirmed → mở thẳng tab Vé
            String openTab = getIntent().getStringExtra("openTab");
            if ("tickets".equals(openTab)) {
                selectTab(R.id.tabTickets);
            } else {
                selectTab(R.id.tabHome);
            }
        }

        // Cập nhật tabCurrentTop khi user nhấn Back để pop sub-fragment
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (currentTabTag == null) return;
            androidx.fragment.app.FragmentManager fm2 = getSupportFragmentManager();
            // Tìm fragment visible đầu tiên (topmost) thuộc tab hiện tại
            String newTop = currentTabTag; // fallback = root
            for (Fragment f : fm2.getFragments()) {
                if (f != null && f.isAdded() && !f.isHidden()) {
                    String t = f.getTag();
                    if (t != null && t.startsWith(currentTabTag)) newTop = t;
                }
            }
            tabCurrentTop.put(currentTabTag, newTop);
        });
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String openTab = intent.getStringExtra("openTab");
        if ("tickets".equals(openTab)) {
            selectTab(R.id.tabTickets);
        }
    }

    /** apply navigation bar bottom inset — KHÔNG apply status bar top inset */
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
        tabCreate .setOnClickListener(v -> openSubFragment(new OrganizerHubFragment()));
        tabTickets.setOnClickListener(v -> handleTicketsTabClick());
        tabProfile.setOnClickListener(v -> selectTab(R.id.tabProfile));
    }

    // Tag cho 4 tab chính — giữ instance để KHÔNG reload dữ liệu mỗi lần chuyển tab
    private static final String TAG_HOME    = "tab_home";
    private static final String TAG_EVENTS  = "tab_events";
    private static final String TAG_TICKETS = "tab_tickets";
    private static final String TAG_PROFILE = "tab_profile";
    private static final String[] TAB_TAGS  = { TAG_HOME, TAG_EVENTS, TAG_TICKETS, TAG_PROFILE };

    // Track fragment đang hiển thị (topmost) cho mỗi tab
    private final java.util.Map<String, String> tabCurrentTop = new java.util.HashMap<>();
    private String currentTabTag = null;

    private String getActiveTabTag() {
        if (activeTabId == R.id.tabHome)    return TAG_HOME;
        if (activeTabId == R.id.tabEvents)  return TAG_EVENTS;
        if (activeTabId == R.id.tabTickets) return TAG_TICKETS;
        if (activeTabId == R.id.tabProfile) return TAG_PROFILE;
        return TAG_HOME;
    }

    public void selectTab(int tabId) {
        activeTabId = tabId;
        updateTabStyles();
        String newTabTag = getActiveTabTag();
        if (newTabTag.equals(currentTabTag)) return;
        switchToTab(newTabTag);
    }

    /** Chuyển sang tab mới: ẩn tất cả fragment đang visible, hiện topmost của tab đích */
    private void switchToTab(String newTabTag) {
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();

        // Ẩn TẤT CẢ fragment đang hiển thị (bao gồm sub-fragment của tab cũ)
        for (Fragment f : fm.getFragments()) {
            if (f != null && f.isAdded() && !f.isHidden()) ft.hide(f);
        }

        // Hiện hoặc tạo mới topmost fragment của tab đích
        String topTag = tabCurrentTop.get(newTabTag);
        Fragment top  = topTag != null ? fm.findFragmentByTag(topTag) : null;
        if (top == null || !top.isAdded()) {
            Fragment root = createRootForTab(newTabTag);
            ft.add(R.id.frameContainerMain, root, newTabTag);
            tabCurrentTop.put(newTabTag, newTabTag);
        } else {
            ft.show(top);
        }

        currentTabTag = newTabTag;
        ft.commitAllowingStateLoss();
    }

    private Fragment createRootForTab(String tabTag) {
        switch (tabTag) {
            case TAG_EVENTS:  return new EventsFragment();
            case TAG_TICKETS: return new MyTicketsFragment();
            case TAG_PROFILE: return new ProfileFragment();
            default:          return new HomeFragment();
        }
    }

    // Legacy — no longer called but kept for compatibility with external callers
    @Deprecated
    private void showTab(String tag, java.util.function.Supplier<Fragment> creator) {
        switchToTab(tag);
    }

    @Override
    public void onBackPressed() {
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        // Nếu có back stack entry cho tab hiện tại → pop để về sub-fragment trước đó
        if (currentTabTag != null && fm.getBackStackEntryCount() > 0) {
            androidx.fragment.app.FragmentManager.BackStackEntry top =
                    fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1);
            if (currentTabTag.equals(top.getName())) {
                fm.popBackStack();
                // tabCurrentTop được cập nhật qua BackStackChangedListener bên dưới
                return;
            }
        }
        super.onBackPressed();
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

        // "Tß¦ío sß+¦ kiß+çn" tab text m+áu x+ím (kh+¦ng c+¦ active state)
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

    // GöÇGöÇ Fragment navigation GöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇGöÇ

    private void openFragment(Fragment fragment) {
        openSubFragment(fragment);
    }

    public void openSearchFragment() {
        openSubFragment(new SearchFragment());
    }

    public void openEventsFragment() {
        selectTab(R.id.tabEvents);
    }

    public void openEventsFragmentWithFilter(String categoryKey, String cityKeyword) {
        activeTabId = R.id.tabEvents;
        updateTabStyles();
        if (!TAG_EVENTS.equals(currentTabTag)) {
            switchToTab(TAG_EVENTS);
        }
        openSubFragment(EventsFragment.newInstance(categoryKey, cityKeyword));
    }

    // ── PIN gate cho tab Vé của tôi ──────────────────────────────────────────
    private static final String KEY_PIN_HASH        = "user_pin_hash";
    private static final String KEY_PIN_FOR_TICKETS = "pin_for_tickets";

    private void handleTicketsTabClick() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_PROFILE, Context.MODE_PRIVATE);
        boolean pinEnabled = prefs.getBoolean(KEY_PIN_FOR_TICKETS, false);
        String  pinHash    = prefs.getString(KEY_PIN_HASH, null);
        if (pinHash == null) pinHash = prefs.getString("user_pin", null);

        if (pinEnabled && pinHash != null && !pinHash.isEmpty()) {
            showPinGateDialog(pinHash);
        } else {
            selectTab(R.id.tabTickets);
        }
    }

    private void showPinGateDialog(String storedHash) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.layout_pin_dialog, null);

        TextView txtTitle    = dialogView.findViewById(R.id.txtPinDialogTitle);
        TextView txtSubtitle = dialogView.findViewById(R.id.txtPinDialogSubtitle);
        TextView txtError    = dialogView.findViewById(R.id.txtPinError);
        if (txtTitle    != null) txtTitle.setText("Nhập mã PIN");
        if (txtSubtitle != null) txtSubtitle.setText("Nhập mã PIN 6 số để vào trang Vé của tôi");

        EditText[] boxes = getPinBoxes(dialogView);
        wireAutoAdvance(boxes);

        // Nút "Quên mã PIN?" bên dưới dialog
        TextView txtForgotLink = new TextView(this);
        txtForgotLink.setText("Quên mã PIN? Vào Hồ sơ > Bảo mật để đặt lại");
        txtForgotLink.setTextColor(0xFF2563EB);
        txtForgotLink.setTextSize(12f);
        txtForgotLink.setPadding(48, 0, 48, 32);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Xác nhận", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(di -> {
            txtForgotLink.setOnClickListener(v -> {
                dialog.dismiss();
                // Điều hướng sang Profile → Security
                selectTab(R.id.tabProfile);
                Toast.makeText(this, "Vào Bảo mật & Mật khẩu để đặt lại PIN", Toast.LENGTH_LONG).show();
            });

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String entered = collectPin(boxes);
                if (entered.length() < 6) {
                    if (txtError != null) { txtError.setText("Vui lòng nhập đủ 6 chữ số"); txtError.setVisibility(View.VISIBLE); }
                    return;
                }
                if (SecurityFragment.sha256(entered).equals(storedHash)) {
                    dialog.dismiss();
                    selectTab(R.id.tabTickets);
                } else {
                    if (txtError != null) { txtError.setText("Mã PIN không đúng, vui lòng thử lại"); txtError.setVisibility(View.VISIBLE); }
                    for (EditText b : boxes) if (b != null) b.setText("");
                    if (boxes[0] != null) boxes[0].requestFocus();
                }
            });
        });

        dialog.show();
        if (boxes[0] != null) boxes[0].requestFocus();
    }

    private EditText[] getPinBoxes(View v) {
        return new EditText[]{
            v.findViewById(R.id.pinBox1), v.findViewById(R.id.pinBox2),
            v.findViewById(R.id.pinBox3), v.findViewById(R.id.pinBox4),
            v.findViewById(R.id.pinBox5), v.findViewById(R.id.pinBox6)
        };
    }

    private void wireAutoAdvance(EditText[] boxes) {
        for (int i = 0; i < boxes.length; i++) {
            if (boxes[i] == null) continue;
            final int cur = i, next = i + 1;
            boxes[i].addTextChangedListener(new android.text.TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) {}
                public void afterTextChanged(android.text.Editable s) {
                    if (s.length() == 1 && next < boxes.length && boxes[next] != null)
                        boxes[next].requestFocus();
                    if (s.length() == 0 && cur > 0 && boxes[cur - 1] != null)
                        boxes[cur - 1].requestFocus();
                }
            });
        }
    }

    private String collectPin(EditText[] boxes) {
        StringBuilder sb = new StringBuilder();
        for (EditText b : boxes) if (b != null) sb.append(b.getText().toString().trim());
        return sb.toString();
    }

    /**
     * Mở sub-fragment trong tab hiện tại: hide fragment đang visible, add fragment mới.
     * Back stack dùng tên tab → pop đúng về fragment trước trong cùng tab.
     */
    public void openSubFragment(Fragment fragment) {
        if (currentTabTag == null) currentTabTag = TAG_HOME;
        String newTag = currentTabTag + "_sub_" + System.currentTimeMillis();

        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();

        // Ẩn fragment đang hiển thị của tab này
        String prevTopTag = tabCurrentTop.get(currentTabTag);
        Fragment prevTop  = prevTopTag != null ? fm.findFragmentByTag(prevTopTag) : null;
        if (prevTop != null && prevTop.isAdded() && !prevTop.isHidden()) ft.hide(prevTop);

        ft.add(R.id.frameContainerMain, fragment, newTag);
        ft.addToBackStack(currentTabTag); // tên = tabTag để onBackPressed biết tab nào
        tabCurrentTop.put(currentTabTag, newTag);
        ft.commitAllowingStateLoss();
    }
}
