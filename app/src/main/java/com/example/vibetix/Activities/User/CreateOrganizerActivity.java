package com.example.vibetix.Activities.User;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.R;
import com.example.vibetix.databinding.ActivityCreateOrganizerBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class CreateOrganizerActivity extends AppCompatActivity {

    private ActivityCreateOrganizerBinding binding;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String userId;

    private Uri logoUri = null;
    private Uri licenseUri = null;

    private ActivityResultLauncher<String> logoPicker;
    private ActivityResultLauncher<String> licensePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateOrganizerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        setupToolbar();
        setupPickers();

        binding.btnSubmit.setOnClickListener(v -> validateAndSubmit());
        binding.ivLogo.setOnClickListener(v -> logoPicker.launch("image/*"));
        binding.cardLicense.setOnClickListener(v -> licensePicker.launch("image/*"));
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupPickers() {
        logoPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        logoUri = uri;
                        Glide.with(this).load(uri).circleCrop().into(binding.ivLogo);
                    }
                }
        );

        licensePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        licenseUri = uri;
                        Glide.with(this).load(uri).centerCrop().into(binding.ivLicense);
                    }
                }
        );
    }

    private void validateAndSubmit() {
        String brandName = binding.etBrandName.getText().toString().trim();
        String email = binding.etContactEmail.getText().toString().trim();
        String phone = binding.etContactPhone.getText().toString().trim();
        String website = binding.etWebsite.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();

        if (brandName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền các thông tin bắt buộc (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userId == null) {
            Toast.makeText(this, "Lỗi xác thực người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSubmit.setEnabled(false);
        binding.btnSubmit.setText("Đang tải lên...");

        uploadImagesAndSave(brandName, email, phone, website, desc);
    }

    private void uploadImagesAndSave(String brandName, String email, String phone, String website, String desc) {
        String organizerId = UUID.randomUUID().toString();
        Organizer organizer = new Organizer(
                organizerId, userId, brandName, "", desc, website, email, phone
        );
        organizer.setCreatedAt(String.valueOf(System.currentTimeMillis()));
        organizer.setVerified(true);
        organizer.setDefault(binding.cbIsDefault.isChecked());

        Task<Uri> logoTask = (logoUri != null) ? uploadFile(logoUri, "logos") : Tasks.forResult(null);
        Task<Uri> licenseTask = (licenseUri != null) ? uploadFile(licenseUri, "licenses") : Tasks.forResult(null);

        Tasks.whenAllSuccess(logoTask, licenseTask)
                .addOnSuccessListener(results -> {
                    Uri logoUploadUri = (Uri) results.get(0);
                    Uri licenseUploadUri = (Uri) results.get(1);

                    if (logoUploadUri != null) organizer.setLogoUrl(logoUploadUri.toString());
                    if (licenseUploadUri != null) organizer.setBusinessLicenseUrl(licenseUploadUri.toString());

                    saveOrganizerToFirestore(organizer);
                })
                .addOnFailureListener(e -> {
                    binding.btnSubmit.setEnabled(true);
                    binding.btnSubmit.setText("Lưu Hồ Sơ");
                    Toast.makeText(this, "Lỗi upload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private Task<Uri> uploadFile(Uri fileUri, String folder) {
        StorageReference ref = storage.getReference()
                .child("organizers/" + userId + "/" + folder + "/" + UUID.randomUUID() + ".jpg");
        return ref.putFile(fileUri).continueWithTask(task -> ref.getDownloadUrl());
    }

    private void saveOrganizerToFirestore(Organizer organizer) {
        if (organizer.isDefault()) {
            // Need to remove default flag from other organizers of this user
            db.collection(FirebaseCollections.ORGANIZERS)
                    .whereEqualTo("user_id", userId)
                    .whereEqualTo("is_default", true)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            batch.update(doc.getReference(), "is_default", false);
                        }
                        batch.set(db.collection(FirebaseCollections.ORGANIZERS).document(organizer.getOrganizerId()), organizer);
                        batch.commit().addOnCompleteListener(task -> finishSuccess(task));
                    });
        } else {
            db.collection(FirebaseCollections.ORGANIZERS)
                    .document(organizer.getOrganizerId())
                    .set(organizer)
                    .addOnCompleteListener(task -> finishSuccess(task));
        }
    }

    private void finishSuccess(Task<Void> task) {
        if (task.isSuccessful()) {
            Toast.makeText(this, "Lưu hồ sơ Ban tổ chức thành công.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            binding.btnSubmit.setEnabled(true);
            binding.btnSubmit.setText("Lưu Hồ Sơ");
            Toast.makeText(this, "Lỗi lưu dữ liệu: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
