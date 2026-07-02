package com.example.vibetix.Firebase;

import android.util.Log;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Utils.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HomepageLoader — tải dữ liệu thật cho từng section trang chủ.
 *
 * NGUYÊN TẮC QUERY: KHÔNG dùng orderBy kết hợp whereIn/whereEqualTo
 * vì Firestore yêu cầu composite index chưa được tạo.
 * → Lấy data thô từ Firestore → sort trong memory.
 *
 * ════════════════════════ CƠ CHẾ TỪNG SECTION ════════════════════════
 *
 * 1. BANNER: app_config/homepage.banner_event_ids (admin chọn)
 *    Fallback: events có is_featured=true, sort start_time ASC (in memory)
 *
 * 2. SỰ KIỆN ĐẶC BIỆT: is_featured=true + approved/ongoing
 *    Sort start_time ASC in memory → sự kiện sắp diễn ra trước
 *
 * 3. XU HƯỚNG: Đếm SUM(quantity) từ order_items theo event_id
 *    → Top 8 event bán nhiều vé nhất (tính từ tất cả đơn hàng)
 *    Fallback: approved/ongoing events sort start_time ASC
 *
 * 4. THEO DANH MỤC: events.category_id = UUID, approved/ongoing
 *    Sort start_time ASC in memory
 * ══════════════════════════════════════════════════════════════════════
 */
public class HomepageLoader {

    private static final String TAG = "HomepageLoader";

    // ── Category IDs — load ĐỘNG từ Firestore, không hardcode ────────────────
    // Dùng FirestoreHelper.CAT_KEY_TO_ID để lấy UUID hiện tại theo app key
    public static String getCatId(String appKey) {
        String id = FirestoreHelper.CAT_KEY_TO_ID.get(appKey);
        return id != null ? id : "";
    }
    // Backward-compat wrappers (dùng trong HomeFragment)
    public static final String CAT_MUSIC    = ""; // sẽ được resolve động
    public static final String CAT_ARTS     = "";
    public static final String CAT_WORKSHOP = "";
    public static final String CAT_TOUR     = "";
    public static final String CAT_SPORTS   = "";
    public static final String CAT_OTHER    = "";

    // Chỉ approved + ongoing mới được ưu tiên hiển thị trên trang chủ
    // completed → ẩn khỏi homepage (còn hiện trong trang Browse ở cuối)
    private static final List<String> ACTIVE = Arrays.asList("approved", "ongoing");

    public interface OnLoaded { void onSuccess(List<Event> events); }

    // ── 1. BANNER ─────────────────────────────────────────────────────────────
    public static void loadBanners(OnLoaded callback) {
        FirebaseFirestore.getInstance()
            .collection("app_config").document("homepage").get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    List<String> ids = (List<String>) doc.get("banner_event_ids");
                    if (ids != null && !ids.isEmpty()) {
                        fetchByIds(ids, 5, callback);
                        return;
                    }
                }
                loadFeatured(callback); // Fallback
            })
            .addOnFailureListener(e -> loadFeatured(callback));
    }

    // ── 2. FEATURED ───────────────────────────────────────────────────────────
    public static void loadFeatured(OnLoaded callback) {
        // Query đơn giản: chỉ filter is_featured — KHÔNG orderBy để tránh index
        FirebaseFirestore.getInstance()
            .collection("events")
            .whereEqualTo("is_featured", true)
            .limit(100)
            .get()
            .addOnSuccessListener(snap -> {
                List<Event> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snap) {
                    String status = doc.getString("status");
                    if (status == null || !ACTIVE.contains(status)) continue;
                    if (!isUpcomingOrOngoing(doc)) continue; // ẩn sự kiện đã qua ngày
                    Event e = FirestoreHelper.docToEvent(doc);
                    if (e != null) list.add(e);
                }
                // Sort: start_time ASC (sắp diễn ra sớm nhất trước)
                sortByStartTime(list, true);
                Log.d(TAG, "loadFeatured: " + snap.size() + " docs from query, " + list.size() + " after filter");
                // Nếu không có featured events → lấy events bất kỳ
                if (list.isEmpty()) loadAnyActiveEvents(8, callback);
                else callback.onSuccess(list.subList(0, Math.min(8, list.size())));
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "loadFeatured failed: " + e.getMessage());
                loadAnyActiveEvents(8, callback);
            });
    }

    // ── 3. TRENDING: Đếm vé bán từ order_items ────────────────────────────────
    public static void loadTrending(OnLoaded callback) {
        FirebaseFirestore.getInstance()
            .collection("order_items")
            .limit(500)
            .get()
            .addOnSuccessListener(snap -> {
                if (snap.isEmpty()) {
                    Log.d(TAG, "order_items empty, fallback to active events");
                    loadAnyActiveEvents(8, callback);
                    return;
                }
                // Đếm SUM(quantity) theo event_id
                Map<String, Integer> sales = new HashMap<>();
                for (QueryDocumentSnapshot doc : snap) {
                    String eid = doc.getString("event_id");
                    int qty = 1;
                    Object q = doc.get("quantity");
                    if (q instanceof Long) qty = ((Long)q).intValue();
                    if (eid != null && !eid.isEmpty())
                        sales.put(eid, sales.containsKey(eid) ? sales.get(eid)+qty : qty);
                }
                Log.d(TAG, "Trending: " + sales.size() + " unique events from order_items");

                // Top 8 IDs theo vé bán
                List<Map.Entry<String,Integer>> sorted = new ArrayList<>(sales.entrySet());
                Collections.sort(sorted, (a, b) -> b.getValue() - a.getValue());
                List<String> topIds = new ArrayList<>();
                for (int i = 0; i < Math.min(8, sorted.size()); i++)
                    topIds.add(sorted.get(i).getKey());

                fetchByIds(topIds, 8, callback);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "order_items query failed: " + e.getMessage());
                loadAnyActiveEvents(8, callback);
            });
    }

    // ── 4. THEO DANH MỤC ──────────────────────────────────────────────────────
    public static void loadByCategory(String categoryId, OnLoaded callback) {
        FirebaseFirestore.getInstance()
            .collection("events")
            .whereEqualTo("category_id", categoryId)
            .limit(100)
            .get()
            .addOnSuccessListener(snap -> {
                List<Event> active = new ArrayList<>();
                List<Event> past = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snap) {
                    String status = doc.getString("status");
                    if (status == null) continue;
                    boolean isActive = ACTIVE.contains(status) && isUpcomingOrOngoing(doc);
                    boolean isPast = "completed".equals(status)
                            || (ACTIVE.contains(status) && !isUpcomingOrOngoing(doc));
                    Event e = FirestoreHelper.docToEvent(doc);
                    if (e == null) continue;
                    if (isActive) active.add(e);
                    else if (isPast) past.add(e);
                }
                // Ưu tiên: sắp diễn ra trước → sự kiện đã qua xếp sau (mới nhất trước)
                sortByStartTime(active, true);
                sortByStartTime(past, false);
                List<Event> result = new ArrayList<>(active);
                int remaining = 10 - result.size();
                if (remaining > 0 && !past.isEmpty()) {
                    result.addAll(past.subList(0, Math.min(remaining, past.size())));
                }
                callback.onSuccess(result.subList(0, Math.min(10, result.size())));
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "loadByCategory(" + categoryId + ") failed: " + e.getMessage());
                callback.onSuccess(new ArrayList<>());
            });
    }

    // ── Helper: lấy events bất kỳ (approved/ongoing) ─────────────────────────
    private static void loadAnyActiveEvents(int limit, OnLoaded callback) {
        // Query đơn giản nhất — KHÔNG orderBy, KHÔNG whereIn
        FirebaseFirestore.getInstance()
            .collection("events")
            .limit(200)
            .get()
            .addOnSuccessListener(snap -> {
                List<Event> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snap) {
                    String status = doc.getString("status");
                    if (!"approved".equals(status) && !"ongoing".equals(status)) continue;
                    if (!isUpcomingOrOngoing(doc)) continue; // bỏ event đã qua ngày
                    Event e = FirestoreHelper.docToEvent(doc);
                    if (e != null) list.add(e);
                }
                sortByStartTime(list, true);
                Log.d(TAG, "loadAnyActiveEvents: " + list.size() + " events");
                callback.onSuccess(list.subList(0, Math.min(limit, list.size())));
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "loadAnyActiveEvents failed: " + e.getMessage());
                callback.onSuccess(new ArrayList<>());
            });
    }

    // ── Helper: fetch events theo list ID — dùng whereIn(documentId) batch (tránh N+1) ──
    private static void fetchByIds(List<String> ids, int limit, OnLoaded callback) {
        if (ids.isEmpty()) { callback.onSuccess(new ArrayList<>()); return; }
        List<String> wanted = new ArrayList<>(ids.subList(0, Math.min(ids.size(), limit)));
        Map<String, Event> byId = new HashMap<>();
        fetchByIdsBatch(wanted, 0, byId, () -> {
            // Giữ đúng thứ tự input (banner/trending cần top N theo thứ tự)
            List<Event> ordered = new ArrayList<>();
            for (String id : wanted) {
                Event e = byId.get(id);
                if (e != null) ordered.add(e);
            }
            callback.onSuccess(ordered);
        });
    }

    /** Fetch events theo batch 30 ID bằng whereIn(documentId) — 1 query / 30 doc thay vì N query */
    private static void fetchByIdsBatch(List<String> ids, int offset,
                                        Map<String, Event> acc, Runnable onDone) {
        if (offset >= ids.size()) { onDone.run(); return; }
        int end = Math.min(offset + 30, ids.size());
        List<String> batch = new ArrayList<>(ids.subList(offset, end));

        Log.d(TAG, "fetchByIdsBatch: querying " + batch.size() + " IDs, offset=" + offset);
        FirebaseFirestore.getInstance()
            .collection("events")
            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
            .get()
            .addOnSuccessListener(snap -> {
                Log.d(TAG, "fetchByIdsBatch: got " + snap.size() + " docs");
                for (QueryDocumentSnapshot doc : snap) {
                    String st = doc.getString("status");
                    if (!ACTIVE.contains(st)) continue; // bỏ completed/pending/draft
                    Event e = FirestoreHelper.docToEvent(doc);
                    if (e != null) acc.put(doc.getId(), e);
                }
                Log.d(TAG, "fetchByIdsBatch: acc size=" + acc.size());
                fetchByIdsBatch(ids, end, acc, onDone);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "fetchByIdsBatch FAILED: " + e.getMessage());
                fetchByIdsBatch(ids, end, acc, onDone);
            });
    }

    /**
     * Trả về true nếu sự kiện chưa kết thúc, dựa vào end_time (hoặc start_time).
     * Áp dụng cho cả "approved" lẫn "ongoing" — Firebase không tự update status theo ngày.
     * Nếu không có timestamp → giữ lại (không đủ thông tin để loại).
     */
    private static boolean isUpcomingOrOngoing(QueryDocumentSnapshot doc) {
        // Dùng end_time nếu có, fallback start_time
        com.google.firebase.Timestamp ts = doc.getTimestamp("end_time");
        if (ts == null) ts = doc.getTimestamp("start_time");
        if (ts == null) return true; // không có ngày → giữ lại
        long eventMs = ts.toDate().getTime();
        long todayStart = getTodayStartMs();
        return eventMs >= todayStart;
    }

    public static long getTodayStartMs() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // ── Sort helper ───────────────────────────────────────────────────────────
    private static void sortByStartTime(List<Event> list, boolean ascending) {
        Collections.sort(list, (a, b) -> {
            String da = parseDate(a.getDate()), db2 = parseDate(b.getDate());
            return ascending ? da.compareTo(db2) : db2.compareTo(da);
        });
    }

    private static String parseDate(String date) {
        if (date == null || date.isEmpty()) return "0000/00/00";
        String[] p = date.split("/");
        if (p.length != 3) return date;
        return p[2] + "/" + p[1] + "/" + p[0]; // yyyy/MM/dd
    }

    // ── Admin: thiết lập banner ───────────────────────────────────────────────
    public static void setAdminBanners(List<String> eventIds) {
        Map<String, Object> config = new HashMap<>();
        config.put("banner_event_ids", eventIds);
        FirebaseFirestore.getInstance()
            .collection("app_config").document("homepage")
            .set(config);
    }
}
