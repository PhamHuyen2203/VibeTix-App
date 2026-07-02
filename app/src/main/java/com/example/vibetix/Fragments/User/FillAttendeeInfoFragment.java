package com.example.vibetix.Fragments.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.vibetix.Models.Event;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.EventRepository;
import com.example.vibetix.Utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class FillAttendeeInfoFragment extends Fragment {

    private static final String PREF_HISTORY_KEY = "attendee_fill_history";
    private static final int MAX_HISTORY = 5;

    private String eventId = "";
    private String ticketTypeName = "Vé Tiêu chuẩn";
    private int quantity = 1;
    private long ticketPrice = 0;
    private long totalPrice = 0;
    private String eventImageUrl = "";
    private boolean isResale = false;   // mua lại vé (resale)
    private String transferId = "";     // id transfer khi mua lại

    private final EventRepository eventRepository = new EventRepository();

    // Views
    private ImageView btnInfoBack;
    private LinearLayout layoutInfoStep1;
    private NestedScrollView scrollInfo;
    private ImageView imvInfoEventThumb;
    private TextView txtInfoEventTitle, txtInfoEventDate, txtInfoEventLocation;
    private TextView lblInfoName, lblInfoEmail, lblInfoPhone;
    private AutoCompleteTextView etInfoName, etInfoEmail, etInfoPhone;
    private EditText etInfoNotes;
    private TextView txtInfoNotesCount;
    private TextView txtInfoBottomTotal;
    private Button btnInfoNext;

    // Referral chips
    private TextView chipFb, chipLi, chipWeb, chipFriends, chipOther;
    private String selectedReferral = "";

    private final DecimalFormat formatter = new DecimalFormat("#,###");
    private final List<AttendeeProfile> history = new ArrayList<>();

    public static FillAttendeeInfoFragment newInstance(String eventId, String ticketTypeName,
                                                       int quantity, long ticketPrice,
                                                       long totalPrice, String eventImageUrl) {
        FillAttendeeInfoFragment fragment = new FillAttendeeInfoFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        args.putString("ticketTypeName", ticketTypeName);
        args.putInt("quantity", quantity);
        args.putLong("ticketPrice", ticketPrice);
        args.putLong("totalPrice", totalPrice);
        args.putString("eventImageUrl", eventImageUrl != null ? eventImageUrl : "");
        fragment.setArguments(args);
        return fragment;
    }

    /** Tạo fragment cho luồng MUA LẠI vé (resale). */
    public static FillAttendeeInfoFragment newInstanceResale(String eventId, String ticketTypeName,
                                                             long price, String eventImageUrl,
                                                             String transferId) {
        FillAttendeeInfoFragment fragment = new FillAttendeeInfoFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        args.putString("ticketTypeName", ticketTypeName);
        args.putInt("quantity", 1);
        args.putLong("ticketPrice", price);
        args.putLong("totalPrice", price);
        args.putString("eventImageUrl", eventImageUrl != null ? eventImageUrl : "");
        args.putBoolean("isResale", true);
        args.putString("transferId", transferId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId        = getArguments().getString("eventId", "");
            ticketTypeName = getArguments().getString("ticketTypeName", "Vé Tiêu chuẩn");
            quantity       = getArguments().getInt("quantity", 1);
            ticketPrice    = getArguments().getLong("ticketPrice", 0);
            totalPrice     = getArguments().getLong("totalPrice", 0);
            eventImageUrl  = getArguments().getString("eventImageUrl", "");
            isResale       = getArguments().getBoolean("isResale", false);
            transferId     = getArguments().getString("transferId", "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fill_attendee_info, container, false);
        bindViews(view);
        applyInsets(view);
        applyRequiredLabels();
        loadEventDetails();
        loadHistory();
        setupNotesCounter();
        setupReferralChips();
        setupClickListeners();
        setupAutocomplete(); // must be last — sets OnFocusChangeListener that must not be overwritten
        txtInfoBottomTotal.setText(formatter.format(totalPrice) + " đ");
        return view;
    }

    private void applyInsets(View view) {
        View header = view.findViewById(R.id.layoutInfoHeader);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                if (top <= 0) {
                    int resId = v.getResources().getIdentifier("status_bar_height", "dimen", "android");
                    if (resId > 0) top = v.getResources().getDimensionPixelSize(resId);
                }
                if (top <= 0) top = (int) (28 * v.getResources().getDisplayMetrics().density);
                v.setPadding(0, top, 0, 0);
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
        }
    }

    private void bindViews(View view) {
        btnInfoBack          = view.findViewById(R.id.btnInfoBack);
        layoutInfoStep1      = view.findViewById(R.id.layoutInfoStep1);
        scrollInfo           = view.findViewById(R.id.scrollInfo);
        imvInfoEventThumb    = view.findViewById(R.id.imvInfoEventThumb);
        txtInfoEventTitle    = view.findViewById(R.id.txtInfoEventTitle);
        txtInfoEventDate     = view.findViewById(R.id.txtInfoEventDate);
        txtInfoEventLocation = view.findViewById(R.id.txtInfoEventLocation);
        lblInfoName          = view.findViewById(R.id.lblInfoName);
        lblInfoEmail         = view.findViewById(R.id.lblInfoEmail);
        lblInfoPhone         = view.findViewById(R.id.lblInfoPhone);
        etInfoName           = view.findViewById(R.id.etInfoName);
        etInfoEmail          = view.findViewById(R.id.etInfoEmail);
        etInfoPhone          = view.findViewById(R.id.etInfoPhone);
        etInfoNotes          = view.findViewById(R.id.etInfoNotes);
        txtInfoNotesCount    = view.findViewById(R.id.txtInfoNotesCount);
        txtInfoBottomTotal   = view.findViewById(R.id.txtInfoBottomTotal);
        btnInfoNext          = view.findViewById(R.id.btnInfoNext);
        chipFb      = view.findViewById(R.id.chipFb);
        chipLi      = view.findViewById(R.id.chipLi);
        chipWeb     = view.findViewById(R.id.chipWeb);
        chipFriends = view.findViewById(R.id.chipFriends);
        chipOther   = view.findViewById(R.id.chipOther);
    }

    private void applyRequiredLabels() {
        setRequiredLabel(lblInfoName,  "Họ & tên");
        setRequiredLabel(lblInfoEmail, "Email");
        setRequiredLabel(lblInfoPhone, "Số điện thoại");
    }

    private void setRequiredLabel(TextView tv, String base) {
        SpannableString s = new SpannableString(base + " *");
        s.setSpan(new ForegroundColorSpan(0xFFD92D20),
                s.length() - 1, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(s);
    }

    private void loadEventDetails() {
        if (!eventImageUrl.isEmpty()) {
            Glide.with(this).load(eventImageUrl)
                    .placeholder(R.drawable.event_live_non_song)
                    .into(imvInfoEventThumb);
        }
        if (eventId.isEmpty()) return;

        eventRepository.getEventById(eventId, new EventRepository.OnEventLoadedListener() {
            @Override
            public void onSuccess(Event event) {
                if (!isAdded() || event == null) return;
                txtInfoEventTitle.setText(event.getTitle() != null ? event.getTitle() : "");
                txtInfoEventDate.setText(event.getDate() != null ? event.getDate() : "--");
                txtInfoEventLocation.setText(event.getLocation() != null ? event.getLocation() : "--");
                String img = event.getImageUrl();
                if (img != null && !img.isEmpty()) {
                    eventImageUrl = img;
                    Glide.with(requireContext())
                            .load(img)
                            .placeholder(R.drawable.event_live_non_song)
                            .into(imvInfoEventThumb);
                }
            }

            @Override
            public void onFailure(Exception e) { /* keep existing image */ }
        });
    }

    // ── History autofill ──────────────────────────────────────────────────────

    private void loadHistory() {
        history.clear();
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(Constants.PREFS_AUTH, Context.MODE_PRIVATE);
        String json = prefs.getString(PREF_HISTORY_KEY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                history.add(new AttendeeProfile(
                        obj.optString("name"),
                        obj.optString("email"),
                        obj.optString("phone")));
            }
        } catch (Exception ignored) {}

        // Seed from user profile if history is empty
        if (history.isEmpty()) {
            String savedName  = prefs.getString(Constants.KEY_USER_NAME, "");
            String savedEmail = prefs.getString(Constants.KEY_USER_EMAIL, "");
            String savedPhone = prefs.getString(Constants.KEY_USER_PHONE, "");

            // Fall back to FirebaseAuth email if prefs email is missing
            if (savedEmail.isEmpty()) {
                com.google.firebase.auth.FirebaseUser fbUser =
                        com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (fbUser != null && fbUser.getEmail() != null) {
                    savedEmail = fbUser.getEmail();
                }
            }

            if (!savedEmail.isEmpty()) {
                history.add(new AttendeeProfile(savedName, savedEmail, savedPhone));
            }
        }
    }

    private void saveToHistory(String name, String email, String phone) {
        history.removeIf(p -> p.email.equalsIgnoreCase(email));
        history.add(0, new AttendeeProfile(name, email, phone));
        if (history.size() > MAX_HISTORY) history.subList(MAX_HISTORY, history.size()).clear();

        JSONArray arr = new JSONArray();
        for (AttendeeProfile p : history) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", p.name);
                obj.put("email", p.email);
                obj.put("phone", p.phone);
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        requireContext().getSharedPreferences(Constants.PREFS_AUTH, Context.MODE_PRIVATE)
                .edit().putString(PREF_HISTORY_KEY, arr.toString()).apply();
    }

    private void setupAutocomplete() {
        if (history.isEmpty()) return;

        // Build display labels: "Tên  •  email"
        List<String> labels = new ArrayList<>();
        for (AttendeeProfile p : history) {
            String label = (!p.name.isEmpty() ? p.name : p.email)
                    + (!p.email.isEmpty() ? "  •  " + p.email : "");
            labels.add(label);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, labels);

        // Attach to all 3 fields — selecting any one fills all fields
        attachAutocomplete(etInfoName,  adapter);
        attachAutocomplete(etInfoEmail, adapter);
        attachAutocomplete(etInfoPhone, adapter);
    }

    private void attachAutocomplete(AutoCompleteTextView field, ArrayAdapter<String> adapter) {
        field.setAdapter(adapter);
        field.setThreshold(0);
        // Combined: clear error + show dropdown on focus (must be set LAST — overrides any earlier listener)
        field.setOnFocusChangeListener((v, hasFocus) -> {
            clearFieldError(field);
            if (hasFocus && !history.isEmpty()) {
                field.post(() -> field.showDropDown());
            }
        });
        // Also show on click (in case already focused)
        field.setOnClickListener(v -> {
            if (!history.isEmpty()) field.showDropDown();
        });
        field.setOnItemClickListener((parent, v, position, id) -> {
            AttendeeProfile p = history.get(position);
            // Suppress the AutoCompleteTextView from replacing field text with label
            field.post(() -> {
                if (field == etInfoName)  etInfoName.setText(p.name);
                else if (field == etInfoEmail) etInfoEmail.setText(p.email);
                else etInfoPhone.setText(p.phone);
            });
            // Fill all fields regardless of which was tapped
            etInfoName.post(() -> {
                if (p.name  != null && !p.name.isEmpty())  etInfoName.setText(p.name);
                if (p.email != null && !p.email.isEmpty())  etInfoEmail.setText(p.email);
                if (p.phone != null && !p.phone.isEmpty())  etInfoPhone.setText(p.phone);
                clearFieldError(etInfoName);
                clearFieldError(etInfoEmail);
                clearFieldError(etInfoPhone);
                // Move cursor to end of phone (last field typically)
                etInfoPhone.setSelection(etInfoPhone.getText().length());
            });
        });
    }

    private int dp(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }

    // ── Notes counter ─────────────────────────────────────────────────────────

    private void setupNotesCounter() {
        etInfoNotes.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (txtInfoNotesCount != null) txtInfoNotesCount.setText(s.length() + "/300");
            }
        });
    }

    // ── Referral chips ────────────────────────────────────────────────────────

    private void setupReferralChips() {
        TextView[] chips = {chipFb, chipLi, chipWeb, chipFriends, chipOther};
        for (TextView chip : chips) {
            chip.setOnClickListener(v -> {
                for (TextView c : chips) {
                    c.setBackgroundResource(R.drawable.bg_quick_chip);
                    c.setTextColor(getResources().getColor(R.color.clr_text_secondary));
                }
                chip.setBackgroundResource(R.drawable.bg_quick_chip_active);
                chip.setTextColor(getResources().getColor(R.color.clr_primary_blue));
                selectedReferral = chip.getText().toString();
            });
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {
        btnInfoBack.setOnClickListener(v -> popBack());
        if (layoutInfoStep1 != null) layoutInfoStep1.setOnClickListener(v -> popBack());

        // Focus + click listeners for name/email/phone are handled in setupAutocomplete()
        btnInfoNext.setOnClickListener(v -> attemptProceed());
    }

    private void popBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }

    private void attemptProceed() {
        String name  = etInfoName.getText().toString().trim();
        String email = etInfoEmail.getText().toString().trim();
        String phone = etInfoPhone.getText().toString().trim();

        boolean valid = true;
        View firstError = null;

        if (name.isEmpty()) {
            setFieldError(etInfoName);
            if (firstError == null) firstError = etInfoName;
            valid = false;
        } else {
            clearFieldError(etInfoName);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setFieldError(etInfoEmail);
            if (firstError == null) firstError = etInfoEmail;
            valid = false;
        } else {
            clearFieldError(etInfoEmail);
        }

        if (phone.isEmpty() || phone.length() < 10 || phone.length() > 11 || !phone.matches("^[0-9]+$")) {
            setFieldError(etInfoPhone);
            if (firstError == null) firstError = etInfoPhone;
            valid = false;
        } else {
            clearFieldError(etInfoPhone);
        }

        if (!valid) {
            if (firstError != null) {
                final View target = firstError;
                scrollInfo.post(() -> {
                    int[] tLoc = new int[2];
                    target.getLocationOnScreen(tLoc);
                    int[] sLoc = new int[2];
                    scrollInfo.getLocationOnScreen(sLoc);
                    scrollInfo.smoothScrollBy(0, tLoc[1] - sLoc[1] - dp(120));
                });
            }
            return;
        }

        String notes = etInfoNotes.getText().toString().trim();
        saveToHistory(name, email, phone);

        SimulatedPaymentFragment paymentFrag = SimulatedPaymentFragment.newInstance(
                eventId, ticketTypeName, quantity, ticketPrice, totalPrice,
                name, email, phone, notes, eventImageUrl);
        if (paymentFrag.getArguments() != null) {
            // Forward resale info
            if (isResale) {
                paymentFrag.getArguments().putBoolean("isResale", true);
                paymentFrag.getArguments().putString("transferId", transferId);
            }
            // Forward chi tiết loại vé (JSON)
            String itemsJson = getArguments() != null ? getArguments().getString("ticketItemsJson", "") : "";
            if (!itemsJson.isEmpty()) {
                paymentFrag.getArguments().putString("ticketItemsJson", itemsJson);
            }
        }

        getParentFragmentManager().beginTransaction()
                .replace(R.id.frameContainerMain, paymentFrag)
                .addToBackStack("booking")
                .commit();
    }

    private void setFieldError(TextView et) {
        et.setBackgroundResource(R.drawable.bg_input_error);
    }

    private void clearFieldError(TextView et) {
        et.setBackgroundResource(R.drawable.bg_input_normal);
    }

    private static class AttendeeProfile {
        String name, email, phone;
        AttendeeProfile(String name, String email, String phone) {
            this.name = name; this.email = email; this.phone = phone;
        }
    }
}
