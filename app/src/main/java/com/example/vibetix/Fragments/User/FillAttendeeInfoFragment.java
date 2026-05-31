package com.example.vibetix.Fragments.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.EventRepository;
import com.example.vibetix.Utils.Constants;

import java.text.DecimalFormat;

public class FillAttendeeInfoFragment extends Fragment {

    private String eventId = "b1";
    private String ticketTypeName = "Vé Tiêu chuẩn";
    private int quantity = 1;
    private long ticketPrice = 400000;
    private long totalPrice = 400000;

    // Repositories
    private final EventRepository eventRepository = new EventRepository();

    // Views
    private ImageView btnInfoBack;
    private ImageView imvInfoEventThumb;
    private TextView txtInfoEventTitle;
    private TextView txtInfoEventDate;
    private TextView txtInfoEventLocation;
    private EditText etInfoName;
    private EditText etInfoEmail;
    private EditText etInfoPhone;
    private EditText etInfoCompany;
    private EditText etInfoRole;
    private EditText etInfoNotes;
    private TextView txtInfoBottomTotal;
    private Button btnInfoNext;

    // Referral chips
    private TextView chipFb, chipLi, chipWeb, chipFriends, chipOther;
    private String selectedReferral = "";

    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public static FillAttendeeInfoFragment newInstance(String eventId, String ticketTypeName,
                                                       int quantity, long ticketPrice, long totalPrice) {
        FillAttendeeInfoFragment fragment = new FillAttendeeInfoFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        args.putString("ticketTypeName", ticketTypeName);
        args.putInt("quantity", quantity);
        args.putLong("ticketPrice", ticketPrice);
        args.putLong("totalPrice", totalPrice);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId", "b1");
            ticketTypeName = getArguments().getString("ticketTypeName", "Vé Tiêu chuẩn");
            quantity = getArguments().getInt("quantity", 1);
            ticketPrice = getArguments().getLong("ticketPrice", 400000);
            totalPrice = getArguments().getLong("totalPrice", 400000);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fill_attendee_info, container, false);
        bindViews(view);
        loadEventDetails();
        prefillUserDetails();
        setupReferralChips();
        setupClickListeners();
        return view;
    }

    private void bindViews(View view) {
        btnInfoBack = view.findViewById(R.id.btnInfoBack);
        imvInfoEventThumb = view.findViewById(R.id.imvInfoEventThumb);
        txtInfoEventTitle = view.findViewById(R.id.txtInfoEventTitle);
        txtInfoEventDate = view.findViewById(R.id.txtInfoEventDate);
        txtInfoEventLocation = view.findViewById(R.id.txtInfoEventLocation);
        etInfoName = view.findViewById(R.id.etInfoName);
        etInfoEmail = view.findViewById(R.id.etInfoEmail);
        etInfoPhone = view.findViewById(R.id.etInfoPhone);
        etInfoCompany = view.findViewById(R.id.etInfoCompany);
        etInfoRole = view.findViewById(R.id.etInfoRole);
        etInfoNotes = view.findViewById(R.id.etInfoNotes);
        txtInfoBottomTotal = view.findViewById(R.id.txtInfoBottomTotal);
        btnInfoNext = view.findViewById(R.id.btnInfoNext);

        chipFb = view.findViewById(R.id.chipFb);
        chipLi = view.findViewById(R.id.chipLi);
        chipWeb = view.findViewById(R.id.chipWeb);
        chipFriends = view.findViewById(R.id.chipFriends);
        chipOther = view.findViewById(R.id.chipOther);
    }

    private void loadEventDetails() {
        eventRepository.getEventById(eventId, new EventRepository.OnEventLoadedListener() {
            @Override
            public void onSuccess(Event event) {
                if (event != null) {
                    txtInfoEventTitle.setText(event.getTitle());
                    txtInfoEventDate.setText(event.getDate());
                    txtInfoEventLocation.setText(event.getLocation());
                    
                    int imageRes = "b1".equals(eventId) || "e1".equals(eventId) || "rs1".equals(eventId)
                            ? R.drawable.event_live_non_song
                            : R.drawable.event_arts_private_fantasy;
                    imvInfoEventThumb.setImageResource(imageRes);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), getString(R.string.str_error_generic), Toast.LENGTH_SHORT).show();
            }
        });

        txtInfoBottomTotal.setText(formatter.format(totalPrice) + " đ");
    }

    private void prefillUserDetails() {
        SharedPreferences prefs = requireContext().getSharedPreferences(Constants.PREFS_AUTH, Context.MODE_PRIVATE);
        String savedEmail = prefs.getString(Constants.KEY_USER_EMAIL, "");
        
        // Only pre-fill email as it's often the login email, but leave name/phone for user to fill
        // if they want to use different attendee info, or just leave it empty if user wants only placeholders.
        // User requested: "các input thì chỉ nên để placeholder thôi chứ không để các thông tin điền sẵn vào đó"
        // So I will NOT prefill anything.
        
        /*
        String savedName = prefs.getString(Constants.KEY_USER_NAME, "");
        String savedPhone = prefs.getString(Constants.KEY_USER_PHONE, "");

        if (!savedEmail.isEmpty()) etInfoEmail.setText(savedEmail);
        if (!savedName.isEmpty()) etInfoName.setText(savedName);
        if (!savedPhone.isEmpty()) etInfoPhone.setText(savedPhone);
        */
    }

    private void setupReferralChips() {
        TextView[] chips = {chipFb, chipLi, chipWeb, chipFriends, chipOther};
        for (TextView chip : chips) {
            chip.setOnClickListener(v -> {
                // Clear all
                for (TextView c : chips) {
                    c.setBackgroundResource(R.drawable.bg_quick_chip);
                    c.setTextColor(getResources().getColor(R.color.clr_text_secondary));
                }
                // Select active
                chip.setBackgroundResource(R.drawable.bg_quick_chip_active);
                chip.setTextColor(getResources().getColor(R.color.clr_primary_blue));
                selectedReferral = chip.getText().toString();
            });
        }
    }

    private void setupClickListeners() {
        btnInfoBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        btnInfoNext.setOnClickListener(v -> {
            String name = etInfoName.getText().toString().trim();
            String email = etInfoEmail.getText().toString().trim();
            String phone = etInfoPhone.getText().toString().trim();
            String company = etInfoCompany.getText().toString().trim();
            String role = etInfoRole.getText().toString().trim();
            String notes = etInfoNotes.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.str_required_fields_warning), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), getString(R.string.str_invalid_email_warning), Toast.LENGTH_SHORT).show();
                return;
            }

            // Save details to SharedPreferences so it pre-fills next time!
            SharedPreferences.Editor editor = requireContext().getSharedPreferences(Constants.PREFS_AUTH, Context.MODE_PRIVATE).edit();
            editor.putString(Constants.KEY_USER_NAME, name);
            editor.putString(Constants.KEY_USER_PHONE, phone);
            editor.apply();

            // Transition to step 3: Simulated payment
            SimulatedPaymentFragment paymentFrag = SimulatedPaymentFragment.newInstance(
                    eventId,
                    ticketTypeName,
                    quantity,
                    ticketPrice,
                    totalPrice,
                    name,
                    email,
                    phone,
                    company,
                    role,
                    notes
            );

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameContainerMain, paymentFrag)
                    .addToBackStack("booking")
                    .commit();
        });
    }
}
