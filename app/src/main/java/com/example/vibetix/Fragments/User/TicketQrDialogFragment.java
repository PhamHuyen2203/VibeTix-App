package com.example.vibetix.Fragments.User;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.vibetix.Models.Ticket;
import com.example.vibetix.R;

/**
 * Dialog hiển thị mã QR của vé.
 * TODO: Tích hợp ZXing để tạo QR từ ticket.getTicketCode() khi backend sẵn sàng.
 */
public class TicketQrDialogFragment extends DialogFragment {

    private static final String ARG_DISPLAY_CODE = "display_code";
    private static final String ARG_EVENT_TITLE  = "event_title";
    private static final String ARG_EVENT_DATE   = "event_date";

    public static TicketQrDialogFragment newInstance(Ticket ticket) {
        TicketQrDialogFragment f = new TicketQrDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DISPLAY_CODE, ticket.getDisplayCode() != null ? ticket.getDisplayCode() : "");
        args.putString(ARG_EVENT_TITLE,  ticket.getEventTitle()  != null ? ticket.getEventTitle()  : "");
        args.putString(ARG_EVENT_DATE,   ticket.getEventDate()   != null ? ticket.getEventDate()   : "");
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_ticket_qr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        // IDs từ dialog_ticket_qr.xml
        TextView txtEventTitle  = view.findViewById(R.id.txtQrDialogEventTitle);
        TextView txtTicketId    = view.findViewById(R.id.txtQrDialogTicketId);

        if (txtEventTitle != null) txtEventTitle.setText(args.getString(ARG_EVENT_TITLE, ""));
        if (txtTicketId   != null) txtTicketId.setText(args.getString(ARG_DISPLAY_CODE, ""));

        View btnClose = view.findViewById(R.id.btnQrDialogClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
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
