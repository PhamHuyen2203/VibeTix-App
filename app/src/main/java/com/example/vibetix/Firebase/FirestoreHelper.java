package com.example.vibetix.Firebase;

import com.example.vibetix.Models.Event;
import com.example.vibetix.Utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Helper load events từ Firestore với schema thật:
 * - banner_url (không phải image_url)
 * - start_time  (không phải start_date)
 * - venue_id    → resolve city từ collection venues
 * - category_id → map sang app key (music/arts/workshop/tour/sports/festival)
 * - status: "approved" | "ongoing" (không có "published")
 */
public class FirestoreHelper {

    public interface OnEventsLoaded {
        void onSuccess(List<Event> events);
        void onFailure(Exception e);
    }

    // ── Category ID → App key — được load ĐỘNG từ Firestore ──────────────────
    // Không hardcode UUID vì teammate thường xuyên tạo lại categories collection
    public static final Map<String, String> CAT_MAP      = new HashMap<>();
    public static final Map<String, String> CAT_KEY_TO_ID = new HashMap<>();
    public static final Map<String, String> CAT_SLUG_TO_NAME = new HashMap<>();
    public static final Map<String, String> CAT_NAME_MAP = new HashMap<>(); // Add missing
    
    public static final Map<String, String> VENUE_NAME_CACHE = new HashMap<>(); // Add missing
    public static final Map<String, String> VENUE_ADDRESS_CACHE = new HashMap<>(); // Add missing

    /** Gọi 1 lần khi app khởi động để load category IDs thật từ Firestore */
    public static void loadCategoryCache(Runnable onDone) {
        FirebaseFirestore.getInstance()
            .collection("categories")
            .get()
            .addOnSuccessListener(snap -> {
                CAT_MAP.clear(); CAT_KEY_TO_ID.clear(); CAT_SLUG_TO_NAME.clear();
                for (QueryDocumentSnapshot doc : snap) {
                    String catId = doc.getString("category_id");
                    String slug  = doc.getString("slug");
                    String name  = doc.getString("name");
                    if (catId == null || slug == null) continue;
                    String appKey = slugToKey(slug);
                    CAT_MAP.put(catId, appKey);
                    CAT_KEY_TO_ID.put(appKey, catId);
                    if (name != null) CAT_SLUG_TO_NAME.put(slug, name);
                }
                if (onDone != null) onDone.run();
            })
            .addOnFailureListener(e -> { if (onDone != null) onDone.run(); });
    }

    private static String slugToKey(String slug) {
        if (slug == null) return "festival";
        if (slug.contains("nhac-song"))   return "music";
        if (slug.contains("san-khau"))    return "arts";
        if (slug.contains("hoi-thao") || slug.contains("workshop")) return "workshop";
        if (slug.contains("tham-quan") || slug.contains("trai-nghiem")) return "tour";
        if (slug.contains("the-thao"))    return "sports";
        return "festival"; // default cho Khác
    }

    // ── Venue cache (public để HomepageLoader dùng chung) ────────────────────
    public static final Map<String, String> VENUE_CACHE = new HashMap<>();

    /**
     * Load toàn bộ published events (approved + ongoing).
     * Tự động resolve venue_id → city và category_id → app key.
     */
    public static void loadEvents(OnEventsLoaded callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Bước 1: Load venues vào cache (nếu chưa có)
        if (VENUE_CACHE.isEmpty()) {
            db.collection("venues").get()
                .addOnSuccessListener(venueSnap -> {
                    for (QueryDocumentSnapshot vd : venueSnap) {
                        String vid  = vd.getString("venue_id");
                        String city = vd.getString("city");
                        if (vid != null && city != null) VENUE_CACHE.put(vid, city);
                    }
                    // Bước 2: Load events sau khi có venue cache
                    fetchEvents(db, callback);
                })
                .addOnFailureListener(e -> fetchEvents(db, callback)); // vẫn fetch events dù venue fail
        } else {
            fetchEvents(db, callback);
        }
    }

    private static void fetchEvents(FirebaseFirestore db, OnEventsLoaded callback) {
        db.collection("events")
            .whereIn("status", Arrays.asList("approved", "ongoing", "completed"))
            .get()
            .addOnSuccessListener(snap -> {
                List<Event> events = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snap) {
                    Event e = docToEvent(doc);
                    if (e != null) events.add(e);
                }
                callback.onSuccess(events);
            })
            .addOnFailureListener(callback::onFailure);
    }

    /** Map Firestore document → Event model với đúng field names */
    public static Event docToEvent(com.google.firebase.firestore.DocumentSnapshot doc) {
        try {
            String title = doc.getString("title");
            if (title == null || title.isEmpty()) return null;

            // Ảnh ngang — banner_url cho horizontal sections (trending, category)
            String imageUrl   = doc.getString("banner_url");
            // Ảnh dọc — poster_url cho portrait sections (featured)
            String posterUrl  = doc.getString("poster_url");
            if (imageUrl == null) imageUrl = posterUrl; // fallback

            // Ngày — start_time là Timestamp
            String date = "";
            com.google.firebase.Timestamp ts = doc.getTimestamp("start_time");
            if (ts != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                date = sdf.format(ts.toDate());
            }

            // Thành phố — resolve từ venue_id
            String venueId = doc.getString("venue_id");
            String city = venueId != null ? VENUE_CACHE.getOrDefault(venueId, "Việt Nam") : "Việt Nam";

            // Danh mục — map category_id → app key
            String catId    = doc.getString("category_id");
            String category = catId != null ? CAT_MAP.getOrDefault(catId, "music") : "music";

            // Giá
            long price = 0;
            Object p = doc.get("min_price");
            if (p instanceof Long)   price = (Long) p;
            else if (p instanceof Double) price = ((Double) p).longValue();

            Event e = new Event(doc.getId(), title, imageUrl, date, city, category, price);
            e.setVenueCity(city);
            e.setStatus(Constants.EVENT_STATUS_PUBLISHED);
            if (posterUrl != null) e.setPortraitImageUrl(posterUrl);
            
            long maxPrice = 0;
            Object pMax = doc.get("max_price");
            if (pMax instanceof Long)   maxPrice = (Long) pMax;
            else if (pMax instanceof Double) maxPrice = ((Double) pMax).longValue();
            
            e.setMaxPrice((double) maxPrice);
            
            // Description
            e.setDescription(doc.getString("description"));

            Boolean featured = doc.getBoolean("is_featured");
            if (Boolean.TRUE.equals(featured)) e.setFeatured(true);

            Long interest = doc.getLong("interest_count");
            if (interest != null) e.setInterestCount(interest.intValue());

            return e;
        } catch (Exception ex) { return null; }
    }

    public interface OnEventStatsLoaded {
        void onStatsLoaded(long totalTickets, double totalRevenue);
    }

    public static void calculateEventStats(String eventId, OnEventStatsLoaded callback) {
        if (eventId == null || callback == null) return;
        
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection(FirebaseCollections.ORDER_ITEMS)
            .whereEqualTo(com.example.vibetix.Utils.Constants.FIELD_EVENT_ID, eventId)
            .get()
            .addOnSuccessListener(snap -> {
                if (snap == null || snap.isEmpty()) {
                    callback.onStatsLoaded(0, 0);
                    return;
                }
                
                java.util.List<com.example.vibetix.Models.OrderItem> allItems = new java.util.ArrayList<>();
                java.util.Set<String> orderIds = new java.util.HashSet<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                    com.example.vibetix.Models.OrderItem item = doc.toObject(com.example.vibetix.Models.OrderItem.class);
                    if (item != null) {
                        allItems.add(item);
                        if (item.getOrderId() != null) orderIds.add(item.getOrderId());
                    }
                }

                if (orderIds.isEmpty()) {
                    callback.onStatsLoaded(0, 0);
                    return;
                }

                java.util.List<String> orderIdList = new java.util.ArrayList<>(orderIds);
                java.util.List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot>> orderTasks = new java.util.ArrayList<>();
                
                for (int i = 0; i < orderIdList.size(); i += 10) {
                    java.util.List<String> chunk = orderIdList.subList(i, Math.min(i + 10, orderIdList.size()));
                    orderTasks.add(db.collection(FirebaseCollections.ORDERS)
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                            .get());
                }

                com.google.android.gms.tasks.Tasks.whenAllSuccess(orderTasks).addOnSuccessListener(orderResults -> {
                    java.util.Map<String, String> orderStatusMap = new java.util.HashMap<>();
                    for (Object orderResult : orderResults) {
                        com.google.firebase.firestore.QuerySnapshot orderSnap = (com.google.firebase.firestore.QuerySnapshot) orderResult;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : orderSnap.getDocuments()) {
                            String status = doc.getString(com.example.vibetix.Utils.Constants.FIELD_STATUS);
                            orderStatusMap.put(doc.getId(), status != null ? status.toUpperCase() : com.example.vibetix.Utils.Constants.ORDER_STATUS_PENDING);
                        }
                    }

                    long totalTickets = 0;
                    double totalRevenue = 0;
                    
                    for (com.example.vibetix.Models.OrderItem item : allItems) {
                        String status = orderStatusMap.get(item.getOrderId());
                        boolean isPaid = status != null &&
                                (status.equals(com.example.vibetix.Utils.Constants.ORDER_STATUS_COMPLETED) || 
                                 status.equals(com.example.vibetix.Utils.Constants.ORDER_STATUS_CONFIRMED) || 
                                 status.equals(com.example.vibetix.Utils.Constants.ORDER_STATUS_PAID));
                        if (isPaid) {
                            long q = item.getQuantity();
                            totalTickets += q;
                            totalRevenue += item.getPricePerTicket() * q;
                        }
                    }
                    
                    callback.onStatsLoaded(totalTickets, totalRevenue);
                }).addOnFailureListener(e -> {
                    callback.onStatsLoaded(0, 0);
                });
            }).addOnFailureListener(e -> {
                callback.onStatsLoaded(0, 0);
            });
    }
}
