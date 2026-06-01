package com.example.vibetix.Activities.Organizer;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vibetix.R;

public class QrScannerActivity extends AppCompatActivity {

    private TextView tvDisplayCode;
    private View scannerPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        initViews();
    }

    private void initViews() {
        tvDisplayCode = findViewById(R.id.tvDisplayCode);
        scannerPreview = findViewById(R.id.scannerPreview);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Giả lập quét được mã sau 2 giây để demo UI phản hồi
        new Handler().postDelayed(() -> {
            handleQrScanned("550e8400-e29b-41d4-a716-446655440000"); // Ví dụ mã UUID
        }, 2000);
    }

    /**
     * Xử lý sau khi camera đọc được mã QR
     * @param rawTicketCode Chuỗi UUID v4 từ mã QR
     */
    private void handleQrScanned(String rawTicketCode) {
        // 1. Tạo Display Code rút gọn (Lấy 4 ký tự đầu và 4 ký tự cuối)
        // Ví dụ: 550e...0000 -> 550E-0000
        String displayCode = "----";
        if (rawTicketCode != null && rawTicketCode.length() > 8) {
            displayCode = (rawTicketCode.substring(0, 4) + "-" + 
                          rawTicketCode.substring(rawTicketCode.length() - 4)).toUpperCase();
        }
        tvDisplayCode.setText(displayCode);

        // 2. Kiểm tra tính hợp lệ (Giả lập logic)
        boolean isValid = rawTicketCode.startsWith("550e"); // Demo: mã bắt đầu bằng 550e là hợp lệ
        
        showResultDialog(isValid);
    }

    /**
     * Hiển thị Dialog phản hồi kết quả trực quan trong 1.5 giây
     */
    private void showResultDialog(boolean isValid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_checkin_result, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View rootLayout = dialogView.findViewById(R.id.rootResultLayout);
        TextView tvStatus = dialogView.findViewById(R.id.tvResultStatus);

        if (isValid) {
            rootLayout.setBackgroundResource(R.drawable.bg_result_success);
            tvStatus.setText(getString(R.string.qr_valid_ticket));
        } else {
            rootLayout.setBackgroundResource(R.drawable.bg_result_error);
            tvStatus.setText(getString(R.string.qr_invalid_ticket));
        }

        dialog.show();

        // Tự động đóng dialog sau 1.5 giây
        new Handler().postDelayed(dialog::dismiss, 1500);
    }
}
