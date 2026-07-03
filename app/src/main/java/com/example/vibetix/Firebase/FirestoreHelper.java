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
                List<Event> ongoing   = new ArrayList<>();
                List<Event> approved  = new ArrayList<>();
                List<Event> completed = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snap) {
                    Event e = docToEvent(doc);
                    if (e == null) continue;
                    String st = doc.getString("status");
                    if ("ongoing".equals(st))        ongoing.add(e);
                    else if ("approved".equals(st))  approved.add(e);
                    else                             completed.add(e);
                }
                // Sort: ongoing đầu, approved (sắp diễn ra gần nhất), completed cuối
                sortByDate(approved, true);
                sortByDate(ongoing, true);
                sortByDate(completed, false); // mới nhất lên đầu trong completed
                List<Event> events = new ArrayList<>();
                events.addAll(ongoing);
                events.addAll(approved);
                events.addAll(completed);
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
            if (catId != null) e.setCategoryId(catId);

            // Đọc status thực từ Firestore
            String firestoreStatus = doc.getString("status");
            if (firestoreStatus == null) firestoreStatus = "";
            // Nếu event approved nhưng end_time đã qua → coi như completed ở client
            if ("approved".equals(firestoreStatus) || "ongoing".equals(firestoreStatus)) {
                com.google.firebase.Timestamp endTs = doc.getTimestamp("end_time");
                if (endTs == null) endTs = doc.getTimestamp("start_time");
                if (endTs != null && endTs.toDate().getTime() < System.currentTimeMillis()) {
                    firestoreStatus = "completed";
                }
            }
            e.setStatus(firestoreStatus);
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

            // start_time / end_time as Timestamp object (getStartTime() sẽ format ra String)
            com.google.firebase.Timestamp startTs = doc.getTimestamp("start_time");
            if (startTs != null) e.setStartTimeObject(startTs);
            com.google.firebase.Timestamp endTsObj = doc.getTimestamp("end_time");
            if (endTsObj != null) e.setEndTimeObject(endTsObj);

            // Venue details
            e.setVenueName(doc.getString("venue_name"));
            e.setVenueAddress(doc.getString("venue_address"));
            String venueCity2 = doc.getString("venue_city");
            if (venueCity2 != null && !venueCity2.isEmpty()) e.setVenueCity(venueCity2);

            // Hình thức tổ chức, link online, giới hạn độ tuổi
            e.setEventType(doc.getString("event_type"));
            e.setOnlineLink(doc.getString("online_link"));
            e.setAgeRestriction(doc.getString("age_restriction"));

            // Organizer
            e.setOrganizerId(doc.getString("organizer_id"));

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
            .whereEqualTo(Constants.FIELD_EVENT_ID, eventId)
            .get()
            .addOnSuccessListener(snap -> {
                if (snap == null || snap.isEmpty()) {
                    callback.onStatsLoaded(0, 0);
                    return;
                }
                
                List<com.example.vibetix.Models.OrderItem> allItems = new ArrayList<>();
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

                List<String> orderIdList = new ArrayList<>(orderIds);
                List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot>> orderTasks = new ArrayList<>();
                
                for (int i = 0; i < orderIdList.size(); i += 10) {
                    List<String> chunk = orderIdList.subList(i, Math.min(i + 10, orderIdList.size()));
                    orderTasks.add(db.collection(FirebaseCollections.ORDERS)
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                            .get());
                }

                com.google.android.gms.tasks.Tasks.whenAllSuccess(orderTasks).addOnSuccessListener(orderResults -> {
                    Map<String, String> orderStatusMap = new HashMap<>();
                    for (Object orderResult : orderResults) {
                        com.google.firebase.firestore.QuerySnapshot orderSnap = (com.google.firebase.firestore.QuerySnapshot) orderResult;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : orderSnap.getDocuments()) {
                            String status = doc.getString(Constants.FIELD_STATUS);
                            orderStatusMap.put(doc.getId(), status != null ? status.toUpperCase() : Constants.ORDER_STATUS_PENDING);
                        }
                    }

                    long totalTickets = 0;
                    double totalRevenue = 0;
                    
                    for (com.example.vibetix.Models.OrderItem item : allItems) {
                        String status = orderStatusMap.get(item.getOrderId());
                        boolean isPaid = status != null &&
                                (status.equals(Constants.ORDER_STATUS_COMPLETED) || 
                                 status.equals(Constants.ORDER_STATUS_CONFIRMED) || 
                                 status.equals(Constants.ORDER_STATUS_PAID));
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

    private static void sortByDate(List<Event> list, boolean ascending) {
        list.sort((a, b) -> {
            String da = a.getDate() != null ? a.getDate() : "";
            String db2 = b.getDate() != null ? b.getDate() : "";
            // dd/MM/yyyy → compare as yyyy/MM/dd
            String ka = da.length() == 10 ? da.substring(6)+da.substring(3,5)+da.substring(0,2) : da;
            String kb = db2.length() == 10 ? db2.substring(6)+db2.substring(3,5)+db2.substring(0,2) : db2;
            return ascending ? ka.compareTo(kb) : kb.compareTo(ka);
        });
    }
}
