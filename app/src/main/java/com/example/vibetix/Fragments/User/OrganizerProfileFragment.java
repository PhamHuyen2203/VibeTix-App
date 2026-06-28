package com.example.vibetix.Fragments.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Models.OrganizerProfile;
import com.example.vibetix.R;
import com.example.vibetix.Utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OrganizerProfileFragment — quản lý danh sách ban tổ chức của người dùng.
 * Hỗ trợ nhiều BTC profile (1 user có thể tổ chức nhiều loại sự kiện).
 */
public class OrganizerProfileFragment extends Fragment {

    private ImageView    btnOrgBack;
    private LinearLayout sectionNoOrg;
    private LinearLayout containerOrgList;
    private Button       btnAddNewOrg;

    private SharedPreferences profilePrefs;
    private List<OrganizerProfile> orgList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profilePrefs = requireContext().getSharedPreferences(Constants.PREFS_PROFILE, Context.MODE_PRIVATE);
        bindViews(view);
        applyInsets(view);
        loadOrgList();
        setupListeners();
    }

    // ── View binding ───────────────────────────────────────────────────────────
    private void bindViews(View v) {
        // btnOrgBack       = v.findViewById(R.id.btnOrgBack);
        // sectionNoOrg     = v.findViewById(R.id.sectionNoOrg);
        // containerOrgList = v.findViewById(R.id.containerOrgList);
        // btnAddNewOrg     = v.findViewById(R.id.btnAddNewOrg);
    }

    // ── Insets ─────────────────────────────────────────────────────────────────
    private void applyInsets(View root) {
    }

    // ── Data ───────────────────────────────────────────────────────────────────
    private void loadOrgList() {
        orgList.clear();
        // Hiện UI từ SharedPreferences cache trước
        migrateLegacyProfile();
        String raw = profilePrefs.getString("org_profiles_json", null);
        if (raw != null) {
            try {
                JSONArray arr = new JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    OrganizerProfile p = OrganizerProfile.fromJson(arr.getJSONObject(i));
                    if (p.hasProfile()) orgList.add(p);
                }
            } catch (Exception ignored) {}
        }
        refreshUI();

        // Fetch từ Firestore để đồng bộ
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
            .collection("organizers")
            .whereEqualTo("user_id", user.getUid())
            .get()
            .addOnSuccessListener(snap -> {
                if (!isAdded()) return;
                orgList.clear();
                for (QueryDocumentSnapshot doc : snap) {
                    OrganizerProfile p = new OrganizerProfile();
                    p.setId(doc.getId());
                    p.setBrandName(doc.getString("brand_name"));
                    p.setDescription(doc.getString("description"));
                    p.setWebsiteUrl(doc.getString("website_url"));
                    Boolean verified = doc.getBoolean("is_verified");
                    p.setStatus(Boolean.TRUE.equals(verified)
                            ? Constants.ORG_STATUS_APPROVED : Constants.ORG_STATUS_PENDING);
                    if (p.hasProfile()) orgList.add(p);
                }
                // Cache lại
                JSONArray arr = new JSONArray();
                for (OrganizerProfile p : orgList) arr.put(p.toJson());
                profilePrefs.edit().putString("org_profiles_json", arr.toString()).apply();
                refreshUI();
            });
    }

    /** Chuyển dữ liệu cũ (single profile) sang list nếu cần */
    private void migrateLegacyProfile() {
        if (profilePrefs.contains("org_profiles_json")) return; // đã migrate
        String oldJson = profilePrefs.getString("org_profile_json", null);
        if (oldJson == null) return;
        try {
            OrganizerProfile old = OrganizerProfile.fromJson(new JSONObject(oldJson));
            if (old.hasProfile()) {
                old.setDefault(true);
                JSONArray arr = new JSONArray();
                arr.put(old.toJson());
                profilePrefs.edit().putString("org_profiles_json", arr.toString()).apply();
            }
        } catch (Exception ignored) {}
    }

    private void saveOrgList() {
        // Cache local
        JSONArray arr = new JSONArray();
        for (OrganizerProfile p : orgList) arr.put(p.toJson());
        profilePrefs.edit().putString("org_profiles_json", arr.toString()).apply();
    }

    /** Ghi/cập nhật 1 organizer profile lên Firestore */
    private void saveOrgToFirestore(OrganizerProfile org, boolean isNew) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("user_id",     user.getUid());
        data.put("brand_name",  org.getBrandName());
        data.put("description", org.getDescription());
        data.put("website_url", org.getWebsiteUrl());
        data.put("is_verified", false); // Admin xét duyệt sau

        if (isNew) {
            data.put("created_at", FieldValue.serverTimestamp());
            FirebaseFirestore.getInstance().collection("organizers")
                .add(data)
                .addOnSuccessListener(ref -> {
                    org.setId(ref.getId());
                    saveOrgList();
                });
        } else {
            FirebaseFirestore.getInstance().collection("organizers")
                .document(org.getId())
                .update(data);
        }
    }

    /** Xóa organizer profile trên Firestore */
    private void deleteOrgFromFirestore(OrganizerProfile org) {
        if (org.getId() == null || org.getId().isEmpty()) return;
        FirebaseFirestore.getInstance().collection("organizers")
            .document(org.getId())
            .delete();
    }

    // ── UI ─────────────────────────────────────────────────────────────────────
    private void refreshUI() {
        /*
        containerOrgList.removeAllViews();

        if (orgList.isEmpty()) {
            sectionNoOrg.setVisibility(View.VISIBLE);
            containerOrgList.setVisibility(View.GONE);
        } else {
            sectionNoOrg.setVisibility(View.GONE);
            containerOrgList.setVisibility(View.VISIBLE);
            for (int i = 0; i < orgList.size(); i++) {
                containerOrgList.addView(buildOrgCard(orgList.get(i), i));
            }
        }
        */
    }

    /** Tạo card cho một BTC profile */
    private View buildOrgCard(OrganizerProfile org, int index) {
        float dp = getResources().getDisplayMetrics().density;

        // Card wrapper
        LinearLayout card = new LinearLayout(requireContext());
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = (int)(12 * dp);
        card.setLayoutParams(cardLp);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_section_card);
        card.setElevation(2 * dp);
        int p = (int)(16 * dp);
        card.setPadding(p, p, p, p);

        // ── Row 1: Avatar + Brand name + Status badge + Edit btn ──────────────
        LinearLayout row1 = new LinearLayout(requireContext());
        row1.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Mascot mini
        ImageView avatar = new ImageView(requireContext());
        avatar.setLayoutParams(new LinearLayout.LayoutParams((int)(48*dp),(int)(48*dp)));
        avatar.setImageResource(org.isApproved()
                ? R.drawable.img_mascot_celebrate : R.drawable.img_mascot_wave);
        avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row1.addView(avatar);

        // Brand + status
        LinearLayout infoCol = new LinearLayout(requireContext());
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoLp.setMarginStart((int)(12*dp));
        infoCol.setLayoutParams(infoLp);
        infoCol.setOrientation(LinearLayout.VERTICAL);

        TextView txtBrand = new TextView(requireContext());
        txtBrand.setText(org.getBrandName());
        txtBrand.setTextColor(0xFF1C1B1B);
        txtBrand.setTextSize(15f);
        txtBrand.setTypeface(null, android.graphics.Typeface.BOLD);
        infoCol.addView(txtBrand);

        TextView txtStatus = new TextView(requireContext());
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = (int)(4*dp);
        txtStatus.setLayoutParams(statusLp);
        int ph = (int)(10*dp), pv = (int)(3*dp);
        txtStatus.setPadding(ph, pv, ph, pv);
        if (org.isPending()) {
            txtStatus.setText("⏳ Đang chờ duyệt");
            txtStatus.setTextColor(0xFFE6B93E);
            txtStatus.setBackgroundResource(R.drawable.bg_status_pending);
        } else if (org.isApproved()) {
            txtStatus.setText("✓ Đã xác minh");
            txtStatus.setTextColor(0xFF27AE60);
            txtStatus.setBackgroundResource(R.drawable.bg_status_approved);
        } else {
            txtStatus.setVisibility(View.GONE);
        }
        txtStatus.setTextSize(11f);
        txtStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        infoCol.addView(txtStatus);
        row1.addView(infoCol);

        // Edit button
        LinearLayout btnEdit = new LinearLayout(requireContext());
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editLp.setMarginStart((int)(8*dp));
        btnEdit.setLayoutParams(editLp);
        btnEdit.setOrientation(LinearLayout.HORIZONTAL);
        btnEdit.setGravity(android.view.Gravity.CENTER);
        btnEdit.setBackgroundResource(R.drawable.bg_organizer_cta);
        int ep = (int)(10*dp), epv = (int)(7*dp);
        btnEdit.setPadding(ep, epv, ep, epv);
        btnEdit.setClickable(true);
        btnEdit.setFocusable(true);

        ImageView icEdit = new ImageView(requireContext());
        icEdit.setLayoutParams(new LinearLayout.LayoutParams((int)(14*dp),(int)(14*dp)));
        icEdit.setImageResource(R.drawable.ic_edit);
        btnEdit.addView(icEdit);

        TextView txtEdit = new TextView(requireContext());
        LinearLayout.LayoutParams editTxtLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editTxtLp.setMarginStart((int)(4*dp));
        txtEdit.setLayoutParams(editTxtLp);
        txtEdit.setText("Sửa");
        txtEdit.setTextColor(0xFF226CEB);
        txtEdit.setTextSize(12f);
        btnEdit.addView(txtEdit);
        row1.addView(btnEdit);

        card.addView(row1);

        // ── Divider ──────────────────────────────────────────────────────────
        View div = new View(requireContext());
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        divLp.topMargin = divLp.bottomMargin = (int)(12*dp);
        div.setLayoutParams(divLp);
        div.setBackgroundColor(0xFFF0F0F0);
        card.addView(div);

        // ── Description ──────────────────────────────────────────────────────
        if (org.getDescription() != null && !org.getDescription().isEmpty()) {
            TextView lblDesc = new TextView(requireContext());
            lblDesc.setText("Mô tả");
            lblDesc.setTextColor(0xFF808E92);
            lblDesc.setTextSize(11f);
            lblDesc.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lblLp.bottomMargin = (int)(3*dp);
            lblDesc.setLayoutParams(lblLp);
            card.addView(lblDesc);

            TextView txtDesc = new TextView(requireContext());
            LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            descLp.bottomMargin = (int)(10*dp);
            txtDesc.setLayoutParams(descLp);
            txtDesc.setText(org.getDescription());
            txtDesc.setTextColor(0xFF1C1B1B);
            txtDesc.setTextSize(13f);
            card.addView(txtDesc);
        }

        // ── Website ──────────────────────────────────────────────────────────
        if (org.getWebsiteUrl() != null && !org.getWebsiteUrl().isEmpty()) {
            TextView lblWeb = new TextView(requireContext());
            lblWeb.setText("Website");
            lblWeb.setTextColor(0xFF808E92);
            lblWeb.setTextSize(11f);
            lblWeb.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams wlLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            wlLp.bottomMargin = (int)(3*dp);
            lblWeb.setLayoutParams(wlLp);
            card.addView(lblWeb);

            TextView txtWeb = new TextView(requireContext());
            LinearLayout.LayoutParams webLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            webLp.bottomMargin = (int)(10*dp);
            txtWeb.setLayoutParams(webLp);
            txtWeb.setText(org.getWebsiteUrl());
            txtWeb.setTextColor(0xFF226CEB);
            txtWeb.setTextSize(13f);
            card.addView(txtWeb);
        }

        // ── Mặc định switch (chỉ khi approved) ───────────────────────────────
        if (org.isApproved()) {
            View div2 = new View(requireContext());
            LinearLayout.LayoutParams d2Lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1);
            d2Lp.bottomMargin = (int)(10*dp);
            div2.setLayoutParams(d2Lp);
            div2.setBackgroundColor(0xFFF0F0F0);
            card.addView(div2);

            LinearLayout rowDefault = new LinearLayout(requireContext());
            rowDefault.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            rowDefault.setOrientation(LinearLayout.HORIZONTAL);
            rowDefault.setGravity(android.view.Gravity.CENTER_VERTICAL);

            LinearLayout defaultInfo = new LinearLayout(requireContext());
            defaultInfo.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            defaultInfo.setOrientation(LinearLayout.VERTICAL);

            TextView txtDefaultLabel = new TextView(requireContext());
            txtDefaultLabel.setText("Mặc định");
            txtDefaultLabel.setTextColor(0xFF1C1B1B);
            txtDefaultLabel.setTextSize(13f);
            txtDefaultLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            defaultInfo.addView(txtDefaultLabel);

            TextView txtDefaultSub = new TextView(requireContext());
            txtDefaultSub.setText(org.isDefault() ? "Đã kích hoạt" : "Chưa kích hoạt");
            txtDefaultSub.setTextColor(org.isDefault() ? 0xFF27AE60 : 0xFF808E92);
            txtDefaultSub.setTextSize(11f);
            defaultInfo.addView(txtDefaultSub);
            rowDefault.addView(defaultInfo);

            Switch sw = new Switch(requireContext());
            sw.setChecked(org.isDefault());
            try { sw.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFF226CEB)); } catch (Exception ignored) {}
            try { sw.setTrackTintList(android.content.res.ColorStateList.valueOf(0xFFBBD4F8)); } catch (Exception ignored) {}
            sw.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) {
                    // Chỉ 1 BTC được đặt làm mặc định
                    for (OrganizerProfile op : orgList) op.setDefault(false);
                }
                org.setDefault(checked);
                saveOrgList();
                txtDefaultSub.setText(checked ? "Đã kích hoạt" : "Chưa kích hoạt");
                txtDefaultSub.setTextColor(checked ? 0xFF27AE60 : 0xFF808E92);
                if (checked)
                    Toast.makeText(requireContext(), "✓ Đã đặt \"" + org.getBrandName() + "\" làm BTC mặc định", Toast.LENGTH_SHORT).show();
            });
            rowDefault.addView(sw);
            card.addView(rowDefault);
        }

        // ── Nút Xóa ──────────────────────────────────────────────────────────
        View div3 = new View(requireContext());
        LinearLayout.LayoutParams d3Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        d3Lp.topMargin = d3Lp.bottomMargin = (int)(10*dp);
        div3.setLayoutParams(d3Lp);
        div3.setBackgroundColor(0xFFF0F0F0);
        card.addView(div3);

        LinearLayout btnDeleteRow = new LinearLayout(requireContext());
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnDeleteRow.setLayoutParams(delLp);
        btnDeleteRow.setOrientation(LinearLayout.HORIZONTAL);
        btnDeleteRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        btnDeleteRow.setClickable(true);
        btnDeleteRow.setFocusable(true);

        ImageView icDel = new ImageView(requireContext());
        icDel.setLayoutParams(new LinearLayout.LayoutParams((int)(18*dp),(int)(18*dp)));
        icDel.setImageResource(R.drawable.ic_delete);
        btnDeleteRow.addView(icDel);

        TextView txtDel = new TextView(requireContext());
        LinearLayout.LayoutParams delTxtLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        delTxtLp.setMarginStart((int)(6*dp));
        txtDel.setLayoutParams(delTxtLp);
        txtDel.setText("Xóa ban tổ chức này");
        txtDel.setTextColor(0xFFEB5757);
        txtDel.setTextSize(13f);
        btnDeleteRow.addView(txtDel);
        card.addView(btnDeleteRow);

        // ── Listeners ─────────────────────────────────────────────────────────
        btnEdit.setOnClickListener(v -> showEditDialog(org));
        btnDeleteRow.setOnClickListener(v -> confirmDelete(org));

        return card;
    }

    // ── Listeners ──────────────────────────────────────────────────────────────
    private void setupListeners() {
        /*
        if (btnOrgBack != null)
            btnOrgBack.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0)
                    getParentFragmentManager().popBackStack();
            });

        if (btnAddNewOrg != null)
            btnAddNewOrg.setOnClickListener(v -> showAddDialog());
        */
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────
    private void showAddDialog() {
        showOrgDialog(null);
    }

    private void showEditDialog(OrganizerProfile org) {
        showOrgDialog(org);
    }

    private void showOrgDialog(@Nullable OrganizerProfile existing) {
        boolean isEdit = existing != null;
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_organizer_register, null);

        EditText edtBrand = dialogView.findViewById(R.id.edtBrandName);
        EditText edtDesc  = dialogView.findViewById(R.id.edtDescription);
        EditText edtWeb   = dialogView.findViewById(R.id.edtWebsite);

        if (isEdit) {
            if (edtBrand != null) edtBrand.setText(existing.getBrandName());
            if (edtDesc  != null) edtDesc.setText(existing.getDescription());
            if (edtWeb   != null) edtWeb.setText(existing.getWebsiteUrl());
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(isEdit ? "Chỉnh sửa Ban tổ chức" : "Thêm Ban tổ chức mới")
                .setView(dialogView)
                .setPositiveButton(isEdit ? "Lưu thay đổi" : "Gửi đăng ký", (dialog, which) -> {
                    String brand = edtBrand != null ? edtBrand.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(brand)) {
                        Toast.makeText(requireContext(), "Vui lòng nhập tên ban tổ chức", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String desc = edtDesc != null ? edtDesc.getText().toString().trim() : "";
                    String web  = edtWeb  != null ? edtWeb.getText().toString().trim() : "";

                    if (isEdit) {
                        existing.setBrandName(brand);
                        existing.setDescription(desc);
                        existing.setWebsiteUrl(web);
                        saveOrgList();
                        saveOrgToFirestore(existing, false);
                    } else {
                        OrganizerProfile newOrg = new OrganizerProfile(brand, desc, web, Constants.ORG_STATUS_PENDING);
                        orgList.add(newOrg);
                        saveOrgList();
                        saveOrgToFirestore(newOrg, true);
                    }
                    refreshUI();
                    String msg = isEdit
                            ? "Đã cập nhật thông tin BTC"
                            : "Đã gửi đăng ký! Chờ duyệt trong 1–3 ngày làm việc.";
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDelete(OrganizerProfile org) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa Ban tổ chức")
                .setMessage("Bạn có chắc muốn xóa \"" + org.getBrandName() + "\" không?\nThao tác này không thể hoàn tác.")
                .setPositiveButton("Xóa", (d, w) -> {
                    deleteOrgFromFirestore(org);
                    orgList.remove(org);
                    saveOrgList();
                    refreshUI();
                    Toast.makeText(requireContext(), "Đã xóa \"" + org.getBrandName() + "\"", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
