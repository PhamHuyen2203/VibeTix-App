package com.example.vibetix.Fragments.User;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vibetix.Activities.AuthActivity;
import com.example.vibetix.R;
import com.example.vibetix.Utils.Constants;

/**
 * ProfileFragment — placeholder.
 * Contains a logout button so the Auth flow can be tested.
 * Full profile UI will be implemented in a future task.
 */
public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Programmatic placeholder layout (no XML needed for this scaffold)
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);

        TextView txtTitle = new TextView(requireContext());
        txtTitle.setText(getString(R.string.str_profile));
        txtTitle.setTextSize(20f);
        txtTitle.setGravity(Gravity.CENTER);
        txtTitle.setTextColor(getResources().getColor(R.color.clr_text_black, null));
        root.addView(txtTitle);

        // Logout button — clears auth state and returns to AuthActivity
        Button btnLogout = new Button(requireContext());
        btnLogout.setText(getString(R.string.str_logout));
        btnLogout.setTextColor(getResources().getColor(R.color.clr_text_white, null));
        btnLogout.setBackgroundResource(R.drawable.bg_btn_primary);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (52 * getResources().getDisplayMetrics().density));
        params.topMargin = (int) (24 * getResources().getDisplayMetrics().density);
        btnLogout.setLayoutParams(params);
        btnLogout.setStateListAnimator(null);
        btnLogout.setOnClickListener(v -> logout());
        root.addView(btnLogout);

        return root;
    }

    private void logout() {
        // Clear auth session
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(Constants.PREFS_AUTH, android.content.Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Navigate to AuthActivity, clear entire back stack
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
