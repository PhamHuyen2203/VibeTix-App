package com.example.vibetix.Fragments.User;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.vibetix.Adapters.EventAdapter;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.EventRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.example.vibetix.Firebase.FirebaseCollections;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EventDetailFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";
    private String eventId = "";
    private final EventRepository eventRepository = new EventRepository();
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    // Views
    private ImageView btnDetailBack;
    private ImageView btnDetailFavorite;
    private ImageView imvDetailEventCover;
    private TextView txtDetailEventTitle;
    private TextView txtDetailEventDate;
    private TextView txtDetailEventLocation;
    private TextView txtDetailOrganizer;
    private TextView txtDetailCategory;
    private TextView txtDetailInterest;
    private WebView webDetailDescription;
    private View viewDescriptionGradient;
    private FrameLayout layoutDescriptionContainer;
    private TextView txtDetailMinPrice;
    private TextView txtDescriptionToggle;
    private TextView txtScheduleTime;
    private LinearLayout layoutScheduleContent;
    private View layoutOrganizerCard;
    private ImageView imvOrganizerAvatar;
    private TextView txtOrganizerCardName;
    private ImageView imvOrganizerVerified;
    private Button btnBookTickets;
    // Extra info rows
    private LinearLayout rowEventType, rowAgeRestriction;
    private TextView txtDetailEventType, txtDetailAgeRestriction;

    private boolean isDescriptionExpanded = false;
    private boolean isFavorited = false;
    private String organizerId = "";
    private int currentInterestCount = 0;
    private String userId = "";

    // Related events section
    private LinearLayout layoutRelatedEvents;
    private RecyclerView rvRelatedEvents;
    private TextView txtRelatedSeeMore;
    private final List<Event> relatedEventsList = new ArrayList<>();
    private EventAdapter relatedEventsAdapter;

    public static EventDetailFragment newInstance(String eventId) {
        EventDetailFragment fragment = new EventDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_detail, container, false);
        bindViews(view);
        applyInsets(view);
        loadEventDetails();
        setupClickListeners(view);
        return view;
    }

    private void bindViews(View view) {
        btnDetailBack = view.findViewById(R.id.btnDetailBack);
        btnDetailFavorite = view.findViewById(R.id.btnDetailFavorite);
        imvDetailEventCover = view.findViewById(R.id.imvDetailEventCover);
        txtDetailEventTitle = view.findViewById(R.id.txtDetailEventTitle);
        txtDetailEventDate = view.findViewById(R.id.txtDetailEventDate);
        txtDetailEventLocation = view.findViewById(R.id.txtDetailEventLocation);
        txtDetailOrganizer = view.findViewById(R.id.txtDetailOrganizer);
        txtDetailCategory = view.findViewById(R.id.txtDetailCategory);
        txtDetailInterest = view.findViewById(R.id.txtDetailInterest);
        webDetailDescription = view.findViewById(R.id.webDetailDescription);
        viewDescriptionGradient = view.findViewById(R.id.viewDescriptionGradient);
        layoutDescriptionContainer = view.findViewById(R.id.layoutDescriptionContainer);
        txtDescriptionToggle = view.findViewById(R.id.txtDescriptionToggle);
        txtScheduleTime = view.findViewById(R.id.txtScheduleTime);
        layoutScheduleContent = view.findViewById(R.id.layoutScheduleContent);
        layoutOrganizerCard = view.findViewById(R.id.layoutOrganizerCard);
        imvOrganizerAvatar = view.findViewById(R.id.imvOrganizerAvatar);
        txtOrganizerCardName = view.findViewById(R.id.txtOrganizerCardName);
        imvOrganizerVerified = view.findViewById(R.id.imvOrganizerVerified);
        txtDetailMinPrice = view.findViewById(R.id.txtDetailMinPrice);
        btnBookTickets = view.findViewById(R.id.btnBookTickets);
        rowEventType            = view.findViewById(R.id.rowEventType);
        rowAgeRestriction       = view.findViewById(R.id.rowAgeRestriction);
        txtDetailEventType      = view.findViewById(R.id.txtDetailEventType);
        txtDetailAgeRestriction = view.findViewById(R.id.txtDetailAgeRestriction);

        layoutRelatedEvents = view.findViewById(R.id.layoutRelatedEvents);
        rvRelatedEvents = view.findViewById(R.id.rvRelatedEvents);
        txtRelatedSeeMore = view.findViewById(R.id.txtRelatedSeeMore);

        relatedEventsAdapter = new EventAdapter(requireContext(), relatedEventsList,
                event -> openRelatedEvent(event));
        rvRelatedEvents.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRelatedEvents.setAdapter(relatedEventsAdapter);
    }

    private void applyInsets(View view) {
        View header = view.findViewById(R.id.layoutDetailHeader);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(0, topInset, 0, 0);
                return insets;
            });
        }
    }

    private void loadEventDetails() {
        if (eventId.isEmpty()) return;

        // Restore user ID for wishlist
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = currentUser != null ? currentUser.getUid() : "";

        checkWishlistStatus();

        // Ensure venue cache is loaded before fetching event details
        // so that docToEvent can resolve full venue address
        ensureVenueCacheAndLoadEvent();
    }

    /**
     * Đảm bảo venue cache đã sẵn sàng trước khi fetch event.
     * Nếu VENUE_CACHE đã có dữ liệu (homepage đã load trước), skip bước này.
     */
    private void ensureVenueCacheAndLoadEvent() {
        if (!com.example.vibetix.Firebase.FirestoreHelper.VENUE_CACHE.isEmpty()) {
            // Venue cache đã sẵn sàng
            fetchEventFromRepository();
            return;
        }

        // Load venues trước rồi mới fetch event
        FirebaseFirestore.getInstance().collection("venues").get()
                .addOnSuccessListener(venueSnap -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot vd : venueSnap) {
                        String vid  = vd.getString("venue_id");
                        String city = vd.getString("city");
                        String name = vd.getString("name");
                        String addr = vd.getString("address");
                        if (vid != null && city != null)
                            com.example.vibetix.Firebase.FirestoreHelper.VENUE_CACHE.put(vid, city);
                        if (vid != null && name != null)
                            com.example.vibetix.Firebase.FirestoreHelper.VENUE_NAME_CACHE.put(vid, name);
                        if (vid != null && addr != null)
                            com.example.vibetix.Firebase.FirestoreHelper.VENUE_ADDRESS_CACHE.put(vid, addr);
                    }
                    if (isAdded()) fetchEventFromRepository();
                })
                .addOnFailureListener(e -> {
                    // Vẫn fetch event dù venue cache fail
                    if (isAdded()) fetchEventFromRepository();
                });
    }

    private void fetchEventFromRepository() {
        eventRepository.getEventById(eventId, new EventRepository.OnEventLoadedListener() {
            @Override
            public void onSuccess(Event event) {
                if (!isAdded() || event == null) return;
                populateEventUI(event);
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.str_toast_load_event_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Hiển thị toàn bộ thông tin sự kiện lên UI.
     */
    private void populateEventUI(Event event) {
        // Báo cho Accessibility Assistant biết sự kiện đang xem
        // (để lệnh "đọc chi tiết" / "mô tả poster" / "đặt vé" hoạt động
        //  cả khi user tự bấm mở sự kiện thay vì dùng giọng nói)
        if (getActivity() instanceof com.example.vibetix.Activities.User.UserMainActivity) {
            com.example.vibetix.Accessibility.AccessibilityAssistantManager mgr =
                    ((com.example.vibetix.Activities.User.UserMainActivity) getActivity())
                            .getAssistantManager();
            if (mgr != null) mgr.setCurrentEvent(event);
        }

        // ── Title ───────────────────────────────────────────────────────
        txtDetailEventTitle.setText(event.getTitle());

        // ── Date range ──────────────────────────────────────────────────
        String fullDate = event.getDate() != null ? event.getDate() : "";
        if (event.getEndDate() != null && !event.getEndDate().isEmpty()
                && !event.getEndDate().equals(event.getDate())) {
            fullDate += " - " + event.getEndDate();
        }
        txtDetailEventDate.setText(fullDate);

        // ── Event type (Offline / Online) ─────────────────────────────
        String evType = event.getEventType();
        boolean isOnline = evType != null && (evType.toLowerCase().contains("online")
                || evType.toLowerCase().contains("tuyến"));
        if (evType != null && !evType.isEmpty()) {
            if (rowEventType != null) rowEventType.setVisibility(View.VISIBLE);
            if (txtDetailEventType != null) txtDetailEventType.setText(getString(R.string.str_event_format_label, evType));
        }
        // ── Location — online event → hiện "Sự kiện trực tuyến" ────────
        String displayLocation;
        if (isOnline) {
            displayLocation = "Sự kiện trực tuyến";
        } else {
            displayLocation = event.getLocation();
            if (displayLocation == null || displayLocation.isEmpty()) {
                displayLocation = event.getVenueCity();
            }
            if (displayLocation == null || displayLocation.isEmpty()) {
                displayLocation = "Việt Nam";
            }
        }
        txtDetailEventLocation.setText(displayLocation);

        // ── Age restriction ────────────────────────────────────────────
        String ageRestriction = event.getAgeRestriction();
        if (ageRestriction != null && !ageRestriction.isEmpty()
                && !ageRestriction.equalsIgnoreCase("Không giới hạn")) {
            if (rowAgeRestriction != null) rowAgeRestriction.setVisibility(View.VISIBLE);
            if (txtDetailAgeRestriction != null)
                txtDetailAgeRestriction.setText(getString(R.string.str_age_restriction_label, ageRestriction));
        }

        // ── Description (render HTML in WebView) ────────────────────────
        if (webDetailDescription != null && event.getDescription() != null && !event.getDescription().isEmpty()) {
            String css = "<style>body{font-family:sans-serif;font-size:14px;color:#666;line-height:1.6;margin:0;padding:0;} img{max-width:100%;height:auto;} h2{font-size:18px;color:#333;} iframe{display:none;}</style>";
            String html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" + css + "</head><body>" + event.getDescription() + "</body></html>";
            webDetailDescription.getSettings().setJavaScriptEnabled(false);
            webDetailDescription.getSettings().setLoadsImagesAutomatically(true);
            webDetailDescription.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            webDetailDescription.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        }
        setupDescriptionToggle();

        // ── Category Name ───────────────────────────────────────────────
        String catDisplayName = resolveCategoryName(event.getCategory());
        txtDetailCategory.setText(catDisplayName);

        // ── Interest Count ──────────────────────────────────────────────
        currentInterestCount = event.getInterestCount();
        updateInterestUI();

        // ── Price range ─────────────────────────────────────────────────
        populatePriceUI(event);

        // ── Organizer ───────────────────────────────────────────────────
        populateOrganizerUI(event);

        // ── Schedule / Ticket Types ─────────────────────────────────────
        if (txtScheduleTime != null) {
            txtScheduleTime.setText(fullDate);
        }
        loadTicketTypes();

        // ── Cover Image ─────────────────────────────────────────────────
        populateCoverImage(event);

        // ── Trạng thái sự kiện → booking button ────────────────────────
        String evStatus = event.getStatus() != null ? event.getStatus().toLowerCase() : "";
        if ("completed".equals(evStatus)) {
            btnBookTickets.setEnabled(false);
            btnBookTickets.setText(getString(R.string.str_event_ended));
            btnBookTickets.setAlpha(0.5f);
        } else if ("cancelled".equals(evStatus)) {
            btnBookTickets.setEnabled(false);
            btnBookTickets.setText(getString(R.string.str_event_cancelled));
            btnBookTickets.setAlpha(0.5f);
        } else if (event.isSoldOut()) {
            btnBookTickets.setEnabled(false);
            btnBookTickets.setText(getString(R.string.str_sold_out));
            btnBookTickets.setAlpha(0.5f);
        } else if ("approved".equals(evStatus) && isEventNotStartedYet(event.getStartTime())) {
            // E4: đã approved nhưng chưa tới giờ mở bán → Coming Soon
            btnBookTickets.setEnabled(false);
            btnBookTickets.setText(getString(R.string.str_coming_soon));
            btnBookTickets.setAlpha(0.7f);
        } else if ("approved".equals(evStatus) || "ongoing".equals(evStatus)) {
            btnBookTickets.setEnabled(true);
            btnBookTickets.setText(getString(R.string.str_book_tickets_now));
            btnBookTickets.setAlpha(1f);
        } else {
            // pending/draft/rejected — không nên vào được màn này nhưng vẫn guard
            btnBookTickets.setEnabled(false);
            btnBookTickets.setText(getString(R.string.str_not_open_yet));
            btnBookTickets.setAlpha(0.5f);
        }

        // Load related events after main event is populated
        loadRelatedEvents(event);
    }

    /**
     * Resolve category app-key (music, arts, ...) → tên hiển thị tiếng Việt.
     */
    private String resolveCategoryName(String categoryKey) {
        if (categoryKey == null || categoryKey.isEmpty()) return "Sự kiện";

        // Thử match từ category UUID trực tiếp (trường hợp Firestore giữ nguyên UUID)
        String directName = com.example.vibetix.Firebase.FirestoreHelper.CAT_NAME_MAP.get(categoryKey);
        if (directName != null) return directName;

        // Thử match từ app-key (music, arts, workshop, ...)
        for (Map.Entry<String, String> entry : com.example.vibetix.Firebase.FirestoreHelper.CAT_MAP.entrySet()) {
            if (entry.getValue().equals(categoryKey)) {
                String name = com.example.vibetix.Firebase.FirestoreHelper.CAT_NAME_MAP.get(entry.getKey());
                if (name != null) return name;
            }
        }

        // Fallback cho các mock event categories cũ
        switch (categoryKey) {
            case "live-music": return "Nhạc sống";
            case "stage-arts": return "Sân khấu & Nghệ thuật";
            case "workshop":   return "Hội thảo & Workshop";
            case "tour":       return "Tham quan & Trải nghiệm";
            case "sports":     return "Thể thao";
            case "festival":   return "Khác/Lễ hội";
            default:           return "Sự kiện";
        }
    }

    /**
     * Hiển thị giá vé: miễn phí / giá đơn / khoảng giá.
     */
    private void populatePriceUI(Event event) {
        if (event.isFree() || (event.getMinPrice() == 0 && event.getMaxPrice() == 0)) {
            txtDetailMinPrice.setText(getString(R.string.str_free_price));
        } else if (event.getMaxPrice() > event.getMinPrice()) {
            txtDetailMinPrice.setText(formatter.format(event.getMinPrice()) + " - " + formatter.format(event.getMaxPrice()) + " đ");
        } else {
            txtDetailMinPrice.setText(formatter.format(event.getMinPrice()) + " đ");
        }
    }

    /**
     * Hiển thị tên ban tổ chức + organizer card.
     */
    private void populateOrganizerUI(Event event) {
        if (event.getOrganizerName() != null && !event.getOrganizerName().isEmpty()) {
            txtDetailOrganizer.setText(event.getOrganizerName());
            if (txtOrganizerCardName != null) txtOrganizerCardName.setText(event.getOrganizerName());
        } else {
            txtDetailOrganizer.setText("Ban tổ chức VibeTix");
            if (txtOrganizerCardName != null) txtOrganizerCardName.setText("Ban tổ chức VibeTix");
        }
        resolveOrganizerFromFirestore();
    }

    /**
     * Thử lấy organizer_name từ event document → organizer_id → organizers collection.
     * Cũng populate organizer card (avatar, verified badge).
     */
    private void resolveOrganizerFromFirestore() {
        if (eventId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection(FirebaseCollections.EVENTS)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null || !doc.exists()) return;

                    // Thử lấy organizer_name trực tiếp
                    String orgName = doc.getString("organizer_name");
                    if (orgName != null && !orgName.isEmpty()) {
                        txtDetailOrganizer.setText(orgName);
                        if (txtOrganizerCardName != null) txtOrganizerCardName.setText(orgName);
                    }

                    // Resolve organizer card từ organizer_id
                    String orgId = doc.getString("organizer_id");
                    if (orgId != null && !orgId.isEmpty()) {
                        organizerId = orgId;
                        FirebaseFirestore.getInstance()
                                .collection(FirebaseCollections.ORGANIZERS)
                                .document(orgId)
                                .get()
                                .addOnSuccessListener(orgDoc -> {
                                    if (!isAdded() || orgDoc == null || !orgDoc.exists()) return;
                                    String brandName = orgDoc.getString("brand_name");
                                    if (brandName == null) brandName = orgDoc.getString("name");
                                    if (brandName != null && !brandName.isEmpty()) {
                                        txtDetailOrganizer.setText(brandName);
                                        if (txtOrganizerCardName != null) txtOrganizerCardName.setText(brandName);
                                    }
                                    // Avatar — hỗ trợ Base64
                                    String logoUrl = orgDoc.getString("logo_url");
                                    if (imvOrganizerAvatar != null) {
                                        com.example.vibetix.Utils.ImageUtils.loadCircle(
                                                this, logoUrl, imvOrganizerAvatar, R.drawable.img_mascot_wave);
                                    }
                                    // Verified badge
                                    Boolean verified = orgDoc.getBoolean("is_verified");
                                    if (imvOrganizerVerified != null) {
                                        imvOrganizerVerified.setVisibility(
                                                Boolean.TRUE.equals(verified) ? View.VISIBLE : View.GONE);
                                    }
                                });
                    }
                });
    }

    /**
     * "Xem thêm" / "Thu gọn" toggle cho description.
     */
    private void setupDescriptionToggle() {
        if (txtDescriptionToggle == null || webDetailDescription == null || layoutDescriptionContainer == null) return;
        txtDescriptionToggle.setVisibility(View.VISIBLE);
        txtDescriptionToggle.setOnClickListener(v -> {
            isDescriptionExpanded = !isDescriptionExpanded;
            ViewGroup.LayoutParams lp = webDetailDescription.getLayoutParams();
            if (isDescriptionExpanded) {
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                txtDescriptionToggle.setText("Thu gọn ▲");
                if (viewDescriptionGradient != null) viewDescriptionGradient.setVisibility(View.GONE);
            } else {
                lp.height = (int)(200 * getResources().getDisplayMetrics().density);
                txtDescriptionToggle.setText(getString(R.string.str_see_more_expand_icon));
                if (viewDescriptionGradient != null) viewDescriptionGradient.setVisibility(View.VISIBLE);
            }
            webDetailDescription.setLayoutParams(lp);
        });
    }

    // Cache ticket types + event time info cho lịch diễn
    private final java.util.List<com.google.firebase.firestore.QueryDocumentSnapshot> cachedTicketDocs = new java.util.ArrayList<>();
    private final java.util.List<String> eventDates = new java.util.ArrayList<>(); // dd/MM/yyyy
    private String currentScheduleDate = null; // ngày đang hiển thị
    private String eventTimeRange = ""; // HH:mm - HH:mm
    private boolean isScheduleExpanded = true; // suất diễn mặc định mở sẵn

    /**
     * Load ticket types + build schedule: Ngày → Suất diễn (khung giờ) → Loại vé
     * Logic: tất cả loại vé thuộc cùng 1 sự kiện, hiện dưới 1 khung giờ
     */
    private void loadTicketTypes() {
        if (eventId.isEmpty() || layoutScheduleContent == null) return;

        // Wire icon lịch
        View v = getView();
        View btnCal = v != null ? v.findViewById(R.id.btnScheduleCalendar) : null;
        if (btnCal != null) btnCal.setOnClickListener(vv -> openScheduleCalendar());

        // Lấy ngày + giờ event từ Firestore
        FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    if (!isAdded() || eventDoc == null) return;
                    java.text.SimpleDateFormat sdfDate = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat sdfTime = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());

                    // Lấy ngày diễn từ event start_time
                    Timestamp startTs = eventDoc.getTimestamp("start_time");
                    Timestamp endTs = eventDoc.getTimestamp("end_time");
                    if (startTs != null) {
                        currentScheduleDate = sdfDate.format(startTs.toDate());
                        eventTimeRange = sdfTime.format(startTs.toDate());
                        if (endTs != null) {
                            eventTimeRange += " - " + sdfTime.format(endTs.toDate());
                        }
                    }
                    eventDates.clear();
                    if (currentScheduleDate != null) eventDates.add(currentScheduleDate);

                    // Load ticket types
                    fetchAndRenderTicketTypes();
                });
    }

    private void fetchAndRenderTicketTypes() {
        FirebaseFirestore.getInstance()
                .collection(FirebaseCollections.TICKET_TYPES)
                .whereEqualTo("event_id", eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    cachedTicketDocs.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        cachedTicketDocs.add(doc);
                    }
                    renderSchedule();
                });
    }

    /** Render lịch diễn: Ngày hiện tại → Suất diễn (dropdown khung giờ) → Tất cả loại vé */
    private void renderSchedule() {
        if (layoutScheduleContent == null) return;
        layoutScheduleContent.removeAllViews();
        float dp = getResources().getDisplayMetrics().density;

        // Hiện ngày diễn hiện tại
        if (txtScheduleTime != null && currentScheduleDate != null) {
            txtScheduleTime.setText(currentScheduleDate);
        }

        if (cachedTicketDocs.isEmpty()) return;

        // Suất diễn header (khung giờ) — clickable dropdown
        LinearLayout showRow = new LinearLayout(requireContext());
        showRow.setOrientation(LinearLayout.HORIZONTAL);
        showRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        showRow.setPadding((int)(12*dp), (int)(12*dp), (int)(12*dp), (int)(12*dp));
        showRow.setBackgroundResource(R.drawable.bg_chip_active);
        LinearLayout.LayoutParams showLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        showLp.bottomMargin = (int)(4*dp);
        showRow.setLayoutParams(showLp);

        // Chevron
        ImageView chevron = new ImageView(requireContext());
        chevron.setLayoutParams(new LinearLayout.LayoutParams((int)(18*dp), (int)(18*dp)));
        chevron.setImageResource(isScheduleExpanded ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_right);
        chevron.setColorFilter(getResources().getColor(R.color.clr_text_white));
        showRow.addView(chevron);

        // Khung giờ + ngày
        TextView txtTime = new TextView(requireContext());
        txtTime.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        String timeDisplay = "  " + eventTimeRange;
        if (currentScheduleDate != null) timeDisplay += "\n  " + currentScheduleDate;
        txtTime.setText(timeDisplay);
        txtTime.setTextSize(13f);
        txtTime.setTextColor(getResources().getColor(R.color.clr_text_white));
        txtTime.setTypeface(null, android.graphics.Typeface.BOLD);
        showRow.addView(txtTime);

        // "MUA VÉ NGAY"
        TextView btnBuy = new TextView(requireContext());
        btnBuy.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        btnBuy.setText(getString(R.string.str_buy_now_caps));
        btnBuy.setTextSize(11f);
        btnBuy.setTextColor(getResources().getColor(R.color.clr_text_white));
        btnBuy.setBackgroundResource(R.drawable.bg_resale_badge);
        btnBuy.setPadding((int)(8*dp), (int)(4*dp), (int)(8*dp), (int)(4*dp));
        btnBuy.setOnClickListener(vv -> btnBookTickets.performClick());
        showRow.addView(btnBuy);

        // Toggle
        showRow.setOnClickListener(vv -> {
            isScheduleExpanded = !isScheduleExpanded;
            renderSchedule();
        });
        layoutScheduleContent.addView(showRow);

        // Loại vé (sổ ra khi expanded)
        if (isScheduleExpanded) {
            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : cachedTicketDocs) {
                String name = doc.getString("name");
                Long price = doc.getLong("price");
                if (name == null) continue;

                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowLp.bottomMargin = (int)(4*dp);
                row.setLayoutParams(rowLp);
                row.setPadding((int)(16*dp), (int)(10*dp), (int)(12*dp), (int)(10*dp));
                row.setBackgroundResource(R.drawable.bg_search_bar);

                TextView txtName = new TextView(requireContext());
                txtName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                txtName.setText(name);
                txtName.setTextSize(14f);
                txtName.setTextColor(getResources().getColor(R.color.clr_text_black));

                TextView txtPrice = new TextView(requireContext());
                txtPrice.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                txtPrice.setText(price != null && price > 0 ? formatter.format(price) + " đ" : getString(R.string.str_free_price));
                txtPrice.setTextSize(14f);
                txtPrice.setTextColor(getResources().getColor(R.color.clr_primary_blue));
                txtPrice.setTypeface(null, android.graphics.Typeface.BOLD);

                row.addView(txtName);
                row.addView(txtPrice);
                layoutScheduleContent.addView(row);
            }
        }
    }

    /** Mở popup lịch highlight ngày diễn sự kiện */
    private void openScheduleCalendar() {
        DateFilterDialog dialog = new DateFilterDialog();
        dialog.setHighlightDates(eventDates);
        dialog.setOnDateFilterApplied((label, start, end) -> {
            if (start != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                currentScheduleDate = sdf.format(start.getTime());
                isScheduleExpanded = true;
                renderSchedule();
            }
        });
        dialog.show(getChildFragmentManager(), "schedule_calendar");
    }

    /**
     * Hiển thị ảnh bìa sự kiện.
     */
    private void populateCoverImage(Event event) {
        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(event.getImageUrl())
                    .placeholder(R.drawable.event_live_non_song)
                    .error(R.drawable.event_live_non_song)
                    .into(imvDetailEventCover);
        } else if (event.getLocalImageResId() != 0) {
            imvDetailEventCover.setImageResource(event.getLocalImageResId());
        } else {
            imvDetailEventCover.setImageResource(R.drawable.event_live_non_song);
        }
    }

    private void checkWishlistStatus() {
        if (userId.isEmpty() || eventId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("event_interests")
                .document(userId + "_" + eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded() && documentSnapshot.exists()) {
                        isFavorited = true;
                        updateFavoriteUI();
                    }
                });
    }

    private void updateFavoriteUI() {
        if (btnDetailFavorite == null) return;
        int color = isFavorited ? getResources().getColor(R.color.clr_red) : getResources().getColor(R.color.clr_grey_1);
        btnDetailFavorite.setImageTintList(ColorStateList.valueOf(color));
    }

    private void updateInterestUI() {
        if (txtDetailInterest != null) {
            txtDetailInterest.setText(getString(R.string.str_interest_count, currentInterestCount));
        }
    }

    private void toggleFavorite() {
        if (userId.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.str_toast_login_to_save), Toast.LENGTH_SHORT).show();
            return;
        }

        isFavorited = !isFavorited;
        updateFavoriteUI();

        if (isFavorited) {
            currentInterestCount++;
        } else {
            currentInterestCount = Math.max(0, currentInterestCount - 1);
        }
        updateInterestUI();

        DocumentReference docRef = FirebaseFirestore.getInstance()
                .collection("event_interests")
                .document(userId + "_" + eventId);

        DocumentReference eventRef = FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId);

        if (isFavorited) {
            Map<String, Object> data = new HashMap<>();
            data.put("user_id", userId);
            data.put("event_id", eventId);
            data.put("created_at", Timestamp.now());
            data.put("notify_on_reminder", true);
            data.put("notify_on_sale", true);
            docRef.set(data);

            // Increment interest_count on Firebase
            eventRef.update("interest_count", FieldValue.increment(1));
        } else {
            docRef.delete();

            // Decrement interest_count on Firebase
            eventRef.update("interest_count", FieldValue.increment(-1));
        }
    }

    private void setupClickListeners(View view) {
        if (btnDetailBack != null) {
            btnDetailBack.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        if (btnDetailFavorite != null) {
            btnDetailFavorite.setOnClickListener(v -> toggleFavorite());
        }

        btnBookTickets.setOnClickListener(v -> {
            // Xác minh CAPTCHA trước khi cho đặt vé
            SliderCaptchaDialogFragment captcha = SliderCaptchaDialogFragment.newInstance();
            captcha.setOnVerifiedListener(() -> {
                SelectTicketFragment selectFrag = SelectTicketFragment.newInstance(eventId);
                if (getActivity() instanceof com.example.vibetix.Activities.User.UserMainActivity) {
                    ((com.example.vibetix.Activities.User.UserMainActivity) getActivity()).openSubFragment(selectFrag);
                }
            });
            captcha.show(getChildFragmentManager(), "captcha");
        });

        // Organizer card → open StarDetailFragment (handles both stars and organizers)
        if (layoutOrganizerCard != null) {
            layoutOrganizerCard.setOnClickListener(v -> {
                if (organizerId.isEmpty()) return;
                String orgName = txtOrganizerCardName != null ? txtOrganizerCardName.getText().toString() : "";
                StarDetailFragment frag = StarDetailFragment.newInstance(organizerId, orgName);
                if (getActivity() instanceof com.example.vibetix.Activities.User.UserMainActivity) {
                    ((com.example.vibetix.Activities.User.UserMainActivity) getActivity()).openSubFragment(frag);
                }
            });
        }

        // Related events: "Xem thêm" → EventsFragment (tab Sự kiện)
        if (txtRelatedSeeMore != null) {
            txtRelatedSeeMore.setOnClickListener(v -> {
                if (getActivity() instanceof com.example.vibetix.Activities.User.UserMainActivity) {
                    ((com.example.vibetix.Activities.User.UserMainActivity) getActivity()).openEventsFragment();
                }
            });
        }
    }

    // ── Related Events ─────────────────────────────────────────────────────────

    private void loadRelatedEvents(Event currentEvent) {
        String catId = currentEvent.getCategoryId();
        String currentTitle = currentEvent.getTitle() != null ? currentEvent.getTitle().toLowerCase() : "";
        String currentCity = currentEvent.getVenueCity() != null ? currentEvent.getVenueCity().toLowerCase() : "";
        Set<String> titleTokens = tokenize(currentTitle);

        // Query events theo cùng category
        com.google.firebase.firestore.Query query = FirebaseFirestore.getInstance()
                .collection("events")
                .limit(60);
        if (catId != null && !catId.isEmpty()) {
            query = FirebaseFirestore.getInstance()
                    .collection("events")
                    .whereEqualTo("category_id", catId)
                    .limit(60);
        }

        query.get().addOnSuccessListener(snap -> {
            if (!isAdded()) return;
            List<ScoredEvent> scored = new ArrayList<>();
            List<String> ACTIVE = Arrays.asList("approved", "ongoing");
            long todayMs = com.example.vibetix.Firebase.HomepageLoader.getTodayStartMs();

            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                if (doc.getId().equals(eventId)) continue; // bỏ chính nó
                String status = doc.getString("status");
                if (status == null || !ACTIVE.contains(status)) continue;
                // Chỉ lấy event chưa kết thúc
                com.google.firebase.Timestamp ts = doc.getTimestamp("end_time");
                if (ts == null) ts = doc.getTimestamp("start_time");
                if (ts != null && ts.toDate().getTime() < todayMs) continue;

                Event e = com.example.vibetix.Firebase.FirestoreHelper.docToEvent(doc);
                if (e == null) continue;

                // Tính điểm tương đồng đơn giản
                int score = 0;
                String otherTitle = e.getTitle() != null ? e.getTitle().toLowerCase() : "";
                Set<String> otherTokens = tokenize(otherTitle);
                // Đếm token chung trong title
                Set<String> inter = new HashSet<>(titleTokens);
                inter.retainAll(otherTokens);
                score += inter.size() * 3;
                // Cùng thành phố
                String otherCity = e.getVenueCity() != null ? e.getVenueCity().toLowerCase() : "";
                if (!currentCity.isEmpty() && currentCity.equals(otherCity)) score += 2;
                scored.add(new ScoredEvent(e, score));
            }

            // Sắp xếp: score cao → thấp, cùng score → ngày gần nhất
            Collections.sort(scored, (a, b) -> {
                if (b.score != a.score) return b.score - a.score;
                String da = a.event.getDate() != null ? a.event.getDate() : "";
                String db2 = b.event.getDate() != null ? b.event.getDate() : "";
                String ka = da.length() == 10 ? da.substring(6)+da.substring(3,5)+da.substring(0,2) : da;
                String kb = db2.length() == 10 ? db2.substring(6)+db2.substring(3,5)+db2.substring(0,2) : db2;
                return ka.compareTo(kb);
            });

            relatedEventsList.clear();
            for (int i = 0; i < Math.min(10, scored.size()); i++) {
                relatedEventsList.add(scored.get(i).event);
            }
            relatedEventsAdapter.notifyDataSetChanged();

            if (layoutRelatedEvents != null) {
                layoutRelatedEvents.setVisibility(relatedEventsList.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null || text.isEmpty()) return tokens;
        // Tách theo khoảng trắng và dấu câu, bỏ stopwords ngắn
        for (String word : text.split("[\\s\\[\\]()\\-:,./]+")) {
            if (word.length() > 2) tokens.add(word);
        }
        return tokens;
    }

    private static class ScoredEvent {
        final Event event;
        final int score;
        ScoredEvent(Event event, int score) { this.event = event; this.score = score; }
    }

    private void openRelatedEvent(Event event) {
        if (event == null || event.getId() == null) return;
        EventDetailFragment frag = EventDetailFragment.newInstance(event.getId());
        if (getActivity() instanceof com.example.vibetix.Activities.User.UserMainActivity) {
            ((com.example.vibetix.Activities.User.UserMainActivity) getActivity()).openSubFragment(frag);
        }
    }

    /** E4: true nếu startTime (string "dd/MM/yyyy HH:mm") vẫn còn trong tương lai */
    private boolean isEventNotStartedYet(String startTimeStr) {
        if (startTimeStr == null || startTimeStr.trim().isEmpty()) return false;
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            java.util.Date start = sdf.parse(startTimeStr);
            return start != null && start.after(new java.util.Date());
        } catch (Exception e) {
            return false;
        }
    }
}
