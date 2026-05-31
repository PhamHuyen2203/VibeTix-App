package com.example.vibetix.Fragments.User;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.vibetix.Models.Ticket;
import com.example.vibetix.R;

/**
 * Dialog cho phép user bán lại / pass vé.
 * TODO: Tích hợp Firestore update khi backend sẵn sàng.
 */
public class PassTicketDialogFragment extends DialogFragment {

    private static final String ARG_TICKET_ID    = "ticket_id";
    private static final String ARG_EVENT_TITLE  = "event_title";

    private Runnable onSuccess;

    public static PassTicketDialogFragment newInstance(Ticket ticket, Runnable onSuccess) {
        PassTicketDialogFragment f = new PassTicketDialogFragment();
        f.onSuccess = onSuccess;
        Bundle args = new Bundle();
        args.putString(ARG_TICKET_ID,   ticket.getId()         != null ? ticket.getId()         : "");
        args.putString(ARG_EVENT_TITLE, ticket.getEventTitle() != null ? ticket.getEventTitle() : "");
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_pass_ticket, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        // IDs từ dialog_pass_ticket.xml
        TextView txtEventTitle = view.findViewById(R.id.txtPassDialogEventTitle);
        if (txtEventTitle != null) txtEventTitle.setText(args.getString(ARG_EVENT_TITLE, ""));

        Button btnConfirm = view.findViewById(R.id.btnPassConfirm);
        Button btnCancel  = view.findViewById(R.id.btnPassCancel);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Tính năng bán lại vé — sắp ra mắt!", Toast.LENGTH_SHORT).show();
                dismiss();
                if (onSuccess != null) onSuccess.run();
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
