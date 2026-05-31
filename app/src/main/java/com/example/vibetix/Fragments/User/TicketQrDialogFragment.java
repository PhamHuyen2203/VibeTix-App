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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.vibetix.Models.Ticket;
import com.example.vibetix.R;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class TicketQrDialogFragment extends DialogFragment {

    private Ticket ticket;

    // Views
    private TextView txtQrDialogEventTitle;
    private TextView txtQrDialogAttendee;
    private TextView txtQrDialogTicketTypeName;
    private TextView txtQrDialogTicketId;
    private ImageView imvQrCodeGraphic;
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
        populateViews();
        generateQrCode();
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
    }

    private void populateViews() {
        if (ticket == null) return;
        txtQrDialogEventTitle.setText(ticket.getEventTitle());
        txtQrDialogAttendee.setText(getString(R.string.str_attendee_label, ticket.getAttendeeName()));
        txtQrDialogTicketTypeName.setText(getString(R.string.str_ticket_type_label, ticket.getTicketTypeName()));
        txtQrDialogTicketId.setText(ticket.getId());

        btnQrDialogClose.setOnClickListener(v -> dismiss());
    }

    private void generateQrCode() {
        if (ticket == null) return;
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            // Generate QR Code bitmap with 400x400 size
            Bitmap bitmap = barcodeEncoder.encodeBitmap(ticket.getId(), BarcodeFormat.QR_CODE, 400, 400);
            imvQrCodeGraphic.setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(requireContext(), getString(R.string.str_save_ticket_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
