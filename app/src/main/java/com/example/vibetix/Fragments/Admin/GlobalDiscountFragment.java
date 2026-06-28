package com.example.vibetix.Fragments.Admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vibetix.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class GlobalDiscountFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_global_discount, container, false);

        recyclerView = view.findViewById(R.id.rv_discounts);
        fab = view.findViewById(R.id.fab_add_discount);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Setup Adapter

        fab.setOnClickListener(v -> showAddDiscountDialog());

        return view;
    }

    private void showAddDiscountDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_discount, null);
        EditText etCode = dialogView.findViewById(R.id.et_discount_code);
        EditText etPercentage = dialogView.findViewById(R.id.et_discount_percentage);
        EditText etExpiry = dialogView.findViewById(R.id.et_expiry_date);
        SwitchCompat switchActive = dialogView.findViewById(R.id.switch_discount_active);

        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save, null)
                .setNegativeButton(R.string.btn_cancel, (d, w) -> d.dismiss())
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            String percentageStr = etPercentage.getText().toString().trim();
            String expiry = etExpiry.getText().toString().trim();

            if (code.isEmpty() || percentageStr.isEmpty() || expiry.isEmpty()) {
                Toast.makeText(getContext(), R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int percentage = Integer.parseInt(percentageStr);
                if (percentage < 1 || percentage > 100) {
                    Toast.makeText(getContext(), R.string.error_invalid_percentage, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Logic to save discount
                Toast.makeText(getContext(), "Saved Discount: " + code, Toast.LENGTH_SHORT).show();
                dialog.dismiss();

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), R.string.error_invalid_number, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
