package com.example.vibetix.Fragments.User;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Ticket;
import com.example.vibetix.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 * QR dialog — query user_tickets từ Firebase để lấy ticket_code + display_code thật.
 * QR encode ticket_code (UUID) → organizer quét khớp Firebase.
 * Hiện display_code (mã ngắn) trên UI.
 */
public class TicketQrDialogFragment extends DialogFragment {

    private Ticket ticket;

    // QR data from Firebase user_tickets
    private final List<String> ticketCodes = new ArrayList<>();   // ticket_code → encode vào QR
    private final List<String> displayCodes = new ArrayList<>();  // display_code → hiện trên UI
    private final List<String> ticketLabels = new ArrayList<>();  // tên loại vé
    private int currentQr = 0;

    private TextView txtQrDialogEventTitle, txtQrDialogAttendee, txtQrDialogTicketTypeName;
    private TextView txtQrDialogTicketId, txtQrCounter;
    private ImageView imvQrCodeGraphic, btnQrPrev, btnQrNext;
    private LinearLayout layoutQrNav;
    private Button btnQrDialogClose;

    public TicketQrDialogFragment() {}

    public static TicketQrDialogFragment newInstance(Ticket ticket) {
        TicketQrDialogFragment fragment = new TicketQrDialogFragment();
        fragment.ticket = ticket;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_ticket_qr, container, false);
        bindViews(view);
        btnQrDialogClose.setOnClickListener(v -> dismiss());
        if (btnQrPrev != null) btnQrPrev.setOnClickListener(v -> { if (currentQr > 0) showQr(currentQr - 1); });
        if (btnQrNext != null) btnQrNext.setOnClickListener(v -> { if (currentQr < ticketCodes.size() - 1) showQr(currentQr + 1); });

        // Load QR data từ Firebase user_tickets
        loadQrDataFromFirebase();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void bindViews(View view) {
        txtQrDialogEventTitle = view.findViewById(R.id.txtQrDialogEventTitle);
        txtQrDialogAttendee = view.findViewById(R.id.txtQrDialogAttendee);
        txtQrDialogTicketTypeName = view.findViewById(R.id.txtQrDialogTicketTypeName);
        txtQrDialogTicketId = view.findViewById(R.id.txtQrDialogTicketId);
        imvQrCodeGraphic = view.findViewById(R.id.imvQrCodeGraphic);
        btnQrDialogClose = view.findViewById(R.id.btnQrDialogClose);
        layoutQrNav = view.findViewById(R.id.layoutQrNav);
        txtQrCounter = view.findViewById(R.id.txtQrCounter);
        btnQrPrev = view.findViewById(R.id.btnQrPrev);
        btnQrNext = view.findViewById(R.id.btnQrNext);
    }

    /**
     * Query user_tickets từ Firebase: event_id + user_id → lấy ticket_code + display_code thật.
     */
    private void loadQrDataFromFirebase() {
        if (ticket == null || ticket.getEventId() == null) {
            fallbackToLegacy();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { fallbackToLegacy(); return; }

        FirebaseFirestore.getInstance()
                .collection(FirebaseCollections.USER_TICKETS)
                .whereEqualTo("event_id", ticket.getEventId())
                .whereEqualTo("user_id", user.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    ticketCodes.clear();
                    displayCodes.clear();
                    ticketLabels.clear();

                    // Parse tên loại vé từ ticketTypeName: "1x Vé VIP, 1x Vé Early bird"
                    List<String> typeNames = parseTypeLabels();

                    int idx = 0;
                    for (QueryDocumentSnapshot doc : snap) {
                        String status = doc.getString("status");
                        if (!"valid".equals(status) && !"ACTIVE".equalsIgnoreCase(status)) continue;

                        String tCode = doc.getString("ticket_code");
                        String dCode = doc.getString("display_code");
                        if (tCode == null) tCode = doc.getId();
                        if (dCode == null) dCode = "VÉ " + (idx + 1);

                        ticketCodes.add(tCode);
                        displayCodes.add(dCode);
                        ticketLabels.add(idx < typeNames.size() ? typeNames.get(idx) : "Vé");
                        idx++;
                    }

                    if (ticketCodes.isEmpty()) {
                        fallbackToLegacy();
                    } else {
                        showQr(0);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) fallbackToLegacy();
                });
    }

    /** Fallback nếu không query được Firebase — dùng ticket.getId() */
    private void fallbackToLegacy() {
        ticketCodes.clear();
        displayCodes.clear();
        ticketLabels.clear();

        List<String> typeNames = parseTypeLabels();
        int total = Math.max(1, typeNames.size());

        for (int i = 0; i < total; i++) {
            ticketCodes.add(total == 1 ? ticket.getId() : ticket.getId() + "-" + (i + 1));
            displayCodes.add("VÉ " + (i + 1));
            ticketLabels.add(i < typeNames.size() ? typeNames.get(i) : "Vé");
        }
        showQr(0);
    }

    /** Parse "1x Vé VIP, 1x Vé Early bird" → ["Vé VIP", "Vé Early bird"] */
    private List<String> parseTypeLabels() {
        List<String> labels = new ArrayList<>();
        String typeName = ticket != null ? ticket.getTicketTypeName() : null;
        if (typeName == null) return labels;

        String[] parts = typeName.split(",\\s*");
        for (String part : parts) {
            part = part.trim();
            if (part.matches("^\\d+x .*")) {
                try {
                    int qty = Integer.parseInt(part.split("x ")[0]);
                    String name = part.substring(part.indexOf("x ") + 2);
                    for (int j = 0; j < qty; j++) labels.add(name);
                } catch (NumberFormatException ignored) {}
            } else {
                labels.add(part);
            }
        }
        return labels;
    }

    private void showQr(int index) {
        if (ticket == null || index < 0 || index >= ticketCodes.size()) return;
        currentQr = index;

        txtQrDialogEventTitle.setText(ticket.getEventTitle());
        txtQrDialogAttendee.setText(getString(R.string.str_attendee_label, ticket.getAttendeeName() != null ? ticket.getAttendeeName() : ""));
        txtQrDialogTicketTypeName.setText(getString(R.string.str_ticket_tier_label, index < ticketLabels.size() ? ticketLabels.get(index) : ""));
        txtQrDialogTicketId.setText(displayCodes.get(index));

        // QR encode ticket_code (UUID) — organizer quét khớp Firebase user_tickets.ticket_code
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bmp = encoder.encodeBitmap(ticketCodes.get(index), BarcodeFormat.QR_CODE, 400, 400);
            imvQrCodeGraphic.setImageBitmap(bmp);
        } catch (Exception e) {
            Toast.makeText(requireContext(), getString(R.string.str_toast_qr_error), Toast.LENGTH_SHORT).show();
        }

        // Navigation
        int total = ticketCodes.size();
        if (total > 1) {
            layoutQrNav.setVisibility(View.VISIBLE);
            txtQrCounter.setText((currentQr + 1) + " / " + total);
            btnQrPrev.setAlpha(currentQr > 0 ? 1f : 0.3f);
            btnQrNext.setAlpha(currentQr < total - 1 ? 1f : 0.3f);
        } else {
            layoutQrNav.setVisibility(View.GONE);
        }
    }
}
