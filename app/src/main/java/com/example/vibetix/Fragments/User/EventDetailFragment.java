package com.example.vibetix.Fragments.User;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.EventRepository;
import com.example.vibetix.Utils.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

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
    private TextView txtDetailEventDescription;
    private TextView txtDetailMinPrice;
    private Button btnBookTickets;

    private boolean isFavorited = false;
    private int currentInterestCount = 0;
    private String userEmail = "";

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
        txtDetailEventDescription = view.findViewById(R.id.txtDetailEventDescription);
        txtDetailMinPrice = view.findViewById(R.id.txtDetailMinPrice);
        btnBookTickets = view.findViewById(R.id.btnBookTickets);
    }

    private void applyInsets(View view) {
        View header = view.findViewById(R.id.layoutDetailHeader);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    private void loadEventDetails() {
        if (eventId.isEmpty()) return;

        // Restore user email for wishlist
        SharedPreferences prefs = requireContext().getSharedPreferences(Constants.PREFS_AUTH, Context.MODE_PRIVATE);
        userEmail = prefs.getString(Constants.KEY_USER_EMAIL, "");

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
                    Toast.makeText(requireContext(), "Lỗi tải sự kiện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Hiển thị toàn bộ thông tin sự kiện lên UI.
     */
    private void populateEventUI(Event event) {
        // ── Title ───────────────────────────────────────────────────────
        txtDetailEventTitle.setText(event.getTitle());

        // ── Date range ──────────────────────────────────────────────────
        String fullDate = event.getDate() != null ? event.getDate() : "";
        if (event.getEndDate() != null && !event.getEndDate().isEmpty()
                && !event.getEndDate().equals(event.getDate())) {
            fullDate += " - " + event.getEndDate();
        }
        txtDetailEventDate.setText(fullDate);

        // ── Location — ưu tiên full location, fallback về venueCity/city ─
        String displayLocation = event.getLocation();
        if (displayLocation == null || displayLocation.isEmpty()) {
            displayLocation = event.getVenueCity();
        }
        if (displayLocation == null || displayLocation.isEmpty()) {
            displayLocation = "Việt Nam";
        }
        txtDetailEventLocation.setText(displayLocation);

        // ── Description ─────────────────────────────────────────────────
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            txtDetailEventDescription.setText(event.getDescription());
        } else {
            txtDetailEventDescription.setText("Chưa có mô tả cho sự kiện này.");
        }

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

        // ── Cover Image ─────────────────────────────────────────────────
        populateCoverImage(event);

        // ── Sold Out → disable booking ──────────────────────────────────
        if (event.isSoldOut()) {
            btnBookTickets.setEnabled(false);
            btnBookTickets.setText("Đã hết vé");
            btnBookTickets.setAlpha(0.5f);
        } else {
            btnBookTickets.setEnabled(true);
            btnBookTickets.setText("Đặt vé ngay");
            btnBookTickets.setAlpha(1f);
        }
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
            txtDetailMinPrice.setText("Miễn phí");
        } else if (event.getMaxPrice() > event.getMinPrice()) {
            txtDetailMinPrice.setText(formatter.format(event.getMinPrice()) + " - " + formatter.format(event.getMaxPrice()) + " đ");
        } else {
            txtDetailMinPrice.setText(formatter.format(event.getMinPrice()) + " đ");
        }
    }

    /**
     * Hiển thị tên ban tổ chức.
     * Nếu event chưa có organizer_name, thử resolve từ Firestore organizers collection.
     */
    private void populateOrganizerUI(Event event) {
        if (event.getOrganizerName() != null && !event.getOrganizerName().isEmpty()) {
            txtDetailOrganizer.setText(event.getOrganizerName());
        } else {
            // Fallback: hiển thị tên mặc định ngay,
            // đồng thời thử resolve từ Firestore nếu có organizer_id
            txtDetailOrganizer.setText("Ban tổ chức VibeTix");
            resolveOrganizerFromFirestore();
        }
    }

    /**
     * Thử lấy organizer_name từ event document → organizer_id → organizers collection.
     */
    private void resolveOrganizerFromFirestore() {
        if (eventId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null || !doc.exists()) return;

                    // Thử lấy organizer_name trực tiếp
                    String orgName = doc.getString("organizer_name");
                    if (orgName != null && !orgName.isEmpty()) {
                        txtDetailOrganizer.setText(orgName);
                        return;
                    }

                    // Thử resolve từ organizer_id
                    String organizerId = doc.getString("organizer_id");
                    if (organizerId != null && !organizerId.isEmpty()) {
                        FirebaseFirestore.getInstance()
                                .collection("organizers")
                                .document(organizerId)
                                .get()
                                .addOnSuccessListener(orgDoc -> {
                                    if (!isAdded() || orgDoc == null || !orgDoc.exists()) return;
                                    String brandName = orgDoc.getString("brand_name");
                                    if (brandName == null) brandName = orgDoc.getString("name");
                                    if (brandName != null && !brandName.isEmpty()) {
                                        txtDetailOrganizer.setText(brandName);
                                    }
                                });
                    }
                });
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
        if (userEmail.isEmpty() || eventId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("wishlists")
                .document(userEmail + "_" + eventId)
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
            txtDetailInterest.setText(currentInterestCount + " lượt quan tâm");
        }
    }

    private void toggleFavorite() {
        if (userEmail.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để lưu sự kiện yêu thích", Toast.LENGTH_SHORT).show();
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
                .collection("wishlists")
                .document(userEmail + "_" + eventId);

        DocumentReference eventRef = FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId);

        if (isFavorited) {
            Map<String, Object> data = new HashMap<>();
            data.put("userEmail", userEmail);
            data.put("eventId", eventId);
            data.put("timestamp", Timestamp.now());
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
            SelectTicketFragment selectFrag = SelectTicketFragment.newInstance(eventId);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameContainerMain, selectFrag)
                    .addToBackStack("booking")
                    .commit();
        });
    }
}
