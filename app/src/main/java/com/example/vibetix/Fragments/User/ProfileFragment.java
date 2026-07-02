package com.example.vibetix.Fragments.User;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Activities.Auth.AuthActivity;
import com.example.vibetix.Activities.User.UserMainActivity;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.vibetix.Fragments.Organizer.OrganizerProfileFragment;
import com.example.vibetix.Models.OrganizerProfile;
import com.example.vibetix.Models.PaymentMethod;
import com.example.vibetix.R;
import com.example.vibetix.Utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    // Header
    private TextView txtProfileName, txtProfileEmail, txtMemberSince;
    private TextView txtEventsInterested, txtFollowingCount;
    private LinearLayout layoutStatFavorites, layoutStatFollowing;


    // Settings rows
    private LinearLayout rowAccountInfo, rowOrganizerProfile, rowSecurity, rowLanguage, rowHelpCenter;
    private TextView     txtOrgBadge;
    private TextView     txtLangFlag, txtLangLabel;
    private LinearLayout btnLangToggle;
    private Switch       switchNotifications;
    private Button       btnLogout;

    private SharedPreferences authPrefs, profilePrefs;

    // Language state: "vi" or "en"
    private String currentLang = "vi";

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authPrefs    = requireContext().getSharedPreferences(Constants.PREFS_AUTH,    Context.MODE_PRIVATE);
        profilePrefs = requireContext().getSharedPreferences(Constants.PREFS_PROFILE, Context.MODE_PRIVATE);

        bindViews(view);
        applyInsets(view);
        loadUserInfo();
        loadProfileStats();
        loadOrganizerBadge();
        loadLanguageState();
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserInfo();
        loadProfileStats();
        loadOrganizerBadge();
    }

    // ── View binding ───────────────────────────────────────────────────────────
    private void bindViews(View v) {
        txtProfileName      = v.findViewById(R.id.txtProfileName);
        txtProfileEmail     = v.findViewById(R.id.txtProfileEmail);
        txtMemberSince      = v.findViewById(R.id.txtMemberSince);
        txtEventsInterested = v.findViewById(R.id.txtEventsInterested);
        txtFollowingCount   = v.findViewById(R.id.txtFollowingCount);
        layoutStatFavorites = v.findViewById(R.id.layoutStatFavorites);
        layoutStatFollowing = v.findViewById(R.id.layoutStatFollowing);

        rowAccountInfo      = v.findViewById(R.id.rowAccountInfo);
        rowOrganizerProfile = v.findViewById(R.id.rowOrganizerProfile);
        rowSecurity         = v.findViewById(R.id.rowSecurity);
        rowLanguage         = v.findViewById(R.id.rowLanguage);
        rowHelpCenter       = v.findViewById(R.id.rowHelpCenter);
        txtOrgBadge         = v.findViewById(R.id.txtOrgBadge);
        btnLangToggle       = v.findViewById(R.id.btnLangToggle);
        txtLangFlag         = v.findViewById(R.id.txtLangFlag);
        txtLangLabel        = v.findViewById(R.id.txtLangLabel);
        switchNotifications = v.findViewById(R.id.switchNotifications);
        btnLogout           = v.findViewById(R.id.btnLogout);
    }

    // ── Status bar inset ───────────────────────────────────────────────────────
    private void applyInsets(View root) {
        View header = root.findViewById(R.id.layoutProfileHeader);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(v.getPaddingLeft(), top + 16, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    // ── Data loading ───────────────────────────────────────────────────────────
    private void loadUserInfo() {
        // Hiển thị cache trước (UI phản hồi ngay)
        String cachedName  = authPrefs.getString(Constants.KEY_USER_NAME,  "");
        String cachedEmail = authPrefs.getString(Constants.KEY_USER_EMAIL, "");
        if (txtProfileName  != null) txtProfileName.setText(cachedName.isEmpty() ? "Người dùng VibeTix" : cachedName);
        if (txtProfileEmail != null && !cachedEmail.isEmpty()) txtProfileEmail.setText(cachedEmail);
        if (txtMemberSince  != null) txtMemberSince.setText("Thành viên từ 2024");

        // Đồng thời fetch từ Firestore để cập nhật dữ liệu mới nhất
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.getUid())
            .get()
            .addOnSuccessListener(doc -> {
                if (!isAdded() || doc == null) return;
                String name  = doc.getString("full_name");
                String email = doc.getString("email");
                if (name  == null) name  = "";
                if (email == null) email = user.getEmail() != null ? user.getEmail() : "";

                // Cập nhật cache
                authPrefs.edit()
                    .putString(Constants.KEY_USER_NAME,  name)
                    .putString(Constants.KEY_USER_EMAIL, email)
                    .apply();

                // Cập nhật UI
                final String finalName  = name;
                final String finalEmail = email;
                if (txtProfileName  != null) txtProfileName.setText(finalName.isEmpty() ? "Người dùng VibeTix" : finalName);
                if (txtProfileEmail != null) txtProfileEmail.setText(finalEmail);
            });
    }

    private void loadProfileStats() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        // Count sự kiện yêu thích
        FirebaseFirestore.getInstance()
                .collection(FirebaseCollections.EVENT_INTERESTS)
                .whereEqualTo("user_id", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (isAdded() && txtEventsInterested != null) {
                        txtEventsInterested.setText(String.valueOf(snap.size()));
                    }
                });

        // Count đang theo dõi (stars/organizers)
        FirebaseFirestore.getInstance()
                .collection(FirebaseCollections.USER_STAR_FOLLOWS)
                .whereEqualTo("user_id", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (isAdded() && txtFollowingCount != null) {
                        txtFollowingCount.setText(String.valueOf(snap.size()));
                    }
                });
    }

    private void loadOrganizerBadge() {
        if (txtOrgBadge == null) return;
        OrganizerProfile org = loadOrgProfile();
        if (org.isPending()) {
            txtOrgBadge.setVisibility(View.VISIBLE);
            txtOrgBadge.setText("Chờ duyệt");
            txtOrgBadge.setTextColor(0xFFE6B93E);
            txtOrgBadge.setBackgroundResource(R.drawable.bg_status_pending);
        } else if (org.isApproved()) {
            txtOrgBadge.setVisibility(View.VISIBLE);
            txtOrgBadge.setText("✓ Xác minh");
            txtOrgBadge.setTextColor(0xFF27AE60);
            txtOrgBadge.setBackgroundResource(R.drawable.bg_status_approved);
        } else {
            txtOrgBadge.setVisibility(View.GONE);
        }
    }

    private void loadLanguageState() {
        currentLang = profilePrefs.getString("app_language", "vi");
        updateLangChip();
    }

    private void updateLangChip() {
        if (txtLangFlag == null || txtLangLabel == null) return;
        if ("en".equals(currentLang)) {
            txtLangFlag.setText("🇬🇧");
            txtLangLabel.setText("English");
        } else {
            txtLangFlag.setText("🇻🇳");
            txtLangLabel.setText("Việt");
        }
    }

    // ── Click listeners ────────────────────────────────────────────────────────
    private void setupListeners() {

        // Stats card click handlers
        if (layoutStatFavorites != null) {
            layoutStatFavorites.setOnClickListener(v -> {
                FavoritesFollowsBottomSheet sheet = FavoritesFollowsBottomSheet.newInstance("favorites");
                sheet.show(getChildFragmentManager(), "favorites");
            });
        }
        if (layoutStatFollowing != null) {
            layoutStatFollowing.setOnClickListener(v -> {
                FavoritesFollowsBottomSheet sheet = FavoritesFollowsBottomSheet.newInstance("following");
                sheet.show(getChildFragmentManager(), "following");
            });
        }

        // Thông tin tài khoản
        if (rowAccountInfo != null)
            rowAccountInfo.setOnClickListener(v -> openSub(new AccountInfoFragment()));

        // Ban tổ chức
        if (rowOrganizerProfile != null)
            rowOrganizerProfile.setOnClickListener(v -> openSub(new OrganizerProfileFragment()));

        // Bảo mật & Mật khẩu
        if (rowSecurity != null)
            rowSecurity.setOnClickListener(v -> openSub(new SecurityFragment()));

        // Ngôn ngữ — toggle chip
        if (btnLangToggle != null)
            btnLangToggle.setOnClickListener(v -> toggleLanguage());
        if (rowLanguage != null)
            rowLanguage.setOnClickListener(v -> toggleLanguage());

        // Trung tâm trợ giúp
        if (rowHelpCenter != null)
            rowHelpCenter.setOnClickListener(v -> openSub(new HelpCenterFragment()));

        // Đăng xuất
        if (btnLogout != null)
            btnLogout.setOnClickListener(v -> performLogout());
    }

    private void toggleLanguage() {
        currentLang = "en".equals(currentLang) ? "vi" : "en";
        profilePrefs.edit().putString("app_language", currentLang).apply();
        updateLangChip();
        String msg = "en".equals(currentLang)
                ? "Switched to English. Restart app to apply fully."
                : "Đã chuyển sang Tiếng Việt.";
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void openSub(Fragment fragment) {
        if (getActivity() instanceof UserMainActivity) {
            ((UserMainActivity) getActivity()).openSubFragment(fragment);
        }
    }

    private void performLogout() {
        // Firebase Auth sign out
        FirebaseAuth.getInstance().signOut();

        // Xóa cache SharedPreferences
        authPrefs.edit()
                .putBoolean(Constants.KEY_IS_LOGGED_IN, false)
                .remove(Constants.KEY_USER_EMAIL)
                .remove(Constants.KEY_USER_NAME)
                .remove(Constants.KEY_USER_PHONE)
                .remove(Constants.KEY_USER_ROLE)
                .apply();
        Toast.makeText(requireContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show();

        // Start AuthActivity and clear backstack
        Intent intent = new Intent(requireContext(), com.example.vibetix.Activities.Auth.AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private View buildPaymentPreviewRow(PaymentMethod pm, float dp) {
        LinearLayout row = new LinearLayout(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int)(8 * dp);
        row.setLayoutParams(lp);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int p = (int)(12 * dp);
        row.setPadding(p, p, p, p);
        row.setBackgroundResource(pm.isDefault() ? R.drawable.bg_payment_item_default : R.drawable.bg_payment_item);

        TextView icon = new TextView(requireContext());
        int sz = (int)(36 * dp);
        icon.setLayoutParams(new LinearLayout.LayoutParams(sz, sz));
        icon.setText(getPaymentInitial(pm.getType()));
        icon.setTextColor(0xFFFFFFFF);
        icon.setTextSize(13f);
        icon.setGravity(android.view.Gravity.CENTER);
        icon.setBackgroundColor(getPaymentColor(pm.getType()));
        row.addView(icon);

        LinearLayout info = new LinearLayout(requireContext());
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ip.setMarginStart((int)(12 * dp));
        info.setLayoutParams(ip);
        info.setOrientation(LinearLayout.VERTICAL);

        TextView nameV = new TextView(requireContext());
        nameV.setText(pm.getDisplayName());
        nameV.setTextColor(pm.isDefault() ? 0xFF226CEB : 0xFF1C1B1B);
        nameV.setTextSize(13f);
        info.addView(nameV);

        if (pm.getAccount() != null && !pm.getAccount().isEmpty()) {
            TextView acc = new TextView(requireContext());
            acc.setText(pm.getAccount());
            acc.setTextColor(0xFF808E92);
            acc.setTextSize(11f);
            info.addView(acc);
        }
        row.addView(info);

        if (pm.isDefault()) {
            TextView def = new TextView(requireContext());
            def.setText("Mặc định");
            def.setTextColor(0xFF226CEB);
            def.setTextSize(10f);
            def.setBackgroundResource(R.drawable.bg_lang_chip);
            int ph = (int)(6 * dp);
            int pv = (int)(3 * dp);
            def.setPadding(ph, pv, ph, pv);
            row.addView(def);
        }
        return row;
    }

    private String getPaymentInitial(String type) {
        if (type == null) return "?";
        switch (type) {
            case Constants.PAYMENT_MOMO:    return "M";
            case Constants.PAYMENT_ZALOPAY: return "Z";
            case Constants.PAYMENT_VNPAY:   return "V";
            case Constants.PAYMENT_VISA:    return "Vi";
            case Constants.PAYMENT_ATM:     return "A";
            default: return "?";
        }
    }

    private int getPaymentColor(String type) {
        if (type == null) return 0xFF808E92;
        switch (type) {
            case Constants.PAYMENT_MOMO:    return 0xFFA50064;
            case Constants.PAYMENT_ZALOPAY: return 0xFF0068FF;
            case Constants.PAYMENT_VNPAY:   return 0xFF0050A0;
            case Constants.PAYMENT_VISA:    return 0xFF1A1F71;
            case Constants.PAYMENT_ATM:     return 0xFFE65100;
            default: return 0xFF808E92;
        }
    }

    /** Trả về profile đầu tiên có isDefault=true, hoặc profile đầu tiên trong list */
    private OrganizerProfile loadOrgProfile() {
        // Đọc từ list mới
        String raw = profilePrefs.getString("org_profiles_json", null);
        if (raw != null) {
            try {
                JSONArray arr = new JSONArray(raw);
                OrganizerProfile first = null;
                for (int i = 0; i < arr.length(); i++) {
                    OrganizerProfile p = OrganizerProfile.fromJson(arr.getJSONObject(i));
                    if (p.hasProfile()) {
                        if (first == null) first = p;
                        if (p.isDefault()) return p;
                    }
                }
                if (first != null) return first;
            } catch (Exception ignored) {}
        }
        // Fallback: đọc key cũ
        String json = profilePrefs.getString("org_profile_json", null);
        if (json == null) return new OrganizerProfile();
        try { return OrganizerProfile.fromJson(new JSONObject(json)); }
        catch (Exception e) { return new OrganizerProfile(); }
    }

    private List<PaymentMethod> loadPaymentMethods() {
        List<PaymentMethod> list = new ArrayList<>();
        String raw = profilePrefs.getString(Constants.KEY_PAYMENT_METHODS, null);
        if (raw == null) return list;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                PaymentMethod pm = PaymentMethod.fromJson(arr.getJSONObject(i));
                if (pm != null) list.add(pm);
            }
        } catch (Exception ignored) {}
        return list;
    }
}
