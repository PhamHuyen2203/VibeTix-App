package com.example.vibetix.Accessibility;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.example.vibetix.BuildConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PosterDescriptionService — gọi Gemini API mô tả ảnh poster sự kiện
 * cho người khiếm thị, kèm cache SharedPreferences để không gọi lặp lại.
 *
 * API key đọc từ BuildConfig.GEMINI_API_KEY (khai báo trong local.properties,
 * không hard-code trong source). Nếu key rỗng → trả lỗi hướng dẫn cấu hình.
 */
public class PosterDescriptionService {

    public interface Callback {
        void onSuccess(String description);
        void onError(String message);
    }

    private static final String PREFS_NAME = "poster_descriptions";
    // gemini-2.0-flash đã bị cắt free tier (limit 0) — dùng 2.5-flash
    private static final String GEMINI_MODEL = "gemini-2.5-flash";
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL
                    + ":generateContent?key=";

    private static final String PROMPT_VI =
            "Bạn là trợ lý cho người khiếm thị dùng ứng dụng mua vé sự kiện. "
            + "Hãy mô tả poster sự kiện trong ảnh bằng tiếng Việt, ngắn gọn trong 3 đến 4 câu, "
            + "tập trung vào: tên sự kiện, nghệ sĩ hoặc nhân vật chính, thời gian, địa điểm "
            + "và không khí tổng thể của poster. Không dùng ký tự đặc biệt, "
            + "trả lời như đang nói chuyện trực tiếp.";

    private final SharedPreferences cache;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PosterDescriptionService(Context context) {
        cache = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Mô tả poster của event. Check cache trước, chỉ gọi Gemini lần đầu.
     * Callback luôn chạy trên main thread.
     */
    public void describePoster(String eventId, String posterUrl, Callback callback) {
        if (posterUrl == null || posterUrl.isEmpty()) {
            callback.onError("Sự kiện này không có ảnh poster");
            return;
        }

        // 1. Cache hit → trả ngay
        String cached = cache.getString(eventId, null);
        if (cached != null && !cached.isEmpty()) {
            callback.onSuccess(cached);
            return;
        }

        // 2. Check API key
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("Chưa cấu hình khóa API Gemini. "
                    + "Thêm GEMINI_API_KEY vào file local.properties rồi build lại ứng dụng");
            return;
        }

        // 3. Gọi API trên background thread
        executor.execute(() -> {
            try {
                byte[] imageBytes = downloadImage(posterUrl);
                String description = callGemini(apiKey, imageBytes, guessMimeType(posterUrl));
                cache.edit().putString(eventId, description).apply();
                mainHandler.post(() -> callback.onSuccess(description));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(
                        "Không mô tả được ảnh lúc này, vui lòng thử lại sau"));
            }
        });
    }

    /** Xóa cache mô tả (dùng khi organizer đổi poster). */
    public void clearCache(String eventId) {
        cache.edit().remove(eventId).apply();
    }

    private static byte[] downloadImage(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setInstanceFollowRedirects(true);
        try (InputStream in = conn.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    private static String guessMimeType(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png"))  return "image/png";
        if (lower.contains(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private static String callGemini(String apiKey, byte[] imageBytes, String mimeType)
            throws Exception {
        // Build request body: prompt + inline image (base64)
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", PROMPT_VI);

        JsonObject inlineData = new JsonObject();
        inlineData.addProperty("mime_type", mimeType);
        inlineData.addProperty("data", Base64.encodeToString(imageBytes, Base64.NO_WRAP));
        JsonObject imagePart = new JsonObject();
        imagePart.add("inline_data", inlineData);

        JsonArray parts = new JsonArray();
        parts.add(textPart);
        parts.add(imagePart);
        JsonObject content = new JsonObject();
        content.add("parts", parts);
        JsonArray contents = new JsonArray();
        contents.add(content);
        JsonObject body = new JsonObject();
        body.add("contents", contents);

        HttpURLConnection conn =
                (HttpURLConnection) new URL(GEMINI_URL + apiKey).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream stream = code >= 200 && code < 300
                ? conn.getInputStream() : conn.getErrorStream();
        String response;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = stream.read(buf)) != -1) out.write(buf, 0, n);
            response = out.toString("UTF-8");
        } finally {
            conn.disconnect();
        }

        if (code < 200 || code >= 300) {
            throw new Exception("Gemini HTTP " + code + ": " + response);
        }

        // Parse: candidates[0].content.parts[0].text
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        return json.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString().trim();
    }
}
