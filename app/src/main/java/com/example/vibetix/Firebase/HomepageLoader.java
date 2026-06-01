package com.example.vibetix.Firebase;

import com.example.vibetix.Models.Event;
import com.example.vibetix.Utils.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HomepageLoader — tải dữ liệu thật cho từng section của trang chủ.
 *
 * ════════════════════════════════════════════════════════════
 *  CƠ CHẾ PHÂN LOẠI SỰ KIỆN
 * ════════════════════════════════════════════════════════════
 *
 *  1. BANNER SLIDES
 *     - Đọc Firestore: app_config/homepage → trường banner_event_ids (mảng ID)
 *     - Admin thiết lập danh sách này qua Firebase Console (hoặc web admin sau)
 *     - Fallback: lấy events is_featured=true, sort theo start_time tăng dần
 *
 *  2. SỰ KIỆN ĐẶC BIỆT (Featured)
 *     - events where is_featured=true AND status IN [approved, ongoing]
 *     - Sort: start_time ascending (sự kiện sắp diễn ra trước)
 *
 *  3. XU HƯỚNG / XẾPHẠNG (Trending)
 *     - Đọc order_items collection → đếm số vé bán theo event_id → top 8
 *     - Fallback nếu order_items trống: dùng interest_count desc
 *
 *  4. THEO DANH MỤC (Live Music, Sân khấu, Workshop...)
 *     - events where category_id = <UUID tương ứng>
 *     - Category UUIDs lấy từ Firestore categories collection
 *     - Nếu teammate chưa gán đúng category → section trống → HomeFragment ẩn section đó
 *
 *  5. KHI TEAMMATE FIX CATEGORY:
 *     - Chỉ cần cập nhật đúng category_id trong Firestore events documents
 *     - Code không cần thay đổi gì — tự động hiển thị đúng section
 *
 * ════════════════════════════════════════════════════════════
 */
public class HomepageLoader {

    // ── Category UUIDs từ Firestore categories collection ─────────────────────
    public static final String CAT_MUSIC    = "7e3014ad-e70f-4349-a8c3-cc3e9a9329b1"; // Nhạc sống
    public static final String CAT_ARTS     = "c9800d36-ecaa-4bb9-a0dd-ed24665b4f46"; // Sân khấu & NT
    public static final String CAT_WORKSHOP = "6bdda95a-2232-4cd6-b3e0-33e223e22bfa"; // Hội thảo
    public static final String CAT_TOUR     = "e810fbc7-79c9-4764-aa81-88ad49e4a069"; // Tham quan
    public static final String CAT_SPORTS   = "afc9ed2a-c01c-4b78-b869-e23efd70a56e"; // Thể thao
    public static final String CAT_OTHER    = "c77c10df-b480-4b41-8fc7-440e9f1e391c"; // Khác

    private static final List<String> ACTIVE_STATUS = Arrays.asList("approved", "ongoing");

    public interface OnLoaded { void onSuccess(List<Event> events); }

    // ── 1. BANNER: Admin-controlled via app_config/homepage ───────────────────
    /**
     * Tải banner events.
     * Ưu tiên: app_config/homepage.banner_event_ids
     * Fallback: is_featured=true events
     */
    public static void loadBanners(OnLoaded callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("app_config").document("homepage").get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    List<String> ids = (List<String>) doc.get("banner_event_ids");
                    if (ids != null && !ids.isEmpty()) {
                        fetchEventsByIds(ids, 5, callback);
                        return;
                    }
                }
                // Fallback: is_featured events
                loadFeatured(callback);
            })
            .addOnFailureListener(e -> loadFeatured(callback));
    }

    // ── 2. FEATURED: is_featured=true, sắp diễn ra sớm nhất ──────────────────
    public static void loadFeatured(OnLoaded callback) {
        FirebaseFirestore.getInstance()
            .collection("events")
            .whereIn("status", ACTIVE_STATUS)
            .whereEqualTo("is_featured", true)
            .orderBy("start_time", Query.Direction.ASCENDING)
            .limit(8)
            .get()
            .addOnSuccessListener(snap -> {
                List<Event> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snap) {
                    Event e = FirestoreHelper.docToEvent(doc);
                    if (e != null) list.add(e);
                }
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onSuccess(new ArrayList<>()));
    }

    // ── 3. TRENDING: Xếp hạng theo vé bán (order_items) ──────────────────────
    /**
     * Đếm số vé bán ra theo event_id từ order_items.
     * Fallback: sort theo interest_count nếu order_items trống.
     */
    public static void loadTrending(OnLoaded callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("order_items").limit(200).get()
            .addOnSuccessListener(snap -> {
                // Đếm số order_items theo event_id
                Map<String, Integer> salesCount = new HashMap<>();
                for (QueryDocumentSnapshot doc : snap) {
                    String eventId = doc.getString("event_id");
                    if (eventId != null) {
                        salesCount.put(eventId, salesCount.containsKey(eventId)
                                ? salesCount.get(eventId) + 1 : 1);
                    }
                }

                if (salesCount.isEmpty()) {
                    // Fallback: interest_count
                    loadTrendingByInterest(callback);
                    return;
                }

                // Top 8 event IDs theo vé bán
                List<Map.Entry<String, Integer>> sorted = new ArrayList<>(salesCount.entrySet());
                Collections.sort(sorted, (a, b) -> b.getValue() - a.getValue());
                List<String> topIds = new ArrayList<>();
                for (int i = 0; i < Math.min(8, sorted.size()); i++) {
                    topIds.add(sorted.get(i).getKey());
                }
                fetchEventsByIds(topIds, 8, callback);
            })
            .addOnFailureListener(e -> loadTrendingByInterest(callback));
    }

    private static void loadTrendingByInterest(OnLoaded callback) {
        FirebaseFirestore.getInstance()
            .collection("events")
            .whereIn("status", ACTIVE_STATUS)
            .orderBy("interest_count", Query.Direction.DESCENDING)
            .limit(8)
            .get()
            .addOnSuccessListener(snap -> {
                List<Event> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snap) {
                    Event e = FirestoreHelper.docToEvent(doc);
                    if (e != null) list.add(e);
                }
                // Nếu vẫn trống, lấy random events mới nhất
                if (list.isEmpty()) loadLatestEvents(callback);
                else callback.onSuccess(list);
            })
            .addOnFailureListener(e -> loadLatestEvents(callback));
    }

    private static void loadLatestEvents(OnLoaded callback) {
        FirebaseFirestore.getInstance()
            .collection("events")
            .whereIn("status", ACTIVE_STATUS)
            .orderBy("start_time", Query.Direction.ASCENDING)
            .limit(8)
            .get()
            .addOnSuccessListener(snap -> {
                List<Event> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snap) {
                    Event e = FirestoreHelper.docToEvent(doc);
                    if (e != null) list.add(e);
                }
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onSuccess(new ArrayList<>()));
    }

    // ── 4. THEO DANH MỤC ──────────────────────────────────────────────────────
    public static void loadByCategory(String categoryId, OnLoaded callback) {
        FirebaseFirestore.getInstance()
            .collection("events")
            .whereIn("status", ACTIVE_STATUS)
            .whereEqualTo("category_id", categoryId)
            .orderBy("start_time", Query.Direction.ASCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener(snap -> {
                List<Event> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snap) {
                    Event e = FirestoreHelper.docToEvent(doc);
                    if (e != null) list.add(e);
                }
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onSuccess(new ArrayList<>()));
    }

    // ── Helper: lấy events theo danh sách IDs ─────────────────────────────────
    private static void fetchEventsByIds(List<String> ids, int limit, OnLoaded callback) {
        if (ids.isEmpty()) { callback.onSuccess(new ArrayList<>()); return; }
        int count = Math.min(ids.size(), limit);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Event> results = new ArrayList<>();
        AtomicInteger remaining = new AtomicInteger(count);

        for (int i = 0; i < count; i++) {
            db.collection("events").document(ids.get(i)).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        try {
                            String title = doc.getString("title");
                            if (title != null) {
                                String imgUrl = doc.getString("banner_url");
                                if (imgUrl == null) imgUrl = doc.getString("poster_url");
                                String date = "";
                                Timestamp ts = doc.getTimestamp("start_time");
                                if (ts != null) {
                                    date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                            .format(ts.toDate());
                                }
                                String city = FirestoreHelper.VENUE_CACHE.containsKey(doc.getString("venue_id"))
                                        ? FirestoreHelper.VENUE_CACHE.get(doc.getString("venue_id")) : "Việt Nam";
                                String catId = doc.getString("category_id");
                                String cat   = catId != null ? FirestoreHelper.CAT_MAP.getOrDefault(catId, "music") : "music";
                                long price = 0;
                                Object p = doc.get("min_price");
                                if (p instanceof Long) price = (Long) p;
                                else if (p instanceof Double) price = ((Double) p).longValue();
                                Event e = new Event(doc.getId(), title, imgUrl, date, city, cat, price);
                                e.setVenueCity(city);
                                e.setStatus(Constants.EVENT_STATUS_PUBLISHED);
                                Boolean feat = doc.getBoolean("is_featured");
                                if (Boolean.TRUE.equals(feat)) e.setFeatured(true);
                                synchronized (results) { results.add(e); }
                            }
                        } catch (Exception ignored) {}
                    }
                    if (remaining.decrementAndGet() == 0) callback.onSuccess(results);
                })
                .addOnFailureListener(e -> {
                    if (remaining.decrementAndGet() == 0) callback.onSuccess(results);
                });
        }
    }

    // ── Admin helper: Thiết lập banner events ─────────────────────────────────
    /**
     * Ghi danh sách event IDs cho banner vào app_config/homepage.
     * Admin gọi method này (hoặc cập nhật trực tiếp Firebase Console).
     *
     * Cách dùng: HomepageLoader.setAdminBanners(Arrays.asList("id1","id2","id3"))
     */
    public static void setAdminBanners(List<String> eventIds) {
        Map<String, Object> config = new HashMap<>();
        config.put("banner_event_ids", eventIds);
        FirebaseFirestore.getInstance()
            .collection("app_config").document("homepage")
            .set(config);
    }
}
