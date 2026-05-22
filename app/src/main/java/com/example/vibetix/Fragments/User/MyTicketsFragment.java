package com.example.vibetix.Fragments.User;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vibetix.R;

public class MyTicketsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        TextView txtPlaceholder = new TextView(requireContext());
        txtPlaceholder.setText(getString(R.string.str_my_tickets));
        txtPlaceholder.setGravity(android.view.Gravity.CENTER);
        return txtPlaceholder;
    }
}
