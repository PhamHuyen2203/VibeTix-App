package com.example.vibetix.Activities.Organizer;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import android.os.Vibrator;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.vibetix.R;
import com.example.vibetix.Utils.OfflineSyncManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class QrScannerActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private static final String TAG = "QrScannerActivity";

    private PreviewView cameraPreview;
    private View scannerFrame;
    private TextView tvInstruction;
    private FrameLayout btnBack;
    
    private LinearLayout panelResult;
    private TextView tvResultIcon, tvResultName, tvResultTicketType, tvResultMessage;
    private Button btnScanNext;
    
    private LinearLayout btnFlashlight, btnManualEntry;
    private ImageView ivFlashlightIcon;
    private Camera camera;
    private boolean isFlashOn = false;

    private ExecutorService cameraExecutor;
    private BarcodeScanner scanner;
    private boolean isScanning = true;

    public static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";
    private String eventId;
    private FirebaseFirestore db;

    private SwitchCompat switchOffline;
    private LinearLayout panelOfflineActions;
    private Button btnDownloadData, btnSyncData;
    private OfflineSyncManager offlineSyncManager;
    private boolean isOfflineMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null) {
            Toast.makeText(this, "Thiếu Event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        db = FirebaseFirestore.getInstance();
        offlineSyncManager = new OfflineSyncManager(this, eventId);

        initViews();
        setupMLKit();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void initViews() {
        cameraPreview = findViewById(R.id.cameraPreview);
        scannerFrame = findViewById(R.id.scannerFrame);
        tvInstruction = findViewById(R.id.tvInstruction);
        btnBack = findViewById(R.id.btnBack);

        panelResult = findViewById(R.id.panelResult);
        tvResultIcon = findViewById(R.id.tvResultIcon);
        tvResultName = findViewById(R.id.tvResultName);
        tvResultTicketType = findViewById(R.id.tvResultTicketType);
        tvResultMessage = findViewById(R.id.tvResultMessage);
        btnScanNext = findViewById(R.id.btnScanNext);
        
        btnFlashlight = findViewById(R.id.btnFlashlight);
        btnManualEntry = findViewById(R.id.btnManualEntry);
        ivFlashlightIcon = findViewById(R.id.ivFlashlightIcon);

        switchOffline = findViewById(R.id.switchOffline);
        panelOfflineActions = findViewById(R.id.panelOfflineActions);
        btnDownloadData = findViewById(R.id.btnDownloadData);
        btnSyncData = findViewById(R.id.btnSyncData);

        btnBack.setOnClickListener(v -> finish());
        btnScanNext.setOnClickListener(v -> resumeScanning());
        
        btnFlashlight.setOnClickListener(v -> toggleFlashlight());
        btnManualEntry.setOnClickListener(v -> showManualEntryDialog());

        switchOffline.setOnCheckedChangeListener((btn, isChecked) -> {
            isOfflineMode = isChecked;
            panelOfflineActions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateSyncButtonText();
        });

        btnDownloadData.setOnClickListener(v -> downloadOfflineData());
        btnSyncData.setOnClickListener(v -> syncOfflineData());
    }
    
    private void updateSyncButtonText() {
        int pendingCount = offlineSyncManager.getPendingSyncTickets().size();
        btnSyncData.setText(getString(R.string.str_qr_sync_count, pendingCount));
    }

    private void downloadOfflineData() {
        tvInstruction.setText(getString(R.string.str_qr_loading_data));
        btnDownloadData.setEnabled(false);
        db.collection("user_tickets").whereEqualTo("event_id", eventId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Key = ticket_code (để tra cứu khi quét QR)
                    // Value.userTicketId = document ID (để sync lên DB sau)
                    Map<String, com.example.vibetix.Models.UserTicket> ticketsMap = new HashMap<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.vibetix.Models.UserTicket ticket = doc.toObject(com.example.vibetix.Models.UserTicket.class);
                        if (ticket != null) {
                            if (ticket.getUserTicketId() == null) ticket.setUserTicketId(doc.getId());
                            // Dùng display_code làm key để thống nhất với nhập thủ công
                            String key = ticket.getDisplayCode();
                            if (key != null && !key.isEmpty()) {
                                ticketsMap.put(key, ticket);
                            }
                        }
                    }
                    offlineSyncManager.saveOfflineTickets(ticketsMap);
                    Toast.makeText(this, getString(R.string.str_qr_load_success, ticketsMap.size()), Toast.LENGTH_SHORT).show();
                    tvInstruction.setText(getString(R.string.str_qr_load_done_instruction));
                    btnDownloadData.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.str_qr_load_data_error), Toast.LENGTH_SHORT).show();
                    tvInstruction.setText(getString(R.string.str_qr_load_data_error));
                    btnDownloadData.setEnabled(true);
                });
    }

    private void syncOfflineData() {
        List<String> pendingIds = offlineSyncManager.getPendingSyncTickets();
        if (pendingIds.isEmpty()) {
            Toast.makeText(this, getString(R.string.str_qr_no_sync_needed), Toast.LENGTH_SHORT).show();
            return;
        }
        tvInstruction.setText(getString(R.string.str_qr_syncing));
        btnSyncData.setEnabled(false);
        
        // pendingIds lưu theo userTicketId (document ID) để update chính xác
        WriteBatch batch = db.batch();
        for (String id : pendingIds) {
            batch.update(db.collection("user_tickets").document(id),
                    "status", "used",
                    "checked_in_at", new java.util.Date());
        }
        
        batch.commit().addOnSuccessListener(aVoid -> {
            offlineSyncManager.savePendingSyncTickets(new java.util.ArrayList<>());
            updateSyncButtonText();
            Toast.makeText(this, getString(R.string.str_qr_sync_success), Toast.LENGTH_SHORT).show();
            tvInstruction.setText(getString(R.string.str_qr_sync_success_instruction));
            btnSyncData.setEnabled(true);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, getString(R.string.str_qr_sync_error), Toast.LENGTH_SHORT).show();
            tvInstruction.setText(getString(R.string.str_qr_sync_error));
            btnSyncData.setEnabled(true);
        });
    }

    private void toggleFlashlight() {
        if (camera != null) {
            CameraControl cameraControl = camera.getCameraControl();
            isFlashOn = !isFlashOn;
            cameraControl.enableTorch(isFlashOn);
            if (isFlashOn) {
                ivFlashlightIcon.setColorFilter(ContextCompat.getColor(this, R.color.clr_warning));
            } else {
                ivFlashlightIcon.setColorFilter(ContextCompat.getColor(this, R.color.clr_text_white));
            }
        }
    }
    
    private void showManualEntryDialog() {
        isScanning = false;
        AlertDialog.Builder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setTitle("Nhập mã vé thủ công");

        final EditText input = new EditText(this);
        input.setHint(getString(R.string.str_qr_hint_manual_code));
        builder.setView(input);
        
        builder.setPositiveButton("Kiểm tra", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (!code.isEmpty()) {
                handleScanResult(code);
            } else {
                resumeScanning();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> {
            dialog.cancel();
            resumeScanning();
        });
        builder.setOnCancelListener(dialog -> resumeScanning());
        builder.show();
    }

    private void setupMLKit() {
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build();
        scanner = BarcodeScanning.getClient(options);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(ImageProxy imageProxy) {
        if (!isScanning) {
            imageProxy.close();
            return;
        }

        if (imageProxy.getImage() != null) {
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty()) {
                            Barcode barcode = barcodes.get(0);
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null && isScanning) {
                                isScanning = false;
                                runOnUiThread(() -> handleScanResult(rawValue));
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Barcode scanning failed", e))
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private void handleScanResult(String code) {
        if (isOfflineMode) {
            handleScanOffline(code);
            return;
        }

        tvInstruction.setText(getString(R.string.str_qr_checking));
        
        // Dùng display_code để thống nhất với màn hình nhập thủ công
        db.collection("user_tickets")
                .whereEqualTo("display_code", code)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                        com.example.vibetix.Models.UserTicket ticket = documentSnapshot.toObject(com.example.vibetix.Models.UserTicket.class);
                        if (ticket != null) {
                            if (!eventId.equals(ticket.getEventId())) {
                                showScanError("Vé này không thuộc sự kiện hiện tại!");
                                return;
                            }
                            
                            tvResultName.setText(ticket.getFullName() != null ? ticket.getFullName() : getString(R.string.str_qr_anonymous_guest));
                            tvResultTicketType.setText(ticket.getTicketTypeName() != null ? ticket.getTicketTypeName() : getString(R.string.str_qr_standard_ticket));
                            
                            if (com.example.vibetix.Models.UserTicket.Status.USED.equals(ticket.getStatus())) {
                                showScanError("Vé đã được sử dụng (Check-in rồi)!");
                            } else if (com.example.vibetix.Models.UserTicket.Status.CANCELLED.equals(ticket.getStatus())) {
                                showScanError("Vé đã bị huỷ, không thể check-in!");
                            } else {
                                // Cập nhật trạng thái bằng document reference thực
                                documentSnapshot.getReference().update(
                                        "status", "used",
                                        "checked_in_at", new java.util.Date()
                                ).addOnSuccessListener(aVoid -> {
                                    showScanSuccess("Check-in thành công!");
                                }).addOnFailureListener(e -> showScanError("Lỗi cập nhật vé!"));
                            }
                        } else {
                            showScanError("Lỗi dữ liệu vé!");
                        }
                    } else {
                        showScanError("Không tìm thấy vé trong hệ thống!");
                    }
                })
                .addOnFailureListener(e -> showScanError("Lỗi kết nối kiểm tra vé!"));
    }
    
    private void handleScanOffline(String code) {
        tvInstruction.setText(getString(R.string.str_qr_checking_offline));
        Map<String, com.example.vibetix.Models.UserTicket> offlineTickets = offlineSyncManager.getOfflineTickets();
        
        // Map được build bằng ticket_code làm key (xem downloadOfflineData)
        if (offlineTickets.containsKey(code)) {
            com.example.vibetix.Models.UserTicket ticket = offlineTickets.get(code);
            if (ticket != null) {
                // Kiểm tra event_id để không cho check-in nhầm sự kiện khi offline
                if (ticket.getEventId() != null && !eventId.equals(ticket.getEventId())) {
                    showScanError("Vé này không thuộc sự kiện hiện tại!");
                    return;
                }
                tvResultName.setText(ticket.getFullName() != null ? ticket.getFullName() : getString(R.string.str_qr_anonymous_guest));
                tvResultTicketType.setText(ticket.getTicketTypeName() != null ? ticket.getTicketTypeName() : getString(R.string.str_qr_standard_ticket));
                
                if (com.example.vibetix.Models.UserTicket.Status.USED.equals(ticket.getStatus())) {
                    showScanError("Vé đã được sử dụng (Check-in rồi)!");
                } else if (com.example.vibetix.Models.UserTicket.Status.CANCELLED.equals(ticket.getStatus())) {
                    showScanError("Vé đã bị huỷ, không thể check-in!");
                } else {
                    // Dùng userTicketId (document ID) để sync lên DB sau
                    String docId = ticket.getUserTicketId() != null ? ticket.getUserTicketId() : code;
                    boolean success = offlineSyncManager.markTicketUsedOfflineByCode(code, docId);
                    if (success) {
                        showScanSuccess("Check-in Offline thành công!");
                        updateSyncButtonText();
                    } else {
                        showScanError("Lỗi cập nhật vé offline!");
                    }
                }
            }
        } else {
            showScanError("Không tìm thấy vé trong dữ liệu offline!");
        }
    }
    
    private void showScanSuccess(String msg) {
        playBeepAndVibrate(true);
        tvResultIcon.setText("✅");
        tvResultMessage.setText(msg);
        tvResultMessage.setTextColor(ContextCompat.getColor(this, R.color.clr_success));
        showResultPanel();
        // D1: tự động tiếp tục quét sau 1.5 giây (không cần bấm nút)
        new Handler(Looper.getMainLooper()).postDelayed(this::resumeScanning, 1500);
    }
    
    private void showScanError(String msg) {
        playBeepAndVibrate(false);
        tvResultName.setText(getString(R.string.str_qr_invalid_code));
        tvResultIcon.setText("❌");
        tvResultTicketType.setText(getString(R.string.str_qr_unknown));
        tvResultMessage.setText(msg);
        tvResultMessage.setTextColor(ContextCompat.getColor(this, R.color.clr_error));
        showResultPanel();
    }

    private void showResultPanel() {
        tvInstruction.setVisibility(View.GONE);
        panelResult.setVisibility(View.VISIBLE);

        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 400f, 0f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(panelResult, pvhY);
        animator.setDuration(400);
        animator.start();
    }
    
    private void playBeepAndVibrate(boolean success) {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (success) {
                    v.vibrate(100);
                } else {
                    v.vibrate(500); // Rung dài hơn nếu lỗi
                }
            }
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            if (success) {
                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200); 
            } else {
                toneG.startTone(ToneGenerator.TONE_SUP_ERROR, 400); 
            }
            new Handler(Looper.getMainLooper()).postDelayed(toneG::release, 450);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resumeScanning() {
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, 400f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(panelResult, pvhY);
        animator.setDuration(250);
        animator.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            panelResult.setVisibility(View.GONE);
            tvInstruction.setVisibility(View.VISIBLE);
            isScanning = true;
        }, 250);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
