package com.example.vibetix.Fragments.User;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.TicketType;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.EventRepository;

import java.text.DecimalFormat;
import java.util.List;

public class SelectTicketFragment extends Fragment {

    private String eventId = "b1";
    private int quantity = 1;
    private long ticketPrice = 400000;
    private String ticketTypeName = "Vé Tiêu chuẩn";
    private String eventTitle = "LUNCH & LEARN: Workshop về Phỏng vấn Ứng viên";

    // Repositories
    private final EventRepository eventRepository = new EventRepository();

    // Views
    private ImageView btnSelectBack;
    private ImageView imvSelectEventThumb;
    private TextView txtSelectEventTitle;
    private TextView txtSelectEventDate;
    private TextView txtSelectEventLocation;
    private TextView txtSelectTicketTypeName;
    private TextView txtSelectTicketPrice;
    private TextView txtSelectRemainingBadge;
    private ImageButton btnSelectQtyMinus;
    private ImageButton btnSelectQtyPlus;
    private TextView txtSelectQtyValue;
    private ImageView imvSelectBottomThumb;
    private TextView txtSelectBottomEventTitle;
    private TextView txtSelectBottomQty;
    private TextView txtSelectBottomTotal;
    private Button btnSelectNext;

    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public static SelectTicketFragment newInstance(String eventId) {
        SelectTicketFragment fragment = new SelectTicketFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId", "b1");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_ticket, container, false);
        bindViews(view);
        loadEventDetails();
        setupQuantityControls();
        setupClickListeners();
        updatePricing();
        return view;
    }

    private void bindViews(View view) {
        btnSelectBack = view.findViewById(R.id.btnSelectBack);
        imvSelectEventThumb = view.findViewById(R.id.imvSelectEventThumb);
        txtSelectEventTitle = view.findViewById(R.id.txtSelectEventTitle);
        txtSelectEventDate = view.findViewById(R.id.txtSelectEventDate);
        txtSelectEventLocation = view.findViewById(R.id.txtSelectEventLocation);
        txtSelectTicketTypeName = view.findViewById(R.id.txtSelectTicketTypeName);
        txtSelectTicketPrice = view.findViewById(R.id.txtSelectTicketPrice);
        txtSelectRemainingBadge = view.findViewById(R.id.txtSelectRemainingBadge);
        btnSelectQtyMinus = view.findViewById(R.id.btnSelectQtyMinus);
        btnSelectQtyPlus = view.findViewById(R.id.btnSelectQtyPlus);
        txtSelectQtyValue = view.findViewById(R.id.txtSelectQtyValue);
        imvSelectBottomThumb = view.findViewById(R.id.imvSelectBottomThumb);
        txtSelectBottomEventTitle = view.findViewById(R.id.txtSelectBottomEventTitle);
        txtSelectBottomQty = view.findViewById(R.id.txtSelectBottomQty);
        txtSelectBottomTotal = view.findViewById(R.id.txtSelectBottomTotal);
        btnSelectNext = view.findViewById(R.id.btnSelectNext);
    }

    private void loadEventDetails() {
        // Fetch event from Firestore using team's style
        eventRepository.getEventById(eventId, new EventRepository.OnEventLoadedListener() {
            @Override
            public void onSuccess(Event event) {
                if (!isAdded() || event == null) return;
                
                eventTitle = event.getTitle();
                txtSelectEventTitle.setText(eventTitle);
                txtSelectBottomEventTitle.setText(eventTitle);
                txtSelectEventDate.setText(event.getDate());
                txtSelectEventLocation.setText(event.getLocation());
                
                // Display correct image based on Firestore data or local fallback
                if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                    com.bumptech.glide.Glide.with(requireContext())
                            .load(event.getImageUrl())
                            .placeholder(R.drawable.event_live_non_song)
                            .into(imvSelectEventThumb);
                    com.bumptech.glide.Glide.with(requireContext())
                            .load(event.getImageUrl())
                            .into(imvSelectBottomThumb);
                } else {
                    int imageRes = "b1".equals(eventId) || "e1".equals(eventId) || "rs1".equals(eventId)
                            ? R.drawable.event_live_non_song
                            : R.drawable.event_arts_private_fantasy;
                    imvSelectEventThumb.setImageResource(imageRes);
                    imvSelectBottomThumb.setImageResource(imageRes);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Fallback to local mock data
                loadMockEventData();
            }
        });

        // Fetch ticket types from Firestore
        eventRepository.getTicketTypesForEvent(eventId, new EventRepository.OnTicketTypesLoadedListener() {
            @Override
            public void onSuccess(List<TicketType> ticketTypes) {
                if (ticketTypes != null && !ticketTypes.isEmpty()) {
                    TicketType type = ticketTypes.get(0); // Take first available ticket type
                    ticketTypeName = type.getName();
                    ticketPrice = (long) type.getPrice();
                    txtSelectTicketTypeName.setText(ticketTypeName);
                    txtSelectTicketPrice.setText(formatter.format(ticketPrice) + " đ");
                    
                    if (txtSelectRemainingBadge != null) {
                        String remainingStr = getString(R.string.str_remaining_tickets, type.getRemainingQuantity());
                        txtSelectRemainingBadge.setText(remainingStr);
                    }
                    updatePricing();
                } else {
                    loadMockTicketTypeData();
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Fallback to local mock ticket type
                loadMockTicketTypeData();
            }
        });
    }

    private void loadMockEventData() {
        if ("b1".equals(eventId) || "e1".equals(eventId) || "rs1".equals(eventId)) {
            eventTitle = "LUNCH & LEARN: Workshop về Phỏng vấn Ứng viên";
            txtSelectEventTitle.setText(eventTitle);
            txtSelectEventDate.setText("09:00 - 12:00, 23 May, 2026");
            txtSelectEventLocation.setText("WB Business Center, Quận 3, Hồ Chí Minh");
            imvSelectEventThumb.setImageResource(R.drawable.event_live_non_song);
            imvSelectBottomThumb.setImageResource(R.drawable.event_live_non_song);
        } else {
            eventTitle = "Private Show in Fantasy - Quốc Thiên";
            txtSelectEventTitle.setText(eventTitle);
            txtSelectEventDate.setText("16:00 - 19:00, 16 May, 2026");
            txtSelectEventLocation.setText("WB Business Center, Gò Vấp, TP. HCM");
            imvSelectEventThumb.setImageResource(R.drawable.event_arts_private_fantasy);
            imvSelectBottomThumb.setImageResource(R.drawable.event_arts_private_fantasy);
        }
        txtSelectBottomEventTitle.setText(eventTitle);
    }

    private void loadMockTicketTypeData() {
        if ("b1".equals(eventId) || "e1".equals(eventId) || "rs1".equals(eventId)) {
            ticketPrice = 400000;
            ticketTypeName = "Vé Tiêu chuẩn";
        } else {
            ticketPrice = 800000;
            ticketTypeName = "Vé VIP";
        }
        txtSelectTicketTypeName.setText(ticketTypeName);
        txtSelectTicketPrice.setText(formatter.format(ticketPrice) + " đ");
        if (txtSelectRemainingBadge != null) {
            String remainingStr = getString(R.string.str_remaining_tickets, 150);
            txtSelectRemainingBadge.setText(remainingStr);
        }
        updatePricing();
    }

    private void setupQuantityControls() {
        btnSelectQtyPlus.setOnClickListener(v -> {
            if (quantity < 5) { // limit purchase quantity to 5
                quantity++;
                updateQuantityUI();
            }
        });

        btnSelectQtyMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                updateQuantityUI();
            }
        });
    }

    private void updateQuantityUI() {
        txtSelectQtyValue.setText(String.valueOf(quantity));
        txtSelectBottomQty.setText("x" + quantity);
        updatePricing();
    }

    private void updatePricing() {
        long total = ticketPrice * quantity;
        String formatted = formatter.format(total) + " đ";
        txtSelectBottomTotal.setText(formatted);
    }

    private void setupClickListeners() {
        btnSelectBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        btnSelectNext.setOnClickListener(v -> {
            // Transition to step 2: attendee info
            FillAttendeeInfoFragment fillFrag = FillAttendeeInfoFragment.newInstance(
                    eventId,
                    ticketTypeName,
                    quantity,
                    ticketPrice,
                    ticketPrice * quantity
            );

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameContainerMain, fillFrag)
                    .addToBackStack("booking")
                    .commit();
        });
    }
}
