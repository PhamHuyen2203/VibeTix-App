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
    private TextView txtDetailEventDescription;
    private TextView txtDetailMinPrice;
    private Button btnBookTickets;

    private boolean isFavorited = false;
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
        
        eventRepository.getEventById(eventId, new EventRepository.OnEventLoadedListener() {
            @Override
            public void onSuccess(Event event) {
                if (!isAdded() || event == null) return;

                txtDetailEventTitle.setText(event.getTitle());
                txtDetailEventDate.setText(event.getDate());
                txtDetailEventLocation.setText(event.getLocation());
                txtDetailEventDescription.setText(event.getDescription());
                txtDetailMinPrice.setText(formatter.format(event.getMinPrice()) + " đ");

                if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                    Glide.with(requireContext())
                            .load(event.getImageUrl())
                            .placeholder(R.drawable.event_live_non_song)
                            .into(imvDetailEventCover);
                } else {
                    int fallbackRes = "b1".equals(eventId) || "e1".equals(eventId) || "rs1".equals(eventId)
                            ? R.drawable.event_live_non_song
                            : R.drawable.event_arts_private_fantasy;
                    imvDetailEventCover.setImageResource(fallbackRes);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Lỗi tải sự kiện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
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

    private void toggleFavorite() {
        if (userEmail.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để lưu sự kiện yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }

        isFavorited = !isFavorited;
        updateFavoriteUI();

        DocumentReference docRef = FirebaseFirestore.getInstance()
                .collection("wishlists")
                .document(userEmail + "_" + eventId);

        if (isFavorited) {
            Map<String, Object> data = new HashMap<>();
            data.put("userEmail", userEmail);
            data.put("eventId", eventId);
            data.put("timestamp", Timestamp.now());
            docRef.set(data);
        } else {
            docRef.delete();
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
