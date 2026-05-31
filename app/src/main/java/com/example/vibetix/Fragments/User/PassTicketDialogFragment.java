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
import com.example.vibetix.R;
import com.example.vibetix.Repositories.TicketRepository;

import java.text.DecimalFormat;

public class PassTicketDialogFragment extends DialogFragment {

    private Ticket ticket;
    private OnTicketPassedListener listener;

    private final TicketRepository ticketRepository = new TicketRepository();

    // Views
    private TextView txtPassDialogEventTitle;
    private TextView txtPassDialogOriginalPrice;
    private EditText etPassResalePrice;
    private Button btnPassCancel;
    private Button btnPassConfirm;

    private final DecimalFormat formatter = new DecimalFormat("#,###");

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
        btnPassCancel = view.findViewById(R.id.btnPassCancel);
        btnPassConfirm = view.findViewById(R.id.btnPassConfirm);
    }

    private void populateViews() {
        if (ticket == null) return;
        txtPassDialogEventTitle.setText(ticket.getEventTitle());
        txtPassDialogOriginalPrice.setText(getString(R.string.str_original_price_label, formatter.format(ticket.getPurchasePrice())));
    }

    private void setupClickListeners() {
        btnPassCancel.setOnClickListener(v -> dismiss());

        btnPassConfirm.setOnClickListener(v -> {
            String priceStr = etPassResalePrice.getText().toString().trim();
            if (priceStr.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.str_ticket_resale_input_empty), Toast.LENGTH_SHORT).show();
                return;
            }

            long resalePrice;
            try {
                resalePrice = Long.parseLong(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), getString(R.string.str_ticket_resale_invalid_price), Toast.LENGTH_SHORT).show();
                return;
            }

            if (resalePrice <= 0) {
                Toast.makeText(requireContext(), getString(R.string.str_ticket_resale_price_min), Toast.LENGTH_SHORT).show();
                return;
            }

            btnPassConfirm.setEnabled(false);
            Toast.makeText(requireContext(), getString(R.string.str_processing_payment), Toast.LENGTH_SHORT).show();

            ticketRepository.resellTicket(ticket.getId(), resalePrice, new TicketRepository.OnTicketActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(requireContext(), getString(R.string.str_ticket_resale_success), Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onTicketPassed();
                    }
                    dismiss();
                }

                @Override
                public void onFailure(Exception e) {
                    btnPassConfirm.setEnabled(true);
                    Toast.makeText(requireContext(), getString(R.string.str_payment_error) + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
