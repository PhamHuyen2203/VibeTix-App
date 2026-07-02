package com.example.vibetix.Fragments.Auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Activities.Auth.AuthActivity;
import com.example.vibetix.Activities.User.UserMainActivity;
import com.example.vibetix.Models.User;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.UserRepository;
import com.example.vibetix.Utils.Constants;
import com.example.vibetix.Utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterFragment extends Fragment {

    // Args cho Google Sign-In mode
    public static final String ARG_GOOGLE_MODE = "google_mode";
    public static final String ARG_PREFILL_EMAIL = "prefill_email";
    public static final String ARG_PREFILL_NAME  = "prefill_name";

    private EditText etFullName, etRegEmail, etPhone, etRegPassword, etConfirmPassword;
    private Button btnRegister;
    private CheckBox cbTerms;
    private TextView txtRegisterError;
    private LinearLayout sectionPassword;

    private boolean isGoogleMode = false;
    private String googleEmail = "";

    private FirebaseAuth mAuth;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    public static RegisterFragment newGoogleInstance(String email, String displayName) {
        RegisterFragment f = new RegisterFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_GOOGLE_MODE, true);
        args.putString(ARG_PREFILL_EMAIL, email != null ? email : "");
        args.putString(ARG_PREFILL_NAME,  displayName != null ? displayName : "");
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);
        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        sessionManager = new SessionManager(requireContext());

        // Đọc args
        if (getArguments() != null) {
            isGoogleMode = getArguments().getBoolean(ARG_GOOGLE_MODE, false);
            googleEmail  = getArguments().getString(ARG_PREFILL_EMAIL, "");
        }

        bindViews(view);
        applyGoogleMode(view);
        setupClickListeners(view);
        return view;
    }

    private void bindViews(View view) {
        etFullName        = view.findViewById(R.id.etFullName);
        etRegEmail        = view.findViewById(R.id.etRegEmail);
        etPhone           = view.findViewById(R.id.etPhone);
        etRegPassword     = view.findViewById(R.id.etRegPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        btnRegister       = view.findViewById(R.id.btnRegister);
        cbTerms           = view.findViewById(R.id.cbTerms);
        txtRegisterError  = view.findViewById(R.id.txtRegisterError);
        sectionPassword   = view.findViewById(R.id.sectionPassword);
    }

    private void applyGoogleMode(View view) {
        if (!isGoogleMode) return;

        // Ẩn phần mật khẩu
        if (sectionPassword != null) sectionPassword.setVisibility(View.GONE);

        // Pre-fill email (không cho sửa)
        if (etRegEmail != null) {
            etRegEmail.setText(googleEmail);
            etRegEmail.setEnabled(false);
            etRegEmail.setAlpha(0.6f);
        }

        // Pre-fill tên nếu có
        String prefillName = getArguments() != null ? getArguments().getString(ARG_PREFILL_NAME, "") : "";
        if (etFullName != null && !prefillName.isEmpty()) {
            etFullName.setText(prefillName);
        }

        if (btnRegister != null) btnRegister.setText("Hoàn tất đăng ký");
    }

    private void setupClickListeners(View view) {
        if (btnRegister != null) btnRegister.setOnClickListener(v -> attemptRegister());

        View btnBack = view.findViewById(R.id.btnRegisterBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (isGoogleMode) {
                    // Đăng xuất Google nếu user bấm back ở màn hoàn tất
                    FirebaseAuth.getInstance().signOut();
                }
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
                if (isGoogleMode) FirebaseAuth.getInstance().signOut();
                if (getActivity() instanceof AuthActivity) {
                    ((AuthActivity) getActivity()).showLoginFragment();
                } else {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
    }

    private void attemptRegister() {
        String name  = etFullName  != null ? etFullName.getText().toString().trim() : "";
        String email = etRegEmail  != null ? etRegEmail.getText().toString().trim() : "";
        String phone = etPhone     != null ? etPhone.getText().toString().trim() : "";

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin.");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Email không đúng định dạng.");
            return;
        }
        if (phone.length() < 10 || phone.length() > 11 || !phone.matches("^[0-9]+$")) {
            showError("Số điện thoại không hợp lệ (10–11 chữ số).");
            return;
        }
        if (cbTerms != null && !cbTerms.isChecked()) {
            showError("Bạn phải đồng ý với điều khoản sử dụng.");
            return;
        }

        if (isGoogleMode) {
            // User đã có Firebase Auth account → chỉ cần cập nhật Firestore
            registerGoogleUser(name, phone);
        } else {
            String password = etRegPassword != null ? etRegPassword.getText().toString().trim() : "";
            String confirm  = etConfirmPassword != null ? etConfirmPassword.getText().toString().trim() : "";
            if (password.length() < 6) { showError("Mật khẩu phải có ít nhất 6 ký tự."); return; }
            if (!password.equals(confirm)) { showError("Mật khẩu xác nhận không khớp."); return; }
            hideError();
            btnRegister.setEnabled(false);
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        saveNewUserToFirestore(mAuth.getCurrentUser().getUid(), name, email, phone);
                    } else {
                        btnRegister.setEnabled(true);
                        showError("Đăng ký thất bại: " + (task.getException() != null
                                ? task.getException().getMessage() : "Lỗi không xác định"));
                    }
                });
        }
    }

    // Đăng ký thường — tạo user mới
    private void saveNewUserToFirestore(String uid, String name, String email, String phone) {
        User user = new User();
        user.setUserId(uid);
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(Constants.ROLE_CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        userRepository.saveUser(user).addOnSuccessListener(v -> {
            sessionManager.createLoginSession(user);
            navigateToMain();
        }).addOnFailureListener(e -> {
            btnRegister.setEnabled(true);
            showError("Lỗi khi lưu thông tin: " + e.getMessage());
        });
    }

    // Google mode — cập nhật document đã tạo sẵn
    private void registerGoogleUser(String name, String phone) {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) { showError("Phiên đăng nhập hết hạn. Vui lòng thử lại."); return; }

        btnRegister.setEnabled(false);
        hideError();

        Map<String, Object> updates = new HashMap<>();
        updates.put("full_name", name);
        updates.put("phone", phone);
        updates.put("role", Constants.ROLE_CUSTOMER);
        updates.put("is_active", true);

        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener(v -> {
                userRepository.getUserById(uid).addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user == null) user = new User();
                    if (user.getUserId() == null) user.setUserId(uid);
                    sessionManager.createLoginSession(user);
                    navigateToMain();
                }).addOnFailureListener(e -> navigateToMain());
            })
            .addOnFailureListener(e -> {
                btnRegister.setEnabled(true);
                showError("Lỗi cập nhật thông tin: " + e.getMessage());
            });
    }

    private void navigateToMain() {
        Intent intent = new Intent(requireContext(), UserMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showError(String msg) {
        if (txtRegisterError != null) { txtRegisterError.setText(msg); txtRegisterError.setVisibility(View.VISIBLE); }
    }

    private void hideError() {
        if (txtRegisterError != null) txtRegisterError.setVisibility(View.GONE);
    }
}
