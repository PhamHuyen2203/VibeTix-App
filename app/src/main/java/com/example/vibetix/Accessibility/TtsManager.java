package com.example.vibetix.Accessibility;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

/**
 * TtsManager — đọc phản hồi bằng giọng tiếng Việt (TextToSpeech).
 * Text speak() trước khi engine init xong sẽ được giữ lại và đọc ngay khi sẵn sàng.
 *
 * Lưu ý: máy cần cài giọng vi-VN (Google TTS mặc định có sẵn trên hầu hết máy).
 */
public class TtsManager {

    private TextToSpeech tts;
    private boolean ready = false;
    private boolean viSupported = true;
    private String pendingText = null;
    private String lastSpoken = "";
    private int utteranceCounter = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable onSpeechDone = null;

    public TtsManager(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("vi", "VN"));
                viSupported = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
                if (!viSupported) {
                    // fallback giọng mặc định của máy — vẫn đọc được, phát âm kém hơn
                    tts.setLanguage(Locale.getDefault());
                }
                tts.setSpeechRate(1.0f);

                // Callback khi đọc XONG một câu (không fire khi bị stop/ngắt giữa chừng)
                // — dùng cho chế độ hội thoại liên tục: đọc xong thì tự nghe tiếp.
                // Lưu ý: onDone chạy trên background thread → post về main thread.
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) {}
                    @Override public void onDone(String utteranceId) {
                        notifySpeechDone();
                    }
                    @Override public void onError(String utteranceId) {
                        // lỗi đọc cũng coi như xong để không kẹt luồng hội thoại
                        notifySpeechDone();
                    }
                });

                ready = true;
                if (pendingText != null) {
                    String t = pendingText;
                    pendingText = null;
                    speak(t);
                }
            }
        });
    }

    private void notifySpeechDone() {
        Runnable callback = onSpeechDone;
        if (callback != null) {
            mainHandler.post(callback);
        }
    }

    /** Đăng ký callback chạy trên main thread mỗi khi TTS đọc xong trọn một câu. */
    public void setOnSpeechDoneListener(Runnable listener) {
        this.onSpeechDone = listener;
    }

    public boolean isReady() { return ready; }

    public boolean isVietnameseSupported() { return viSupported; }

    /** Đọc text, ngắt câu đang đọc dở (QUEUE_FLUSH). */
    public void speak(String text) {
        if (text == null || text.isEmpty()) return;
        lastSpoken = text;
        if (!ready) {
            pendingText = text;
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vibetix_tts_" + (utteranceCounter++));
    }

    /** Đọc nối tiếp sau câu hiện tại (QUEUE_ADD). */
    public void speakQueued(String text) {
        if (text == null || text.isEmpty()) return;
        if (!ready) {
            pendingText = (pendingText == null ? "" : pendingText + ". ") + text;
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "vibetix_tts_" + (utteranceCounter++));
    }

    /** Câu cuối cùng đã đọc — dùng cho lệnh "nhắc lại". */
    public String getLastSpoken() { return lastSpoken; }

    public boolean isSpeaking() {
        return ready && tts.isSpeaking();
    }

    public void stop() {
        pendingText = null;
        if (ready) tts.stop();
    }

    public void shutdown() {
        onSpeechDone = null;
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            ready = false;
        }
    }
}
