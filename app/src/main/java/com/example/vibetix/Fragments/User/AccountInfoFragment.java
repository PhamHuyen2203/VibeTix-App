package com.example.vibetix.Fragments.User;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.vibetix.R;
import com.example.vibetix.Utils.Constants;
import com.example.vibetix.Utils.ImageUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AccountInfoFragment extends Fragment {

    private ImageView btnBack;
    private ImageView imgAccountAvatar;
    private View      btnCameraAvatar;
    private EditText  edtFullName, edtEmail, edtPhone, edtDob;
    private RadioGroup  rgGender;
    private RadioButton rbMale, rbFemale, rbOther;
    private Button    btnSaveInfo;
    private SharedPreferences authPrefs, profilePrefs;
    private String pendingAvatarUrl = null; // URL mới sau khi upload thành công

    // ── Image picker ──────────────────────────────────────────────────────────
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) uploadAvatar(uri);
                    });

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authPrefs    = requireContext().getSharedPreferences(Constants.PREFS_AUTH,    Context.MODE_PRIVATE);
        profilePrefs = requireContext().getSharedPreferences(Constants.PREFS_PROFILE, Context.MODE_PRIVATE);

        bindViews(view);
        applyInsets();
        loadFromFirebase(); // luôn đồng bộ từ Firebase
        setupListeners();
    }

    private void bindViews(View v) {
        btnBack          = v.findViewById(R.id.btnBack);
        imgAccountAvatar = v.findViewById(R.id.imgAccountAvatar);
        btnCameraAvatar  = v.findViewById(R.id.btnCameraAvatar);
        edtFullName      = v.findViewById(R.id.edtFullName);
        edtEmail         = v.findViewById(R.id.edtEmail);
        edtPhone         = v.findViewById(R.id.edtPhone);
        edtDob           = v.findViewById(R.id.edtDob);
        rgGender         = v.findViewById(R.id.rgGender);
        rbMale           = v.findViewById(R.id.rbMale);
        rbFemale         = v.findViewById(R.id.rbFemale);
        rbOther          = v.findViewById(R.id.rbOther);
        btnSaveInfo      = v.findViewById(R.id.btnSaveInfo);
        // Hiển thị dữ liệu cache trước khi Firebase trả về
        loadFromCache();
    }

    // ── Status bar inset ──────────────────────────────────────────────────────
    private void applyInsets() {
        View header = requireView().findViewById(R.id.layoutAccountInfoHeader);
        if (header == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, top, 0, 0);
            return insets;
        });
    }

    // ── Load cache trước (hiện ngay, không chờ Firebase) ─────────────────────
    private void loadFromCache() {
        edtFullName.setText(authPrefs.getString(Constants.KEY_USER_NAME,  ""));
        edtEmail.setText(authPrefs.getString(Constants.KEY_USER_EMAIL, ""));
        edtPhone.setText(authPrefs.getString(Constants.KEY_USER_PHONE, ""));
        edtDob.setText(profilePrefs.getString(Constants.KEY_USER_DOB, ""));

        String gender = profilePrefs.getString(Constants.KEY_USER_GENDER, "");
        applyGender(gender);

        // Avatar từ cache
        String cachedAvatar = authPrefs.getString(Constants.KEY_USER_AVATAR, null);
        loadAvatarImage(cachedAvatar);
    }

    // ── Đồng bộ dữ liệu thật từ Firestore ───────────────────────────────────
    private void loadFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.getUid())
            .get()
            .addOnSuccessListener(doc -> {
                if (!isAdded() || doc == null || !doc.exists()) return;
                populateFromDoc(doc);
            });
    }

    private void populateFromDoc(DocumentSnapshot doc) {
        // Họ tên
        String name = doc.getString("full_name");
        if (name == null) name = doc.getString("fullName");
        if (name != null && edtFullName != null) edtFullName.setText(name);

        // Email (readonly)
        String email = doc.getString("email");
        if (email != null && edtEmail != null) edtEmail.setText(email);

        // Số điện thoại
        String phone = doc.getString("phone");
        if (phone != null && edtPhone != null) edtPhone.setText(phone);

        // Ngày sinh
        String dob = doc.getString("dob");
        if (dob != null && edtDob != null) edtDob.setText(dob);

        // Giới tính
        String gender = doc.getString("gender");
        if (gender != null) applyGender(gender);

        // Avatar URL
        String avatarUrl = doc.getString("avatar_url");
        loadAvatarImage(avatarUrl);

        // Cập nhật cache
        String finalName  = name  != null ? name  : "";
        String finalPhone = phone != null ? phone : "";
        String finalDob   = dob   != null ? dob   : "";
        String finalGender = gender != null ? gender : "";
        String finalEmail = email != null ? email : "";

        authPrefs.edit()
            .putString(Constants.KEY_USER_NAME,   finalName)
            .putString(Constants.KEY_USER_PHONE,  finalPhone)
            .putString(Constants.KEY_USER_EMAIL,  finalEmail)
            .putString(Constants.KEY_USER_AVATAR, avatarUrl != null ? avatarUrl : "")
            .apply();
        profilePrefs.edit()
            .putString(Constants.KEY_USER_DOB,    finalDob)
            .putString(Constants.KEY_USER_GENDER, finalGender)
            .apply();
    }

    // ── Hiển thị avatar tròn — hỗ trợ cả HTTP URL lẫn Base64 ────────────────
    private void loadAvatarImage(@Nullable String urlOrBase64) {
        if (imgAccountAvatar == null || !isAdded()) return;
        ImageUtils.loadCircle(this, urlOrBase64, imgAccountAvatar, R.drawable.img_mascot_normal);
    }

    private void applyGender(String gender) {
        if (gender == null) return;
        switch (gender) {
            case "male":   if (rbMale   != null) rbMale.setChecked(true);   break;
            case "female": if (rbFemale != null) rbFemale.setChecked(true); break;
            case "other":  if (rbOther  != null) rbOther.setChecked(true);  break;
        }
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }

        if (btnCameraAvatar != null) {
            btnCameraAvatar.setOnClickListener(v ->
                    imagePickerLauncher.launch("image/*"));
        }

        if (edtDob != null) {
            edtDob.setOnClickListener(v -> showDatePicker());
        }
        View containerDob = requireView().findViewById(R.id.containerDob);
        if (containerDob != null) {
            containerDob.setOnClickListener(v -> showDatePicker());
        }

        if (btnSaveInfo != null) {
            btnSaveInfo.setOnClickListener(v -> saveInfo());
        }
    }

    // ── Upload avatar → compress → Base64 → Firestore (không cần Firebase Storage) ──
    private void uploadAvatar(Uri uri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        if (btnCameraAvatar != null) btnCameraAvatar.setEnabled(false);
        Toast.makeText(requireContext(), getString(R.string.str_toast_processing_image), Toast.LENGTH_SHORT).show();

        // Compress: max 400×400, max 150 KB
        byte[] compressed = ImageUtils.compressToJpeg(requireContext(), uri, 400, 150);
        if (compressed == null) {
            if (btnCameraAvatar != null) btnCameraAvatar.setEnabled(true);
            Toast.makeText(requireContext(), getString(R.string.str_toast_cannot_read_image), Toast.LENGTH_SHORT).show();
            return;
        }

        // Preview ngay từ bytes đã compress
        if (imgAccountAvatar != null) {
            Glide.with(this).load(compressed)
                .apply(new com.bumptech.glide.request.RequestOptions().circleCrop())
                .into(imgAccountAvatar);
        }

        String base64 = ImageUtils.toBase64(compressed);
        pendingAvatarUrl = base64;

        // Lưu thẳng vào Firestore — không dùng Storage
        FirebaseFirestore.getInstance()
            .collection("users").document(user.getUid())
            .update("avatar_url", base64)
            .addOnSuccessListener(v -> {
                if (!isAdded()) return;
                if (btnCameraAvatar != null) btnCameraAvatar.setEnabled(true);
                authPrefs.edit().putString(Constants.KEY_USER_AVATAR, base64).apply();
                Toast.makeText(requireContext(), getString(R.string.str_toast_avatar_updated), Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                if (btnCameraAvatar != null) btnCameraAvatar.setEnabled(true);
                Toast.makeText(requireContext(), getString(R.string.str_toast_save_avatar_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
            });
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR) - 20;
        String dobStr = edtDob.getText().toString().trim();
        if (!dobStr.isEmpty() && dobStr.contains("/")) {
            try {
                String[] parts = dobStr.split("/");
                year = Integer.parseInt(parts[2]);
                int month = Integer.parseInt(parts[1]) - 1;
                int day   = Integer.parseInt(parts[0]);
                new DatePickerDialog(requireContext(),
                        (dp, y, m, d) -> edtDob.setText(
                                String.format("%02d/%02d/%04d", d, m + 1, y)),
                        year, month, day).show();
                return;
            } catch (Exception ignored) {}
        }
        new DatePickerDialog(requireContext(),
                (dp, y, m, d) -> edtDob.setText(
                        String.format("%02d/%02d/%04d", d, m + 1, y)),
                year,
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveInfo() {
        String name  = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String dob   = edtDob.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            edtFullName.setError(getString(R.string.str_error_enter_full_name));
            edtFullName.requestFocus();
            return;
        }

        String gender = "other";
        if (rgGender != null) {
            int checkedId = rgGender.getCheckedRadioButtonId();
            if (checkedId == R.id.rbMale)        gender = "male";
            else if (checkedId == R.id.rbFemale) gender = "female";
        }

        // Cache local ngay
        authPrefs.edit()
                .putString(Constants.KEY_USER_NAME,  name)
                .putString(Constants.KEY_USER_PHONE, phone)
                .apply();
        profilePrefs.edit()
                .putString(Constants.KEY_USER_DOB,    dob)
                .putString(Constants.KEY_USER_GENDER, gender)
                .apply();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("full_name", name);
            updates.put("phone",     phone);
            updates.put("dob",       dob);
            updates.put("gender",    gender);

            if (btnSaveInfo != null) { btnSaveInfo.setEnabled(false); btnSaveInfo.setText(getString(R.string.str_btn_saving)); }

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    if (btnSaveInfo != null) { btnSaveInfo.setEnabled(true); btnSaveInfo.setText(getString(R.string.str_save_changes)); }
                    Toast.makeText(requireContext(), getString(R.string.str_toast_info_saved), Toast.LENGTH_SHORT).show();
                    if (getParentFragmentManager().getBackStackEntryCount() > 0)
                        getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    if (btnSaveInfo != null) { btnSaveInfo.setEnabled(true); btnSaveInfo.setText(getString(R.string.str_save_changes)); }
                    Toast.makeText(requireContext(), getString(R.string.str_toast_save_info_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
        } else {
            Toast.makeText(requireContext(), getString(R.string.str_toast_info_saved), Toast.LENGTH_SHORT).show();
            if (getParentFragmentManager().getBackStackEntryCount() > 0)
                getParentFragmentManager().popBackStack();
        }
    }
}
