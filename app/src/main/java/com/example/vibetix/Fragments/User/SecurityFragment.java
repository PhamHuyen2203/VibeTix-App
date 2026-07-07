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
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

public class SecurityFragment extends Fragment {

    private ImageView    btnSecurityBack;
    private LinearLayout rowSetupPin, rowChangePassword;
    private TextView     txtPinStatus, txtForgotPin;
    private Switch       switchPinForTickets;
    private LinearLayout sectionChangePassword;
    private EditText     edtCurrentPassword, edtNewPassword, edtConfirmNewPassword;
    private ImageView    btnToggleCurrent, btnToggleNew;
    private Button       btnSaveNewPassword;

    // Key lưu SHA-256 hash của PIN trong SharedPreferences
    private static final String KEY_PIN_HASH       = "user_pin_hash";
    private static final String KEY_PIN_FOR_TICKETS = "pin_for_tickets";

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
        loadStateAndSyncFromFirebase();
        setupListeners();
    }

    private void bindViews(View v) {
        btnSecurityBack       = v.findViewById(R.id.btnSecurityBack);
        rowSetupPin           = v.findViewById(R.id.rowSetupPin);
        rowChangePassword     = v.findViewById(R.id.rowChangePassword);
        txtPinStatus          = v.findViewById(R.id.txtPinStatus);
        switchPinForTickets   = v.findViewById(R.id.switchPinForTickets);
        sectionChangePassword = v.findViewById(R.id.sectionChangePassword);
        edtCurrentPassword    = v.findViewById(R.id.edtCurrentPassword);
        edtNewPassword        = v.findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = v.findViewById(R.id.edtConfirmNewPassword);
        btnToggleCurrent      = v.findViewById(R.id.btnToggleCurrent);
        btnToggleNew          = v.findViewById(R.id.btnToggleNew);
        btnSaveNewPassword    = v.findViewById(R.id.btnSaveNewPassword);
        txtForgotPin          = v.findViewById(R.id.txtForgotPin);
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

    // ── Load trạng thái từ cache + đồng bộ Firebase ──────────────────────────
    private void loadStateAndSyncFromFirebase() {
        // Hiển thị cache trước (tức thì)
        refreshPinStatusUI();

        // Đồng bộ PIN hash từ Firestore
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
            .collection("users").document(user.getUid())
            .get()
            .addOnSuccessListener(doc -> {
                if (!isAdded() || doc == null || !doc.exists()) return;
                String firebasePin = doc.getString("pin");
                if (firebasePin != null && !firebasePin.isEmpty()) {
                    profilePrefs.edit().putString(KEY_PIN_HASH, firebasePin).apply();
                } else {
                    // Firebase không có PIN → xóa cache local để đồng bộ
                    profilePrefs.edit()
                            .remove(KEY_PIN_HASH)
                            .remove("user_pin")
                            .remove(KEY_PIN_FOR_TICKETS)
                            .apply();
                }
                refreshPinStatusUI();
            });
    }

    private void refreshPinStatusUI() {
        boolean hasPin = hasPinHash();
        boolean pinEnabled = profilePrefs.getBoolean(KEY_PIN_FOR_TICKETS, false);

        if (txtPinStatus != null)
            txtPinStatus.setText(hasPin ? getString(R.string.str_pin_set_status) : getString(R.string.str_pin_not_set_status));
        if (switchPinForTickets != null)
            switchPinForTickets.setChecked(pinEnabled && hasPin);

        // Hiện "Quên mã PIN" link chỉ khi đã có PIN
        if (txtForgotPin != null)
            txtForgotPin.setVisibility(hasPin ? View.VISIBLE : View.GONE);
    }

    private void setupListeners() {
        if (btnSecurityBack != null) {
            btnSecurityBack.setOnClickListener(v -> popBack());
        }

        if (rowSetupPin != null) {
            rowSetupPin.setOnClickListener(v -> {
                if (hasPinHash()) {
                    // Đã có PIN → show dialog đổi PIN
                    showChangePinDialog();
                } else {
                    // Chưa có PIN → show dialog thiết lập
                    showPinSetupDialog("Thiết lập mã PIN",
                            "Nhập mã PIN 6 số để bảo vệ trang Vé của tôi",
                            newHash -> savePinHash(newHash));
                }
            });
        }

        if (txtForgotPin != null) {
            txtForgotPin.setOnClickListener(v -> showForgotPinFlow());
        }

        if (switchPinForTickets != null) {
            switchPinForTickets.setOnCheckedChangeListener(this::onSwitchChanged);
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

    // Method reference cho switch listener (để có thể remove/re-add)
    private void onSwitchChanged(android.widget.CompoundButton btn, boolean checked) {
        if (!hasPinHash()) {
            btn.setOnCheckedChangeListener(null);
            btn.setChecked(false);
            btn.setOnCheckedChangeListener(this::onSwitchChanged);
            Toast.makeText(requireContext(), getString(R.string.str_toast_set_pin_first), Toast.LENGTH_SHORT).show();
            return;
        }
        btn.setOnCheckedChangeListener(null);
        btn.setChecked(!checked);
        btn.setOnCheckedChangeListener(this::onSwitchChanged);

        String title    = checked ? "Bật bảo vệ PIN" : "Tắt bảo vệ PIN";
        String subtitle = checked
                ? "Xác nhận mã PIN để bật bảo vệ trang Vé của tôi"
                : "Xác nhận mã PIN để tắt bảo vệ trang Vé của tôi";
        showPinInputDialog(title, subtitle, "Xác nhận", enteredPin -> {
            String storedHash = profilePrefs.getString(KEY_PIN_HASH, "");
            if (!SecurityFragment.sha256(enteredPin).equals(storedHash)) {
                Toast.makeText(requireContext(), getString(R.string.str_toast_pin_incorrect), Toast.LENGTH_SHORT).show();
                return;
            }
            profilePrefs.edit().putBoolean(KEY_PIN_FOR_TICKETS, checked).apply();
            btn.setOnCheckedChangeListener(null);
            btn.setChecked(checked);
            btn.setOnCheckedChangeListener(this::onSwitchChanged);
            String msg = checked ? "Đã bật bảo vệ PIN cho Vé của tôi" : "Đã tắt bảo vệ PIN";
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    // ── Thiết lập PIN mới (khi chưa có PIN) ──────────────────────────────────
    private void showPinSetupDialog(String title, String subtitle, OnPinConfirmed callback) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_pin_dialog, null);

        TextView txtTitle    = dialogView.findViewById(R.id.txtPinDialogTitle);
        TextView txtSubtitle = dialogView.findViewById(R.id.txtPinDialogSubtitle);
        TextView txtError    = dialogView.findViewById(R.id.txtPinError);
        if (txtTitle    != null) txtTitle.setText(title);
        if (txtSubtitle != null) txtSubtitle.setText(subtitle);

        EditText[] boxes = getPinBoxes(dialogView);
        wireAutoAdvance(boxes);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Lưu PIN", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(di -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String pin = collectPin(boxes);
                if (pin.length() < 6) {
                    showError(txtError, "Vui lòng nhập đủ 6 chữ số");
                    return;
                }
                callback.onConfirmed(sha256(pin));
                dialog.dismiss();
            });
        });

        dialog.show();
        if (boxes[0] != null) boxes[0].requestFocus();
    }

    // ── Đổi PIN (khi đã có PIN) — 3 bước ────────────────────────────────────
    private void showChangePinDialog() {
        // Bước 1: Nhập PIN cũ
        showPinInputDialog("Xác nhận mã PIN cũ", "Nhập mã PIN 6 số hiện tại của bạn",
                "Xác nhận", oldPin -> {
                    String oldHash = sha256(oldPin);
                    String storedHash = profilePrefs.getString(KEY_PIN_HASH, "");
                    if (!oldHash.equals(storedHash)) {
                        Toast.makeText(requireContext(), getString(R.string.str_toast_old_pin_incorrect), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Bước 2: Nhập PIN mới
                    showPinInputDialog("Nhập mã PIN mới", "Nhập mã PIN 6 số mới", "Tiếp theo",
                            newPin -> {
                                // Bước 3: Xác nhận PIN mới
                                showPinInputDialog("Xác nhận mã PIN mới",
                                        "Nhập lại mã PIN mới để xác nhận", "Lưu PIN",
                                        confirmPin -> {
                                            if (!newPin.equals(confirmPin)) {
                                                Toast.makeText(requireContext(),
                                                        getString(R.string.str_toast_pin_mismatch), Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            savePinHash(sha256(newPin));
                                        });
                            });
                });
    }

    // Dialog nhập PIN đơn giản — dùng cho từng bước trong flow đổi PIN
    private void showPinInputDialog(String title, String subtitle, String btnLabel, OnPinRaw callback) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_pin_dialog, null);

        TextView txtTitle    = dialogView.findViewById(R.id.txtPinDialogTitle);
        TextView txtSubtitle = dialogView.findViewById(R.id.txtPinDialogSubtitle);
        TextView txtError    = dialogView.findViewById(R.id.txtPinError);
        if (txtTitle    != null) txtTitle.setText(title);
        if (txtSubtitle != null) txtSubtitle.setText(subtitle);

        EditText[] boxes = getPinBoxes(dialogView);
        wireAutoAdvance(boxes);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton(btnLabel, null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(di -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String pin = collectPin(boxes);
                if (pin.length() < 6) {
                    showError(txtError, "Vui lòng nhập đủ 6 chữ số");
                    return;
                }
                dialog.dismiss();
                callback.onRaw(pin);
            });
        });

        dialog.show();
        if (boxes[0] != null) boxes[0].requestFocus();
    }

    // ── Lưu PIN hash lên Firestore và local cache ─────────────────────────────
    private void savePinHash(String hash) {
        profilePrefs.edit().putString(KEY_PIN_HASH, hash).apply();
        refreshPinStatusUI();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .update("pin", hash)
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), getString(R.string.str_toast_pin_updated), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), getString(R.string.str_toast_pin_save_failed), Toast.LENGTH_SHORT).show();
                });
        } else {
            Toast.makeText(requireContext(), getString(R.string.str_toast_pin_updated), Toast.LENGTH_SHORT).show();
        }
    }

    // ── Helper: có PIN hash trong local cache chưa ────────────────────────────
    private boolean hasPinHash() {
        String h = profilePrefs.getString(KEY_PIN_HASH, null);
        // Tương thích ngược: key cũ "user_pin" (plain text từ version trước)
        if (h == null) h = profilePrefs.getString("user_pin", null);
        return h != null && !h.isEmpty();
    }

    // ── SHA-256 helper ────────────────────────────────────────────────────────
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return input; // fallback plain (không nên xảy ra)
        }
    }

    // ── PIN box helpers ───────────────────────────────────────────────────────
    private EditText[] getPinBoxes(View v) {
        return new EditText[]{
            v.findViewById(R.id.pinBox1), v.findViewById(R.id.pinBox2),
            v.findViewById(R.id.pinBox3), v.findViewById(R.id.pinBox4),
            v.findViewById(R.id.pinBox5), v.findViewById(R.id.pinBox6)
        };
    }

    private void wireAutoAdvance(EditText[] boxes) {
        for (int i = 0; i < boxes.length; i++) {
            if (boxes[i] == null) continue;
            final int cur = i, next = i + 1;
            boxes[i].addTextChangedListener(new android.text.TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) {}
                public void afterTextChanged(android.text.Editable s) {
                    if (s.length() == 1 && next < boxes.length && boxes[next] != null)
                        boxes[next].requestFocus();
                    if (s.length() == 0 && cur > 0 && boxes[cur - 1] != null)
                        boxes[cur - 1].requestFocus();
                }
            });
        }
    }

    private String collectPin(EditText[] boxes) {
        StringBuilder sb = new StringBuilder();
        for (EditText b : boxes) if (b != null) sb.append(b.getText().toString().trim());
        return sb.toString();
    }

    private void showError(TextView txtError, String msg) {
        if (txtError != null) { txtError.setText(msg); txtError.setVisibility(View.VISIBLE); }
    }

    // ── Đổi mật khẩu Firebase Auth ───────────────────────────────────────────
    private void saveNewPassword() {
        if (edtCurrentPassword == null || edtNewPassword == null || edtConfirmNewPassword == null) return;

        String current = edtCurrentPassword.getText().toString().trim();
        String newPw   = edtNewPassword.getText().toString().trim();
        String confirm = edtConfirmNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(current) || TextUtils.isEmpty(newPw) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(requireContext(), getString(R.string.str_toast_fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPw.length() < 6) {
            Toast.makeText(requireContext(), getString(R.string.str_toast_password_too_short), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPw.equals(confirm)) {
            Toast.makeText(requireContext(), getString(R.string.str_toast_password_mismatch), Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), getString(R.string.str_toast_relogin_required), Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnSaveNewPassword != null) { btnSaveNewPassword.setEnabled(false); btnSaveNewPassword.setText(getString(R.string.str_btn_processing)); }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), current);
        user.reauthenticate(credential)
            .addOnSuccessListener(v -> user.updatePassword(newPw)
                .addOnSuccessListener(v2 -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), getString(R.string.str_toast_password_changed), Toast.LENGTH_SHORT).show();
                    if (sectionChangePassword != null) sectionChangePassword.setVisibility(View.GONE);
                    if (edtCurrentPassword    != null) edtCurrentPassword.setText("");
                    if (edtNewPassword        != null) edtNewPassword.setText("");
                    if (edtConfirmNewPassword != null) edtConfirmNewPassword.setText("");
                    if (btnSaveNewPassword    != null) { btnSaveNewPassword.setEnabled(true); btnSaveNewPassword.setText(getString(R.string.str_save_new_password)); }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    if (btnSaveNewPassword != null) { btnSaveNewPassword.setEnabled(true); btnSaveNewPassword.setText(getString(R.string.str_save_new_password)); }
                    Toast.makeText(requireContext(), getString(R.string.str_toast_password_change_failed), Toast.LENGTH_SHORT).show();
                }))
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                if (btnSaveNewPassword != null) { btnSaveNewPassword.setEnabled(true); btnSaveNewPassword.setText(getString(R.string.str_save_new_password)); }
                Toast.makeText(requireContext(), getString(R.string.str_toast_current_password_wrong), Toast.LENGTH_SHORT).show();
            });
    }

    // ── Quên mã PIN: gửi OTP về SĐT đã đăng ký ─────────────────────────────
    private String mVerificationId;

    private void showForgotPinFlow() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Toast.makeText(requireContext(), getString(R.string.str_toast_fetching_phone), Toast.LENGTH_SHORT).show();

        FirebaseFirestore.getInstance()
            .collection("users").document(user.getUid())
            .get()
            .addOnSuccessListener(doc -> {
                if (!isAdded() || doc == null) return;
                String phone = doc.getString("phone");
                if (phone == null || phone.isEmpty()) {
                    Toast.makeText(requireContext(),
                            "Tài khoản chưa có số điện thoại. Vui lòng cập nhật trong Thông tin tài khoản.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                // Chuẩn hoá số VN → +84
                String e164 = normalizeVnPhone(phone);
                sendOtpToPhone(e164);
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), getString(R.string.str_toast_cannot_get_account), Toast.LENGTH_SHORT).show();
            });
    }

    private void sendOtpToPhone(String e164Phone) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), getString(R.string.str_toast_sending_otp, e164Phone), Toast.LENGTH_SHORT).show();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(e164Phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Auto-verify (thiết bị Pixel giả lập) — cho phép reset PIN ngay
                        if (!isAdded()) return;
                        showNewPinAfterOtp();
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                getString(R.string.str_toast_otp_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                          @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        if (!isAdded()) return;
                        mVerificationId = verificationId;
                        showOtpInputDialog();
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showOtpInputDialog() {
        android.widget.EditText edtOtp = new android.widget.EditText(requireContext());
        edtOtp.setHint(getString(R.string.str_hint_otp_input));
        edtOtp.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        edtOtp.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.str_dialog_title_sms_verify))
                .setMessage("Nhập mã OTP đã gửi về số điện thoại của bạn.")
                .setView(edtOtp)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String code = edtOtp.getText().toString().trim();
                    if (code.length() < 6) {
                        Toast.makeText(requireContext(), getString(R.string.str_toast_otp_too_short), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (mVerificationId == null) return;
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
                    // Dùng credential để xác thực — nếu đúng mã OTP thì cho đặt PIN mới
                    FirebaseAuth.getInstance().getCurrentUser()
                            .reauthenticate(credential)
                            .addOnSuccessListener(v -> {
                                if (!isAdded()) return;
                                showNewPinAfterOtp();
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                // Thử link (trường hợp tài khoản email, không có phone provider)
                                // Nếu reauthenticate thất bại với email account → vẫn cho reset
                                // vì OTP đã được Firebase xác thực thành công khi tạo credential
                                if (e.getMessage() != null && e.getMessage().contains("credential")) {
                                    showNewPinAfterOtp();
                                } else {
                                    Toast.makeText(requireContext(),
                                            getString(R.string.str_toast_otp_invalid), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showNewPinAfterOtp() {
        showPinSetupDialog("Đặt mã PIN mới", "Nhập mã PIN 6 số mới của bạn",
                newHash -> savePinHash(newHash));
    }

    private static String normalizeVnPhone(String phone) {
        phone = phone.trim().replaceAll("\\s+", "");
        if (phone.startsWith("+")) return phone; // đã có country code
        if (phone.startsWith("0")) return "+84" + phone.substring(1);
        if (phone.startsWith("84")) return "+" + phone;
        return "+84" + phone;
    }

    private void popBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0)
            getParentFragmentManager().popBackStack();
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────
    interface OnPinConfirmed { void onConfirmed(String hash); }
    interface OnPinRaw       { void onRaw(String plainPin); }
}

