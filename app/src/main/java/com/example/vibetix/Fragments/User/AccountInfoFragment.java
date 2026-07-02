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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.vibetix.R;
import com.example.vibetix.Utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

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

    // ── Hiển thị avatar tròn với Glide ───────────────────────────────────────
    private void loadAvatarImage(@Nullable String url) {
        if (imgAccountAvatar == null || !isAdded()) return;
        Glide.with(this)
            .load((url != null && !url.isEmpty()) ? url : R.drawable.img_mascot_normal)
            .apply(new RequestOptions()
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.img_mascot_normal)
                .error(R.drawable.img_mascot_normal))
            .into(imgAccountAvatar);
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

    // ── Upload avatar lên Firebase Storage ───────────────────────────────────
    private void uploadAvatar(Uri uri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Preview ngay trước khi upload
        if (imgAccountAvatar != null) {
            Glide.with(this)
                .load(uri)
                .apply(new RequestOptions().circleCrop())
                .into(imgAccountAvatar);
        }

        if (btnCameraAvatar != null) btnCameraAvatar.setEnabled(false);
        Toast.makeText(requireContext(), "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        // Đọc bytes qua ContentResolver — tránh lỗi permission với content:// URI trên Android 10+
        byte[] imageBytes;
        try {
            java.io.InputStream stream =
                requireContext().getContentResolver().openInputStream(uri);
            if (stream == null) throw new Exception("Cannot open stream");
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[1024 * 64];
            int len;
            while ((len = stream.read(buf)) != -1) baos.write(buf, 0, len);
            stream.close();
            imageBytes = baos.toByteArray();
        } catch (Exception e) {
            if (!isAdded()) return;
            if (btnCameraAvatar != null) btnCameraAvatar.setEnabled(true);
            loadAvatarImage(authPrefs.getString(Constants.KEY_USER_AVATAR, null));
            Toast.makeText(requireContext(), "Không đọc được ảnh, thử lại", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference ref = FirebaseStorage.getInstance()
            .getReference("avatars/" + user.getUid() + ".jpg");

        StorageMetadata metadata = new com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build();

        ref.putBytes(imageBytes, metadata)
            .addOnSuccessListener(taskSnapshot ->
                ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    if (!isAdded()) return;
                    String newUrl = downloadUri.toString();
                    pendingAvatarUrl = newUrl;

                    FirebaseFirestore.getInstance()
                        .collection("users").document(user.getUid())
                        .update("avatar_url", newUrl)
                        .addOnSuccessListener(v2 -> {
                            if (!isAdded()) return;
                            authPrefs.edit()
                                .putString(Constants.KEY_USER_AVATAR, newUrl)
                                .apply();
                            Glide.get(requireContext()).clearMemory();
                            loadAvatarImage(newUrl);
                            Toast.makeText(requireContext(), "✓ Đã cập nhật ảnh đại diện", Toast.LENGTH_SHORT).show();
                        });

                    if (btnCameraAvatar != null) btnCameraAvatar.setEnabled(true);
                })
            )
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                if (btnCameraAvatar != null) btnCameraAvatar.setEnabled(true);
                loadAvatarImage(authPrefs.getString(Constants.KEY_USER_AVATAR, null));
                Toast.makeText(requireContext(),
                    "Tải ảnh thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
            edtFullName.setError("Vui lòng nhập họ và tên");
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

            if (btnSaveInfo != null) { btnSaveInfo.setEnabled(false); btnSaveInfo.setText("Đang lưu..."); }

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update(updates)
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "✓ Đã lưu thông tin", Toast.LENGTH_SHORT).show();
                    if (getParentFragmentManager().getBackStackEntryCount() > 0)
                        getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    if (btnSaveInfo != null) { btnSaveInfo.setEnabled(true); btnSaveInfo.setText("Lưu thay đổi"); }
                    Toast.makeText(requireContext(), "Lưu thất bại, thử lại sau", Toast.LENGTH_SHORT).show();
                });
        } else {
            Toast.makeText(requireContext(), "✓ Đã lưu thông tin", Toast.LENGTH_SHORT).show();
            if (getParentFragmentManager().getBackStackEntryCount() > 0)
                getParentFragmentManager().popBackStack();
        }
    }
}
