package com.example.vibetix.Repositories;

import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.TicketType;
import com.example.vibetix.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventRepository {

    private final FirebaseFirestore db;
    private final CollectionReference eventsRef;
    private final CollectionReference ticketTypesRef;

    private static final Map<String, Event> MOCK_EVENTS = new HashMap<>();
    static {
        // e1: Võ Hà Trâm
        Event e1 = new Event("e1", "Mừng Ngày Hội Non Sông - Võ Hà Trâm", null, "01/05/2026", "Tòa nhà WB Business Center, Tầng G, 200 Pasteur, Quận 3, Hồ Chí Minh", "live-music", 350000);
        e1.setLocalImageResId(R.drawable.event_live_non_song);
        e1.setLocalPortraitImageResId(R.drawable.event_live_non_song_2);
        e1.setDescription("Mừng Ngày Hội Non Sông là đêm nhạc vô cùng đặc biệt mang âm hưởng hào hùng, sâu lắng với sự góp mặt của ca sĩ Võ Hà Trâm và các khách mời nổi tiếng. Hãy cùng hòa mình vào không gian nghệ thuật sang trọng.");
        MOCK_EVENTS.put("e1", e1);

        // e2 / b2: Quốc Thiên
        Event e2 = new Event("e2", "Private Show in Fantasy - Quốc Thiên", null, "16/05/2026", "Tòa nhà WB Business Center, Tầng G, 200 Pasteur, Gò Vấp, TP. HCM", "stage-arts", 800000);
        e2.setLocalImageResId(R.drawable.event_arts_private_fantasy);
        e2.setLocalPortraitImageResId(R.drawable.banner_private_show_fantasy);
        e2.setDescription("Private Show in Fantasy hứa hẹn mang đến không gian âm nhạc ấm cúng và đầy cảm xúc với giọng hát đầy tự sự và bay bổng của Quốc Thiên. Đến với đêm nhạc, quý vị sẽ được hòa mình vào những ca khúc ballad ngọt ngào.");
        MOCK_EVENTS.put("e2", e2);
        MOCK_EVENTS.put("b2", e2);

        // b1: VinhVerse / Lunch & Learn
        Event b1 = new Event("b1", "LUNCH & LEARN: Workshop về Phỏng vấn Ứng viên", null, "23/05/2026", "Tòa nhà WB Business Center, Tầng G, 200 Pasteur, Quận 3, Hồ Chí Minh", "workshop", 400000);
        b1.setLocalImageResId(R.drawable.event_live_non_song);
        b1.setLocalPortraitImageResId(R.drawable.event_live_non_song_2);
        b1.setDescription("Read Every Candidate Like A Pro là buổi workshop chuyên sâu dành cho các nhà quản lý, tuyển dụng chuyên nghiệp nhằm tối ưu hóa quy trình đánh giá và phỏng vấn ứng viên.\n\nTại buổi chia sẻ này, các chuyên gia hàng đầu sẽ hướng dẫn các kỹ thuật đặt câu hỏi tình huống, quan sát ngôn ngữ cơ thể, nắm bắt tâm lý ứng viên.");
        MOCK_EVENTS.put("b1", b1);

        // b3: Hòa nhạc Non Sông 2025
        Event b3 = new Event("b3", "Hòa nhạc Non Sông 2025", null, "01/06/2025", "Đà Nẵng", "live-music", 350000);
        b3.setLocalImageResId(R.drawable.banner_non_song);
        b3.setDescription("Hòa nhạc Non Sông 2025 là sự kiện nghệ thuật giao hưởng đỉnh cao tại Đà Nẵng, kết hợp giữa truyền thống hào hùng và âm nhạc hiện đại tinh tế.");
        MOCK_EVENTS.put("b3", b3);

        // e3: BẰNG KIỀU - CÒN MƯA NGANG QUA
        Event e3 = new Event("e3", "BẰNG KIỀU - CÒN MƯA NGANG QUA", null, "19/05/2025", "TP.HCM", "live-music", 350000);
        e3.setLocalImageResId(R.drawable.event_featured_bang_kieu);
        e3.setLocalPortraitImageResId(R.drawable.event_featured_bang_kieu);
        e3.setDescription("Đêm nhạc đặc biệt của nam ca sĩ Bằng Kiều với những tình khúc bất hủ đi cùng năm tháng, mang đến những xúc cảm sâu lắng đầy hồi tưởng.");
        MOCK_EVENTS.put("e3", e3);
        MOCK_EVENTS.put("rs1", e3);

        // e4: Vì Lý Do Đời - Mr. Siro
        Event e4 = new Event("e4", "Vì Lý Do Đời - Mr. Siro", null, "01/06/2025", "TP.HCM", "live-music", 600000);
        e4.setLocalImageResId(R.drawable.event_featured_vi_ly_doi);
        e4.setLocalPortraitImageResId(R.drawable.event_featured_vi_ly_doi);
        e4.setDescription("Gặp gỡ ông hoàng nhạc sầu Mr. Siro trong liveshow lớn nhất năm, thưởng thức những bản ballad làm nên tên tuổi của anh.");
        MOCK_EVENTS.put("e4", e4);

        // t1: FWS SEA 2025
        Event t1 = new Event("t1", "FWS SEA 2025", null, "15/06/2025", "TP.HCM", "sports", 1200000);
        t1.setLocalImageResId(R.drawable.event_trending_fws_sea);
        t1.setDescription("Giải đấu thể thao điện tử lớn nhất khu vực Đông Nam Á, quy tụ hàng loạt đội tuyển chuyên nghiệp và những trận đấu kịch tính.");
        MOCK_EVENTS.put("t1", t1);
        MOCK_EVENTS.put("rs3", t1);

        // t2: RISING FEST 2025
        Event t2 = new Event("t2", "RISING FEST 2025", null, "22/06/2025", "Hà Nội", "live-music", 900000);
        t2.setLocalImageResId(R.drawable.event_trending_rising_fest);
        t2.setDescription("Đại nhạc hội mùa hè sôi động bậc nhất dành cho giới trẻ, với sự bùng nổ của dàn line-up nghệ sĩ hot nhất hiện nay.");
        MOCK_EVENTS.put("t2", t2);

        // t3: Ultra Vietnam 2025
        Event t3 = new Event("t3", "Ultra Vietnam 2025", null, "12/07/2025", "TP.HCM", "live-music", 1500000);
        t3.setLocalImageResId(R.drawable.event_trending_rising_2);
        t3.setDescription("Lễ hội âm nhạc điện tử đẳng cấp quốc tế Ultra Music Festival lần đầu tiên có mặt tại Việt Nam với hệ thống âm thanh, ánh sáng đỉnh cao.");
        MOCK_EVENTS.put("t3", t3);
        MOCK_EVENTS.put("rs4", t3);

        // rs2: The Story Concert
        Event rs2 = new Event("rs2", "The Story Concert", null, "27/05/2025", "TP.HCM", "live-music", 580000);
        rs2.setLocalImageResId(R.drawable.event_featured_the_story);
        rs2.setDescription("Concert âm nhạc mang tính kể chuyện đầy sáng tạo, kết nối tâm hồn của các nghệ sĩ gạo cội hàng đầu Việt Nam.");
        MOCK_EVENTS.put("rs2", rs2);

        // lm1: Đêm nhạc Trịnh Công Sơn
        Event lm1 = new Event("lm1", "Đêm nhạc Trịnh Công Sơn", null, "28/05/2025", "TP.HCM", "live-music", 150000);
        lm1.setLocalImageResId(R.drawable.event_live_concert_1);
        lm1.setDescription("Đêm nhạc tưởng nhớ nhạc sĩ tài hoa Trịnh Công Sơn, hòa cùng những giai điệu mộc mạc hoài niệm.");
        MOCK_EVENTS.put("lm1", lm1);

        // lm2: NON SONG Live Show
        Event lm2 = new Event("lm2", "NON SONG Live Show", null, "05/06/2025", "Hà Nội", "live-music", 250000);
        lm2.setLocalImageResId(R.drawable.event_live_non_song);
        lm2.setDescription("Live show hoành tráng kỷ niệm chặng đường âm nhạc truyền thống kết hợp hiện đại đặc sắc tại Hà Nội.");
        MOCK_EVENTS.put("lm2", lm2);

        // lm3: Jazz Night HCM
        Event lm3 = new Event("lm3", "Jazz Night HCM", null, "10/06/2025", "TP.HCM", "live-music", 300000);
        lm3.setLocalImageResId(R.drawable.event_live_non_song_2);
        lm3.setDescription("Đêm nhạc Jazz nhẹ nhàng, sang trọng và đầy ngẫu hứng tinh tế cùng các nghệ sĩ Jazz nổi tiếng trong nước và quốc tế.");
        MOCK_EVENTS.put("lm3", lm3);

        // lm4: Live Concert Vol.2
        Event lm4 = new Event("lm4", "Live Concert Vol.2", null, "18/06/2025", "TP.HCM", "live-music", 400000);
        lm4.setLocalImageResId(R.drawable.event_live_concert_2);
        lm4.setDescription("Concert âm nhạc hoành tráng phần hai hứa hẹn những màn trình diễn bùng nổ vượt ngoài mong đợi.");
        MOCK_EVENTS.put("lm4", lm4);

        // sa1: Chèo Đất Việt
        Event sa1 = new Event("sa1", "Chèo Đất Việt", null, "30/05/2025", "Hà Nội", "stage-arts", 100000);
        sa1.setLocalImageResId(R.drawable.event_arts_traditional);
        sa1.setDescription("Vở chèo cổ truyền đậm đà bản sắc dân tộc, tái hiện những câu chuyện dân gian đầy triết lý nhân sinh.");
        MOCK_EVENTS.put("sa1", sa1);

        // sa2: Opera Night 2025
        Event sa2 = new Event("sa2", "Opera Night 2025", null, "07/06/2025", "TP.HCM", "stage-arts", 450000);
        sa2.setLocalImageResId(R.drawable.event_arts_concert);
        sa2.setDescription("Đêm nhạc kịch Opera cổ điển hoàng gia đỉnh cao, mang lại trải nghiệm nghệ thuật sang trọng hàng đầu.");
        MOCK_EVENTS.put("sa2", sa2);

        // sa3: Private Show in Fantasy
        Event sa3 = new Event("sa3", "Private Show in Fantasy", null, "14/06/2025", "TP.HCM", "stage-arts", 700000);
        sa3.setLocalImageResId(R.drawable.event_arts_private_fantasy);
        sa3.setDescription("Private show âm nhạc kết hợp kịch nghệ đầy huyền ảo, mở ra thế giới giả tưởng sống động.");
        MOCK_EVENTS.put("sa3", sa3);

        // sa4: Quốc Thiên Live
        Event sa4 = new Event("sa4", "Quốc Thiên Live", null, "21/06/2025", "TP.HCM", "stage-arts", 300000);
        sa4.setLocalImageResId(R.drawable.event_arts_quoc_thien);
        sa4.setDescription("Show ca nhạc đặc biệt của ca sĩ Quốc Thiên, thể hiện toàn bộ các bản hit được yêu thích nhất.");
        MOCK_EVENTS.put("sa4", sa4);

        // ws1: GSTAR SUMMIT 2025
        Event ws1 = new Event("ws1", "GSTAR SUMMIT 2025", null, "25/05/2025", "TP.HCM", "workshop", 500000);
        ws1.setLocalImageResId(R.drawable.event_ws_gstar_summit);
        ws1.setDescription("Hội nghị thượng đỉnh công nghệ và khởi nghiệp quy mô lớn nhất Việt Nam, kết nối hàng nghìn chuyên gia.");
        MOCK_EVENTS.put("ws1", ws1);

        // ws2: Workshop Âm nhạc & Nghệ thuật
        Event ws2 = new Event("ws2", "Workshop Âm nhạc & Nghệ thuật", null, "01/06/2025", "TP.HCM", "workshop", 100000);
        ws2.setLocalImageResId(R.drawable.event_ws_concert);
        ws2.setDescription("Buổi trải nghiệm thực hành và trao đổi kiến thức nghệ thuật, âm nhạc bổ ích cho những người đam mê sáng tạo.");
        MOCK_EVENTS.put("ws2", ws2);

        // tr1: Tour Di tích Văn Hóa - Vân Mộc
        Event tr1 = new Event("tr1", "Tour Di tích Văn Hóa - Vân Mộc", null, "29/05/2025", "Quảng Bình", "tour", 350000);
        tr1.setLocalImageResId(R.drawable.event_tour_mountain);
        tr1.setDescription("Tour hành trình lịch sử khám phá các di tích văn hóa nổi tiếng tại Vân Mộc, Quảng Bình.");
        MOCK_EVENTS.put("tr1", tr1);

        // tr2: Trải nghiệm suối nước nóng Đà Lạt
        Event tr2 = new Event("tr2", "Trải nghiệm suối nước nóng Đà Lạt", null, "10/06/2025", "Đà Lạt", "tour", 200000);
        tr2.setLocalImageResId(R.drawable.event_tour_outdoor);
        tr2.setDescription("Tour thư giãn, nghỉ dưỡng hòa mình cùng suối nước nóng thiên nhiên thơ mộng tại phố hoa Đà Lạt.");
        MOCK_EVENTS.put("tr2", tr2);

        // sp1: FWS SEA 2025 - Finals
        Event sp1 = new Event("sp1", "FWS SEA 2025 - Finals", null, "20/06/2025", "TP.HCM", "sports", 150000);
        sp1.setLocalImageResId(R.drawable.event_sports_esport);
        sp1.setDescription("Trận chung kết thể thao điện tử nảy lửa tranh ngôi vô địch danh giá nhất Đông Nam Á.");
        MOCK_EVENTS.put("sp1", sp1);

        // sp2: THE GLOBAL CHAMPIONSHIP 2025
        Event sp2 = new Event("sp2", "THE GLOBAL CHAMPIONSHIP 2025", null, "25/06/2025", "TP.HCM", "sports", 200000);
        sp2.setLocalImageResId(R.drawable.event_sports_global_champ);
        sp2.setDescription("Đại hội thi đấu thể thao quốc tế hoành tráng bậc nhất hội tụ các siêu sao từ mọi châu lục.");
        MOCK_EVENTS.put("sp2", sp2);

        // sp3: Playoffs 2025
        Event sp3 = new Event("sp3", "Playoffs 2025", null, "15/06/2025", "TP.HCM", "sports", 100000);
        sp3.setLocalImageResId(R.drawable.event_sports_playoffs);
        sp3.setDescription("Loạt trận vòng loại trực tiếp Playoffs căng thẳng nghẹt thở tranh tấm vé vào chung kết thế giới.");
        MOCK_EVENTS.put("sp3", sp3);
    }

    public interface OnEventLoadedListener {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }

    public interface OnTicketTypesLoadedListener {
        void onSuccess(List<TicketType> ticketTypes);
        void onFailure(Exception e);
    }

    public EventRepository() {
        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection(FirebaseCollections.EVENTS);
        ticketTypesRef = db.collection(FirebaseCollections.TICKET_TYPES);
    }

    public void getEventById(String eventId, OnEventLoadedListener listener) {
        eventsRef.document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null && event.getId() == null) {
                            event.setId(documentSnapshot.getId());
                        }
                        listener.onSuccess(event);
                    } else {
                        // Document does not exist in Firestore, fallback to local mock database
                        Event mockEvent = MOCK_EVENTS.get(eventId);
                        if (mockEvent != null) {
                            listener.onSuccess(mockEvent);
                        } else {
                            listener.onFailure(new Exception("Event not found in Firestore or mock database"));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Firestore failed, fallback to local mock database
                    Event mockEvent = MOCK_EVENTS.get(eventId);
                    if (mockEvent != null) {
                        listener.onSuccess(mockEvent);
                    } else {
                        listener.onFailure(e);
                    }
                });
    }

    public void getTicketTypesForEvent(String eventId, OnTicketTypesLoadedListener listener) {
        ticketTypesRef.whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<TicketType> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        TicketType type = doc.toObject(TicketType.class);
                        if (type.getId() == null) {
                            type.setId(doc.getId());
                        }
                        list.add(type);
                    }
                    listener.onSuccess(list);
                })
                .addOnFailureListener(listener::onFailure);
    }
}
