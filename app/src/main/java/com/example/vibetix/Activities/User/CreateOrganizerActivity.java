package com.example.vibetix.Activities.User;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.R;
import com.example.vibetix.databinding.ActivityCreateOrganizerBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.UUID;

public class CreateOrganizerActivity extends AppCompatActivity {

    private ActivityCreateOrganizerBinding binding;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateOrganizerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        setupToolbar();
        binding.btnSubmit.setOnClickListener(v -> submitProfile());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void submitProfile() {
        String brandName = binding.etBrandName.getText().toString().trim();
        String email = binding.etContactEmail.getText().toString().trim();
        String phone = binding.etContactPhone.getText().toString().trim();
        String website = binding.etWebsite.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();

        if (brandName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền các thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userId == null) {
            Toast.makeText(this, "Lỗi xác thực người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        String organizerId = UUID.randomUUID().toString();
        Organizer organizer = new Organizer(
                organizerId,
                userId,
                brandName,
                "", // logoUrl
                desc,
                website,
                email,
                phone
        );
        organizer.setCreatedAt(String.valueOf(System.currentTimeMillis()));
        organizer.setVerified(false);

        binding.btnSubmit.setEnabled(false);
        db.collection(FirebaseCollections.ORGANIZERS)
                .document(organizerId)
                .set(organizer)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã gửi hồ sơ đăng ký Ban tổ chức", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
