package com.example.vibetix.Fragments.User;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.vibetix.Models.Ticket;
import com.example.vibetix.Models.TicketTransfer;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.TicketTransferRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class PassTicketDialogFragment extends DialogFragment {

    private Ticket ticket;
    private OnTicketPassedListener listener;

    private final TicketTransferRepository ticketTransferRepository = new TicketTransferRepository();

    // Views
    private TextView txtPassDialogEventTitle;
    private TextView txtPassDialogOriginalPrice;
    private EditText etPassResalePrice;
    private EditText etPassMessage;
    private Button btnPassCancel;
    private Button btnPassConfirm;

    public interface OnTicketPassedListener {
        void onTicketPassed();
    }

    public PassTicketDialogFragment() {}

    public static PassTicketDialogFragment newInstance(Ticket ticket, OnTicketPassedListener listener) {
        PassTicketDialogFragment fragment = new PassTicketDialogFragment();
        fragment.ticket = ticket;
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_pass_ticket, container, false);
        bindViews(view);
        populateViews();
        setupClickListeners();
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
        txtPassDialogEventTitle = view.findViewById(R.id.txtPassDialogEventTitle);
        txtPassDialogOriginalPrice = view.findViewById(R.id.txtPassDialogOriginalPrice);
        etPassResalePrice = view.findViewById(R.id.etPassResalePrice);
        etPassMessage = view.findViewById(R.id.etPassMessage);
        btnPassCancel = view.findViewById(R.id.btnPassCancel);
        btnPassConfirm = view.findViewById(R.id.btnPassConfirm);
    }

    private final java.text.DecimalFormat priceFormatter = new java.text.DecimalFormat("#,###");

    private void populateViews() {
        if (ticket == null) return;
        txtPassDialogEventTitle.setText(ticket.getEventTitle());
        long original = ticket.getPurchasePrice();
        if (original > 0) {
            txtPassDialogOriginalPrice.setText("Giá mua gốc: " + priceFormatter.format(original) + " đ");
            txtPassDialogOriginalPrice.setVisibility(View.VISIBLE);
        } else {
            txtPassDialogOriginalPrice.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnPassCancel.setOnClickListener(v -> dismiss());

        btnPassConfirm.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate giá bán
            String priceStr = etPassResalePrice.getText().toString().trim().replaceAll("[^0-9]", "");
            if (priceStr.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập giá bạn muốn bán", Toast.LENGTH_SHORT).show();
                return;
            }
            long resalePrice;
            try {
                resalePrice = Long.parseLong(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Giá bán không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            if (resalePrice <= 0) {
                Toast.makeText(requireContext(), "Giá bán phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }

            btnPassConfirm.setEnabled(false);
            Toast.makeText(requireContext(), "Đang xử lý...", Toast.LENGTH_SHORT).show();

            // Build TicketTransfer
            String transferId = UUID.randomUUID().toString();
            TicketTransfer transfer = new TicketTransfer();
            transfer.setTransferId(transferId);
            transfer.setSenderId(currentUser.getUid());
            transfer.setReceiverId("");
            transfer.setReceiverEmail(null);
            transfer.setUserTicketId(ticket.getId()); // maps Ticket.id → user_ticket_id
            String message = etPassMessage != null ? etPassMessage.getText().toString().trim() : "";
            if (message.isEmpty()) message = "Đăng bán lại vé";

            transfer.setStatus("pending");
            transfer.setPrice(resalePrice);
            transfer.setOriginalPrice(ticket.getPurchasePrice());
            transfer.setMessage(message);
            // Denormalize event display data từ vé → không cần join khi hiển thị
            transfer.setEventId(ticket.getEventId());
            transfer.setEventTitle(ticket.getEventTitle());
            transfer.setEventDate(ticket.getEventDate());
            transfer.setEventLocation(ticket.getEventLocation());
            transfer.setEventImageUrl(ticket.getEventImageUrl());
            transfer.setTransferToken(UUID.randomUUID().toString());
            transfer.setCreatedAt(Timestamp.now());
            transfer.setCompletedAt(null);

            // expires_at = now + 30 days
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 30);
            transfer.setExpiresAt(new Timestamp(cal.getTime()));

            ticketTransferRepository.createTransfer(transfer, new TicketTransferRepository.OnTransferActionListener() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Đã đăng bán lại vé thành công!", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onTicketPassed();
                        }
                        dismiss();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (isAdded()) {
                        btnPassConfirm.setEnabled(true);
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        });
    }
}
