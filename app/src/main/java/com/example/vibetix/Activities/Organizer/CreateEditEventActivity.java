package com.example.vibetix.Activities.Organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.EventRepository;
import com.example.vibetix.Repositories.OrganizerRepository;
import com.example.vibetix.Utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * CreateEditEventActivity — Tạo hoặc sửa Event.
 *
 * Truyền vào:
 *   - Không có extra → Tạo mới
 *   - extra "event_id" → Sửa event đó
 *
 * Flow:
 *   1. Load danh sách organizer profiles của user (để chọn BTC)
 *   2. Pre-fill từ default organizer (nếu tạo mới) hoặc từ event (nếu sửa)
 *   3. Upload poster lên Firebase Storage
 *   4. Lưu event vào Firestore
 */
public class CreateEditEventActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";

    // Form fields
    private TextInputEditText etTitle, etDescription, etStartTime, etEndTime;
    private TextInputEditText etVenueName, etVenueAddress, etVenueCity;
    private AutoCompleteTextView ddCategory, ddOrganizer;
    private ShapeableImageView ivEventPoster;
    private MaterialButton btnSaveDraft, btnSubmitForApproval;

    // Data
    private SessionManager sessionManager;
    private EventRepository eventRepository;
    private OrganizerRepository organizerRepository;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private String existingEventId = null;
    private Event editingEvent = null;
    private Uri selectedPosterUri = null;
    private List<Organizer> myOrganizers = new ArrayList<>();
    private Organizer selectedOrganizer = null;
    private List<com.example.vibetix.Models.Category> categoryList = new ArrayList<>();
    private com.example.vibetix.Models.Category selectedCategory = null;

    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();
    private final SimpleDateFormat dtFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    // Image picker
    private ActivityResultLauncher<String> imagePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_edit_event);

        sessionManager = new SessionManager(this);
        eventRepository = new EventRepository();
        organizerRepository = new OrganizerRepository();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        bindViews();
        setupToolbar();
        setupImagePicker();
        setupDateTimePickers();

        // Check xem đang tạo mới hay sửa
        existingEventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (existingEventId != null) {
            loadExistingEvent(existingEventId);
        } else {
            loadOrganizerProfiles(); // Load để pre-fill
            loadCategories();
        }

        setupButtons();
    }

    private void bindViews() {
        etTitle        = findViewById(R.id.etEventTitle);
        etDescription  = findViewById(R.id.etEventDescription);
        etStartTime    = findViewById(R.id.etStartTime);
        etEndTime      = findViewById(R.id.etEndTime);
        etVenueName    = findViewById(R.id.etVenueName);
        etVenueAddress = findViewById(R.id.etVenueAddress);
        etVenueCity    = findViewById(R.id.etVenueCity);
        ddCategory     = findViewById(R.id.ddCategory);
        ddOrganizer    = findViewById(R.id.ddOrganizer);
        ivEventPoster  = findViewById(R.id.ivEventPoster);
        btnSaveDraft   = findViewById(R.id.btnSaveDraft);
        btnSubmitForApproval = findViewById(R.id.btnSubmitForApproval);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarCreateEvent);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(existingEventId != null ? "Chỉnh sửa sự kiện" : "Tạo sự kiện mới");
        }
    }

    private void setupImagePicker() {
        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPosterUri = uri;
                        Glide.with(this).load(uri).into(ivEventPoster);
                    }
                });
        if (ivEventPoster != null) {
            ivEventPoster.setOnClickListener(v -> imagePicker.launch("image/*"));
        }
    }

    private void setupDateTimePickers() {
        if (etStartTime != null) {
            etStartTime.setOnClickListener(v -> showDateTimePicker(startCalendar, etStartTime));
            etStartTime.setFocusable(false);
        }
        if (etEndTime != null) {
            etEndTime.setOnClickListener(v -> showDateTimePicker(endCalendar, etEndTime));
            etEndTime.setFocusable(false);
        }
    }

    private void showDateTimePicker(Calendar cal, TextInputEditText target) {
        new DatePickerDialog(this, (view, y, m, d) -> {
            cal.set(Calendar.YEAR, y);
            cal.set(Calendar.MONTH, m);
            cal.set(Calendar.DAY_OF_MONTH, d);
            new TimePickerDialog(this, (v2, h, min) -> {
                cal.set(Calendar.HOUR_OF_DAY, h);
                cal.set(Calendar.MINUTE, min);
                target.setText(dtFormat.format(cal.getTime()));
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Load danh sách organizer profiles của user để hiện trong dropdown.
     */
    private void loadOrganizerProfiles() {
        String userId = sessionManager.getUserDetails() != null
                ? sessionManager.getUserDetails().getUserId() : null;
        if (userId == null) return;

        organizerRepository.getOrganizersByUserId(userId)
                .addOnSuccessListener(snapshot -> {
                    myOrganizers.clear();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Organizer org = doc.toObject(Organizer.class);
                            if (org != null) {
                                if (org.getOrganizerId() == null) org.setOrganizerId(doc.getId());
                                myOrganizers.add(org);
                            }
                        }
                    }
                    setupOrganizerDropdown();
                    // Pre-select default organizer
                    String defaultId = sessionManager.getActiveOrganizerId();
                    for (Organizer org : myOrganizers) {
                        if (org.getOrganizerId().equals(defaultId)) {
                            selectedOrganizer = org;
                            if (ddOrganizer != null) ddOrganizer.setText(org.getBrandName(), false);
                            break;
                        }
                    }
                    if (selectedOrganizer == null && !myOrganizers.isEmpty()) {
                        selectedOrganizer = myOrganizers.get(0);
                        if (ddOrganizer != null) ddOrganizer.setText(selectedOrganizer.getBrandName(), false);
                    }
                });
    }

    private void loadCategories() {
        db.collection(FirebaseCollections.CATEGORIES).get()
                .addOnSuccessListener(snapshot -> {
                    categoryList.clear();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            com.example.vibetix.Models.Category cat = doc.toObject(com.example.vibetix.Models.Category.class);
                            if (cat.getCategoryId() == null) cat.setCategoryId(doc.getId());
                            categoryList.add(cat);
                        }
                    }
                    setupCategoryDropdown();
                });
    }

    private void setupCategoryDropdown() {
        if (ddCategory == null || categoryList.isEmpty()) return;
        List<String> names = new ArrayList<>();
        for (com.example.vibetix.Models.Category cat : categoryList) names.add(cat.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        ddCategory.setAdapter(adapter);
        ddCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = categoryList.get(position);
        });
    }

    private void setupOrganizerDropdown() {
        if (ddOrganizer == null) return;
        
        if (myOrganizers.isEmpty()) {
            // Hiển thị thông báo yêu cầu tạo hồ sơ Ban tổ chức
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chưa có hồ sơ Ban tổ chức")
                .setMessage("Bạn cần tạo ít nhất một hồ sơ Ban tổ chức trước khi có thể tạo sự kiện. Bạn có muốn tạo ngay bây giờ không?")
                .setPositiveButton("Tạo ngay", (dialog, which) -> {
                    startActivity(new android.content.Intent(this, com.example.vibetix.Activities.User.CreateOrganizerActivity.class));
                    finish();
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
            return;
        }
        
        List<String> names = new ArrayList<>();
        for (Organizer org : myOrganizers) names.add(org.getBrandName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        ddOrganizer.setAdapter(adapter);
        ddOrganizer.setOnItemClickListener((parent, view, position, id) -> {
            selectedOrganizer = myOrganizers.get(position);
        });
    }

    private void setupButtons() {
        if (btnSaveDraft != null) {
            btnSaveDraft.setOnClickListener(v -> saveEvent("draft"));
        }
        if (btnSubmitForApproval != null) {
            btnSubmitForApproval.setOnClickListener(v -> saveEvent("pending"));
        }
    }

    private void loadExistingEvent(String eventId) {
        db.collection(FirebaseCollections.EVENTS).document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        editingEvent = doc.toObject(Event.class);
                        loadOrganizerProfiles();
                        loadCategories();
                        if (editingEvent != null) populateForm(editingEvent);
                    }
                });
    }

    private void populateForm(Event event) {
        if (etTitle != null) etTitle.setText(event.getTitle());
        if (etDescription != null) etDescription.setText(event.getDescription());
        if (etVenueName != null) etVenueName.setText(event.getVenueName());
        if (etVenueAddress != null) etVenueAddress.setText(event.getVenueAddress());
        if (etVenueCity != null) etVenueCity.setText(event.getVenueCity());
        if (etStartTime != null && event.getStartTime() != null) etStartTime.setText(event.getStartTime());
        if (etEndTime != null && event.getEndTime() != null) etEndTime.setText(event.getEndTime());
        
        // Populate category dropdown logic would go here
        
        if (ivEventPoster != null && event.getPosterUrl() != null) {
            Glide.with(this).load(event.getPosterUrl()).into(ivEventPoster);
        }
    }

    private void saveEvent(String status) {
        if (!validateForm()) return;

        String title       = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String venueName   = etVenueName.getText() != null ? etVenueName.getText().toString().trim() : "";
        String venueAddr   = etVenueAddress.getText() != null ? etVenueAddress.getText().toString().trim() : "";
        String venueCity   = etVenueCity.getText() != null ? etVenueCity.getText().toString().trim() : "";
        String startTime   = etStartTime.getText() != null ? etStartTime.getText().toString() : "";
        String endTime     = etEndTime.getText() != null ? etEndTime.getText().toString() : "";

        if (selectedPosterUri != null) {
            uploadPosterThenSave(title, description, venueName, venueAddr, venueCity, startTime, endTime, status);
        } else {
            String existingPoster = editingEvent != null ? editingEvent.getPosterUrl() : null;
            buildAndSaveEvent(title, description, venueName, venueAddr, venueCity, startTime, endTime, status, existingPoster);
        }
    }

    private void uploadPosterThenSave(String title, String desc, String venueName,
                                       String venueAddr, String venueCity,
                                       String startTime, String endTime, String status) {
        btnSaveDraft.setEnabled(false);
        btnSubmitForApproval.setEnabled(false);

        String userId = sessionManager.getUserDetails().getUserId();
        StorageReference ref = storage.getReference()
                .child("event_posters/" + userId + "/" + UUID.randomUUID() + ".jpg");

        ref.putFile(selectedPosterUri)
                .addOnSuccessListener(snap -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            buildAndSaveEvent(title, desc, venueName, venueAddr, venueCity,
                                    startTime, endTime, status, uri.toString());
                        }))
                .addOnFailureListener(e -> {
                    btnSaveDraft.setEnabled(true);
                    btnSubmitForApproval.setEnabled(true);
                    Toast.makeText(this, "Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void buildAndSaveEvent(String title, String desc, String venueName,
                                   String venueAddr, String venueCity,
                                   String startTime, String endTime, String status, String posterUrl) {
        String eventId = existingEventId != null ? existingEventId : UUID.randomUUID().toString();
        String organizerId = selectedOrganizer != null ? selectedOrganizer.getOrganizerId() :
                sessionManager.getActiveOrganizerId();
        String userId = sessionManager.getUserDetails().getUserId();
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        Event event = new Event();
        event.setEventId(eventId);
        event.setOrganizerId(organizerId);
        event.setUserId(userId);
        event.setTitle(title);
        event.setDescription(desc);
        event.setVenueName(venueName);
        event.setVenueAddress(venueAddr);
        event.setVenueCity(venueCity);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setStatusStr(status);
        event.setPosterUrl(posterUrl);
        if (selectedCategory != null) {
            event.setCategoryId(selectedCategory.getCategoryId());
        } else if (editingEvent != null) {
            event.setCategoryId(editingEvent.getCategoryId());
        }
        if (existingEventId == null) event.setCreatedAt(now);

        eventRepository.createEvent(event)
                .addOnSuccessListener(v -> {
                    String msg = "draft".equals(status) ? "Đã lưu nháp!" : "Đã gửi duyệt!";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveDraft.setEnabled(true);
                    btnSubmitForApproval.setEnabled(true);
                });
    }

    private boolean validateForm() {
        if (etTitle == null || etTitle.getText() == null || etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề sự kiện", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedOrganizer == null) {
            Toast.makeText(this, "Vui lòng chọn Ban tổ chức", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
