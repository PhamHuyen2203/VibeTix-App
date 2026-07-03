package com.example.vibetix.Activities.User;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.R;
import com.example.vibetix.databinding.ActivityCreateOrganizerBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateOrganizerActivity extends AppCompatActivity {

    public static final String EXTRA_ORGANIZER_JSON = "extra_organizer_json";

    private ActivityCreateOrganizerBinding binding;
    private FirebaseFirestore db;
    private String userId;

    private Uri logoUri = null;
    private byte[] logoBytes = null;
    private boolean editMode = false;
    private Organizer editOrganizer = null;
    private String existingLogoUrl = null;

    private ActivityResultLauncher<String> logoPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateOrganizerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        String orgJson = getIntent().getStringExtra(EXTRA_ORGANIZER_JSON);
        if (orgJson != null) {
            editOrganizer = new Gson().fromJson(orgJson, Organizer.class);
            editMode = true;
        }

        setupToolbar();
        setupLogoPicker();
        if (editMode) prefillFields();

        binding.btnSubmit.setOnClickListener(v -> validateAndSubmit());
        binding.ivLogo.setOnClickListener(v -> logoPicker.launch("image/*"));
        binding.btnRemoveLogo.setOnClickListener(v -> {
            logoUri = null;
            logoBytes = null;
            existingLogoUrl = null;
            binding.ivLogo.setImageResource(R.drawable.ic_organizer_placeholder);
            binding.btnRemoveLogo.setVisibility(View.GONE);
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(editMode ? "Chỉnh sửa hồ sơ BTC" : "Hồ sơ Ban tổ chức");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        if (editMode) binding.btnSubmit.setText("Lưu thay đổi");
    }

    private void prefillFields() {
        Organizer org = editOrganizer;
        binding.etBrandName.setText(org.getBrandName());
        binding.etWebsite.setText(org.getWebsiteUrl());
        binding.etDescription.setText(org.getDescription());
        binding.cbIsDefault.setChecked(org.isDefault());

        existingLogoUrl = org.getLogoUrl();
        if (existingLogoUrl != null && !existingLogoUrl.isEmpty()) {
            com.example.vibetix.Utils.ImageUtils.loadCircle(this, existingLogoUrl, binding.ivLogo,
                    R.drawable.ic_organizer_placeholder);
            binding.btnRemoveLogo.setVisibility(View.VISIBLE);
        }
    }

    private void setupLogoPicker() {
        logoPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        logoUri = uri;
                        // Compress ngay khi chọn: max 400×400, max 150 KB
                        logoBytes = com.example.vibetix.Utils.ImageUtils.compressToJpeg(this, uri, 400, 150);
                        // Preview từ bytes đã compress (hoặc URI nếu compress thất bại)
                        if (logoBytes != null) {
                            Glide.with(this).load(logoBytes).circleCrop().into(binding.ivLogo);
                        } else {
                            Glide.with(this).load(uri).circleCrop().into(binding.ivLogo);
                        }
                        binding.btnRemoveLogo.setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    private void validateAndSubmit() {
        String brandName = binding.etBrandName.getText().toString().trim();
        String website = binding.etWebsite.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();

        if (brandName.isEmpty()) {
            binding.etBrandName.setError("Tên thương hiệu không được trống");
            binding.etBrandName.requestFocus();
            return;
        }

        if (userId == null) {
            Toast.makeText(this, "Lỗi xác thực người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSubmit.setEnabled(false);
        binding.btnSubmit.setText("Đang lưu...");

        if (editMode) {
            uploadLogoAndEdit(brandName, website, desc);
        } else {
            uploadLogoAndSave(brandName, website, desc);
        }
    }

    private void uploadLogoAndEdit(String brandName, String website, String desc) {
        // Logo: dùng Base64 mới nếu có, không thì giữ URL cũ
        String finalLogoUrl = (logoBytes != null && logoBytes.length > 0)
                ? com.example.vibetix.Utils.ImageUtils.toBase64(logoBytes)
                : (existingLogoUrl != null ? existingLogoUrl : "");

        Map<String, Object> data = new HashMap<>();
        data.put("brand_name", brandName);
        data.put("website_url", website);
        data.put("description", desc);
        data.put("logo_url", finalLogoUrl);
        data.put("is_default", binding.cbIsDefault.isChecked());

        db.collection(FirebaseCollections.ORGANIZERS)
                .document(editOrganizer.getOrganizerId())
                .update(data)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Đã cập nhật hồ sơ BTC thành công.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnSubmit.setEnabled(true);
                    binding.btnSubmit.setText("Lưu thay đổi");
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadLogoAndSave(String brandName, String website, String desc) {
        String logoBase64 = (logoBytes != null && logoBytes.length > 0)
                ? com.example.vibetix.Utils.ImageUtils.toBase64(logoBytes) : "";

        String organizerId = UUID.randomUUID().toString();
        Map<String, Object> data = new HashMap<>();
        data.put("organizer_id", organizerId);
        data.put("user_id", userId);
        data.put("brand_name", brandName);
        data.put("website_url", website);
        data.put("description", desc);
        data.put("logo_url", logoBase64);
        data.put("is_default", binding.cbIsDefault.isChecked());
        data.put("is_verified", false);
        data.put("created_at", com.google.firebase.firestore.FieldValue.serverTimestamp());

        saveToFirestore(organizerId, data, binding.cbIsDefault.isChecked());
    }

    private void saveToFirestore(String organizerId, Map<String, Object> data, boolean isDefault) {
        if (isDefault) {
            db.collection(FirebaseCollections.ORGANIZERS)
                    .whereEqualTo("user_id", userId)
                    .whereEqualTo("is_default", true)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            batch.update(doc.getReference(), "is_default", false);
                        }
                        batch.set(db.collection(FirebaseCollections.ORGANIZERS).document(organizerId), data);
                        batch.commit().addOnCompleteListener(task -> finishSuccess(task.isSuccessful(), task.getException()));
                    });
        } else {
            db.collection(FirebaseCollections.ORGANIZERS)
                    .document(organizerId)
                    .set(data)
                    .addOnCompleteListener(task -> finishSuccess(task.isSuccessful(), task.getException()));
        }
    }

    private void finishSuccess(boolean success, Exception e) {
        if (success) {
            Toast.makeText(this, "Lưu hồ sơ Ban tổ chức thành công.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            binding.btnSubmit.setEnabled(true);
            binding.btnSubmit.setText("Lưu Hồ Sơ");
            Toast.makeText(this, "Lỗi lưu dữ liệu: " + (e != null ? e.getMessage() : ""), Toast.LENGTH_SHORT).show();
        }
    }
}
