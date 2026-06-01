package com.example.vibetix.Fragments.Auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Activities.Auth.AuthActivity;
import com.example.vibetix.R;
import com.example.vibetix.Models.User;
import com.example.vibetix.Repositories.UserRepository;
import com.example.vibetix.Utils.Constants;
import com.example.vibetix.Utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegisterFragment extends Fragment {

    private EditText etFullName, etOrgName, etRegEmail, etPhone, etRegPassword, etConfirmPassword;
    private View btnRegister;
    private CheckBox cbTerms;
    private TextView txtRegisterError;
    private View sectionUserName, sectionOrgName;
    private View btnRoleCustomer, btnRoleOrganizer;

    private String selectedRole = Constants.ROLE_CUSTOMER;
    private FirebaseAuth mAuth;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);
        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        sessionManager = new SessionManager(requireContext());

        bindViews(view);
        setupRoleSelector();
        setupClickListeners(view);
        return view;
    }

    private void bindViews(View view) {
        etFullName = view.findViewById(R.id.etFullName);
        etOrgName = view.findViewById(R.id.etOrgName);
        etRegEmail = view.findViewById(R.id.etRegEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etRegPassword = view.findViewById(R.id.etRegPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        btnRegister = view.findViewById(R.id.btnRegister);
        cbTerms = view.findViewById(R.id.cbTerms);
        txtRegisterError = view.findViewById(R.id.txtRegisterError);
        sectionUserName = view.findViewById(R.id.sectionUserName);
        sectionOrgName = view.findViewById(R.id.sectionOrgName);
        btnRoleCustomer = view.findViewById(R.id.btnRoleCustomer);
        btnRoleOrganizer = view.findViewById(R.id.btnRoleOrganizer);
    }

    private void setupRoleSelector() {
        if (btnRoleCustomer != null && btnRoleOrganizer != null) {
            btnRoleCustomer.setOnClickListener(v -> selectRole(Constants.ROLE_CUSTOMER));
            btnRoleOrganizer.setOnClickListener(v -> selectRole(Constants.ROLE_ORGANIZER));
        }
    }

    private void selectRole(String role) {
        selectedRole = role;
        if (Constants.ROLE_CUSTOMER.equals(role)) {
            if (sectionUserName != null) sectionUserName.setVisibility(View.VISIBLE);
            if (sectionOrgName != null) sectionOrgName.setVisibility(View.GONE);
        } else {
            if (sectionUserName != null) sectionUserName.setVisibility(View.GONE);
            if (sectionOrgName != null) sectionOrgName.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners(View view) {
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> attemptRegister());
        }

        View btnBack = view.findViewById(R.id.btnRegisterBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() instanceof AuthActivity) {
                    ((AuthActivity) getActivity()).showLoginFragment();
                } else {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }

        TextView txtGoToLogin = view.findViewById(R.id.txtGoToLogin);
        if (txtGoToLogin != null) {
            txtGoToLogin.setOnClickListener(v -> {
                if (getActivity() instanceof AuthActivity) {
                    ((AuthActivity) getActivity()).showLoginFragment();
                } else {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
    }

    private void attemptRegister() {
        boolean isCustomer = Constants.ROLE_CUSTOMER.equals(selectedRole);

        String name = "";
        if (isCustomer && etFullName != null) {
            name = etFullName.getText().toString().trim();
        } else if (!isCustomer && etOrgName != null) {
            name = etOrgName.getText().toString().trim();
        }

        String email = etRegEmail != null ? etRegEmail.getText().toString().trim() : "";
        String phone = etPhone != null ? etPhone.getText().toString().trim() : "";
        String password = etRegPassword != null ? etRegPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword != null ? etConfirmPassword.getText().toString().trim() : "";

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp.");
            return;
        }

        if (cbTerms != null && !cbTerms.isChecked()) {
            showError("Bạn phải đồng ý với điều khoản sử dụng.");
            return;
        }

        hideError();
        btnRegister.setEnabled(false);

        final String finalName = name;
        final String finalEmail = email;
        final String finalPhone = phone;

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                    saveUserToFirestore(mAuth.getCurrentUser().getUid(), finalName, finalEmail, finalPhone, selectedRole);
                } else {
                    btnRegister.setEnabled(true);
                    showError("Đăng ký thất bại: " + (task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định"));
                }
            });
    }

    private void saveUserToFirestore(String userId, String name, String email, String phone, String role) {
        User user = new User();
        user.setUserId(userId);
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        userRepository.saveUser(user).addOnSuccessListener(aVoid -> {
            onRegisterSuccess(user);
        }).addOnFailureListener(e -> {
            btnRegister.setEnabled(true);
            showError("Lỗi khi lưu thông tin người dùng.");
        });
    }

    private void onRegisterSuccess(User user) {
        sessionManager.createLoginSession(user);
        navigateToMain(user.getRole());
    }

    private void navigateToMain(String role) {
        Intent intent;
        if (Constants.ROLE_ADMIN.equals(role)) {
            intent = new Intent(requireContext(), com.example.vibetix.Activities.Admin.AdminMainActivity.class);
        } else if (Constants.ROLE_ORGANIZER.equals(role)) {
            intent = new Intent(requireContext(), com.example.vibetix.Activities.Organizer.OrganizerMainActivity.class);
        } else {
            intent = new Intent(requireContext(), com.example.vibetix.Activities.User.UserMainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showError(String message) {
        if (txtRegisterError != null) {
            txtRegisterError.setText(message);
            txtRegisterError.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {
        if (txtRegisterError != null) {
            txtRegisterError.setVisibility(View.GONE);
        }
    }
}
