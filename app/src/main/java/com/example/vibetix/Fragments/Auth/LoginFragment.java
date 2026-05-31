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

/**
 * LoginFragment — email + password login with optional "Remember me",
 * Google / Apple social stubs, and forgot-password link.
 *
 * UI behaviour:
 *  • Status-bar top inset applied only to the blue header.
 *  • Input containers change border colour on focus / blur.
 *  • Eye icon toggles password visibility.
 *  • Remember me: if checked the login state is persisted in SharedPreferences;
 *    if unchecked the KEY_REMEMBER_ME flag is stored as false (used later when
 *    Firebase Auth is wired up to decide whether to persist the token).
 *  • Validation: non-empty, valid email format, password ≥ 6 chars.
 *  • On success: saves credentials + remember preference, navigates to UserMainActivity.
 */
public class LoginFragment extends Fragment {

    // ── Views ─────────────────────────────────────────────────────────────────
    LinearLayout layoutLoginHeader;
    LinearLayout containerEmail;
    LinearLayout containerPassword;
    EditText     etEmail;
    EditText     etPassword;
    ImageView    btnTogglePassword;
    TextView     txtForgotPassword;
    CheckBox     cbRememberMe;
    TextView     txtLoginError;
    Button       btnLogin;
    LinearLayout btnLoginGoogle;
    LinearLayout btnLoginApple;
    TextView     txtGoToRegister;

    // ── State ──────────────────────────────────────────────────────────────────
    private boolean isPasswordVisible = false;

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        bindViews(view);
        restoreRememberMe();
        applyInsets();
        setupFocusEffects();
        setupClickListeners();
        return view;
    }

    // ── View binding ───────────────────────────────────────────────────────────
    private void bindViews(View view) {
        layoutLoginHeader  = view.findViewById(R.id.layoutLoginHeader);
        containerEmail     = view.findViewById(R.id.containerEmail);
        containerPassword  = view.findViewById(R.id.containerPassword);
        etEmail            = view.findViewById(R.id.etEmail);
        etPassword         = view.findViewById(R.id.etPassword);
        btnTogglePassword  = view.findViewById(R.id.btnTogglePassword);
        txtForgotPassword  = view.findViewById(R.id.txtForgotPassword);
        cbRememberMe       = view.findViewById(R.id.cbRememberMe);
        txtLoginError      = view.findViewById(R.id.txtLoginError);
        btnLogin           = view.findViewById(R.id.btnLogin);
        btnLoginGoogle     = view.findViewById(R.id.btnLoginGoogle);
        btnLoginApple      = view.findViewById(R.id.btnLoginApple);
        txtGoToRegister    = view.findViewById(R.id.txtGoToRegister);
    }

    // ── Restore "Remember me" state from previous session ─────────────────────
    private void restoreRememberMe() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(Constants.PREFS_AUTH, android.content.Context.MODE_PRIVATE);
        boolean remembered = prefs.getBoolean(Constants.KEY_REMEMBER_ME, false);
        if (cbRememberMe != null) {
            cbRememberMe.setChecked(remembered);
        }
        // If user was remembered, pre-fill email
        if (remembered) {
            String savedEmail = prefs.getString(Constants.KEY_USER_EMAIL, "");
            if (etEmail != null && !savedEmail.isEmpty()) {
                etEmail.setText(savedEmail);
            }
        }
    }

    // ── Status-bar inset (top only — same pattern as HomeFragment) ─────────────
    private void applyInsets() {
        if (layoutLoginHeader == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(layoutLoginHeader, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    // ── Input focus border effects ─────────────────────────────────────────────
    private void setupFocusEffects() {
        applyFocusEffect(containerEmail, etEmail);
        applyFocusEffect(containerPassword, etPassword);
    }

    private void applyFocusEffect(LinearLayout container, EditText editText) {
        editText.setOnFocusChangeListener((v, hasFocus) ->
                container.setBackgroundResource(hasFocus
                        ? R.drawable.bg_input_focused
                        : R.drawable.bg_input_normal));
    }

    // ── Click listeners ────────────────────────────────────────────────────────
    private void setupClickListeners() {

        // Eye icon — toggle password visibility
        btnTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            etPassword.setTransformationMethod(isPasswordVisible
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(isPasswordVisible
                    ? R.drawable.ic_auth_eye
                    : R.drawable.ic_auth_eye_off);
            // Keep cursor at end after toggling
            etPassword.setSelection(etPassword.getText().length());
        });

        // Quên mật khẩu — placeholder (Firebase reset password will go here)
        txtForgotPassword.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Tính năng sắp ra mắt", Toast.LENGTH_SHORT).show());

        // Google social login — placeholder
        btnLoginGoogle.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Đăng nhập Google — sắp ra mắt", Toast.LENGTH_SHORT).show());

        // Apple social login — placeholder
        btnLoginApple.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Đăng nhập Apple — sắp ra mắt", Toast.LENGTH_SHORT).show());

        // Đăng nhập
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Chưa có tài khoản? → Register
        txtGoToRegister.setOnClickListener(v -> {
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).showRegisterFragment();
            }
        });
    }

    // ── Validation + login logic ───────────────────────────────────────────────
    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.str_error_empty_field));
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.str_error_invalid_email));
            return;
        }
        if (password.length() < 6) {
            showError(getString(R.string.str_error_password_too_short));
            return;
        }

        hideError();

        // TODO: replace with Firebase Auth call when backend is ready.
        // Mock: any valid-format credentials → login as customer.
        onLoginSuccess(email, Constants.ROLE_CUSTOMER);
    }

    private void onLoginSuccess(String email, String role) {
        boolean rememberMe = cbRememberMe != null && cbRememberMe.isChecked();

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(Constants.PREFS_AUTH, android.content.Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean(Constants.KEY_IS_LOGGED_IN, true)
                .putString(Constants.KEY_USER_EMAIL, email)
                .putString(Constants.KEY_USER_ROLE, role)
                .putBoolean(Constants.KEY_REMEMBER_ME, rememberMe);

        // If "Remember me" is NOT checked we still log in for this session,
        // but we clear the stored email so it won't be pre-filled next time.
        // If "Remember me" is NOT checked, we still keep the email for the active session.
        // The session will be cleared on next app start in MainActivity if rememberMe is false.
        editor.apply();

        Intent intent = new Intent(requireContext(), UserMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // ── Error helpers ──────────────────────────────────────────────────────────
    private void showError(String message) {
        txtLoginError.setText(message);
        txtLoginError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        txtLoginError.setVisibility(View.GONE);
    }
}
