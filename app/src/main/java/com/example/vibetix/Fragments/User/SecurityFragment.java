package com.example.vibetix.Fragments.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.vibetix.R;
import com.example.vibetix.Utils.Constants;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SecurityFragment extends Fragment {

    private ImageView   btnSecurityBack;
    private LinearLayout rowSetupPin, rowChangePassword;
    private TextView    txtPinStatus;
    private Switch      switchPinForTickets;
    private LinearLayout sectionChangePassword;
    private EditText    edtCurrentPassword, edtNewPassword, edtConfirmNewPassword;
    private ImageView   btnToggleCurrent, btnToggleNew;
    private Button      btnSaveNewPassword;

    private SharedPreferences authPrefs, profilePrefs;
    private boolean showCurrentPw = false, showNewPw = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_security, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authPrefs    = requireContext().getSharedPreferences(Constants.PREFS_AUTH,    Context.MODE_PRIVATE);
        profilePrefs = requireContext().getSharedPreferences(Constants.PREFS_PROFILE, Context.MODE_PRIVATE);

        bindViews(view);
        applyInsets(view);
        loadState();
        setupListeners();
    }

    private void bindViews(View v) {
        btnSecurityBack      = v.findViewById(R.id.btnSecurityBack);
        rowSetupPin          = v.findViewById(R.id.rowSetupPin);
        rowChangePassword    = v.findViewById(R.id.rowChangePassword);
        txtPinStatus         = v.findViewById(R.id.txtPinStatus);
        switchPinForTickets  = v.findViewById(R.id.switchPinForTickets);
        sectionChangePassword = v.findViewById(R.id.sectionChangePassword);
        edtCurrentPassword   = v.findViewById(R.id.edtCurrentPassword);
        edtNewPassword       = v.findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = v.findViewById(R.id.edtConfirmNewPassword);
        btnToggleCurrent     = v.findViewById(R.id.btnToggleCurrent);
        btnToggleNew         = v.findViewById(R.id.btnToggleNew);
        btnSaveNewPassword   = v.findViewById(R.id.btnSaveNewPassword);
    }

    private void applyInsets(View root) {
        View header = root.findViewById(R.id.layoutSecurityHeader);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(0, top, 0, 0);
                return insets;
            });
        }
    }

    private void loadState() {
        boolean hasPin = profilePrefs.getString("user_pin", null) != null;
        boolean pinEnabled = profilePrefs.getBoolean("pin_for_tickets", false);

        if (txtPinStatus != null)
            txtPinStatus.setText(hasPin ? "Đã thiết lập" : "Chưa thiết lập");
        if (switchPinForTickets != null)
            switchPinForTickets.setChecked(pinEnabled && hasPin);
    }

    private void setupListeners() {

        if (btnSecurityBack != null) {
            btnSecurityBack.setOnClickListener(v -> popBack());
        }

        if (rowSetupPin != null) {
            rowSetupPin.setOnClickListener(v -> showPinSetupDialog());
        }

        if (switchPinForTickets != null) {
            switchPinForTickets.setOnCheckedChangeListener((btn, checked) -> {
                boolean hasPin = profilePrefs.getString("user_pin", null) != null;
                if (checked && !hasPin) {
                    btn.setChecked(false);
                    Toast.makeText(requireContext(),
                            "Vui lòng thiết lập mã PIN trước", Toast.LENGTH_SHORT).show();
                    return;
                }
                profilePrefs.edit().putBoolean("pin_for_tickets", checked).apply();
                String msg = checked ? "Đã bật bảo vệ PIN cho Vé của tôi" : "Đã tắt bảo vệ PIN";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            });
        }

        if (rowChangePassword != null) {
            rowChangePassword.setOnClickListener(v -> {
                if (sectionChangePassword != null) {
                    boolean show = sectionChangePassword.getVisibility() == View.VISIBLE;
                    sectionChangePassword.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
        }

        if (btnToggleCurrent != null && edtCurrentPassword != null) {
            btnToggleCurrent.setOnClickListener(v -> {
                showCurrentPw = !showCurrentPw;
                edtCurrentPassword.setTransformationMethod(showCurrentPw
                        ? HideReturnsTransformationMethod.getInstance()
                        : PasswordTransformationMethod.getInstance());
                btnToggleCurrent.setImageResource(showCurrentPw
                        ? R.drawable.ic_auth_eye : R.drawable.ic_auth_eye_off);
                edtCurrentPassword.setSelection(edtCurrentPassword.getText().length());
            });
        }

        if (btnToggleNew != null && edtNewPassword != null) {
            btnToggleNew.setOnClickListener(v -> {
                showNewPw = !showNewPw;
                edtNewPassword.setTransformationMethod(showNewPw
                        ? HideReturnsTransformationMethod.getInstance()
                        : PasswordTransformationMethod.getInstance());
                btnToggleNew.setImageResource(showNewPw
                        ? R.drawable.ic_auth_eye : R.drawable.ic_auth_eye_off);
                edtNewPassword.setSelection(edtNewPassword.getText().length());
            });
        }

        if (btnSaveNewPassword != null) {
            btnSaveNewPassword.setOnClickListener(v -> saveNewPassword());
        }
    }

    private void saveNewPassword() {
        if (edtCurrentPassword == null || edtNewPassword == null || edtConfirmNewPassword == null) return;

        String current = edtCurrentPassword.getText().toString().trim();
        String newPw   = edtNewPassword.getText().toString().trim();
        String confirm = edtConfirmNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(current) || TextUtils.isEmpty(newPw) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(requireContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPw.length() < 6) {
            Toast.makeText(requireContext(), "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPw.equals(confirm)) {
            Toast.makeText(requireContext(), "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập lại để đổi mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnSaveNewPassword != null) { btnSaveNewPassword.setEnabled(false); btnSaveNewPassword.setText("Đang xử lý..."); }

        // Bước 1: Re-authenticate với mật khẩu hiện tại
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), current);
        user.reauthenticate(credential)
            .addOnSuccessListener(v -> {
                // Bước 2: Đổi mật khẩu mới
                user.updatePassword(newPw)
                    .addOnSuccessListener(v2 -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "✓ Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                        if (sectionChangePassword != null) sectionChangePassword.setVisibility(View.GONE);
                        if (edtCurrentPassword != null) edtCurrentPassword.setText("");
                        if (edtNewPassword     != null) edtNewPassword.setText("");
                        if (edtConfirmNewPassword != null) edtConfirmNewPassword.setText("");
                        if (btnSaveNewPassword != null) { btnSaveNewPassword.setEnabled(true); btnSaveNewPassword.setText("Lưu mật khẩu mới"); }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        if (btnSaveNewPassword != null) { btnSaveNewPassword.setEnabled(true); btnSaveNewPassword.setText("Lưu mật khẩu mới"); }
                        Toast.makeText(requireContext(), "Đổi mật khẩu thất bại. Thử lại sau.", Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                if (btnSaveNewPassword != null) { btnSaveNewPassword.setEnabled(true); btnSaveNewPassword.setText("Lưu mật khẩu mới"); }
                Toast.makeText(requireContext(), "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
            });
    }

    /** Dialog nhập mã PIN 6 ô — mỗi ô 1 số, auto-focus ô tiếp theo */
    private void showPinSetupDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_pin_dialog, null);

        EditText[] boxes = {
            dialogView.findViewById(R.id.pinBox1),
            dialogView.findViewById(R.id.pinBox2),
            dialogView.findViewById(R.id.pinBox3),
            dialogView.findViewById(R.id.pinBox4),
            dialogView.findViewById(R.id.pinBox5),
            dialogView.findViewById(R.id.pinBox6)
        };
        TextView txtError = dialogView.findViewById(R.id.txtPinError);

        // Auto-advance focus khi nhập xong mỗi ô
        for (int i = 0; i < boxes.length - 1; i++) {
            final int next = i + 1;
            boxes[i].addTextChangedListener(new android.text.TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) {}
                public void afterTextChanged(android.text.Editable s) {
                    if (s.length() == 1) boxes[next].requestFocus();
                }
            });
        }

        androidx.appcompat.app.AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Lưu PIN", null) // null để override
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(di -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                StringBuilder pin = new StringBuilder();
                for (EditText box : boxes) pin.append(box.getText().toString().trim());

                if (pin.length() < 6) {
                    if (txtError != null) {
                        txtError.setText("Vui lòng nhập đủ 6 chữ số");
                        txtError.setVisibility(View.VISIBLE);
                    }
                    return;
                }
                profilePrefs.edit().putString("user_pin", pin.toString()).apply();
                if (txtPinStatus != null) txtPinStatus.setText("Đã thiết lập");
                Toast.makeText(requireContext(), "✓ Đã thiết lập mã PIN thành công!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
        if (boxes[0] != null) boxes[0].requestFocus();
    }

    private void popBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }
}
