package com.example.vibetix.Fragments.Auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Activities.AuthActivity;
import com.example.vibetix.Activities.UserMainActivity;
import com.example.vibetix.R;
import com.example.vibetix.Utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * RegisterFragment — đăng ký tài khoản cá nhân mới.
 *
 * Mọi tài khoản đều bắt đầu với role CUSTOMER.
 * Sau này người dùng có thể cập nhật thông tin ban tổ chức
 * trong trang quản lý hồ sơ để nâng cấp lên Organizer.
 *
 * Fields: Họ và tên, Email, Số điện thoại, Mật khẩu, Xác nhận mật khẩu, Terms.
 *
 * (Thay bằng Firebase Auth + Firestore write khi backend sẵn sàng.)
 */
public class RegisterFragment extends Fragment {

    // ── Views ──────────────────────────────────────────────────────────────────
    View      layoutRegisterHeader;
    ImageView btnRegisterBack;

    // Name field
    LinearLayout containerFullName;
    EditText     etFullName;

    // Common fields
    LinearLayout containerRegEmail;
    LinearLayout containerPhone;
    LinearLayout containerRegPassword;
    LinearLayout containerConfirmPassword;
    EditText     etRegEmail;
    EditText     etPhone;
    EditText     etRegPassword;
    EditText     etConfirmPassword;
    ImageView    btnToggleRegPassword;
    ImageView    btnToggleConfirmPassword;

    // Terms
    CheckBox cbTerms;
    TextView txtTermsLink;

    // Bottom
    TextView txtRegisterError;
    Button   btnRegister;
    TextView txtGoToLogin;

    // ── State ──────────────────────────────────────────────────────────────────
    private boolean isPasswordVisible        = false;
    private boolean isConfirmPasswordVisible = false;

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);
        bindViews(view);
        applyInsets();
        setupFocusEffects();
        setupClickListeners();
        return view;
    }

    // ── View binding ───────────────────────────────────────────────────────────
    private void bindViews(View view) {
        layoutRegisterHeader     = view.findViewById(R.id.layoutRegisterHeader);
        btnRegisterBack          = view.findViewById(R.id.btnRegisterBack);

        containerFullName        = view.findViewById(R.id.containerFullName);
        etFullName               = view.findViewById(R.id.etFullName);

        containerRegEmail        = view.findViewById(R.id.containerRegEmail);
        containerPhone           = view.findViewById(R.id.containerPhone);
        containerRegPassword     = view.findViewById(R.id.containerRegPassword);
        containerConfirmPassword = view.findViewById(R.id.containerConfirmPassword);
        etRegEmail               = view.findViewById(R.id.etRegEmail);
        etPhone                  = view.findViewById(R.id.etPhone);
        etRegPassword            = view.findViewById(R.id.etRegPassword);
        etConfirmPassword        = view.findViewById(R.id.etConfirmPassword);
        btnToggleRegPassword     = view.findViewById(R.id.btnToggleRegPassword);
        btnToggleConfirmPassword = view.findViewById(R.id.btnToggleConfirmPassword);

        cbTerms                  = view.findViewById(R.id.cbTerms);
        txtTermsLink             = view.findViewById(R.id.txtTermsLink);

        txtRegisterError         = view.findViewById(R.id.txtRegisterError);
        btnRegister              = view.findViewById(R.id.btnRegister);
        txtGoToLogin             = view.findViewById(R.id.txtGoToLogin);
    }

    // ── Status-bar inset ───────────────────────────────────────────────────────
    private void applyInsets() {
        if (btnRegisterBack == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(btnRegisterBack, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.topMargin = topInset + 8;
            v.setLayoutParams(lp);
            return insets;
        });
    }

    // ── Input focus effects ────────────────────────────────────────────────────
    private void setupFocusEffects() {
        applyFocusEffect(containerFullName,        etFullName);
        applyFocusEffect(containerRegEmail,        etRegEmail);
        applyFocusEffect(containerPhone,           etPhone);
        applyFocusEffect(containerRegPassword,     etRegPassword);
        applyFocusEffect(containerConfirmPassword, etConfirmPassword);
    }

    private void applyFocusEffect(LinearLayout container, EditText editText) {
        if (container == null || editText == null) return;
        editText.setOnFocusChangeListener((v, hasFocus) ->
                container.setBackgroundResource(hasFocus
                        ? R.drawable.bg_input_focused
                        : R.drawable.bg_input_normal));
    }

    // ── Click listeners ────────────────────────────────────────────────────────
    private void setupClickListeners() {

        // Back button → return to Login
        btnRegisterBack.setOnClickListener(v -> {
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).showLoginFragment();
            }
        });

        // Password eye toggle
        btnToggleRegPassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            etRegPassword.setTransformationMethod(isPasswordVisible
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            btnToggleRegPassword.setImageResource(isPasswordVisible
                    ? R.drawable.ic_auth_eye
                    : R.drawable.ic_auth_eye_off);
            etRegPassword.setSelection(etRegPassword.getText().length());
        });

        // Confirm-password eye toggle
        btnToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            etConfirmPassword.setTransformationMethod(isConfirmPasswordVisible
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            btnToggleConfirmPassword.setImageResource(isConfirmPasswordVisible
                    ? R.drawable.ic_auth_eye
                    : R.drawable.ic_auth_eye_off);
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });

        // Terms & Conditions link
        if (txtTermsLink != null) {
            txtTermsLink.setOnClickListener(v ->
                    Toast.makeText(requireContext(),
                            "Điều khoản & Điều kiện — sắp ra mắt", Toast.LENGTH_SHORT).show());
        }

        // Đăng ký button
        btnRegister.setOnClickListener(v -> attemptRegister());

        // Đã có tài khoản? → back to Login
        txtGoToLogin.setOnClickListener(v -> {
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).showLoginFragment();
            }
        });
    }

    // ── Validation + register logic ────────────────────────────────────────────
    private void attemptRegister() {
        String name            = etFullName.getText().toString().trim();
        String email           = etRegEmail.getText().toString().trim();
        String phone           = etPhone.getText().toString().trim();
        String password        = etRegPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Non-empty check
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty()) {
            showError(getString(R.string.str_error_empty_field));
            return;
        }

        // Email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.str_error_invalid_email));
            return;
        }

        // Password length
        if (password.length() < 6) {
            showError(getString(R.string.str_error_password_too_short));
            return;
        }

        // Password match
        if (!password.equals(confirmPassword)) {
            showError(getString(R.string.str_error_password_mismatch));
            return;
        }

        // Terms must be accepted
        if (cbTerms != null && !cbTerms.isChecked()) {
            showError(getString(R.string.str_error_terms_required));
            return;
        }

        hideError();
        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");

        final String finalName  = name;
        final String finalPhone = phone;

        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                String uid = authResult.getUser().getUid();

                // Tạo document trong Firestore collection "users"
                Map<String, Object> userData = new HashMap<>();
                userData.put("user_id",    uid);
                userData.put("full_name",  finalName);
                userData.put("email",      email);
                userData.put("phone",      finalPhone);
                userData.put("role",       "customer");
                userData.put("created_at", FieldValue.serverTimestamp());

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(userData)
                    .addOnSuccessListener(v -> onRegisterSuccess(finalName, email, finalPhone))
                    .addOnFailureListener(e -> {
                        // Firestore write thất bại nhưng Auth đã tạo → vẫn cho vào
                        onRegisterSuccess(finalName, email, finalPhone);
                    });
            })
            .addOnFailureListener(e -> {
                btnRegister.setEnabled(true);
                btnRegister.setText("Đăng ký");
                String msg = "Đăng ký thất bại";
                if (e.getMessage() != null) {
                    if (e.getMessage().contains("email address is already in use"))
                        msg = "Email này đã được đăng ký. Vui lòng dùng email khác.";
                    else if (e.getMessage().contains("badly formatted"))
                        msg = "Email không hợp lệ.";
                }
                showError(msg);
            });
    }

    private void onRegisterSuccess(String name, String email, String phone) {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(Constants.PREFS_AUTH, android.content.Context.MODE_PRIVATE);

        prefs.edit()
                .putBoolean(Constants.KEY_IS_LOGGED_IN, true)
                .putString(Constants.KEY_USER_NAME,  name)
                .putString(Constants.KEY_USER_EMAIL, email)
                .putString(Constants.KEY_USER_PHONE, phone)
                .putString(Constants.KEY_USER_ROLE,  Constants.ROLE_CUSTOMER)
                .apply();

        Intent intent = new Intent(requireContext(), UserMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // ── Error helpers ──────────────────────────────────────────────────────────
    private void showError(String message) {
        if (txtRegisterError == null) return;
        txtRegisterError.setText(message);
        txtRegisterError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        if (txtRegisterError == null) return;
        txtRegisterError.setVisibility(View.GONE);
    }
}
