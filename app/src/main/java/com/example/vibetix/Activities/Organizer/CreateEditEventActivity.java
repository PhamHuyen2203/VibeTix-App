package com.example.vibetix.Activities.Organizer;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vibetix.Adapters.Organizer.DraftStarAdapter;
import com.example.vibetix.Adapters.Organizer.DraftTicketTypeAdapter;
import com.example.vibetix.Adapters.Organizer.StarSearchAdapter;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.EventStar;
import com.example.vibetix.Models.Organizer;
import com.example.vibetix.Models.Star;
import com.example.vibetix.Models.TicketType;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.EventRepository;
import com.example.vibetix.Repositories.OrganizerRepository;
import com.example.vibetix.Utils.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * CreateEditEventActivity — Tạo hoặc sửa Event (stepper 5 bước).
 *
 * Truyền vào:
 *   - Không có extra                  → Tạo mới (step 1→5)
 *   - extra "event_id"                → Sửa event (pre-fill form)
 *   - extra "IS_READ_ONLY" = true     → Chỉ xem (pending đang chờ duyệt)
 *   - extra "REJECTION_REASON"        → Draft bị admin từ chối → hiện banner
 *
 * Draft case 1: user tự lưu nháp chưa xong.
 * Draft case 2: admin từ chối pending → trả về draft kèm rejection_reason.
 */
public class CreateEditEventActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID      = "event_id";
    public static final String EXTRA_IS_READ_ONLY  = "IS_READ_ONLY";
    public static final String EXTRA_REJECTION_REASON = "REJECTION_REASON";

    private static final int TOTAL_STEPS = 5;

    // ─── Step indicator ──────────────────────────────────────────────────────
    private android.widget.ViewFlipper viewFlipper;
    private TextView tvStepIndicator;
    private LinearProgressIndicator stepProgressIndicator;
    private MaterialButton btnNextStep, btnPreviousStep;
    private int currentStep = 0;

    // ─── Rejection banner ────────────────────────────────────────────────────
    private LinearLayout bannerRejection;
    private View dividerRejection;
    private TextView tvRejectionReason;

    // ─── Step 1: Basic Info ──────────────────────────────────────────────────
    private TextInputEditText etTitle, etDescription;
    private AutoCompleteTextView ddCategory;
    private ShapeableImageView ivEventPoster;
    private com.google.android.material.switchmaterial.SwitchMaterial swIsFree;
    private AutoCompleteTextView ddEventType;
    private TextInputLayout tilOnlineLink;
    private TextInputEditText etOnlineLink;
    private AutoCompleteTextView ddAgeRestriction;
    private TextInputEditText etMaxTicketsPerTransaction;
    // Required field TextInputLayouts (for red * hint + inline error)
    private TextInputLayout tilTitle, tilCategory, tilStartTime, tilEndTime;
    private TextInputLayout tilVenueName, tilVenueCity;
    private TextInputLayout tilNewOrgName, tilNewOrgEmail, tilNewOrgPhone;

    // ─── Step 2: Time & Venue ────────────────────────────────────────────────
    private TextInputEditText etStartTime, etEndTime;
    private TextInputEditText etVenueName, etVenueAddress;
    private AutoCompleteTextView etVenueCity; // E5: dropdown tỉnh/thành phố từ API

    // ─── Step 3: Organizer ───────────────────────────────────────────────────
    private TextView tvPreviewTitle, tvPreviewTime, tvPreviewVenue;
    private RadioGroup rgOrganizerMode;
    private LinearLayout layoutSavedOrganizer, layoutNewOrganizer;
    private com.google.android.material.card.MaterialCardView cardSelectedOrgInfo;
    private TextView tvSelectedOrgEmail, tvSelectedOrgPhone, tvSelectedOrgWebsite;
    private TextInputEditText etNewOrgName, etNewOrgEmail, etNewOrgPhone, etNewOrgWebsite;
    private AutoCompleteTextView ddOrganizer;

    // ─── Step 4: Ticket Types ────────────────────────────────────────────────
    private RecyclerView rvTicketTypesDraft;
    private LinearLayout layoutEmptyTicketTypes;
    private MaterialButton btnAddTicketType;
    private DraftTicketTypeAdapter draftTtAdapter;
    private final List<TicketType> draftTicketTypes = new ArrayList<>();
    // Ticket types đã có trên Firestore (khi edit), track để delete nếu cần
    private final List<String> existingTtIds = new ArrayList<>();

    // ─── Step 5: Stars ───────────────────────────────────────────────────────
    private RecyclerView rvStarsDraft;
    private LinearLayout layoutEmptyStars;
    private MaterialButton btnAddStar;
    private MaterialButton btnSaveDraft, btnSubmitForApproval;
    private DraftStarAdapter draftStarAdapter;
    private final List<Star> draftStars = new ArrayList<>();
    // Stars đã có trên Firestore (khi edit), track để delete nếu cần
    private final List<String> existingStarIds = new ArrayList<>();

    // ─── Repositories & Services ─────────────────────────────────────────────
    private SessionManager sessionManager;
    private EventRepository eventRepository;
    private OrganizerRepository organizerRepository;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // ─── State ───────────────────────────────────────────────────────────────
    private String existingEventId = null;
    private Event editingEvent = null;
    private Uri selectedPosterUri = null;
    private byte[] selectedPosterBytes = null;
    private boolean isReadOnly = false;
    private String pendingRejectionReason = null;

    private List<Organizer> myOrganizers = new ArrayList<>();
    private Organizer selectedOrganizer = null;
    private List<com.example.vibetix.Models.Category> categoryList = new ArrayList<>();
    private com.example.vibetix.Models.Category selectedCategory = null;

    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar   = Calendar.getInstance();
    private final SimpleDateFormat dtFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private ActivityResultLauncher<String> imagePicker;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_edit_event);

        sessionManager      = new SessionManager(this);
        eventRepository     = new EventRepository();
        organizerRepository = new OrganizerRepository();
        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        existingEventId         = getIntent().getStringExtra(EXTRA_EVENT_ID);
        isReadOnly              = getIntent().getBooleanExtra(EXTRA_IS_READ_ONLY, false);
        pendingRejectionReason  = getIntent().getStringExtra(EXTRA_REJECTION_REASON);

        bindViews();
        setupToolbar();
        setupImagePicker();
        setupDateTimePickers();
        setupStepper();
        setupButtons();

        if (existingEventId != null) {
            loadExistingEvent(existingEventId);
        } else {
            loadOrganizerProfiles();
            loadCategories();
        }

        if (isReadOnly) disableAllFields();

        // Hiển thị rejection banner nếu có
        if (pendingRejectionReason != null && !pendingRejectionReason.isEmpty()) {
            showRejectionBanner(pendingRejectionReason);
        }
    }

    // ── Bind Views ────────────────────────────────────────────────────────────
    private void bindViews() {
        viewFlipper           = findViewById(R.id.viewFlipper);
        tvStepIndicator       = findViewById(R.id.tvStepIndicator);
        stepProgressIndicator = findViewById(R.id.stepProgressIndicator);
        btnNextStep           = findViewById(R.id.btnNextStep);
        btnPreviousStep       = findViewById(R.id.btnPreviousStep);

        // Rejection banner
        bannerRejection  = findViewById(R.id.bannerRejection);
        dividerRejection = findViewById(R.id.dividerRejection);
        tvRejectionReason = findViewById(R.id.tvRejectionReason);

        // Step 1
        etTitle       = findViewById(R.id.etEventTitle);
        etDescription = findViewById(R.id.etEventDescription);
        ddCategory    = findViewById(R.id.ddCategory);
        ivEventPoster = findViewById(R.id.ivEventPoster);
        swIsFree      = findViewById(R.id.swIsFree);
        ddEventType   = findViewById(R.id.ddEventType);
        tilOnlineLink = findViewById(R.id.tilOnlineLink);
        etOnlineLink  = findViewById(R.id.etOnlineLink);
        ddAgeRestriction = findViewById(R.id.ddAgeRestriction);
        etMaxTicketsPerTransaction = findViewById(R.id.etMaxTicketsPerTransaction);

        setupNewFieldsUI();

        // Step 2
        etStartTime    = findViewById(R.id.etStartTime);
        etEndTime      = findViewById(R.id.etEndTime);
        etVenueName    = findViewById(R.id.etVenueName);
        etVenueAddress = findViewById(R.id.etVenueAddress);
        etVenueCity    = findViewById(R.id.etVenueCity); // AutoCompleteTextView — E5
        loadProvinces();

        // Step 3
        tvPreviewTitle   = findViewById(R.id.tvPreviewTitle);
        tvPreviewTime    = findViewById(R.id.tvPreviewTime);
        tvPreviewVenue   = findViewById(R.id.tvPreviewVenue);
        rgOrganizerMode  = findViewById(R.id.rgOrganizerMode);
        layoutSavedOrganizer = findViewById(R.id.layoutSavedOrganizer);
        layoutNewOrganizer   = findViewById(R.id.layoutNewOrganizer);
        cardSelectedOrgInfo  = findViewById(R.id.cardSelectedOrgInfo);
        tvSelectedOrgEmail   = findViewById(R.id.tvSelectedOrgEmail);
        tvSelectedOrgPhone   = findViewById(R.id.tvSelectedOrgPhone);
        tvSelectedOrgWebsite = findViewById(R.id.tvSelectedOrgWebsite);
        ddOrganizer          = findViewById(R.id.ddOrganizer);
        etNewOrgName     = findViewById(R.id.etNewOrgName);
        etNewOrgEmail    = findViewById(R.id.etNewOrgEmail);
        etNewOrgPhone    = findViewById(R.id.etNewOrgPhone);
        etNewOrgWebsite  = findViewById(R.id.etNewOrgWebsite);

        // Required/validated field TILs (for red * hints + inline errors)
        tilTitle        = findViewById(R.id.tilTitle);
        tilCategory     = findViewById(R.id.tilCategory);
        tilStartTime    = findViewById(R.id.tilStartTime);
        tilEndTime      = findViewById(R.id.tilEndTime);
        tilVenueName    = findViewById(R.id.tilVenueName);
        tilVenueCity    = findViewById(R.id.tilVenueCity);
        tilNewOrgName   = findViewById(R.id.tilNewOrgName);
        tilNewOrgEmail  = findViewById(R.id.tilNewOrgEmail);
        tilNewOrgPhone  = findViewById(R.id.tilNewOrgPhone);
        markRequiredHints();
        setupErrorClearWatchers();

        if (rgOrganizerMode != null) {
            rgOrganizerMode.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbSavedOrganizer) {
                    if (layoutSavedOrganizer != null) layoutSavedOrganizer.setVisibility(View.VISIBLE);
                    if (layoutNewOrganizer != null)   layoutNewOrganizer.setVisibility(View.GONE);
                } else if (checkedId == R.id.rbNewOrganizer) {
                    if (layoutSavedOrganizer != null) layoutSavedOrganizer.setVisibility(View.GONE);
                    if (layoutNewOrganizer != null)   layoutNewOrganizer.setVisibility(View.VISIBLE);
                }
            });
        }

        // Step 4
        rvTicketTypesDraft       = findViewById(R.id.rvTicketTypesDraft);
        layoutEmptyTicketTypes   = findViewById(R.id.layoutEmptyTicketTypes);
        btnAddTicketType         = findViewById(R.id.btnAddTicketType);

        // Step 5
        rvStarsDraft      = findViewById(R.id.rvStarsDraft);
        layoutEmptyStars  = findViewById(R.id.layoutEmptyStars);
        btnAddStar        = findViewById(R.id.btnAddStar);
        btnSaveDraft      = findViewById(R.id.btnSaveDraft);
        btnSubmitForApproval = findViewById(R.id.btnSubmitForApproval);

        setupTicketTypeStep();
        setupStarStep();
    }

    private void setupNewFieldsUI() {
        if (ddEventType != null) {
            String[] eventTypes = {"Trực tiếp (Offline)", "Trực tuyến (Online)"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, eventTypes);
            ddEventType.setAdapter(adapter);
            ddEventType.setOnItemClickListener((parent, view, position, id) -> {
                if (position == 1) { // Online
                    if (tilOnlineLink != null) tilOnlineLink.setVisibility(View.VISIBLE);
                } else { // Offline
                    if (tilOnlineLink != null) tilOnlineLink.setVisibility(View.GONE);
                    if (etOnlineLink != null) etOnlineLink.setText("");
                }
            });
        }

        if (ddAgeRestriction != null) {
            String[] ageRestrictions = {"Không giới hạn", "12+", "16+", "18+"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, ageRestrictions);
            ddAgeRestriction.setAdapter(adapter);
        }
    }

    // ── Rejection Banner ──────────────────────────────────────────────────────
    private void showRejectionBanner(String reason) {
        if (bannerRejection != null) {
            bannerRejection.setVisibility(View.VISIBLE);
            if (dividerRejection != null) dividerRejection.setVisibility(View.VISIBLE);
            if (tvRejectionReason != null) tvRejectionReason.setText("Lý do: " + reason);
        }
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarCreateEvent);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(existingEventId != null
                    ? "Chỉnh sửa sự kiện"
                    : "Tạo sự kiện mới");
        }
    }

    // ── Stepper ───────────────────────────────────────────────────────────────
    private void setupStepper() {
        if (btnNextStep == null) return;
        updateStepperUI();

        btnNextStep.setOnClickListener(v -> {
            if (currentStep < TOTAL_STEPS - 1) {
                if (!isReadOnly && !validateCurrentStep()) return;
                currentStep++;
                viewFlipper.setDisplayedChild(currentStep);
                updateStepperUI();
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
        String[] titles = {
                "Bước 1/5: Thông tin cơ bản",
                "Bước 2/5: Lịch trình & Địa điểm",
                "Bước 3/5: Ban tổ chức",
                "Bước 4/5: Loại vé",
                "Bước 5/5: Nghệ sĩ"
        };
        int[] progresses = {20, 40, 60, 80, 100};

        if (tvStepIndicator != null)
            tvStepIndicator.setText(titles[currentStep]);
        if (stepProgressIndicator != null)
            stepProgressIndicator.setProgress(progresses[currentStep]);

        // Show/hide nav buttons
        if (btnPreviousStep != null)
            btnPreviousStep.setVisibility(currentStep > 0 ? View.VISIBLE : View.GONE);

        // Step 5 — ẩn "Tiếp theo", hiện nút Save/Submit trong nội dung
        boolean isLastStep = currentStep == TOTAL_STEPS - 1;
        if (btnNextStep != null)
            btnNextStep.setVisibility(isLastStep ? View.GONE : View.VISIBLE);
    }

    // ── Image picker ─────────────────────────────────────────────────────────
    private void setupImagePicker() {
        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPosterUri = uri;
                        // Read bytes immediately while URI permission is still valid
                        try (InputStream is = getContentResolver().openInputStream(uri);
                             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
                            selectedPosterBytes = bos.toByteArray();
                        } catch (Exception e) {
                            selectedPosterBytes = null;
                        }
                        Glide.with(this).load(uri).into(ivEventPoster);
                    }
                });
        if (ivEventPoster != null) {
            ivEventPoster.setOnClickListener(v -> imagePicker.launch("image/*"));
        }
    }

    // ── DateTime pickers ──────────────────────────────────────────────────────
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
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày")
                .setSelection(cal.getTimeInMillis())
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            // selection là UTC millis; cập nhật ngày vào cal
            Calendar picked = Calendar.getInstance();
            picked.setTimeInMillis(selection);
            cal.set(Calendar.YEAR,         picked.get(Calendar.YEAR));
            cal.set(Calendar.MONTH,        picked.get(Calendar.MONTH));
            cal.set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH));

            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(cal.get(Calendar.HOUR_OF_DAY))
                    .setMinute(cal.get(Calendar.MINUTE))
                    .setTitleText("Chọn giờ")
                    .build();
            timePicker.addOnPositiveButtonClickListener(v -> {
                cal.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                cal.set(Calendar.MINUTE,      timePicker.getMinute());
                target.setText(dtFormat.format(cal.getTime()));
            });
            timePicker.show(getSupportFragmentManager(), "time_picker");
        });
        datePicker.show(getSupportFragmentManager(), "date_picker");
    }

    private void showSaleDatePicker(Calendar cal, TextInputEditText target,
                                    java.text.SimpleDateFormat fmt, String title) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(title)
                .setSelection(cal.getTimeInMillis())
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar picked = Calendar.getInstance();
            picked.setTimeInMillis(selection);
            cal.set(Calendar.YEAR,         picked.get(Calendar.YEAR));
            cal.set(Calendar.MONTH,        picked.get(Calendar.MONTH));
            cal.set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH));

            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(cal.get(Calendar.HOUR_OF_DAY))
                    .setMinute(cal.get(Calendar.MINUTE))
                    .setTitleText("Chọn giờ")
                    .build();
            timePicker.addOnPositiveButtonClickListener(v -> {
                cal.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                cal.set(Calendar.MINUTE,      timePicker.getMinute());
                target.setText(fmt.format(cal.getTime()));
            });
            timePicker.show(getSupportFragmentManager(), "sale_time_picker");
        });
        datePicker.show(getSupportFragmentManager(), "sale_date_picker");
    }

    // ── Step 4: Ticket Types ──────────────────────────────────────────────────
    private void setupTicketTypeStep() {
        if (rvTicketTypesDraft == null) return;
        draftTtAdapter = new DraftTicketTypeAdapter(draftTicketTypes, new DraftTicketTypeAdapter.Listener() {
            @Override
            public void onEdit(int position, TicketType tt) {
                showTicketTypeBottomSheet(tt, position);
            }
            @Override
            public void onDelete(int position, TicketType tt) {
                draftTicketTypes.remove(position);
                draftTtAdapter.notifyItemRemoved(position);
                updateTicketTypeEmptyState();
            }
        });
        rvTicketTypesDraft.setLayoutManager(new LinearLayoutManager(this));
        rvTicketTypesDraft.setAdapter(draftTtAdapter);
        rvTicketTypesDraft.setNestedScrollingEnabled(false);

        if (btnAddTicketType != null) {
            btnAddTicketType.setOnClickListener(v -> showTicketTypeBottomSheet(null, -1));
        }
    }

    /** Mở BottomSheet thêm / sửa loại vé (tái dùng bottom_sheet_ticket_type.xml). */
    private void showTicketTypeBottomSheet(TicketType existing, int editPosition) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_ticket_type, null);
        sheet.setContentView(sheetView);

        // Title
        TextView tvTitle = sheetView.findViewById(R.id.tvBottomSheetTitle);
        if (tvTitle != null) tvTitle.setText(existing == null ? "Thêm loại vé" : "Sửa loại vé");

        TextInputEditText etName     = sheetView.findViewById(R.id.etTicketName);
        TextInputEditText etDesc     = sheetView.findViewById(R.id.etTicketDesc);
        TextInputEditText etPrice    = sheetView.findViewById(R.id.etTicketPrice);
        TextInputEditText etQty      = sheetView.findViewById(R.id.etTotalQty);
        TextInputEditText etSaleStart = sheetView.findViewById(R.id.etSaleStart);
        TextInputEditText etSaleEnd   = sheetView.findViewById(R.id.etSaleEnd);
        MaterialButton btnSave = sheetView.findViewById(R.id.btnSaveTicketType);

        java.text.SimpleDateFormat saleDateFmt = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
        Calendar saleStartCal = Calendar.getInstance();
        Calendar saleEndCal   = Calendar.getInstance();

        // Pre-fill nếu đang sửa
        if (existing != null) {
            if (etName  != null && existing.getName() != null)        etName.setText(existing.getName());
            if (etDesc  != null && existing.getDescription() != null) etDesc.setText(existing.getDescription());
            if (etPrice != null) etPrice.setText(String.valueOf(existing.getPrice()));
            if (etQty   != null) etQty.setText(String.valueOf(existing.getTotalQuantity()));
            if (existing.getSaleStart() != null) {
                saleStartCal.setTimeInMillis(existing.getSaleStart().toDate().getTime());
                if (etSaleStart != null) etSaleStart.setText(saleDateFmt.format(saleStartCal.getTime()));
            }
            if (existing.getSaleEnd() != null) {
                saleEndCal.setTimeInMillis(existing.getSaleEnd().toDate().getTime());
                if (etSaleEnd != null) etSaleEnd.setText(saleDateFmt.format(saleEndCal.getTime()));
            }
        }

        // Date pickers for sale start/end
        if (etSaleStart != null) {
            etSaleStart.setOnClickListener(v -> showSaleDatePicker(saleStartCal, etSaleStart, saleDateFmt, "Ngày bắt đầu mở bán"));
            etSaleStart.setFocusable(false);
        }
        if (etSaleEnd != null) {
            etSaleEnd.setOnClickListener(v -> showSaleDatePicker(saleEndCal, etSaleEnd, saleDateFmt, "Ngày kết thúc mở bán"));
            etSaleEnd.setFocusable(false);
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String name     = etName  != null && etName.getText() != null  ? etName.getText().toString().trim()  : "";
                String priceStr = etPrice != null && etPrice.getText() != null ? etPrice.getText().toString().trim() : "0";
                String qtyStr   = etQty   != null && etQty.getText() != null   ? etQty.getText().toString().trim()   : "0";

                if (name.isEmpty()) {
                    if (etName != null) etName.setError("Vui lòng nhập tên loại vé");
                    return;
                }

                long price = 0;
                long qty   = 0;
                try { price = Long.parseLong(priceStr.replace(".", "").replace(",", "")); } catch (Exception ignored) {}
                try { qty   = Long.parseLong(qtyStr); } catch (Exception ignored) {}

                TicketType tt = existing != null ? existing : new TicketType();
                if (tt.getTicketTypeId() == null) tt.setTicketTypeId(UUID.randomUUID().toString());
                tt.setName(name);
                tt.setDescription(etDesc != null && etDesc.getText() != null ? etDesc.getText().toString().trim() : "");
                tt.setPrice(price);
                tt.setTotalQuantity(qty);
                tt.setAvailableQuantity(qty);
                tt.setActive(true);

                // Save sale dates if set
                String saleStartStr = etSaleStart != null && etSaleStart.getText() != null ? etSaleStart.getText().toString().trim() : "";
                String saleEndStr   = etSaleEnd   != null && etSaleEnd.getText()   != null ? etSaleEnd.getText().toString().trim()   : "";
                if (!saleStartStr.isEmpty()) tt.setSaleStart(new com.google.firebase.Timestamp(saleStartCal.getTime()));
                if (!saleEndStr.isEmpty())   tt.setSaleEnd(new com.google.firebase.Timestamp(saleEndCal.getTime()));

                if (editPosition >= 0 && editPosition < draftTicketTypes.size()) {
                    draftTicketTypes.set(editPosition, tt);
                    draftTtAdapter.notifyItemChanged(editPosition);
                } else {
                    tt.setSortOrder(draftTicketTypes.size());
                    draftTicketTypes.add(tt);
                    draftTtAdapter.notifyItemInserted(draftTicketTypes.size() - 1);
                }
                updateTicketTypeEmptyState();
                sheet.dismiss();
            });
        }

        sheet.show();
    }

    private void updateTicketTypeEmptyState() {
        boolean isEmpty = draftTicketTypes.isEmpty();
        if (layoutEmptyTicketTypes != null) layoutEmptyTicketTypes.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (rvTicketTypesDraft != null)     rvTicketTypesDraft.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // ── Step 5: Stars ─────────────────────────────────────────────────────────
    private void setupStarStep() {
        if (rvStarsDraft == null) return;
        draftStarAdapter = new DraftStarAdapter(draftStars, (position, star) -> {
            draftStars.remove(position);
            draftStarAdapter.notifyItemRemoved(position);
            updateStarEmptyState();
        });
        rvStarsDraft.setLayoutManager(new LinearLayoutManager(this));
        rvStarsDraft.setAdapter(draftStarAdapter);
        rvStarsDraft.setNestedScrollingEnabled(false);

        if (btnAddStar != null) {
            btnAddStar.setOnClickListener(v -> showStarSearchBottomSheet());
        }
    }

    private void showStarSearchBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_search_stars, null);
        sheet.setContentView(sheetView);

        TextInputEditText etSearch = sheetView.findViewById(R.id.etSearchStar);
        RecyclerView rvSearch      = sheetView.findViewById(R.id.rvSearchStars);
        android.widget.ProgressBar pb = sheetView.findViewById(R.id.pbSearchLoading);
        TextView tvEmpty           = sheetView.findViewById(R.id.tvEmptySearch);
        View btnClose              = sheetView.findViewById(R.id.btnCloseSheet);

        if (btnClose != null) btnClose.setOnClickListener(v -> sheet.dismiss());

        List<Star> allStarsMaster = new ArrayList<>();
        List<Star> searchResults = new ArrayList<>();
        StarSearchAdapter searchAdapter = new StarSearchAdapter(searchResults, star -> {
            // Kiểm tra đã thêm chưa
            for (Star s : draftStars) {
                if (s.getStarId() != null && s.getStarId().equals(star.getStarId())) {
                    Toast.makeText(this, "Nghệ sĩ này đã được thêm", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            draftStars.add(star);
            draftStarAdapter.notifyItemInserted(draftStars.size() - 1);
            updateStarEmptyState();
            sheet.dismiss();
        });

        if (rvSearch != null) {
            rvSearch.setLayoutManager(new LinearLayoutManager(this));
            rvSearch.setAdapter(searchAdapter);
        }

        // Load all active stars khi mở sheet
        if (pb != null) pb.setVisibility(View.VISIBLE);
        db.collection(FirebaseCollections.STARS)
                .whereEqualTo("is_active", true)
                .limit(100)
                .get()
                .addOnSuccessListener(snap -> {
                    if (pb != null) pb.setVisibility(View.GONE);
                    allStarsMaster.clear();
                    searchResults.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Star s = doc.toObject(Star.class);
                        if (s != null) {
                            if (s.getStarId() == null) s.setStarId(doc.getId());
                            allStarsMaster.add(s);
                        }
                    }
                    // Sắp xếp danh sách nghệ sĩ theo stage_name cục bộ
                    java.util.Collections.sort(allStarsMaster, (o1, o2) -> {
                        String name1 = o1.getStageName() != null ? o1.getStageName() : "";
                        String name2 = o2.getStageName() != null ? o2.getStageName() : "";
                        return name1.compareToIgnoreCase(name2);
                    });
                    searchResults.addAll(allStarsMaster);
                    searchAdapter.notifyDataSetChanged();
                    if (tvEmpty != null) tvEmpty.setVisibility(searchResults.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (pb != null) pb.setVisibility(View.GONE);
                    android.util.Log.e("CreateEditEvent", "Error fetching stars", e);
                });

        // Search filter
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim();
                    searchResults.clear();
                    if (query.isEmpty()) {
                        searchResults.addAll(allStarsMaster);
                    } else {
                        // Filter locally with CosineSimilarity
                        for (Star star : allStarsMaster) {
                            if (star.getStageName() != null) {
                                double sim = com.example.vibetix.Utils.CosineSimilarityUtils.calculateSimilarity(query, star.getStageName());
                                if (sim > 0.3 || star.getStageName().toLowerCase().contains(query.toLowerCase())) {
                                    searchResults.add(star);
                                }
                            }
                        }
                    }
                    searchAdapter.notifyDataSetChanged();
                    if (tvEmpty != null) tvEmpty.setVisibility(searchResults.isEmpty() ? View.VISIBLE : View.GONE);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        sheet.show();
    }

    private void updateStarEmptyState() {
        boolean isEmpty = draftStars.isEmpty();
        if (layoutEmptyStars != null) layoutEmptyStars.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (rvStarsDraft != null)     rvStarsDraft.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // ── Action Buttons ────────────────────────────────────────────────────────
    private void setupButtons() {
        if (btnSaveDraft != null) {
            btnSaveDraft.setOnClickListener(v -> saveEvent("draft"));
        }
        if (btnSubmitForApproval != null) {
            btnSubmitForApproval.setOnClickListener(v -> saveEvent("pending"));
        }
    }

    // ── Preview Card (Step 3) ─────────────────────────────────────────────────
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

    // ── Load data ─────────────────────────────────────────────────────────────
    private void loadOrganizerProfiles() {
        String userId = getCurrentUserId();
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
                    String defaultId = sessionManager.getActiveOrganizerId();
                    for (Organizer org : myOrganizers) {
                        if (org.getOrganizerId().equals(defaultId)) {
                            selectedOrganizer = org;
                            if (ddOrganizer != null) ddOrganizer.setText(org.getBrandName(), false);
                            break;
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

    private void loadExistingEvent(String eventId) {
        db.collection(FirebaseCollections.EVENTS).document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    editingEvent = doc.toObject(Event.class);
                    if (editingEvent != null && editingEvent.getId() == null)
                        editingEvent.setId(doc.getId());

                    // Hiển thị rejection banner nếu event bị reject (draft case 2)
                    if (editingEvent != null && editingEvent.isRejected()) {
                        showRejectionBanner(editingEvent.getRejectionReason());
                    }

                    String userId = getCurrentUserId();
                    com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> categoriesTask =
                            db.collection(FirebaseCollections.CATEGORIES).get();
                    com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> organizersTask =
                            userId != null
                                    ? db.collection(FirebaseCollections.ORGANIZERS).whereEqualTo("user_id", userId).get()
                                    : null;
                    com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> ticketTypesTask =
                            db.collection(FirebaseCollections.TICKET_TYPES).whereEqualTo("event_id", eventId).get();
                    com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> starsTask =
                            db.collection(FirebaseCollections.EVENT_STARS).whereEqualTo("event_id", eventId).get();

                    List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
                    tasks.add(categoriesTask);
                    if (organizersTask != null) tasks.add(organizersTask);
                    tasks.add(ticketTypesTask);
                    tasks.add(starsTask);

                    com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                        // Categories
                        categoryList.clear();
                        com.google.firebase.firestore.QuerySnapshot catSnap =
                                (com.google.firebase.firestore.QuerySnapshot) results.get(0);
                        if (catSnap != null) {
                            for (QueryDocumentSnapshot catDoc : catSnap) {
                                com.example.vibetix.Models.Category cat =
                                        catDoc.toObject(com.example.vibetix.Models.Category.class);
                                if (cat.getCategoryId() == null) cat.setCategoryId(catDoc.getId());
                                categoryList.add(cat);
                            }
                        }
                        setupCategoryDropdown();

                        int offset = 1;
                        // Organizers
                        if (organizersTask != null && results.size() > offset) {
                            myOrganizers.clear();
                            com.google.firebase.firestore.QuerySnapshot orgSnap =
                                    (com.google.firebase.firestore.QuerySnapshot) results.get(offset++);
                            if (orgSnap != null) {
                                for (QueryDocumentSnapshot orgDoc : orgSnap) {
                                    Organizer org = orgDoc.toObject(Organizer.class);
                                    if (org != null) {
                                        if (org.getOrganizerId() == null) org.setOrganizerId(orgDoc.getId());
                                        myOrganizers.add(org);
                                    }
                                }
                            }
                            setupOrganizerDropdown();
                        }

                        // Ticket Types
                        if (results.size() > offset) {
                            draftTicketTypes.clear();
                            existingTtIds.clear();
                            com.google.firebase.firestore.QuerySnapshot ttSnap =
                                    (com.google.firebase.firestore.QuerySnapshot) results.get(offset++);
                            if (ttSnap != null) {
                                for (QueryDocumentSnapshot ttDoc : ttSnap) {
                                    TicketType tt = ttDoc.toObject(TicketType.class);
                                    tt.setTicketTypeId(ttDoc.getId());
                                    draftTicketTypes.add(tt);
                                    existingTtIds.add(ttDoc.getId());
                                }
                            }
                            draftTtAdapter.notifyDataSetChanged();
                            updateTicketTypeEmptyState();
                        }

                        // Stars
                        if (results.size() > offset) {
                            draftStars.clear();
                            existingStarIds.clear();
                            com.google.firebase.firestore.QuerySnapshot starsSnap =
                                    (com.google.firebase.firestore.QuerySnapshot) results.get(offset);
                            if (starsSnap != null) {
                                // Load star details cho mỗi event_star record
                                List<String> starIds = new ArrayList<>();
                                for (QueryDocumentSnapshot esDoc : starsSnap) {
                                    EventStar es = esDoc.toObject(EventStar.class);
                                    if (es.getStarId() != null) {
                                        starIds.add(es.getStarId());
                                        existingStarIds.add(esDoc.getId());
                                    }
                                }
                                loadStarDetails(starIds);
                            }
                        }

                        if (editingEvent != null) populateForm(editingEvent);
                    });
                });
    }

    private void loadStarDetails(List<String> starIds) {
        if (starIds.isEmpty()) return;
        // Chunk 10
        int chunkSize = 10;
        for (int i = 0; i < starIds.size(); i += chunkSize) {
            List<String> chunk = starIds.subList(i, Math.min(i + chunkSize, starIds.size()));
            db.collection(FirebaseCollections.STARS)
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .addOnSuccessListener(snap -> {
                        for (QueryDocumentSnapshot doc : snap) {
                            Star s = doc.toObject(Star.class);
                            if (s.getStarId() == null) s.setStarId(doc.getId());
                            draftStars.add(s);
                        }
                        draftStarAdapter.notifyDataSetChanged();
                        updateStarEmptyState();
                    });
        }
    }

    private void setupCategoryDropdown() {
        if (ddCategory == null || categoryList.isEmpty()) return;
        List<String> names = new ArrayList<>();
        for (com.example.vibetix.Models.Category cat : categoryList) names.add(cat.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
        ddCategory.setAdapter(adapter);
        ddCategory.setOnItemClickListener((parent, view, position, id) -> selectedCategory = categoryList.get(position));
        if (selectedCategory != null) ddCategory.setText(selectedCategory.getName(), false);
    }

    private void setupOrganizerDropdown() {
        if (ddOrganizer == null) return;
        List<String> names = new ArrayList<>();
        for (Organizer org : myOrganizers) names.add(org.getBrandName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
        ddOrganizer.setAdapter(adapter);
        ddOrganizer.setOnItemClickListener((parent, view, position, id) -> {
            selectedOrganizer = myOrganizers.get(position);
            updateOrganizerUI(selectedOrganizer);
        });
    }

    private void populateForm(Event event) {
        if (etTitle != null) etTitle.setText(event.getTitle());
        if (etDescription != null) etDescription.setText(event.getDescription());
        if (etStartTime != null && event.getStartTime() != null) etStartTime.setText(event.getStartTime());
        if (etEndTime != null && event.getEndTime() != null)   etEndTime.setText(event.getEndTime());

        // Venue
        if (event.getVenueId() != null && !event.getVenueId().isEmpty()) {
            db.collection(FirebaseCollections.VENUES).document(event.getVenueId()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            if (etVenueName != null)    etVenueName.setText(doc.getString("name"));
                            if (etVenueAddress != null) etVenueAddress.setText(doc.getString("address"));
                            if (etVenueCity != null)    etVenueCity.setText(doc.getString("city"));
                            updatePreviewCard();
                        } else {
                            populateVenueFallback(event);
                        }
                    });
        } else {
            populateVenueFallback(event);
        }

        // Organizer
        if (event.getOrganizerId() != null) {
            if (rgOrganizerMode != null) {
                rgOrganizerMode.check(R.id.rbSavedOrganizer);
            }
            if (layoutSavedOrganizer != null) {
                layoutSavedOrganizer.setVisibility(View.VISIBLE);
            }
            if (layoutNewOrganizer != null) {
                layoutNewOrganizer.setVisibility(View.GONE);
            }

            boolean found = false;
            for (Organizer org : myOrganizers) {
                if (org.getOrganizerId().equals(event.getOrganizerId())) {
                    selectedOrganizer = org;
                    updateOrganizerUI(org);
                    found = true;
                    break;
                }
            }
            if (!found) {
                db.collection(FirebaseCollections.ORGANIZERS).document(event.getOrganizerId()).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                Organizer org = doc.toObject(Organizer.class);
                                if (org != null) {
                                    if (org.getOrganizerId() == null) org.setOrganizerId(doc.getId());
                                    selectedOrganizer = org;
                                    updateOrganizerUI(org);
                                }
                            }
                        });
            }
        }

        // Category
        if (event.getCategoryId() != null) {
            for (com.example.vibetix.Models.Category cat : categoryList) {
                if (cat.getCategoryId().equals(event.getCategoryId())) {
                    selectedCategory = cat;
                    if (ddCategory != null) ddCategory.setText(cat.getName(), false);
                    break;
                }
            }
        }

        // Poster
        if (ivEventPoster != null && event.getPosterUrl() != null) {
            Glide.with(this).load(event.getPosterUrl()).into(ivEventPoster);
        }

        if (swIsFree != null) {
            swIsFree.setChecked(event.isFree());
        }

        if (ddEventType != null && event.getEventType() != null) {
            String typeText = "online".equalsIgnoreCase(event.getEventType()) ? "Trực tuyến (Online)" : "Trực tiếp (Offline)";
            ddEventType.setText(typeText, false);
            if ("online".equalsIgnoreCase(event.getEventType())) {
                if (tilOnlineLink != null) tilOnlineLink.setVisibility(View.VISIBLE);
                if (etOnlineLink != null) etOnlineLink.setText(event.getOnlineLink());
            } else {
                if (tilOnlineLink != null) tilOnlineLink.setVisibility(View.GONE);
            }
        }

        if (ddAgeRestriction != null && event.getAgeRestriction() != null) {
            ddAgeRestriction.setText(event.getAgeRestriction(), false);
        }

        if (etMaxTicketsPerTransaction != null && event.getMaxTicketsPerTransaction() > 0) {
            etMaxTicketsPerTransaction.setText(String.valueOf(event.getMaxTicketsPerTransaction()));
        }

        // Adjust submit buttons for approved/ongoing events
        String status = event.getStatusStr();
        if ("approved".equals(status) || "ongoing".equals(status)) {
            if (btnSaveDraft != null) btnSaveDraft.setVisibility(View.GONE);
            if (btnSubmitForApproval != null) {
                btnSubmitForApproval.setText("Cập nhật");
                btnSubmitForApproval.setOnClickListener(v -> saveEvent(status));
            }
        }
    }

    private void populateVenueFallback(Event event) {
        if (etVenueName != null)    etVenueName.setText(event.getVenueName());
        if (etVenueAddress != null) etVenueAddress.setText(event.getVenueAddress());
        if (etVenueCity != null)    etVenueCity.setText(event.getVenueCity());
    }

    private void updateOrganizerUI(Organizer org) {
        if (ddOrganizer != null) {
            ddOrganizer.setText(org.getBrandName(), false);
        }
        if (cardSelectedOrgInfo != null) {
            cardSelectedOrgInfo.setVisibility(View.VISIBLE);
            if (tvSelectedOrgEmail != null) {
                String email = org.getContactEmail();
                if (email != null && !email.trim().isEmpty()) {
                    tvSelectedOrgEmail.setText("Email: " + email);
                    tvSelectedOrgEmail.setVisibility(View.VISIBLE);
                } else {
                    tvSelectedOrgEmail.setVisibility(View.GONE);
                }
            }
            if (tvSelectedOrgPhone != null) {
                String phone = org.getContactPhone();
                if (phone != null && !phone.trim().isEmpty()) {
                    tvSelectedOrgPhone.setText("SĐT: " + phone);
                    tvSelectedOrgPhone.setVisibility(View.VISIBLE);
                } else {
                    tvSelectedOrgPhone.setVisibility(View.GONE);
                }
            }
            if (tvSelectedOrgWebsite != null) {
                String web = org.getWebsiteUrl();
                if (web != null && !web.trim().isEmpty()) {
                    tvSelectedOrgWebsite.setText("Web: " + web);
                    tvSelectedOrgWebsite.setVisibility(View.VISIBLE);
                } else {
                    tvSelectedOrgWebsite.setVisibility(View.GONE);
                }
            }
        }
    }

    // ── Disable all fields (read-only mode) ───────────────────────────────────
    private void disableAllFields() {
        TextInputEditText[] fields = {etTitle, etDescription, etStartTime, etEndTime,
                etVenueName, etVenueAddress,
                etNewOrgName, etNewOrgEmail, etNewOrgPhone, etNewOrgWebsite,
                etOnlineLink, etMaxTicketsPerTransaction};
        for (TextInputEditText f : fields) if (f != null) f.setEnabled(false);
        if (etVenueCity != null) etVenueCity.setEnabled(false);
        if (ddCategory  != null) ddCategory.setEnabled(false);
        if (ddOrganizer != null) ddOrganizer.setEnabled(false);
        if (swIsFree    != null) swIsFree.setEnabled(false);
        if (ddEventType != null) ddEventType.setEnabled(false);
        if (ddAgeRestriction != null) ddAgeRestriction.setEnabled(false);
        if (ivEventPoster != null) ivEventPoster.setEnabled(false);
        if (rgOrganizerMode != null) rgOrganizerMode.setEnabled(false);
        if (btnAddTicketType != null) btnAddTicketType.setEnabled(false);
        if (btnAddStar != null)       btnAddStar.setEnabled(false);
        if (btnSaveDraft != null)     btnSaveDraft.setVisibility(View.GONE);
        if (btnSubmitForApproval != null) {
            btnSubmitForApproval.setText(getString(R.string.event_pending_approval_btn));
            btnSubmitForApproval.setEnabled(false);
            btnSubmitForApproval.setBackgroundColor(getResources().getColor(R.color.clr_grey_2, null));
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────
    private boolean validateCurrentStep() {
        clearStepErrors();
        switch (currentStep) {
            case 0:
                if (etTitle == null || etTitle.getText() == null
                        || etTitle.getText().toString().trim().isEmpty()) {
                    setFieldError(tilTitle, "Vui lòng nhập tiêu đề sự kiện");
                    return false;
                }
                if (selectedCategory == null) {
                    setFieldError(tilCategory, "Vui lòng chọn danh mục sự kiện");
                    return false;
                }
                break;
            case 1:
                if (etStartTime == null || etStartTime.getText() == null
                        || etStartTime.getText().toString().trim().isEmpty()) {
                    setFieldError(tilStartTime, "Vui lòng chọn thời gian bắt đầu");
                    return false;
                }
                if (etEndTime != null && etEndTime.getText() != null
                        && !etEndTime.getText().toString().trim().isEmpty()) {
                    if (endCalendar.getTimeInMillis() <= startCalendar.getTimeInMillis()) {
                        setFieldError(tilEndTime, "Thời gian kết thúc phải sau thời gian bắt đầu");
                        return false;
                    }
                }
                if (etVenueName == null || etVenueName.getText() == null
                        || etVenueName.getText().toString().trim().isEmpty()) {
                    setFieldError(tilVenueName, "Vui lòng nhập tên địa điểm tổ chức");
                    return false;
                }
                if (etVenueCity == null || etVenueCity.getText() == null
                        || etVenueCity.getText().toString().trim().isEmpty()) {
                    setFieldError(tilVenueCity, "Vui lòng chọn tỉnh/thành phố");
                    return false;
                }
                break;
            case 2:
                boolean isNewOrgMode = rgOrganizerMode != null
                        && rgOrganizerMode.getCheckedRadioButtonId() == R.id.rbNewOrganizer;
                if (isNewOrgMode) {
                    String orgName  = etNewOrgName  != null && etNewOrgName.getText()  != null ? etNewOrgName.getText().toString().trim()  : "";
                    String orgEmail = etNewOrgEmail != null && etNewOrgEmail.getText() != null ? etNewOrgEmail.getText().toString().trim() : "";
                    String orgPhone = etNewOrgPhone != null && etNewOrgPhone.getText() != null ? etNewOrgPhone.getText().toString().trim() : "";
                    if (orgName.isEmpty()) {
                        setFieldError(tilNewOrgName, "Vui lòng nhập tên Ban tổ chức");
                        return false;
                    }
                    if (orgEmail.isEmpty()) {
                        setFieldError(tilNewOrgEmail, "Vui lòng nhập email liên hệ");
                        return false;
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(orgEmail).matches()) {
                        setFieldError(tilNewOrgEmail, "Email không đúng định dạng");
                        return false;
                    }
                    if (orgPhone.isEmpty()) {
                        setFieldError(tilNewOrgPhone, "Vui lòng nhập số điện thoại");
                        return false;
                    }
                    if (!orgPhone.matches("^(\\+84|0)[0-9]{8,10}$")) {
                        setFieldError(tilNewOrgPhone, "Số điện thoại không đúng định dạng (VD: 0912345678)");
                        return false;
                    }
                }
                break;
        }
        return true;
    }

    private void setFieldError(TextInputLayout til, String message) {
        if (til == null) return;
        til.setErrorEnabled(true);
        til.setError(message);
        View innerField = til.getEditText();
        if (innerField != null) innerField.requestFocus();
    }

    private void clearStepErrors() {
        TextInputLayout[] all = {
                tilTitle, tilCategory, tilStartTime, tilEndTime,
                tilVenueName, tilVenueCity,
                tilNewOrgName, tilNewOrgEmail, tilNewOrgPhone
        };
        for (TextInputLayout t : all) {
            if (t != null) { t.setError(null); t.setErrorEnabled(false); }
        }
    }

    private boolean validateForm(String status) {
        if ("draft".equals(status)) return true;

        if (etTitle == null || etTitle.getText() == null || etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.err_empty_event_title), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (ddEventType == null || ddEventType.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn hình thức tổ chức", Toast.LENGTH_SHORT).show();
            return false;
        }

        String evType = ddEventType.getText().toString().trim().toLowerCase();
        if (evType.contains("online") || evType.contains("tuyến")) {
            if (etOnlineLink == null || etOnlineLink.getText() == null || etOnlineLink.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đường dẫn trực tuyến", Toast.LENGTH_SHORT).show();
                if (etOnlineLink != null) etOnlineLink.requestFocus();
                return false;
            }
        }

        if (ddAgeRestriction == null || ddAgeRestriction.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn giới hạn độ tuổi", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (etMaxTicketsPerTransaction == null || etMaxTicketsPerTransaction.getText() == null || etMaxTicketsPerTransaction.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số vé tối đa mỗi giao dịch", Toast.LENGTH_SHORT).show();
            if (etMaxTicketsPerTransaction != null) etMaxTicketsPerTransaction.requestFocus();
            return false;
        }

        try {
            int maxTickets = Integer.parseInt(etMaxTicketsPerTransaction.getText().toString().trim());
            if (maxTickets <= 0) {
                Toast.makeText(this, "Số vé tối đa phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                if (etMaxTicketsPerTransaction != null) etMaxTicketsPerTransaction.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số vé tối đa không hợp lệ", Toast.LENGTH_SHORT).show();
            if (etMaxTicketsPerTransaction != null) etMaxTicketsPerTransaction.requestFocus();
            return false;
        }

        boolean isNewOrgMode = rgOrganizerMode != null
                && rgOrganizerMode.getCheckedRadioButtonId() == R.id.rbNewOrganizer;
        boolean hasSelectedOrganizer = selectedOrganizer != null;
        boolean hasTypedOrganizer    = isNewOrgMode && etNewOrgName != null
                && etNewOrgName.getText() != null
                && !etNewOrgName.getText().toString().trim().isEmpty();

        if (!hasSelectedOrganizer && !hasTypedOrganizer) {
            Toast.makeText(this, getString(R.string.err_empty_organizer), Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate: phải có ít nhất 1 ticket type
        if (draftTicketTypes.isEmpty()) {
            // Chuyển về Step 4
            currentStep = 3;
            viewFlipper.setDisplayedChild(currentStep);
            updateStepperUI();
            Toast.makeText(this, "Vui lòng thêm ít nhất một loại vé", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    // ── Save Event ────────────────────────────────────────────────────────────
    private void saveEvent(String status) {
        if (!validateForm(status)) return;

        String title       = etTitle != null && etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        if (title.isEmpty() && "draft".equals(status))
            title = getString(R.string.default_event_title_draft);

        String description = etDescription != null && etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String venueName   = etVenueName   != null && etVenueName.getText() != null   ? etVenueName.getText().toString().trim()   : "";
        String venueAddr   = etVenueAddress!= null && etVenueAddress.getText() != null? etVenueAddress.getText().toString().trim(): "";
        String venueCity   = etVenueCity   != null && etVenueCity.getText() != null   ? etVenueCity.getText().toString().trim()   : "";
        String startTime   = etStartTime   != null && etStartTime.getText() != null   ? etStartTime.getText().toString()          : "";
        String endTime     = etEndTime     != null && etEndTime.getText() != null     ? etEndTime.getText().toString()            : "";

        boolean isNewOrgMode = rgOrganizerMode != null
                && rgOrganizerMode.getCheckedRadioButtonId() == R.id.rbNewOrganizer;

        if (isNewOrgMode && etNewOrgName != null && etNewOrgName.getText() != null) {
            String newOrgName    = etNewOrgName.getText().toString().trim();
            String newOrgEmail   = etNewOrgEmail   != null && etNewOrgEmail.getText() != null   ? etNewOrgEmail.getText().toString().trim()   : "";
            String newOrgPhone   = etNewOrgPhone   != null && etNewOrgPhone.getText() != null   ? etNewOrgPhone.getText().toString().trim()   : "";
            String newOrgWebsite = etNewOrgWebsite != null && etNewOrgWebsite.getText() != null ? etNewOrgWebsite.getText().toString().trim() : "";

            if (!"draft".equals(status) && (newOrgName.isEmpty() || newOrgEmail.isEmpty() || newOrgPhone.isEmpty())) {
                Toast.makeText(this, getString(R.string.err_empty_new_org_info), Toast.LENGTH_SHORT).show();
                return;
            }
            // E3: kiểm tra định dạng email ban tổ chức
            if (!newOrgEmail.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(newOrgEmail).matches()) {
                Toast.makeText(this, "Email ban tổ chức không đúng định dạng", Toast.LENGTH_SHORT).show();
                if (etNewOrgEmail != null) etNewOrgEmail.requestFocus();
                return;
            }
            if (!"draft".equals(status) || !newOrgName.isEmpty()) {
                createNewOrganizerAndSave(newOrgName, newOrgEmail, newOrgPhone, newOrgWebsite,
                        title, description, venueName, venueAddr, venueCity, startTime, endTime, status);
                return;
            }
        } else {
            if (!"draft".equals(status) && selectedOrganizer == null) {
                Toast.makeText(this, getString(R.string.err_empty_saved_org), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        continueSaveFlow(title, description, venueName, venueAddr, venueCity, startTime, endTime, status);
    }

    private void createNewOrganizerAndSave(String name, String email, String phone, String website,
                                           String title, String desc, String venueName, String venueAddr,
                                           String venueCity, String startTime, String endTime, String status) {
        setButtonsEnabled(false);
        String userId = getCurrentUserId();
        String newOrgId = UUID.randomUUID().toString();
        Organizer org = new Organizer();
        org.setOrganizerId(newOrgId);
        org.setUserId(userId != null ? userId : "");
        org.setBrandName(name);
        org.setContactEmail(email);
        org.setContactPhone(phone);
        org.setWebsiteUrl(website);
        org.setVerified(false);

        db.collection(FirebaseCollections.ORGANIZERS).document(newOrgId).set(org)
                .addOnSuccessListener(v -> {
                    selectedOrganizer = org;
                    continueSaveFlow(title, desc, venueName, venueAddr, venueCity, startTime, endTime, status);
                })
                .addOnFailureListener(e -> {
                    setButtonsEnabled(true);
                    Toast.makeText(this, "Lỗi tạo Ban tổ chức: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void continueSaveFlow(String title, String desc, String venueName, String venueAddr,
                                  String venueCity, String startTime, String endTime, String status) {
        if (selectedPosterBytes != null) {
            uploadPosterThenSave(title, desc, venueName, venueAddr, venueCity, startTime, endTime, status);
        } else {
            String existingPoster = editingEvent != null ? editingEvent.getPosterUrl() : null;
            buildAndSaveEvent(title, desc, venueName, venueAddr, venueCity, startTime, endTime, status, existingPoster);
        }
    }

    private void uploadPosterThenSave(String title, String desc, String venueName, String venueAddr,
                                      String venueCity, String startTime, String endTime, String status) {
        setButtonsEnabled(false);
        String userId = getCurrentUserId();
        StorageReference ref = storage.getReference()
                .child("event_posters/" + (userId != null ? userId : "unknown") + "/" + UUID.randomUUID() + ".jpg");
        com.google.firebase.storage.StorageMetadata metadata = new com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg").build();
        ref.putBytes(selectedPosterBytes, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> buildAndSaveEvent(title, desc, venueName, venueAddr, venueCity,
                        startTime, endTime, status, uri.toString()))
                .addOnFailureListener(e -> {
                    setButtonsEnabled(true);
                    Toast.makeText(this, "Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void buildAndSaveEvent(String title, String desc, String venueName, String venueAddr,
                                   String venueCity, String startTime, String endTime, String status, String posterUrl) {
        String eventId = existingEventId != null ? existingEventId : UUID.randomUUID().toString();
        String venueId = (editingEvent != null && editingEvent.getVenueId() != null
                && !editingEvent.getVenueId().isEmpty())
                ? editingEvent.getVenueId() : UUID.randomUUID().toString();

        Map<String, Object> venueMap = new HashMap<>();
        venueMap.put("name", venueName);
        venueMap.put("address", venueAddr);
        venueMap.put("city", venueCity);

        String finalEventId = eventId;
        String finalVenueId = venueId;
        db.collection(FirebaseCollections.VENUES).document(venueId).set(venueMap)
                .addOnSuccessListener(v ->
                        saveEventObject(finalEventId, finalVenueId, title, desc, venueName, venueAddr, venueCity,
                                startTime, endTime, status, posterUrl))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi lưu địa điểm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setButtonsEnabled(true);
                });
    }

    private void saveEventObject(String eventId, String venueId, String title, String desc,
                                 String venueName, String venueAddr, String venueCity,
                                 String startTime, String endTime, String status, String posterUrl) {
        String organizerId = selectedOrganizer != null ? selectedOrganizer.getOrganizerId()
                : sessionManager.getActiveOrganizerId();
        String userId = getCurrentUserId();
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        Event event = new Event();
        event.setEventId(eventId);
        event.setVenueId(venueId);
        event.setOrganizerId(organizerId);
        event.setUserId(userId != null ? userId : "");
        event.setTitle(title);
        event.setDescription(desc);
        event.setVenueName(venueName);
        event.setVenueAddress(venueAddr);
        event.setVenueCity(venueCity);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setPosterUrl(posterUrl);
        if (selectedCategory != null) event.setCategoryId(selectedCategory.getCategoryId());
        else if (editingEvent != null) event.setCategoryId(editingEvent.getCategoryId());
        if (existingEventId == null) event.setCreatedAt(now);

        // Save new properties
        if (swIsFree != null) {
            event.setFree(swIsFree.isChecked());
        }
        if (ddEventType != null) {
            String selectedType = ddEventType.getText().toString().trim();
            if (selectedType.toLowerCase().contains("online") || selectedType.toLowerCase().contains("tuyến")) {
                event.setEventType("online");
                if (etOnlineLink != null && etOnlineLink.getText() != null) {
                    event.setOnlineLink(etOnlineLink.getText().toString().trim());
                }
            } else {
                event.setEventType("offline");
                event.setOnlineLink(null);
            }
        }
        if (ddAgeRestriction != null) {
            event.setAgeRestriction(ddAgeRestriction.getText().toString().trim());
        }
        if (etMaxTicketsPerTransaction != null && etMaxTicketsPerTransaction.getText() != null) {
            try {
                int maxTickets = Integer.parseInt(etMaxTicketsPerTransaction.getText().toString().trim());
                event.setMaxTicketsPerTransaction(maxTickets);
            } catch (Exception ignored) {}
        }

        // Xác định status cuối cùng
        String finalStatus = status;
        if ("pending".equals(status) && selectedOrganizer != null && selectedOrganizer.isVerified()) {
            finalStatus = "approved";
        }
        event.setStatusStr(finalStatus);

        // Copy rejection details if saving as draft, clear them if submitting for approval
        if ("draft".equals(status) && editingEvent != null) {
            event.setRejectionReason(editingEvent.getRejectionReason());
            event.setRejectedAt(editingEvent.getRejectedAt());
        } else if ("pending".equals(status)) {
            event.setRejectionReason(null);
            event.setRejectedAt(null);
        }

        // Tính min_price từ ticket types
        if (!draftTicketTypes.isEmpty()) {
            long minPrice = Long.MAX_VALUE;
            for (TicketType tt : draftTicketTypes) {
                if (tt.getPrice() < minPrice) minPrice = tt.getPrice();
            }
            event.setMinPrice(minPrice == Long.MAX_VALUE ? 0 : minPrice);
        }

        boolean wasPending  = editingEvent != null && "pending".equals(editingEvent.getStatusStr());
        boolean isNowApproved = "approved".equals(finalStatus);
        boolean triggerApproved = wasPending && isNowApproved;
        boolean isNewEvent = existingEventId == null;
        String finalFinalStatus = finalStatus;

        eventRepository.createEvent(event)
                .addOnSuccessListener(v -> {
                    if (triggerApproved) {
                        com.example.vibetix.Utils.NotificationTriggerManager.triggerEventApproved(eventId, title);
                    }
                    // Save ticket types and stars in batch
                    saveTicketTypesAndStars(eventId, isNewEvent, userId, finalFinalStatus);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setButtonsEnabled(true);
                });
    }

    /**
     * Lưu ticket_types và event_stars vào Firestore.
     * Sử dụng WriteBatch để atomic write.
     */
    private void saveTicketTypesAndStars(String eventId, boolean isNewEvent, String userId, String status) {
        WriteBatch batch = db.batch();

        // Ticket types
        for (TicketType tt : draftTicketTypes) {
            String ttId = tt.getTicketTypeId() != null ? tt.getTicketTypeId() : UUID.randomUUID().toString();
            tt.setTicketTypeId(ttId);
            tt.setEventId(eventId);
            Map<String, Object> ttMap = new HashMap<>();
            ttMap.put("ticket_type_id", ttId);
            ttMap.put("event_id", eventId);
            ttMap.put("name", tt.getName());
            ttMap.put("description", tt.getDescription() != null ? tt.getDescription() : "");
            ttMap.put("price", tt.getPrice());
            ttMap.put("total_quantity", tt.getTotalQuantity());
            ttMap.put("available_quantity", tt.getAvailableQuantity() > 0 ? tt.getAvailableQuantity() : tt.getTotalQuantity());
            ttMap.put("sold_quantity", 0L);
            ttMap.put("is_active", true);
            ttMap.put("sort_order", tt.getSortOrder());
            batch.set(db.collection(FirebaseCollections.TICKET_TYPES).document(ttId), ttMap);
        }

        // Event stars
        for (Star star : draftStars) {
            String esId = UUID.randomUUID().toString();
            Map<String, Object> esMap = new HashMap<>();
            esMap.put("event_id", eventId);
            esMap.put("star_id", star.getStarId());
            esMap.put("role", "Performer");
            esMap.put("is_confirmed", true);
            esMap.put("added_at", Timestamp.now());
            esMap.put("performance_order", draftStars.indexOf(star));
            batch.set(db.collection(FirebaseCollections.EVENT_STARS).document(esId), esMap);
        }

        batch.commit()
                .addOnSuccessListener(v -> {
                    if (isNewEvent) {
                        // Tạo event_staff owner record
                        String staffId = UUID.randomUUID().toString();
                        com.example.vibetix.Models.EventStaff ownerStaff = new com.example.vibetix.Models.EventStaff();
                        ownerStaff.setStaffId(staffId);
                        ownerStaff.setEventId(eventId);
                        ownerStaff.setUserId(userId != null ? userId : "");
                        ownerStaff.setRoleStr("owner");
                        ownerStaff.setAssignedAt(Timestamp.now());
                        ownerStaff.setActive(true);
                        db.collection(FirebaseCollections.EVENT_STAFF).document(staffId).set(ownerStaff)
                                .addOnSuccessListener(d -> finishSaving(status))
                                .addOnFailureListener(e -> finishSaving(status));
                    } else {
                        finishSaving(status);
                    }
                })
                .addOnFailureListener(e -> {
                    // Event đã lưu rồi — chỉ báo lỗi nhẹ
                    Toast.makeText(this, "Lưu loại vé/nghệ sĩ lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finishSaving(status);
                });
    }

    private void finishSaving(String status) {
        String msg = "draft".equals(status) ? "Đã lưu nháp!" : "Đã gửi duyệt!";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    // ── Danh sách 34 tỉnh/thành phố sau sáp nhập 2025 ──────────────────────────
    private void loadProvinces() {
        List<String> provinces = new ArrayList<>(java.util.Arrays.asList(
                "Hà Nội",
                "Hồ Chí Minh",
                "Hải Phòng",
                "Đà Nẵng",
                "Cần Thơ",
                "Vĩnh Phúc - Phú Thọ",
                "Hà Giang - Tuyên Quang",
                "Cao Bằng - Bắc Kạn",
                "Lào Cai - Yên Bái",
                "Điện Biên - Lai Châu",
                "Sơn La - Hòa Bình",
                "Thái Nguyên - Bắc Giang - Lạng Sơn",
                "Quảng Ninh",
                "Bắc Ninh - Hưng Yên",
                "Hải Dương",
                "Thái Bình - Hà Nam",
                "Nam Định - Ninh Bình",
                "Thanh Hóa",
                "Nghệ An - Hà Tĩnh",
                "Quảng Bình - Quảng Trị",
                "Thừa Thiên Huế",
                "Quảng Nam - Đà Nẵng",
                "Quảng Ngãi - Bình Định",
                "Phú Yên - Khánh Hòa",
                "Ninh Thuận - Bình Thuận",
                "Kon Tum - Gia Lai",
                "Đắk Lắk - Đắk Nông",
                "Lâm Đồng - Bình Phước",
                "Bình Dương",
                "Đồng Nai - Bà Rịa Vũng Tàu",
                "Long An - Tiền Giang",
                "Bến Tre - Vĩnh Long - Trà Vinh",
                "Đồng Tháp - An Giang - Kiên Giang",
                "Cà Mau - Bạc Liêu - Sóc Trăng - Hậu Giang"
        ));
        if (etVenueCity == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, provinces);
        etVenueCity.setAdapter(adapter);
    }

    // ── Required field red * hints ────────────────────────────────────────────
    private void markRequiredHints() {
        markRequired(tilTitle,      "Tiêu đề sự kiện");
        markRequired(tilCategory,   "Chọn danh mục");
        markRequired(tilStartTime,  "Thời gian bắt đầu");
        markRequired(tilVenueName,  "Tên địa điểm");
        markRequired(tilVenueCity,  "Thành phố / Tỉnh");
        markRequired(tilNewOrgName,  "Tên Ban tổ chức");
        markRequired(tilNewOrgEmail, "Email liên hệ");
        markRequired(tilNewOrgPhone, "Số điện thoại");
    }

    private void markRequired(TextInputLayout til, String label) {
        if (til == null) return;
        android.text.SpannableString span = new android.text.SpannableString(label + " *");
        span.setSpan(
                new android.text.style.ForegroundColorSpan(android.graphics.Color.RED),
                span.length() - 1, span.length(),
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        til.setHint(span);
    }

    // Auto-clear field error as soon as user edits the field
    private void setupErrorClearWatchers() {
        attachClearWatcher(etTitle,      tilTitle);
        attachClearWatcher(etStartTime,  tilStartTime);
        attachClearWatcher(etEndTime,    tilEndTime);
        attachClearWatcher(etVenueName,  tilVenueName);
        attachClearWatcher(etVenueCity,  tilVenueCity);
        attachClearWatcher(etNewOrgName,  tilNewOrgName);
        attachClearWatcher(etNewOrgEmail, tilNewOrgEmail);
        attachClearWatcher(etNewOrgPhone, tilNewOrgPhone);
        // Category dropdown — clear error on item selected
        if (ddCategory != null) {
            ddCategory.setOnItemClickListener((parent, view, pos, id) -> {
                if (tilCategory != null) { tilCategory.setError(null); tilCategory.setErrorEnabled(false); }
            });
        }
        // VenueCity dropdown
        if (etVenueCity != null) {
            etVenueCity.setOnItemClickListener((parent, view, pos, id) -> {
                if (tilVenueCity != null) { tilVenueCity.setError(null); tilVenueCity.setErrorEnabled(false); }
            });
        }
    }

    private void attachClearWatcher(android.widget.EditText et, TextInputLayout til) {
        if (et == null || til == null) return;
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                til.setError(null);
                til.setErrorEnabled(false);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String getCurrentUserId() {
        if (sessionManager.getUserDetails() != null
                && sessionManager.getUserDetails().getUserId() != null)
            return sessionManager.getUserDetails().getUserId();
        com.google.firebase.auth.FirebaseUser u =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    private void setButtonsEnabled(boolean enabled) {
        if (btnSaveDraft != null)        btnSaveDraft.setEnabled(enabled);
        if (btnSubmitForApproval != null) btnSubmitForApproval.setEnabled(enabled);
    }

    // ── Back press ────────────────────────────────────────────────────────────
    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    private void handleBackPress() {
        if (isReadOnly || (existingEventId != null && editingEvent != null
                && !"draft".equals(editingEvent.getStatusStr()))) {
            finish();
            return;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_save_draft_title))
                .setMessage(getString(R.string.dialog_save_draft_msg))
                .setPositiveButton(getString(R.string.btn_save_draft), (dialog, which) -> {
                    if (etTitle != null && etTitle.getText() != null
                            && etTitle.getText().toString().trim().isEmpty()) {
                        etTitle.setText(getString(R.string.default_event_title_draft));
                    }
                    saveEvent("draft");
                })
                .setNegativeButton(getString(R.string.btn_skip), (dialog, which) -> finish())
                .setNeutralButton(getString(R.string.btn_cancel), null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackPress();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
