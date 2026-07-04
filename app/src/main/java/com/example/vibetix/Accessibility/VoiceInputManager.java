package com.example.vibetix.Accessibility;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/**
 * VoiceInputManager — wrapper quanh SpeechRecognizer của Android.
 * Nhận diện giọng nói tiếng Việt (vi-VN), trả kết quả qua Listener.
 *
 * Yêu cầu: quyền RECORD_AUDIO đã được cấp TRƯỚC khi gọi startListening()
 * (Activity chịu trách nhiệm xin quyền).
 */
public class VoiceInputManager {

    public interface Listener {
        /** Recognizer sẵn sàng — báo user bắt đầu nói. */
        void onReadyForSpeech();
        /** Nhận diện xong, trả về câu có độ tin cậy cao nhất. */
        void onResult(String text);
        /** Lỗi nhận diện (mất mạng, không nghe thấy gì...). message đã ở dạng tiếng Việt. */
        void onError(String message);
    }

    private final Context context;
    private SpeechRecognizer recognizer;
    private Listener listener;
    private boolean isListening = false;

    public VoiceInputManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isAvailable() {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    public boolean isListening() {
        return isListening;
    }

    /** Bắt đầu nghe. Nếu đang nghe rồi thì dừng phiên cũ trước. */
    public void startListening(Listener listener) {
        this.listener = listener;
        if (!isAvailable()) {
            listener.onError("Thiết bị không hỗ trợ nhận diện giọng nói");
            return;
        }
        stopListening();

        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                isListening = true;
                if (VoiceInputManager.this.listener != null)
                    VoiceInputManager.this.listener.onReadyForSpeech();
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { isListening = false; }

            @Override public void onError(int error) {
                isListening = false;
                if (VoiceInputManager.this.listener != null)
                    VoiceInputManager.this.listener.onError(errorMessage(error));
            }

            @Override public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()
                        && VoiceInputManager.this.listener != null) {
                    VoiceInputManager.this.listener.onResult(matches.get(0));
                } else if (VoiceInputManager.this.listener != null) {
                    VoiceInputManager.this.listener.onError("Không nghe rõ, vui lòng thử lại");
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        recognizer.startListening(intent);
    }

    /** Dừng nghe và giải phóng recognizer hiện tại. */
    public void stopListening() {
        isListening = false;
        if (recognizer != null) {
            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (Exception ignored) { }
            recognizer = null;
        }
    }

    public void destroy() {
        stopListening();
        listener = null;
    }

    private static String errorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Lỗi mạng, vui lòng kiểm tra kết nối";
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Không nghe thấy gì, vui lòng thử lại";
            case SpeechRecognizer.ERROR_AUDIO:
                return "Lỗi ghi âm";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Chưa được cấp quyền ghi âm";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Trợ lý đang bận, vui lòng thử lại";
            default:
                return "Lỗi nhận diện giọng nói, vui lòng thử lại";
        }
    }
}
