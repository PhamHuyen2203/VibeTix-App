package com.example.vibetix.Fragments.User;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.vibetix.Activities.Auth.AuthActivity;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.Models.User;
import com.example.vibetix.R;
import com.example.vibetix.Utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * ProfileFragment — Tab "Tôi": Hồ sơ cá nhân, quản lý BTC, settings, đăng xuất.
 */
public class ProfileFragment extends Fragment {

    private ImageView ivAvatar;
    private TextView tvName, tvEmail, tvPhone;
    private View layoutOrgProfiles;
    private TextView tvOrgCount;
    private View btnEditProfile, btnAddOrganizer, btnNotifications;
    private View btnTerms, btnSupport, btnLogout;

    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private FirebaseUser fbUser;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        db = FirebaseFirestore.getInstance();
        fbUser = FirebaseAuth.getInstance().getCurrentUser();

        bindViews(view);
        loadUserProfile();
        setupClickListeners();
    }

    private void bindViews(View v) {
        ivAvatar     = v.findViewById(R.id.ivProfileAvatar);
        tvName       = v.findViewById(R.id.tvProfileName);
        tvEmail      = v.findViewById(R.id.tvProfileEmail);
        tvPhone      = v.findViewById(R.id.tvProfilePhone);
        tvOrgCount   = v.findViewById(R.id.tvOrgProfileCount);
        layoutOrgProfiles = v.findViewById(R.id.layoutOrganizerProfiles);
        btnEditProfile   = v.findViewById(R.id.btnEditProfile);
        btnAddOrganizer  = v.findViewById(R.id.btnAddOrganizer);
        btnNotifications = v.findViewById(R.id.btnNotifications);
        btnTerms    = v.findViewById(R.id.btnTerms);
        btnSupport  = v.findViewById(R.id.btnSupport);
        btnLogout   = v.findViewById(R.id.btnLogout);
    }

    private void loadUserProfile() {
        if (fbUser == null) return;

        // Load từ Firestore
        db.collection(FirebaseCollections.USERS).document(fbUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && getContext() != null) {
                        String name   = doc.getString("full_name");
                        String email  = doc.getString("email");
                        String phone  = doc.getString("phone");
                        String avatar = doc.getString("avatar_url");

                        if (tvName  != null) tvName.setText(name != null ? name : fbUser.getDisplayName());
                        if (tvEmail != null) tvEmail.setText(email != null ? email : fbUser.getEmail());
                        if (tvPhone != null) tvPhone.setText(phone != null ? phone : "Chưa cập nhật");

                        if (ivAvatar != null && avatar != null && !avatar.isEmpty()) {
                            Glide.with(this).load(avatar).circleCrop()
                                    .placeholder(R.drawable.ic_nav_profile).into(ivAvatar);
                        }
                    }
                });

        // Load organizer profiles
        db.collection(FirebaseCollections.ORGANIZERS)
                .whereEqualTo("user_id", fbUser.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    int count = snap != null ? snap.size() : 0;
                    if (tvOrgCount != null) {
                        tvOrgCount.setText(count > 0
                                ? count + " hồ sơ Ban tổ chức"
                                : "Chưa có hồ sơ Ban tổ chức");
                    }
                });
    }

    private void setupClickListeners() {
        if (btnEditProfile != null)
            btnEditProfile.setOnClickListener(v -> Toast.makeText(getContext(), "Chỉnh sửa thông tin", Toast.LENGTH_SHORT).show());

        if (btnAddOrganizer != null)
            btnAddOrganizer.setOnClickListener(v ->
                    startActivity(new Intent(getActivity(), com.example.vibetix.Activities.User.CreateOrganizerActivity.class)));

        if (btnNotifications != null)
            btnNotifications.setOnClickListener(v -> Toast.makeText(getContext(), "Thông báo", Toast.LENGTH_SHORT).show());

        if (btnTerms != null)
            btnTerms.setOnClickListener(v -> Toast.makeText(getContext(), "Điều khoản sử dụng", Toast.LENGTH_SHORT).show());

        if (btnSupport != null)
            btnSupport.setOnClickListener(v -> Toast.makeText(getContext(), "Liên hệ hỗ trợ", Toast.LENGTH_SHORT).show());

        if (btnLogout != null)
            btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        sessionManager.logoutUser();
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }
}
