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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Activities.Admin.AdminMainActivity;
import com.example.vibetix.Activities.Auth.AuthActivity;
import com.example.vibetix.Activities.User.UserMainActivity;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.Models.User;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.UserRepository;
import com.example.vibetix.Utils.Constants;
import com.example.vibetix.Utils.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LoginFragment extends Fragment {

    private static final int RC_GOOGLE_SIGN_IN = 1001;

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private CheckBox cbRememberMe;
    private TextView txtLoginError;
    private ProgressBar pbLogin;
    private LinearLayout btnLoginGoogle, btnLoginApple;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private GoogleSignInClient googleSignInClient;

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

        setupGoogleSignIn();
        bindViews(view);
        restoreRememberMe();
        setupClickListeners(view);
        return view;
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    private void bindViews(View view) {
        etEmail        = view.findViewById(R.id.etEmail);
        etPassword     = view.findViewById(R.id.etPassword);
        btnLogin       = view.findViewById(R.id.btnLogin);
        cbRememberMe   = view.findViewById(R.id.cbRememberMe);
        txtLoginError  = view.findViewById(R.id.txtLoginError);
        pbLogin        = view.findViewById(R.id.pbLogin);
        btnLoginGoogle = view.findViewById(R.id.btnLoginGoogle);
        btnLoginApple  = view.findViewById(R.id.btnLoginApple);
    }

    private void restoreRememberMe() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(Constants.PREFS_AUTH, android.content.Context.MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean(Constants.KEY_REMEMBER_ME, false);
        if (cbRememberMe != null) cbRememberMe.setChecked(rememberMe);
    }

    private void setupClickListeners(View view) {
        if (btnLogin != null) btnLogin.setOnClickListener(v -> attemptLogin());

        if (btnLoginGoogle != null) {
            btnLoginGoogle.setOnClickListener(v -> startGoogleSignIn());
        }

        if (btnLoginApple != null) {
            btnLoginApple.setOnClickListener(v ->
                Toast.makeText(requireContext(), getString(R.string.str_toast_apple_not_supported), Toast.LENGTH_SHORT).show());
        }

        TextView txtGoRegister = view.findViewById(R.id.txtGoToRegister);
        if (txtGoRegister != null) {
            txtGoRegister.setOnClickListener(v -> {
                if (getActivity() instanceof AuthActivity) {
                    ((AuthActivity) getActivity()).showRegisterFragment();
                }
            });
        }

        TextView txtForgot = view.findViewById(R.id.txtForgotPassword);
        if (txtForgot != null) {
            txtForgot.setOnClickListener(v -> {
                // TODO: ForgotPasswordFragment
            });
        }
    }

    // ── Đăng nhập Email/Password ──────────────────────────────────────────────
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

    // ── Google Sign-In ────────────────────────────────────────────────────────
    private void startGoogleSignIn() {
        // Sign out khỏi Google trước để luôn hiện account picker
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                if (e.getStatusCode() != 12501) { // 12501 = user cancelled
                    showError("Đăng nhập Google thất bại: " + e.getMessage());
                }
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        setLoading(true);
        hideError();
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    String uid = mAuth.getCurrentUser().getUid();
                    // Kiểm tra đã có Firestore document chưa, nếu chưa thì tạo mới
                    userRepository.getUserById(uid).addOnSuccessListener(doc -> {
                        String phone = doc.exists() ? doc.getString("phone") : null;
                        if (doc.exists() && phone != null && !phone.isEmpty()) {
                            // User đã có đầy đủ thông tin → vào thẳng app
                            fetchUserData(uid);
                        } else {
                            // User mới hoặc chưa hoàn tất → tạo document tối thiểu rồi redirect sang RegisterFragment
                            java.util.Map<String, Object> base = new java.util.HashMap<>();
                            base.put("user_id", uid);
                            base.put("email", account.getEmail() != null ? account.getEmail() : "");
                            base.put("full_name", account.getDisplayName() != null ? account.getDisplayName() : "");
                            base.put("avatar_url", account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null);
                            base.put("phone", "");
                            base.put("role", Constants.ROLE_CUSTOMER);
                            base.put("is_active", true);
                            base.put("created_at", com.google.firebase.Timestamp.now());
                            db.collection(FirebaseCollections.USERS).document(uid).set(base)
                                .addOnCompleteListener(t -> {
                                    setLoading(false);
                                    if (!isAdded()) return;
                                    if (getActivity() instanceof com.example.vibetix.Activities.Auth.AuthActivity) {
                                        RegisterFragment reg = RegisterFragment.newGoogleInstance(
                                                account.getEmail(), account.getDisplayName());
                                        ((com.example.vibetix.Activities.Auth.AuthActivity) getActivity())
                                                .showFragment(reg);
                                    }
                                });
                        }
                    }).addOnFailureListener(e -> {
                        setLoading(false);
                        showError("Lỗi kết nối: " + e.getMessage());
                    });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Xác thực Google thất bại: " + e.getMessage());
                });
    }

    // ── Fetch user data sau khi auth thành công ────────────────────────────────
    private void fetchUserData(String uid) {
        userRepository.getUserById(uid)
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            if (user.getUserId() == null) user.setUserId(uid);
                            sessionManager.createLoginSession(user);
                            fetchOrganizerProfiles(user);
                        } else {
                            setLoading(false);
                            showError("Dữ liệu người dùng không hợp lệ.");
                        }
                    } else {
                        setLoading(false);
                        showError("Tài khoản chưa có dữ liệu. Vui lòng liên hệ hỗ trợ.");
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Lỗi kết nối: " + e.getMessage());
                });
    }

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

                    if (!organizers.isEmpty()) {
                        Organizer activeOrg = findDefaultOrganizer(organizers, user.getDefaultOrganizerId());
                        sessionManager.setActiveOrganizer(
                                activeOrg.getOrganizerId(),
                                activeOrg.getBrandName(),
                                activeOrg.getLogoUrl()
                        );
                        sessionManager.setStaffRole("owner", null);
                    }

                    boolean rememberMe = cbRememberMe != null && cbRememberMe.isChecked();
                    requireContext()
                            .getSharedPreferences(Constants.PREFS_AUTH, android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean(Constants.KEY_REMEMBER_ME, rememberMe).apply();

                    setLoading(false);
                    navigateToMain(user);
                })
                .addOnFailureListener(e -> {
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
        return organizers.get(0);
    }

    private void navigateToMain(User user) {
        com.google.firebase.auth.FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser != null && fbUser.getEmail() != null) {
            requireContext()
                .getSharedPreferences(Constants.PREFS_AUTH, android.content.Context.MODE_PRIVATE)
                .edit()
                .putString(Constants.KEY_USER_EMAIL, fbUser.getEmail())
                .apply();
        }

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
