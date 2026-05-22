package com.example.vibetix.Fragments.Auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.content.res.ColorStateList;
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
 * RegisterFragment — collects user information and creates a new account.
 *
 * Role selector at top:
 *  • "Người mua vé" (customer) → shows Họ và tên field (sectionUserName)
 *  • "Ban tổ chức" (organizer)  → shows Tên tổ chức field (sectionOrgName)
 *
 * Both roles share: Email, Phone, Password, Confirm Password, Terms checkbox.
 *
 * On success: saves to SharedPreferences and navigates to UserMainActivity.
 * (Replace with Firebase Auth / Firestore write when backend is ready.)
 */
public class RegisterFragment extends Fragment {

    // ── Views ──────────────────────────────────────────────────────────────────
    // layoutRegisterHeader is a FrameLayout in XML — held as View to avoid ClassCastException
    View      layoutRegisterHeader;
    ImageView btnRegisterBack;

    // Role toggle
    LinearLayout btnRoleCustomer;
    LinearLayout btnRoleOrganizer;
    ImageView    icRoleCustomer;
    ImageView    icRoleOrganizer;
    TextView     txtRoleCustomer;
    TextView     txtRoleOrganizer;

    // Dynamic name sections
    LinearLayout sectionUserName;          // visible when Customer selected
    LinearLayout sectionOrgName;           // visible when Organizer selected
    LinearLayout containerFullName;
    LinearLayout containerOrgName;
    EditText     etFullName;
    EditText     etOrgName;

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
    private String  selectedRole             = Constants.ROLE_CUSTOMER;
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
        setupRoleSelector();
        setupClickListeners();
        return view;
    }

    // ── View binding ───────────────────────────────────────────────────────────
    private void bindViews(View view) {
        layoutRegisterHeader     = view.findViewById(R.id.layoutRegisterHeader);
        btnRegisterBack          = view.findViewById(R.id.btnRegisterBack);

        btnRoleCustomer          = view.findViewById(R.id.btnRoleCustomer);
        btnRoleOrganizer         = view.findViewById(R.id.btnRoleOrganizer);
        icRoleCustomer           = view.findViewById(R.id.icRoleCustomer);
        icRoleOrganizer          = view.findViewById(R.id.icRoleOrganizer);
        txtRoleCustomer          = view.findViewById(R.id.txtRoleCustomer);
        txtRoleOrganizer         = view.findViewById(R.id.txtRoleOrganizer);

        sectionUserName          = view.findViewById(R.id.sectionUserName);
        sectionOrgName           = view.findViewById(R.id.sectionOrgName);
        containerFullName        = view.findViewById(R.id.containerFullName);
        containerOrgName         = view.findViewById(R.id.containerOrgName);
        etFullName               = view.findViewById(R.id.etFullName);
        etOrgName                = view.findViewById(R.id.etOrgName);

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
    // Back button lives in a FrameLayout overlay, so we push it down with marginTop
    // instead of adding paddingTop to a parent row (which used to squish the button).
    private void applyInsets() {
        if (btnRegisterBack == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(btnRegisterBack, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.topMargin = topInset + 8;   // 8dp breathing room below status bar
            v.setLayoutParams(lp);
            return insets;
        });
    }

    // ── Input focus effects ────────────────────────────────────────────────────
    private void setupFocusEffects() {
        applyFocusEffect(containerFullName, etFullName);
        applyFocusEffect(containerOrgName,  etOrgName);
        applyFocusEffect(containerRegEmail, etRegEmail);
        applyFocusEffect(containerPhone,    etPhone);
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

    // ── Role selector ──────────────────────────────────────────────────────────
    private void setupRoleSelector() {
        // Default: Customer selected (mirrors XML default visibility)
        setRoleSelected(Constants.ROLE_CUSTOMER);

        btnRoleCustomer.setOnClickListener(v  -> setRoleSelected(Constants.ROLE_CUSTOMER));
        btnRoleOrganizer.setOnClickListener(v -> setRoleSelected(Constants.ROLE_ORGANIZER));
    }

    /**
     * Updates toggle button appearances AND shows / hides the appropriate
     * name-input section for the given role.
     */
    private void setRoleSelected(String role) {
        selectedRole = role;
        boolean isCustomer = Constants.ROLE_CUSTOMER.equals(role);

        // ── Toggle button backgrounds, text colours & icon tints ──
        int activeColor   = requireContext().getColor(R.color.clr_text_white);
        int inactiveColor = requireContext().getColor(R.color.clr_primary_blue);

        btnRoleCustomer.setBackgroundResource(isCustomer
                ? R.drawable.bg_role_active
                : R.drawable.bg_role_inactive);
        txtRoleCustomer.setTextColor(isCustomer ? activeColor : inactiveColor);
        if (icRoleCustomer != null) {
            icRoleCustomer.setImageTintList(
                    ColorStateList.valueOf(isCustomer ? activeColor : inactiveColor));
        }

        btnRoleOrganizer.setBackgroundResource(isCustomer
                ? R.drawable.bg_role_inactive
                : R.drawable.bg_role_active);
        txtRoleOrganizer.setTextColor(isCustomer ? inactiveColor : activeColor);
        if (icRoleOrganizer != null) {
            icRoleOrganizer.setImageTintList(
                    ColorStateList.valueOf(isCustomer ? inactiveColor : activeColor));
        }

        // ── Show / hide name sections ──
        if (sectionUserName != null) {
            sectionUserName.setVisibility(isCustomer ? View.VISIBLE : View.GONE);
        }
        if (sectionOrgName != null) {
            sectionOrgName.setVisibility(isCustomer ? View.GONE : View.VISIBLE);
        }

        // Clear the hidden section's field so stale text doesn't sneak into validation
        if (isCustomer && etOrgName  != null) etOrgName.setText("");
        if (!isCustomer && etFullName != null) etFullName.setText("");

        // Reset error message when role changes
        hideError();
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

        // Terms & Conditions link — show placeholder dialog / Toast
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
        boolean isCustomer = Constants.ROLE_CUSTOMER.equals(selectedRole);

        // Grab the relevant name field
        String name  = isCustomer
                ? etFullName.getText().toString().trim()
                : etOrgName.getText().toString().trim();
        String email          = etRegEmail.getText().toString().trim();
        String phone          = etPhone.getText().toString().trim();
        String password       = etRegPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // ── Non-empty check ──
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty()) {
            showError(getString(R.string.str_error_empty_field));
            return;
        }

        // ── Email format ──
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.str_error_invalid_email));
            return;
        }

        // ── Password length ──
        if (password.length() < 6) {
            showError(getString(R.string.str_error_password_too_short));
            return;
        }

        // ── Password match ──
        if (!password.equals(confirmPassword)) {
            showError(getString(R.string.str_error_password_mismatch));
            return;
        }

        // ── Terms must be accepted ──
        if (cbTerms != null && !cbTerms.isChecked()) {
            showError(getString(R.string.str_error_terms_required));
            return;
        }

        hideError();

        // TODO: write to Firebase Auth + Firestore when backend is ready.
        onRegisterSuccess(name, email, phone, selectedRole, isCustomer);
    }

    private void onRegisterSuccess(String name, String email, String phone,
                                   String role, boolean isCustomer) {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(Constants.PREFS_AUTH, android.content.Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean(Constants.KEY_IS_LOGGED_IN, true)
                .putString(Constants.KEY_USER_EMAIL,    email)
                .putString(Constants.KEY_USER_PHONE,    phone)
                .putString(Constants.KEY_USER_ROLE,     role);

        if (isCustomer) {
            editor.putString(Constants.KEY_USER_NAME,     name);
            editor.remove(Constants.KEY_USER_ORG_NAME);
        } else {
            editor.putString(Constants.KEY_USER_ORG_NAME, name);
            editor.remove(Constants.KEY_USER_NAME);
        }
        editor.apply();

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
