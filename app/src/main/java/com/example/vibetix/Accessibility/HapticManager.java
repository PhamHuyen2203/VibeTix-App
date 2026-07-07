package com.example.vibetix.Accessibility;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * HapticManager — phản hồi rung theo từng tình huống cho người khiếm thị.
 * minSdk 26 nên dùng thẳng VibrationEffect.
 */
public class HapticManager {

    private final Vibrator vibrator;

    public HapticManager(Context context) {
        vibrator = (Vibrator) context.getApplicationContext()
                .getSystemService(Context.VIBRATOR_SERVICE);
    }

    private boolean canVibrate() {
        return vibrator != null && vibrator.hasVibrator();
    }

    /** Rung ngắn 1 nhịp — trợ lý bắt đầu nghe. */
    public void listening() {
        if (!canVibrate()) return;
        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    /** Rung 2 nhịp ngắn — lệnh thực hiện thành công. */
    public void success() {
        if (!canVibrate()) return;
        long[] pattern = {0, 60, 80, 60};
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
    }

    /** Rung 1 nhịp dài — lỗi / không hiểu lệnh. */
    public void error() {
        if (!canVibrate()) return;
        vibrator.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    /** Rung theo AssistantResult. */
    public void feedback(AssistantResult result) {
        if (result == null) return;
        switch (result.getHapticType()) {
            case SUCCESS: success(); break;
            case ERROR:   error();   break;
            case NONE:    break;
        }
    }
}
