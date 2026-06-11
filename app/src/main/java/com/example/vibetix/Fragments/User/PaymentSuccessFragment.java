package com.example.vibetix.Fragments.User;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Activities.User.UserMainActivity;
import com.example.vibetix.R;

import java.text.DecimalFormat;

public class PaymentSuccessFragment extends Fragment {

    private String orderId = "";
    private String paymentMethod = "";
    private String ticketTypeName = "";
    private int quantity = 1;
    private long finalTotal = 0;
    private String formattedDate = "";
    private String email = "";
    private String eventId = "";

    // Views
    private TextView txtSuccessOrderId;
    private TextView txtSuccessPaymentMethod;
    private TextView txtSuccessTicketType;
    private TextView txtSuccessQty;
    private TextView txtSuccessTotal;
    private TextView txtSuccessTime;
    private TextView txtSuccessEmail;

    private ImageView imvSuccessEventThumb;
    private TextView txtSuccessEventTitle;
    private TextView txtSuccessEventDate;
    private TextView txtSuccessEventLocation;

    private Button btnSuccessViewTickets;
    private Button btnSuccessGoHome;

    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public static PaymentSuccessFragment newInstance(String orderId, String paymentMethod, String ticketTypeName,
                                                     int quantity, long finalTotal, String formattedDate,
                                                     String email, String eventId) {
        PaymentSuccessFragment fragment = new PaymentSuccessFragment();
        Bundle args = new Bundle();
        args.putString("orderId", orderId);
        args.putString("paymentMethod", paymentMethod);
        args.putString("ticketTypeName", ticketTypeName);
        args.putInt("quantity", quantity);
        args.putLong("finalTotal", finalTotal);
        args.putString("formattedDate", formattedDate);
        args.putString("email", email);
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderId = getArguments().getString("orderId", "");
            paymentMethod = getArguments().getString("paymentMethod", "");
            ticketTypeName = getArguments().getString("ticketTypeName", "");
            quantity = getArguments().getInt("quantity", 1);
            finalTotal = getArguments().getLong("finalTotal", 0);
            formattedDate = getArguments().getString("formattedDate", "");
            email = getArguments().getString("email", "");
            eventId = getArguments().getString("eventId", "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment_success, container, false);
        bindViews(view);
        populateReceipt();
        setupClickListeners();
        return view;
    }

    private void bindViews(View view) {
        txtSuccessOrderId = view.findViewById(R.id.txtSuccessOrderId);
        txtSuccessPaymentMethod = view.findViewById(R.id.txtSuccessPaymentMethod);
        txtSuccessTicketType = view.findViewById(R.id.txtSuccessTicketType);
        txtSuccessQty = view.findViewById(R.id.txtSuccessQty);
        txtSuccessTotal = view.findViewById(R.id.txtSuccessTotal);
        txtSuccessTime = view.findViewById(R.id.txtSuccessTime);
        txtSuccessEmail = view.findViewById(R.id.txtSuccessEmail);

        imvSuccessEventThumb = view.findViewById(R.id.imvSuccessEventThumb);
        txtSuccessEventTitle = view.findViewById(R.id.txtSuccessEventTitle);
        txtSuccessEventDate = view.findViewById(R.id.txtSuccessEventDate);
        txtSuccessEventLocation = view.findViewById(R.id.txtSuccessEventLocation);

        btnSuccessViewTickets = view.findViewById(R.id.btnSuccessViewTickets);
        btnSuccessGoHome = view.findViewById(R.id.btnSuccessGoHome);
    }

    private void populateReceipt() {
        txtSuccessOrderId.setText(orderId);
        txtSuccessPaymentMethod.setText(paymentMethod);
        txtSuccessTicketType.setText(ticketTypeName);
        txtSuccessQty.setText(quantity + " vé");
        txtSuccessTotal.setText(formatter.format(finalTotal) + " đ");
        txtSuccessTime.setText(formattedDate);
        txtSuccessEmail.setText(email);

        // Fetch event data to show correct title and image
        new com.example.vibetix.Repositories.EventRepository().getEventById(eventId, new com.example.vibetix.Repositories.EventRepository.OnEventLoadedListener() {
            @Override
            public void onSuccess(com.example.vibetix.Models.Event event) {
                if (!isAdded() || event == null) return;
                txtSuccessEventTitle.setText(event.getTitle());
                txtSuccessEventDate.setText(event.getDate());
                txtSuccessEventLocation.setText(event.getLocation());
                
                if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                    com.bumptech.glide.Glide.with(requireContext())
                            .load(event.getImageUrl())
                            .placeholder(R.drawable.event_live_non_song)
                            .into(imvSuccessEventThumb);
                } else {
                    int coverRes = "b1".equals(eventId) || "e1".equals(eventId) || "rs1".equals(eventId)
                            ? R.drawable.event_live_non_song
                            : R.drawable.event_arts_private_fantasy;
                    imvSuccessEventThumb.setImageResource(coverRes);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Keep default layout or mock
            }
        });
    }

    private void setupClickListeners() {
        btnSuccessViewTickets.setOnClickListener(v -> {
            if (getActivity() instanceof UserMainActivity) {
                ((UserMainActivity) getActivity()).selectTab(R.id.tabTickets);
            }
        });

        btnSuccessGoHome.setOnClickListener(v -> {
            if (getActivity() instanceof UserMainActivity) {
                ((UserMainActivity) getActivity()).selectTab(R.id.tabHome);
            }
        });
    }
}
