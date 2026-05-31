package com.example.vibetix.Fragments.User;

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
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.EventRepository;
import com.example.vibetix.Utils.Constants;

import java.text.DecimalFormat;

public class EventDetailFragment extends Fragment {

    private String eventId = "b1"; // default mock event
    private final EventRepository eventRepository = new EventRepository();
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    // Views
    private ImageView btnDetailBack;
    private ImageView imvDetailEventCover;
    private TextView txtDetailEventTitle;
    private TextView txtDetailEventDate;
    private TextView txtDetailEventLocation;
    private TextView txtDetailEventDescription;
    private TextView txtDetailMinPrice;
    private Button btnBookTickets;

    public static EventDetailFragment newInstance(String eventId) {
        EventDetailFragment fragment = new EventDetailFragment();
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
        View view = inflater.inflate(R.layout.fragment_event_detail, container, false);
        bindViews(view);
        loadEventDetails();
        setupClickListeners();
        return view;
    }

    private void bindViews(View view) {
        btnDetailBack = view.findViewById(R.id.btnDetailBack);
        imvDetailEventCover = view.findViewById(R.id.imvDetailEventCover);
        txtDetailEventTitle = view.findViewById(R.id.txtDetailEventTitle);
        txtDetailEventDate = view.findViewById(R.id.txtDetailEventDate);
        txtDetailEventLocation = view.findViewById(R.id.txtDetailEventLocation);
        txtDetailEventDescription = view.findViewById(R.id.txtDetailEventDescription);
        txtDetailMinPrice = view.findViewById(R.id.txtDetailMinPrice);
        btnBookTickets = view.findViewById(R.id.btnBookTickets);
    }

    private void loadEventDetails() {
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
                } else if (event.getLocalImageResId() != 0) {
                    imvDetailEventCover.setImageResource(event.getLocalImageResId());
                } else {
                    // Default fallback
                    int fallbackRes = "b1".equals(eventId) || "e1".equals(eventId) || "rs1".equals(eventId)
                            ? R.drawable.event_live_non_song
                            : R.drawable.event_arts_private_fantasy;
                    imvDetailEventCover.setImageResource(fallbackRes);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), getString(R.string.str_error_generic), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        btnDetailBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                // If loaded without stack, replace with home
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.frameContainerMain, new HomeFragment())
                        .commit();
            }
        });

        btnBookTickets.setOnClickListener(v -> {
            // Open ticket selection fragment
            SelectTicketFragment selectFrag = SelectTicketFragment.newInstance(eventId);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameContainerMain, selectFrag)
                    .addToBackStack("booking")
                    .commit();
        });
    }
}
