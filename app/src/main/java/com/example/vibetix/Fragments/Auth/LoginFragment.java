package com.example.vibetix.Fragments.Auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Activities.Auth.AuthActivity;
import com.example.vibetix.Activities.User.UserMainActivity;
import com.example.vibetix.Activities.Admin.AdminMainActivity;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.Models.User;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.UserRepository;
import com.example.vibetix.Utils.Constants;
import com.example.vibetix.Utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * LoginFragment — Đăng nhập bằng Firebase Auth.
 *
 * Sau khi xác thực:
 *  1. Fetch User document từ Firestore
 *  2. Fetch danh sách Organizer profiles của user (1:N)
 *  3. Lưu vào SessionManager
 *  4. Điều hướng → UserMainActivity (luôn bắt đầu từ User mode)
 *     (User tự chuyển sang Organizer mode từ Profile)
 *
 * Exception: Admin user → AdminMainActivity
 */
public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private CheckBox cbRememberMe;
    private TextView txtLoginError;
    private ProgressBar pbLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userRepository = new UserRepository();
        sessionManager = new SessionManager(requireContext());

        bindViews(view);
        restoreRememberMe();
        setupClickListeners(view);
        return view;
    }

    private void bindViews(View view) {
        etEmail       = view.findViewById(R.id.etEmail);
        etPassword    = view.findViewById(R.id.etPassword);
        btnLogin      = view.findViewById(R.id.btnLogin);
        cbRememberMe  = view.findViewById(R.id.cbRememberMe);
        txtLoginError = view.findViewById(R.id.txtLoginError);
        pbLogin       = view.findViewById(R.id.pbLogin);
    }

    private void restoreRememberMe() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(Constants.PREFS_AUTH, android.content.Context.MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean(Constants.KEY_REMEMBER_ME, false);
        if (cbRememberMe != null) cbRememberMe.setChecked(rememberMe);
    }

    private void setupClickListeners(View view) {
        if (btnLogin != null) btnLogin.setOnClickListener(v -> attemptLogin());

        // Nút Register
        TextView txtGoRegister = view.findViewById(R.id.txtGoToRegister);
        if (txtGoRegister != null) {
            txtGoRegister.setOnClickListener(v -> {
                if (getActivity() instanceof AuthActivity) {
                    ((AuthActivity) getActivity()).showRegisterFragment();
                }
            });
        }

        // Nút Quên mật khẩu
        TextView txtForgot = view.findViewById(R.id.txtForgotPassword);
        if (txtForgot != null) {
            txtForgot.setOnClickListener(v -> {
                // TODO: Điều hướng sang ForgotPasswordFragment nếu có
            });
        }
    }

    private void attemptLogin() {
        if (etEmail == null || etPassword == null) return;

        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        hideError();
        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        fetchUserData(mAuth.getCurrentUser().getUid());
                    } else {
                        setLoading(false);
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Sai email hoặc mật khẩu.";
                        showError("Đăng nhập thất bại: " + error);
                    }
                });
    }

    /**
     * Bước 1: Lấy thông tin User từ Firestore.
     */
    private void fetchUserData(String uid) {
        userRepository.getUserById(uid)
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            // Set userId nếu Firestore không trả về trong object
                            if (user.getUserId() == null) user.setUserId(uid);
                            sessionManager.createLoginSession(user);
                            fetchOrganizerProfiles(user);
                        } else {
                            setLoading(false);
                            showError("Dữ liệu người dùng không hợp lệ.");
                        }
                    } else {
                        // User đã auth nhưng chưa có document Firestore → tạo mới
                        setLoading(false);
                        showError("Tài khoản chưa có dữ liệu. Vui lòng liên hệ hỗ trợ.");
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Lỗi kết nối: " + e.getMessage());
                });
    }

    /**
     * Bước 2: Lấy danh sách Organizer profiles.
     * Lưu active organizer = default hoặc profile đầu tiên.
     */
    private void fetchOrganizerProfiles(User user) {
        db.collection(FirebaseCollections.ORGANIZERS)
                .whereEqualTo("user_id", user.getUserId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Organizer> organizers = new ArrayList<>();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Organizer org = doc.toObject(Organizer.class);
                            if (org != null) {
                                if (org.getOrganizerId() == null) org.setOrganizerId(doc.getId());
                                organizers.add(org);
                            }
                        }
                    }

                    // Xác định active organizer
                    if (!organizers.isEmpty()) {
                        Organizer activeOrg = findDefaultOrganizer(organizers, user.getDefaultOrganizerId());
                        sessionManager.setActiveOrganizer(
                                activeOrg.getOrganizerId(),
                                activeOrg.getBrandName(),
                                activeOrg.getLogoUrl()
                        );
                        // Mặc định là owner (user sở hữu organizer này)
                        sessionManager.setStaffRole("owner", null);
                    }

                    // Lưu remember me
                    boolean rememberMe = cbRememberMe != null && cbRememberMe.isChecked();
                    SharedPreferences prefs = requireContext()
                            .getSharedPreferences(Constants.PREFS_AUTH, android.content.Context.MODE_PRIVATE);
                    prefs.edit().putBoolean(Constants.KEY_REMEMBER_ME, rememberMe).apply();

                    setLoading(false);
                    navigateToMain(user);
                })
                .addOnFailureListener(e -> {
                    // Vẫn cho vào app dù không load được organizer profiles
                    setLoading(false);
                    navigateToMain(user);
                });
    }

    private Organizer findDefaultOrganizer(List<Organizer> organizers, String defaultId) {
        if (defaultId != null) {
            for (Organizer org : organizers) {
                if (defaultId.equals(org.getOrganizerId())) return org;
            }
        }
        return organizers.get(0); // Fallback: profile đầu tiên
    }

    /**
     * Bước 3: Điều hướng.
     * Admin → AdminMainActivity
     * Tất cả còn lại → UserMainActivity (User tự chuyển sang Organizer mode)
     */
    private void navigateToMain(User user) {
        Intent intent;
        if (Constants.ROLE_ADMIN.equals(user.getRole())) {
            intent = new Intent(requireContext(), AdminMainActivity.class);
        } else {
            intent = new Intent(requireContext(), UserMainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        if (btnLogin != null) btnLogin.setEnabled(!loading);
        if (pbLogin != null) pbLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        if (txtLoginError != null) {
            txtLoginError.setText(message);
            txtLoginError.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {
        if (txtLoginError != null) txtLoginError.setVisibility(View.GONE);
    }
}
