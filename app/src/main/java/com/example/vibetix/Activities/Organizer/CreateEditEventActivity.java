package com.example.vibetix.Activities.Organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;
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
import com.google.android.material.progressindicator.LinearProgressIndicator;
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
    private MaterialButton btnNextStep, btnPreviousStep;
    private android.widget.ViewFlipper viewFlipper;
    private android.widget.TextView tvStepIndicator;
    private LinearProgressIndicator stepProgressIndicator;
    private int currentStep = 0;

    // Step 3 preview views and Organizer Mode views
    private android.widget.TextView tvPreviewTitle, tvPreviewTime, tvPreviewVenue;
    private RadioGroup rgOrganizerMode;
    private android.widget.LinearLayout layoutSavedOrganizer, layoutNewOrganizer;
    private com.google.android.material.card.MaterialCardView cardSelectedOrgInfo;
    private android.widget.TextView tvSelectedOrgEmail, tvSelectedOrgPhone, tvSelectedOrgWebsite;
    private TextInputEditText etNewOrgName, etNewOrgEmail, etNewOrgPhone, etNewOrgWebsite;

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

        // Check xem đang tạo mới hay sửa — phải đọc TRƯỚC setupToolbar()
        existingEventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        bindViews();
        setupToolbar();
        setupImagePicker();
        setupDateTimePickers();

        if (existingEventId != null) {
            loadExistingEvent(existingEventId);
        } else {
            loadOrganizerProfiles(); // Load để pre-fill
            loadCategories();
        }

        setupButtons();
        setupStepper();
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
        btnNextStep = findViewById(R.id.btnNextStep);
        btnPreviousStep = findViewById(R.id.btnPreviousStep);
        viewFlipper = findViewById(R.id.viewFlipper);
        tvStepIndicator = findViewById(R.id.tvStepIndicator);
        stepProgressIndicator = findViewById(R.id.stepProgressIndicator);

        // Views Step 3 preview and Organizer setup
        tvPreviewTitle        = findViewById(R.id.tvPreviewTitle);
        tvPreviewTime         = findViewById(R.id.tvPreviewTime);
        tvPreviewVenue        = findViewById(R.id.tvPreviewVenue);

        rgOrganizerMode = findViewById(R.id.rgOrganizerMode);
        layoutSavedOrganizer = findViewById(R.id.layoutSavedOrganizer);
        layoutNewOrganizer = findViewById(R.id.layoutNewOrganizer);
        cardSelectedOrgInfo = findViewById(R.id.cardSelectedOrgInfo);
        tvSelectedOrgEmail = findViewById(R.id.tvSelectedOrgEmail);
        tvSelectedOrgPhone = findViewById(R.id.tvSelectedOrgPhone);
        tvSelectedOrgWebsite = findViewById(R.id.tvSelectedOrgWebsite);

        etNewOrgName = findViewById(R.id.etNewOrgName);
        etNewOrgEmail = findViewById(R.id.etNewOrgEmail);
        etNewOrgPhone = findViewById(R.id.etNewOrgPhone);
        etNewOrgWebsite = findViewById(R.id.etNewOrgWebsite);

        if (rgOrganizerMode != null) {
            rgOrganizerMode.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbSavedOrganizer) {
                    if (layoutSavedOrganizer != null) layoutSavedOrganizer.setVisibility(android.view.View.VISIBLE);
                    if (layoutNewOrganizer != null) layoutNewOrganizer.setVisibility(android.view.View.GONE);
                } else if (checkedId == R.id.rbNewOrganizer) {
                    if (layoutSavedOrganizer != null) layoutSavedOrganizer.setVisibility(android.view.View.GONE);
                    if (layoutNewOrganizer != null) layoutNewOrganizer.setVisibility(android.view.View.VISIBLE);
                }
            });
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarCreateEvent);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(existingEventId != null ? "Chỉnh sửa sự kiện" : "Tạo sự kiện mới");
        }
    }

    private void setupStepper() {
        if (btnNextStep == null) return;
        updateStepperUI();
        btnNextStep.setOnClickListener(v -> {
            if (currentStep < 2) {
                if (!validateCurrentStep()) return;
                currentStep++;
                viewFlipper.setDisplayedChild(currentStep);
                updateStepperUI();
                // Khi vào Step 3: cập nhật preview card
                if (currentStep == 2) updatePreviewCard();
            }
        });
        btnPreviousStep.setOnClickListener(v -> {
            if (currentStep > 0) {
                currentStep--;
                viewFlipper.setDisplayedChild(currentStep);
                updateStepperUI();
            }
        });
    }

    private void updateStepperUI() {
        if (currentStep == 0) {
            tvStepIndicator.setText("Bước 1/3: Thông tin cơ bản");
            if (stepProgressIndicator != null) stepProgressIndicator.setProgress(33);
            btnPreviousStep.setVisibility(android.view.View.GONE);
            btnNextStep.setVisibility(android.view.View.VISIBLE);
        } else if (currentStep == 1) {
            tvStepIndicator.setText("Bước 2/3: Lịch trình & Địa điểm");
            if (stepProgressIndicator != null) stepProgressIndicator.setProgress(66);
            btnPreviousStep.setVisibility(android.view.View.VISIBLE);
            btnNextStep.setVisibility(android.view.View.VISIBLE);
        } else if (currentStep == 2) {
            tvStepIndicator.setText("Bước 3/3: Ban tổ chức");
            if (stepProgressIndicator != null) stepProgressIndicator.setProgress(100);
            btnPreviousStep.setVisibility(android.view.View.VISIBLE);
            btnNextStep.setVisibility(android.view.View.GONE); // Use submit buttons instead
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
        String userId = sessionManager.getUserDetails() != null ? sessionManager.getUserDetails().getUserId() : null;
        if (userId == null) {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            userId = user != null ? user.getUid() : null;
        }
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
                    if (existingEventId == null) {
                        // Pre-select default organizer if creating new
                        String defaultId = sessionManager.getActiveOrganizerId();
                        for (Organizer org : myOrganizers) {
                            if (org.getOrganizerId().equals(defaultId)) {
                                selectedOrganizer = org;
                                if (ddOrganizer != null) ddOrganizer.setText(org.getBrandName(), false);
                                break;
                            }
                        }
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
        
        List<String> names = new ArrayList<>();
        for (Organizer org : myOrganizers) names.add(org.getBrandName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        ddOrganizer.setAdapter(adapter);
        ddOrganizer.setOnItemClickListener((parent, view, position, id) -> {
            selectedOrganizer = myOrganizers.get(position);
            if (cardSelectedOrgInfo != null) {
                cardSelectedOrgInfo.setVisibility(android.view.View.VISIBLE);
                if (tvSelectedOrgEmail != null) tvSelectedOrgEmail.setText("Email: " + (selectedOrganizer.getContactEmail() != null ? selectedOrganizer.getContactEmail() : "—"));
                if (tvSelectedOrgPhone != null) tvSelectedOrgPhone.setText("SĐT: " + (selectedOrganizer.getContactPhone() != null ? selectedOrganizer.getContactPhone() : "—"));
                if (tvSelectedOrgWebsite != null) tvSelectedOrgWebsite.setText("Web: " + (selectedOrganizer.getWebsiteUrl() != null && !selectedOrganizer.getWebsiteUrl().isEmpty() ? selectedOrganizer.getWebsiteUrl() : "—"));
            }
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

    /** Cập nhật preview card ở Step 3 với dữ liệu từ Step 1 & 2. */
    private void updatePreviewCard() {
        if (tvPreviewTitle != null && etTitle != null && etTitle.getText() != null) {
            String t = etTitle.getText().toString().trim();
            tvPreviewTitle.setText(t.isEmpty() ? "—" : t);
        }
        if (tvPreviewTime != null) {
            String start = (etStartTime != null && etStartTime.getText() != null)
                    ? etStartTime.getText().toString().trim() : "";
            String end   = (etEndTime != null && etEndTime.getText() != null)
                    ? etEndTime.getText().toString().trim() : "";
            String time = start.isEmpty() ? "—" : (end.isEmpty() ? start : start + " → " + end);
            tvPreviewTime.setText(time);
        }
        if (tvPreviewVenue != null) {
            String name = (etVenueName != null && etVenueName.getText() != null)
                    ? etVenueName.getText().toString().trim() : "";
            String city = (etVenueCity != null && etVenueCity.getText() != null)
                    ? etVenueCity.getText().toString().trim() : "";
            String venue = name.isEmpty() && city.isEmpty() ? "—"
                    : (city.isEmpty() ? name : (name.isEmpty() ? city : name + ", " + city));
            tvPreviewVenue.setText(venue);
        }
    }

    private void loadExistingEvent(String eventId) {
        db.collection(FirebaseCollections.EVENTS).document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        editingEvent = doc.toObject(Event.class);
                        loadCategories();
                        
                        String userId = sessionManager.getUserDetails() != null ? sessionManager.getUserDetails().getUserId() : null;
                        if (userId != null) {
                            organizerRepository.getOrganizersByUserId(userId)
                                    .addOnSuccessListener(snapshot -> {
                                        myOrganizers.clear();
                                        if (snapshot != null) {
                                            for (QueryDocumentSnapshot orgDoc : snapshot) {
                                                Organizer org = orgDoc.toObject(Organizer.class);
                                                if (org != null) {
                                                    if (org.getOrganizerId() == null) org.setOrganizerId(orgDoc.getId());
                                                    myOrganizers.add(org);
                                                }
                                            }
                                        }
                                        setupOrganizerDropdown();
                                        if (editingEvent != null) populateForm(editingEvent);
                                    });
                        } else {
                            if (editingEvent != null) populateForm(editingEvent);
                        }
                    }
                });
    }

    private void populateForm(Event event) {
        if (etTitle != null) etTitle.setText(event.getTitle());
        if (etDescription != null) etDescription.setText(event.getDescription());
        
        if (event.getOrganizerId() != null) {
            for (Organizer org : myOrganizers) {
                if (org.getOrganizerId().equals(event.getOrganizerId())) {
                    selectedOrganizer = org;
                    if (ddOrganizer != null) {
                        ddOrganizer.setText(org.getBrandName(), false);
                        if (cardSelectedOrgInfo != null) {
                            cardSelectedOrgInfo.setVisibility(android.view.View.VISIBLE);
                            if (tvSelectedOrgEmail != null) tvSelectedOrgEmail.setText("Email: " + (selectedOrganizer.getContactEmail() != null ? selectedOrganizer.getContactEmail() : "—"));
                            if (tvSelectedOrgPhone != null) tvSelectedOrgPhone.setText("SĐT: " + (selectedOrganizer.getContactPhone() != null ? selectedOrganizer.getContactPhone() : "—"));
                            if (tvSelectedOrgWebsite != null) tvSelectedOrgWebsite.setText("Web: " + (selectedOrganizer.getWebsiteUrl() != null && !selectedOrganizer.getWebsiteUrl().isEmpty() ? selectedOrganizer.getWebsiteUrl() : "—"));
                        }
                    }
                    break;
                }
            }
        }
        
        if (event.getCategoryId() != null && ddCategory != null) {
            for (com.example.vibetix.Models.Category cat : categoryList) {
                if (cat.getCategoryId().equals(event.getCategoryId())) {
                    selectedCategory = cat;
                    ddCategory.setText(cat.getName(), false);
                    break;
                }
            }
        }
        
        // Fetch venue details if venueId exists
        if (event.getVenueId() != null && !event.getVenueId().isEmpty()) {
            db.collection(FirebaseCollections.VENUES).document(event.getVenueId()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            if (etVenueName != null) etVenueName.setText(doc.getString("name"));
                            if (etVenueAddress != null) etVenueAddress.setText(doc.getString("address"));
                            if (etVenueCity != null) etVenueCity.setText(doc.getString("city"));
                            updatePreviewCard();
                        } else {
                            if (etVenueName != null) etVenueName.setText(event.getVenueName());
                            if (etVenueAddress != null) etVenueAddress.setText(event.getVenueAddress());
                            if (etVenueCity != null) etVenueCity.setText(event.getVenueCity());
                        }
                    });
        } else {
            if (etVenueName != null) etVenueName.setText(event.getVenueName());
            if (etVenueAddress != null) etVenueAddress.setText(event.getVenueAddress());
            if (etVenueCity != null) etVenueCity.setText(event.getVenueCity());
        }
        if (etStartTime != null && event.getStartTime() != null) etStartTime.setText(event.getStartTime());
        if (etEndTime != null && event.getEndTime() != null) etEndTime.setText(event.getEndTime());
        
        // Populate category dropdown logic would go here
        
        if (ivEventPoster != null && event.getPosterUrl() != null) {
            Glide.with(this).load(event.getPosterUrl()).into(ivEventPoster);
        }

        // Khóa các trường nhạy cảm nếu event đã được duyệt hoặc đang diễn ra
        if (event.getStatusStr() != null && (event.getStatusStr().equals("approved") || event.getStatusStr().equals("ongoing"))) {
            if (etStartTime != null) etStartTime.setEnabled(false);
            if (etEndTime != null) etEndTime.setEnabled(false);
            if (etVenueName != null) etVenueName.setEnabled(false);
            if (etVenueAddress != null) etVenueAddress.setEnabled(false);
            if (etVenueCity != null) etVenueCity.setEnabled(false);
            // Optionally disable the dropdown as well
            if (ddCategory != null) ddCategory.setEnabled(false);
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

        boolean isNewOrgMode = rgOrganizerMode != null && rgOrganizerMode.getCheckedRadioButtonId() == R.id.rbNewOrganizer;
        if (isNewOrgMode) {
            String newOrgName = etNewOrgName.getText() != null ? etNewOrgName.getText().toString().trim() : "";
            String newOrgEmail = etNewOrgEmail.getText() != null ? etNewOrgEmail.getText().toString().trim() : "";
            String newOrgPhone = etNewOrgPhone.getText() != null ? etNewOrgPhone.getText().toString().trim() : "";
            String newOrgWebsite = etNewOrgWebsite.getText() != null ? etNewOrgWebsite.getText().toString().trim() : "";

            if (newOrgName.isEmpty() || newOrgEmail.isEmpty() || newOrgPhone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập Tên, Email và Số điện thoại Ban tổ chức", Toast.LENGTH_SHORT).show();
                return;
            }
            createNewOrganizerAndSaveFull(newOrgName, newOrgEmail, newOrgPhone, newOrgWebsite, title, description, venueName, venueAddr, venueCity, startTime, endTime, status);
            return;
        } else {
            if (selectedOrganizer == null) {
                Toast.makeText(this, "Vui lòng chọn Ban tổ chức đã lưu", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        continueSaveFlow(title, description, venueName, venueAddr, venueCity, startTime, endTime, status);
    }

    private void createNewOrganizerAndSaveFull(String name, String email, String phone, String website, String title, String desc, String venueName, String venueAddr, String venueCity, String startTime, String endTime, String status) {
        btnSaveDraft.setEnabled(false);
        btnSubmitForApproval.setEnabled(false);
        
        String userId = sessionManager.getUserDetails() != null ? sessionManager.getUserDetails().getUserId() : null;
        if (userId == null) {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            userId = user != null ? user.getUid() : "";
        }
        String newOrgId = UUID.randomUUID().toString();
        
        Organizer org = new Organizer();
        org.setOrganizerId(newOrgId);
        org.setUserId(userId);
        org.setBrandName(name);
        org.setContactEmail(email);
        org.setContactPhone(phone);
        org.setWebsiteUrl(website);
        org.setVerified(false);
        
        db.collection("organizers").document(newOrgId).set(org)
                .addOnSuccessListener(v -> {
                    selectedOrganizer = org; // Fake selection to continue
                    continueSaveFlow(title, desc, venueName, venueAddr, venueCity, startTime, endTime, status);
                })
                .addOnFailureListener(e -> {
                    btnSaveDraft.setEnabled(true);
                    btnSubmitForApproval.setEnabled(true);
                    Toast.makeText(this, "Lỗi tạo Ban tổ chức: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void continueSaveFlow(String title, String desc, String venueName, String venueAddr, String venueCity, String startTime, String endTime, String status) {
        if (selectedPosterUri != null) {
            uploadPosterThenSave(title, desc, venueName, venueAddr, venueCity, startTime, endTime, status);
        } else {
            String existingPoster = editingEvent != null ? editingEvent.getPosterUrl() : null;
            buildAndSaveEvent(title, desc, venueName, venueAddr, venueCity, startTime, endTime, status, existingPoster);
        }
    }

    private void uploadPosterThenSave(String title, String desc, String venueName,
                                       String venueAddr, String venueCity,
                                       String startTime, String endTime, String status) {
        btnSaveDraft.setEnabled(false);
        btnSubmitForApproval.setEnabled(false);

        String userId = sessionManager.getUserDetails() != null ? sessionManager.getUserDetails().getUserId() : null;
        if (userId == null) {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            userId = user != null ? user.getUid() : "";
        }
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
        String venueId = (editingEvent != null && editingEvent.getVenueId() != null && !editingEvent.getVenueId().isEmpty()) ? editingEvent.getVenueId() : UUID.randomUUID().toString();

        java.util.Map<String, Object> venueMap = new java.util.HashMap<>();
        venueMap.put("name", venueName);
        venueMap.put("address", venueAddr);
        venueMap.put("city", venueCity);

        db.collection(FirebaseCollections.VENUES).document(venueId).set(venueMap)
                .addOnSuccessListener(v -> {
                    saveEventObject(eventId, venueId, title, desc, venueName, venueAddr, venueCity, startTime, endTime, status, posterUrl);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi lưu địa điểm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveDraft.setEnabled(true);
                    btnSubmitForApproval.setEnabled(true);
                });
    }

    private void saveEventObject(String eventId, String venueId, String title, String desc, String venueName,
                                 String venueAddr, String venueCity,
                                 String startTime, String endTime, String status, String posterUrl) {
        String organizerId = selectedOrganizer != null ? selectedOrganizer.getOrganizerId() :
                sessionManager.getActiveOrganizerId();
        String tempUserId = sessionManager.getUserDetails() != null ? sessionManager.getUserDetails().getUserId() : null;
        if (tempUserId == null) {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            tempUserId = user != null ? user.getUid() : "";
        }
        final String userId = tempUserId;
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        Event event = new Event();
        event.setEventId(eventId);
        event.setVenueId(venueId);
        event.setOrganizerId(organizerId);
        event.setUserId(userId);
        event.setTitle(title);
        event.setDescription(desc);
        event.setVenueName(venueName);
        event.setVenueAddress(venueAddr);
        event.setVenueCity(venueCity);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        String finalStatus = status;
        if ("pending".equals(status) && selectedOrganizer != null && selectedOrganizer.isVerified()) {
            finalStatus = "approved";
        }
        event.setStatusStr(finalStatus);
        event.setPosterUrl(posterUrl);
        if (selectedCategory != null) {
            event.setCategoryId(selectedCategory.getCategoryId());
        } else if (editingEvent != null) {
            event.setCategoryId(editingEvent.getCategoryId());
        }
        if (existingEventId == null) event.setCreatedAt(now);

        boolean wasPending = editingEvent != null && "pending".equals(editingEvent.getStatusStr());
        boolean isNowApproved = "approved".equals(finalStatus);
        boolean shouldTriggerApproved = wasPending && isNowApproved;

        eventRepository.createEvent(event)
                .addOnSuccessListener(v -> {
                    if (shouldTriggerApproved) {
                        com.example.vibetix.Utils.NotificationTriggerManager.triggerEventApproved(eventId, title);
                    }
                    if (existingEventId == null) {
                        String staffId = java.util.UUID.randomUUID().toString();
                        com.example.vibetix.Models.EventStaff ownerStaff = new com.example.vibetix.Models.EventStaff();
                        ownerStaff.setStaffId(staffId);
                        ownerStaff.setEventId(eventId);
                        ownerStaff.setUserId(userId);
                        ownerStaff.setRoleStr("owner");
                        ownerStaff.setAssignedBy(userId);
                        ownerStaff.setAssignedAt(com.google.firebase.Timestamp.now());
                        ownerStaff.setActive(true);
                        
                        db.collection("event_staff").document(staffId).set(ownerStaff)
                                .addOnSuccessListener(doc -> finishSaving(status))
                                .addOnFailureListener(e -> finishSaving(status));
                    } else {
                        finishSaving(status);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveDraft.setEnabled(true);
                    btnSubmitForApproval.setEnabled(true);
                });
    }

    private void finishSaving(String status) {
        String msg = "draft".equals(status) ? "Đã lưu nháp!" : "Đã gửi duyệt!";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    /** Kiểm tra dữ liệu hợp lệ cho step hiện tại trước khi Next. */
    private boolean validateCurrentStep() {
        switch (currentStep) {
            case 0: // Thông tin cơ bản
                if (etTitle == null || etTitle.getText() == null
                        || etTitle.getText().toString().trim().isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập tiêu đề sự kiện", Toast.LENGTH_SHORT).show();
                    if (etTitle != null) etTitle.requestFocus();
                    return false;
                }
                break;
            case 1: // Lịch trình & Địa điểm
                if (etStartTime == null || etStartTime.getText() == null
                        || etStartTime.getText().toString().trim().isEmpty()) {
                    Toast.makeText(this, "Vui lòng chọn thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (etVenueName == null || etVenueName.getText() == null
                        || etVenueName.getText().toString().trim().isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập địa điểm tổ chức", Toast.LENGTH_SHORT).show();
                    if (etVenueName != null) etVenueName.requestFocus();
                    return false;
                }
                break;
        }
        return true;
    }

    private boolean validateForm() {
        if (etTitle == null || etTitle.getText() == null || etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề sự kiện", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        boolean hasSelectedOrganizer = selectedOrganizer != null;
        boolean hasTypedOrganizer = etNewOrgName != null && etNewOrgName.getText() != null && !etNewOrgName.getText().toString().trim().isEmpty();
        
        if (!hasSelectedOrganizer && !hasTypedOrganizer) {
            Toast.makeText(this, "Vui lòng chọn hoặc nhập Ban tổ chức", Toast.LENGTH_SHORT).show();
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
