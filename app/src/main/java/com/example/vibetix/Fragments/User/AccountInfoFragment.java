package com.example.vibetix.Fragments.User;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.vibetix.R;
import com.example.vibetix.Utils.Constants;

import java.util.Calendar;

public class AccountInfoFragment extends Fragment {

    private ImageView btnBack;
    private ImageView imgAccountAvatar;
    private View      btnCameraAvatar;
    private EditText  edtFullName, edtEmail, edtPhone, edtDob;
    private RadioGroup  rgGender;
    private RadioButton rbMale, rbFemale, rbOther;
    private Button    btnSaveInfo;

    private SharedPreferences authPrefs, profilePrefs;

    // ── Image picker ──────────────────────────────────────────────────────────
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            imgAccountAvatar.setImageURI(uri);
                            // TODO: Upload to Firebase Storage and save URL
                        }
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
        loadCurrentData();
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

    private void loadCurrentData() {
        edtFullName.setText(authPrefs.getString(Constants.KEY_USER_NAME,  ""));
        edtEmail.setText(authPrefs.getString(Constants.KEY_USER_EMAIL, ""));
        edtPhone.setText(authPrefs.getString(Constants.KEY_USER_PHONE, ""));
        edtDob.setText(profilePrefs.getString(Constants.KEY_USER_DOB, ""));

        String gender = profilePrefs.getString(Constants.KEY_USER_GENDER, "");
        switch (gender) {
            case "male":   if (rbMale   != null) rbMale.setChecked(true);   break;
            case "female": if (rbFemale != null) rbFemale.setChecked(true); break;
            case "other":  if (rbOther  != null) rbOther.setChecked(true);  break;
        }
    }

    private void setupListeners() {

        // Back button — pop back stack
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }

        // Camera → open image picker
        if (btnCameraAvatar != null) {
            btnCameraAvatar.setOnClickListener(v ->
                    imagePickerLauncher.launch("image/*"));
        }

        // DOB — date picker
        if (edtDob != null) {
            edtDob.setOnClickListener(v -> showDatePicker());
        }

        // DOB container tap
        View containerDob = requireView().findViewById(R.id.containerDob);
        if (containerDob != null) {
            containerDob.setOnClickListener(v -> showDatePicker());
        }

        // Save
        if (btnSaveInfo != null) {
            btnSaveInfo.setOnClickListener(v -> saveInfo());
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR) - 20;
        // If DOB is already set, parse it
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

        // Cache local ngay lập tức
        authPrefs.edit()
                .putString(Constants.KEY_USER_NAME,  name)
                .putString(Constants.KEY_USER_PHONE, phone)
                .apply();
        profilePrefs.edit()
                .putString(Constants.KEY_USER_DOB,    dob)
                .putString(Constants.KEY_USER_GENDER, gender)
                .apply();

        // Sync lên Firestore
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
                    if (btnSaveInfo != null) { btnSaveInfo.setEnabled(true); btnSaveInfo.setText("Lưu thông tin"); }
                    Toast.makeText(requireContext(), "Lưu thất bại, thử lại sau", Toast.LENGTH_SHORT).show();
                });
        } else {
            Toast.makeText(requireContext(), "✓ Đã lưu thông tin", Toast.LENGTH_SHORT).show();
            if (getParentFragmentManager().getBackStackEntryCount() > 0)
                getParentFragmentManager().popBackStack();
        }
    }
}
