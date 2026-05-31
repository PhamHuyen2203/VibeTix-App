package com.example.vibetix.Fragments.User;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.vibetix.R;

/**
 * EventDetailFragment — trang chi tiết sự kiện.
 * TODO: Load event data từ Firestore theo eventId khi backend sẵn sàng.
 */
public class EventDetailFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";

    public static EventDetailFragment newInstance(String eventId) {
        EventDetailFragment f = new EventDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String eventId = getArguments() != null ? getArguments().getString(ARG_EVENT_ID, "") : "";

        // Áp insets cho header
        View header = view.findViewById(R.id.layoutDetailHeader);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        // Back button
        View btnBack = view.findViewById(R.id.btnDetailBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        // Buy ticket button
        View btnBuyTicket = view.findViewById(R.id.btnBookTickets);
        if (btnBuyTicket != null) {
            btnBuyTicket.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Tính năng mua vé — sắp ra mắt!", Toast.LENGTH_SHORT).show());
        }

        // TODO: Load full event detail from Firestore with eventId
    }
}
