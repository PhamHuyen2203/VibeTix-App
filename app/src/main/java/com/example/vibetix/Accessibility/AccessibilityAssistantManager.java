package com.example.vibetix.Accessibility;

import android.os.Handler;
import android.os.Looper;
import android.text.Html;

import com.example.vibetix.Activities.User.UserMainActivity;
import com.example.vibetix.Firebase.FirebaseCollections;
import com.example.vibetix.Firebase.FirestoreHelper;
import com.example.vibetix.Fragments.User.EventDetailFragment;
import com.example.vibetix.Fragments.User.SelectTicketFragment;
import com.example.vibetix.Models.Event;
import com.example.vibetix.Models.Ticket;
import com.example.vibetix.R;
import com.example.vibetix.Repositories.TicketRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AccessibilityAssistantManager — bộ điều phối trung tâm của trợ lý accessibility.
 *
 * Luồng: user bấm mic → VoiceInputManager nghe → SpeechCommandParser parse
 * → executeCommand thực hiện → TtsManager đọc kết quả → HapticManager rung.
 *
 * Gắn với UserMainActivity (1 instance / activity). Activity chịu trách nhiệm
 * xin quyền RECORD_AUDIO trước khi gọi startListening().
 */
public class AccessibilityAssistantManager {

    private static final int MAX_SEARCH_RESULTS = 5;

    /** Tự tắt chế độ nghe nếu quá 10 giây không nhận được lệnh mới. */
    private static final long SILENCE_TIMEOUT_MS = 10_000;
    /** Delay nhỏ sau khi TTS đọc xong mới bật mic lại, tránh nghe dính đuôi giọng TTS. */
    private static final long RESUME_LISTEN_DELAY_MS = 300;

    private final UserMainActivity activity;
    private final VoiceInputManager voiceInput;
    private final SpeechCommandParser parser = new SpeechCommandParser();
    private final TtsManager tts;
    private final HapticManager haptic;
    private final PosterDescriptionService posterService;
    private final DecimalFormat priceFormatter = new DecimalFormat("#,###");
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Trạng thái hội thoại
    private ScreenContext screenContext = ScreenContext.HOME;
    private final List<Event> lastSearchResults = new ArrayList<>();
    private Event currentEvent = null;

    // ── Chế độ hội thoại liên tục ────────────────────────────────────────────
    // true = sau mỗi câu trả lời, trợ lý tự bật mic nghe lệnh tiếp theo
    private boolean conversationMode = false;
    // thời điểm nhận được lệnh hợp lệ gần nhất — để tính timeout 10s im lặng
    private long lastCommandTime = 0;
    // true = câu TTS đang đọc là câu trả lời cuối của 1 lệnh → đọc xong thì nghe tiếp
    private boolean resumeListenAfterSpeech = false;

    public AccessibilityAssistantManager(UserMainActivity activity) {
        this.activity = activity;
        this.voiceInput = new VoiceInputManager(activity);
        this.tts = new TtsManager(activity);
        this.haptic = new HapticManager(activity);
        this.posterService = new PosterDescriptionService(activity);

        // Đọc xong câu trả lời → tự bật mic nghe lệnh tiếp theo
        tts.setOnSpeechDoneListener(() -> {
            if (conversationMode && resumeListenAfterSpeech) {
                resumeListenAfterSpeech = false;
                mainHandler.postDelayed(this::beginListenCycle, RESUME_LISTEN_DELAY_MS);
            }
        });
    }

    // ── Screen context hooks (fragment gọi khi hiển thị) ────────────────────

    public void setScreenContext(ScreenContext context) {
        this.screenContext = context;
    }

    /** EventDetailFragment gọi sau khi load xong event để trợ lý biết đang xem gì. */
    public void setCurrentEvent(Event event) {
        this.currentEvent = event;
        this.screenContext = ScreenContext.EVENT_DETAIL;
    }

    // ── Voice flow ───────────────────────────────────────────────────────────

    /**
     * Gọi khi user bấm nút mic (quyền RECORD_AUDIO đã được cấp).
     * Hành vi theo trạng thái:
     * - Chưa bật             → bật chế độ hội thoại liên tục, bắt đầu nghe
     * - Đang đọc câu trả lời → ngắt đọc, nghe ngay (user muốn nói luôn)
     * - Đang nghe            → tắt chế độ nghe, thông báo
     */
    public void startListening() {
        if (!conversationMode) {
            conversationMode = true;
            lastCommandTime = System.currentTimeMillis();
            beginListenCycle();
        } else if (tts.isSpeaking()) {
            resumeListenAfterSpeech = false;
            beginListenCycle();
        } else {
            exitConversation("Chức năng nghe đã tắt. Nhấn nút mic để bật lại");
        }
    }

    /** Một vòng nghe: tắt TTS đang đọc dở, rung báo hiệu, bật recognizer. */
    private void beginListenCycle() {
        if (!conversationMode) return;
        tts.stop(); // tránh recognizer nghe nhầm giọng TTS
        haptic.listening();
        voiceInput.startListening(new VoiceInputManager.Listener() {
            @Override public void onReadyForSpeech() {
                // rung ở trên đã báo hiệu; không đọc gì để không lẫn vào ghi âm
            }
            @Override public void onResult(String text) {
                lastCommandTime = System.currentTimeMillis();
                handleSpokenText(text);
            }
            @Override public void onError(String message) {
                handleListenError(message);
            }
        });
    }

    /**
     * Lỗi nghe (thường là im lặng ~5s → SPEECH_TIMEOUT/NO_MATCH):
     * - Nếu đã quá 10s không có lệnh mới → tắt chế độ nghe + thông báo
     * - Chưa quá 10s → lặng lẽ nghe lại tiếp
     */
    private void handleListenError(String message) {
        if (!conversationMode) return;
        long silentFor = System.currentTimeMillis() - lastCommandTime;
        if (silentFor >= SILENCE_TIMEOUT_MS) {
            exitConversation("Không nghe thấy lệnh nào. Chức năng nghe đã tắt, "
                    + "vui lòng nhấn nút mic để bật lại");
        } else {
            beginListenCycle();
        }
    }

    /** Tắt chế độ hội thoại, đọc thông báo (không tự nghe lại sau câu này). */
    private void exitConversation(String announcement) {
        conversationMode = false;
        resumeListenAfterSpeech = false;
        voiceInput.stopListening();
        haptic.error();
        tts.speak(announcement);
    }

    /** Public để test không cần mic: đưa thẳng text vào pipeline. */
    public void handleSpokenText(String text) {
        AssistantCommand command = parser.parse(text);
        executeCommand(command);
    }

    // ── Command dispatch ─────────────────────────────────────────────────────

    private void executeCommand(AssistantCommand command) {
        switch (command.getType()) {
            case SEARCH_EVENT:    doSearch(command.getArgument()); break;
            case OPEN_EVENT:      doOpenEvent(command.getArgument()); break;
            case READ_DETAIL:     doReadDetail(); break;
            case DESCRIBE_POSTER: doDescribePoster(); break;
            case BOOK_TICKET:     doBookTicket(); break;
            case GO_BACK:         doGoBack(); break;
            case GO_HOME:         doGoHome(); break;
            case OPEN_MY_TICKETS: doOpenMyTickets(); break;
            case OPEN_EVENTS_TAB: doOpenEventsTab(); break;
            case OPEN_PROFILE:    doOpenProfile(); break;
            case DESCRIBE_SCREEN: doDescribeScreen(); break;
            case HELP:            deliver(AssistantResult.ok(helpText())); break;
            case STOP:
                exitConversation("Đã dừng. Chức năng nghe đã tắt, "
                        + "nhấn nút mic khi cần trợ giúp");
                break;
            case REPEAT:          doRepeat(); break;
            case UNKNOWN:
            default:
                deliver(AssistantResult.error(
                        "Tôi chưa hiểu câu \"" + command.getRawText()
                                + "\". Hãy nói trợ giúp để nghe các lệnh có thể dùng"));
        }
    }

    /**
     * Đọc message + rung theo kết quả. Đây là câu trả lời CUỐI của một lệnh
     * → đánh dấu để khi TTS đọc xong sẽ tự bật mic nghe lệnh tiếp theo.
     */
    private void deliver(AssistantResult result) {
        haptic.feedback(result);
        resumeListenAfterSpeech = conversationMode;
        tts.speak(result.getMessage());
    }

    // ── SEARCH_EVENT ─────────────────────────────────────────────────────────

    private void doSearch(String keyword) {
        if (keyword.isEmpty()) {
            deliver(AssistantResult.error(
                    "Bạn muốn tìm sự kiện gì? Ví dụ: tìm sự kiện nhạc rock"));
            return;
        }
        tts.speak("Đang tìm sự kiện " + keyword);

        FirebaseFirestore.getInstance()
                .collection(FirebaseCollections.EVENTS)
                .whereIn("status", Arrays.asList(Event.APPROVED, Event.ONGOING))
                .limit(100)
                .get()
                .addOnSuccessListener(snap -> {
                    String normKeyword = SpeechCommandParser.normalize(keyword);
                    lastSearchResults.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Event e = FirestoreHelper.docToEvent(doc);
                        if (e == null) continue;
                        if (e.getId() == null) e.setId(doc.getId());
                        if (matchesKeyword(e, normKeyword)) {
                            lastSearchResults.add(e);
                            if (lastSearchResults.size() >= MAX_SEARCH_RESULTS) break;
                        }
                    }
                    screenContext = ScreenContext.EVENT_LIST;
                    if (lastSearchResults.isEmpty()) {
                        deliver(AssistantResult.error("Không tìm thấy sự kiện nào cho từ khóa "
                                + keyword + ". Hãy thử từ khóa khác"));
                    } else {
                        deliver(AssistantResult.ok(buildSearchResultSpeech(keyword)));
                    }
                })
                .addOnFailureListener(e ->
                        deliver(AssistantResult.error("Lỗi khi tìm kiếm, vui lòng thử lại")));
    }

    private boolean matchesKeyword(Event e, String normKeyword) {
        StringBuilder haystack = new StringBuilder();
        if (e.getTitle() != null)     haystack.append(e.getTitle()).append(' ');
        if (e.getVenueName() != null) haystack.append(e.getVenueName()).append(' ');
        if (e.getVenueCity() != null) haystack.append(e.getVenueCity()).append(' ');
        if (e.getLocation() != null)  haystack.append(e.getLocation()).append(' ');
        return SpeechCommandParser.normalize(haystack.toString()).contains(normKeyword);
    }

    private String buildSearchResultSpeech(String keyword) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tìm thấy ").append(lastSearchResults.size())
          .append(" sự kiện cho từ khóa ").append(keyword).append(". ");
        for (int i = 0; i < lastSearchResults.size(); i++) {
            Event e = lastSearchResults.get(i);
            sb.append("Số ").append(i + 1).append(": ").append(e.getTitle());
            if (e.getStartTime() != null) sb.append(", diễn ra ").append(e.getStartTime());
            String venue = e.getVenueName() != null ? e.getVenueName() : e.getLocation();
            if (venue != null) sb.append(", tại ").append(venue);
            sb.append(". ");
        }
        sb.append("Hãy nói: mở số 1, để xem chi tiết sự kiện đầu tiên");
        return sb.toString();
    }

    // ── OPEN_EVENT ───────────────────────────────────────────────────────────

    private void doOpenEvent(String argument) {
        if (lastSearchResults.isEmpty()) {
            deliver(AssistantResult.error(
                    "Chưa có danh sách sự kiện. Hãy tìm kiếm trước, ví dụ: tìm sự kiện ca nhạc"));
            return;
        }

        Event target = null;
        int index = SpeechCommandParser.parseIndex(argument);
        if (index >= 1 && index <= lastSearchResults.size()) {
            target = lastSearchResults.get(index - 1);
        } else if (!argument.isEmpty()) {
            // thử khớp theo tên
            String normArg = SpeechCommandParser.normalize(argument);
            for (Event e : lastSearchResults) {
                if (e.getTitle() != null
                        && SpeechCommandParser.normalize(e.getTitle()).contains(normArg)) {
                    target = e;
                    break;
                }
            }
        } else if (lastSearchResults.size() == 1) {
            target = lastSearchResults.get(0);
        }

        if (target == null) {
            deliver(AssistantResult.error("Không rõ bạn muốn mở sự kiện nào. Hãy nói: mở số 1,"
                    + " đến số " + lastSearchResults.size()));
            return;
        }

        currentEvent = target;
        screenContext = ScreenContext.EVENT_DETAIL;
        activity.openSubFragment(EventDetailFragment.newInstance(target.getId()));
        deliver(AssistantResult.ok("Đang mở sự kiện " + target.getTitle()
                + ". Bạn có thể nói: đọc chi tiết, mô tả poster, hoặc đặt vé"));
    }

    // ── READ_DETAIL ──────────────────────────────────────────────────────────

    private void doReadDetail() {
        if (currentEvent == null) {
            deliver(AssistantResult.error(
                    "Chưa mở sự kiện nào. Hãy tìm và mở một sự kiện trước"));
            return;
        }
        deliver(AssistantResult.ok(buildDetailSpeech(currentEvent)));
    }

    private String buildDetailSpeech(Event e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sự kiện ").append(e.getTitle()).append(". ");
        if (e.getStartTime() != null) sb.append("Diễn ra ").append(e.getStartTime()).append(". ");
        String venue = e.getVenueName() != null ? e.getVenueName() : e.getLocation();
        if (venue != null) {
            sb.append("Địa điểm: ").append(venue);
            if (e.getVenueAddress() != null && !e.getVenueAddress().equals(venue)) {
                sb.append(", ").append(e.getVenueAddress());
            }
            sb.append(". ");
        }
        if (e.isFree()) {
            sb.append("Sự kiện miễn phí. ");
        } else if (e.getMinPrice() > 0) {
            sb.append("Giá vé từ ").append(priceFormatter.format(e.getMinPrice()))
              .append(" đồng. ");
        }
        if (e.getDescription() != null && !e.getDescription().isEmpty()) {
            String desc = Html.fromHtml(e.getDescription(), Html.FROM_HTML_MODE_COMPACT)
                    .toString().replaceAll("\\s+", " ").trim();
            if (desc.length() > 300) desc = desc.substring(0, 300) + "...";
            sb.append("Mô tả: ").append(desc).append(". ");
        }
        sb.append("Nói: đặt vé, để tiếp tục mua vé");
        return sb.toString();
    }

    // ── DESCRIBE_POSTER ──────────────────────────────────────────────────────

    private void doDescribePoster() {
        if (currentEvent == null) {
            deliver(AssistantResult.error(
                    "Chưa mở sự kiện nào. Hãy mở một sự kiện trước rồi yêu cầu mô tả poster"));
            return;
        }
        tts.speak("Đang phân tích ảnh poster, vui lòng đợi trong giây lát");
        posterService.describePoster(currentEvent.getId(), currentEvent.getPosterUrl(),
                new PosterDescriptionService.Callback() {
                    @Override public void onSuccess(String description) {
                        deliver(AssistantResult.ok(description));
                    }
                    @Override public void onError(String message) {
                        deliver(AssistantResult.error(message));
                    }
                });
    }

    // ── BOOK_TICKET ──────────────────────────────────────────────────────────

    private void doBookTicket() {
        if (currentEvent == null) {
            deliver(AssistantResult.error(
                    "Chưa chọn sự kiện nào để đặt vé. Hãy tìm và mở một sự kiện trước"));
            return;
        }
        // Mở thẳng màn chọn vé — bỏ qua slider captcha vì người khiếm thị
        // không thao tác kéo thả được (đã xác thực bằng đăng nhập)
        screenContext = ScreenContext.TICKET_SELECTION;
        activity.openSubFragment(SelectTicketFragment.newInstance(currentEvent.getId()));
        deliver(AssistantResult.ok("Đang mở màn chọn vé cho sự kiện "
                + currentEvent.getTitle()
                + ". Vui lòng chọn loại vé và số lượng trên màn hình"));
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private void doGoBack() {
        activity.onBackPressed();
        deliver(AssistantResult.ok("Đã quay lại màn hình trước"));
    }

    private void doGoHome() {
        activity.selectTab(R.id.tabHome);
        screenContext = ScreenContext.HOME;
        currentEvent = null;
        fetchAndReadEvents("Đã về trang chủ. ", true);
    }

    // ── OPEN_MY_TICKETS ──────────────────────────────────────────────────────

    private void doOpenMyTickets() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            deliver(AssistantResult.error(
                    "Bạn chưa đăng nhập. Vui lòng đăng nhập để xem vé của tôi"));
            return;
        }
        screenContext = ScreenContext.MY_TICKETS;
        activity.openTicketsTab();

        if (activity.isTicketsPinEnabled()) {
            // Có PIN gate → không đọc nội dung vé (chính là thứ PIN bảo vệ)
            deliver(AssistantResult.ok("Trang vé của tôi được bảo vệ bằng mã PIN. "
                    + "Vui lòng nhập mã PIN 6 số trên màn hình để tiếp tục"));
            return;
        }
        tts.speak("Đang mở vé của tôi");
        readMyTickets(user.getUid(), "Đã mở trang vé của tôi. ");
    }

    /** Đọc danh sách vé còn hiệu lực của user (tối đa 5 vé). */
    private void readMyTickets(String userId, String intro) {
        new TicketRepository().getActiveTickets(userId,
                new TicketRepository.OnTicketsLoadedListener() {
            @Override public void onSuccess(List<Ticket> tickets) {
                if (tickets.isEmpty()) {
                    deliver(AssistantResult.ok(intro
                            + "Bạn chưa có vé nào sắp diễn ra. "
                            + "Hãy nói: tìm sự kiện, để tìm và đặt vé"));
                    return;
                }
                StringBuilder sb = new StringBuilder(intro);
                sb.append("Bạn có ").append(tickets.size()).append(" vé. ");
                int count = Math.min(tickets.size(), 5);
                for (int i = 0; i < count; i++) {
                    Ticket t = tickets.get(i);
                    sb.append("Vé ").append(i + 1).append(": ");
                    if (t.getEventTitle() != null) sb.append(t.getEventTitle());
                    if (t.getTicketTypeName() != null)
                        sb.append(", loại vé ").append(t.getTicketTypeName());
                    if (t.getEventDate() != null)
                        sb.append(", diễn ra ").append(t.getEventDate());
                    sb.append(", ").append(speakableTicketStatus(t.getStatus()));
                    sb.append(". ");
                }
                if (tickets.size() > count) {
                    sb.append("Và ").append(tickets.size() - count).append(" vé khác. ");
                }
                deliver(AssistantResult.ok(sb.toString()));
            }
            @Override public void onFailure(Exception e) {
                deliver(AssistantResult.error("Lỗi khi tải danh sách vé, vui lòng thử lại"));
            }
        });
    }

    private static String speakableTicketStatus(String status) {
        if (status == null) return "";
        switch (status) {
            case "valid":   return "còn hiệu lực";
            case "used":    return "đã sử dụng";
            case "expired": return "đã hết hạn";
            default:        return status;
        }
    }

    // ── OPEN_EVENTS_TAB / OPEN_PROFILE / DESCRIBE_SCREEN ─────────────────────

    private void doOpenEventsTab() {
        activity.selectTab(R.id.tabEvents);
        screenContext = ScreenContext.EVENT_LIST;
        fetchAndReadEvents("Đã mở trang sự kiện. ", false);
    }

    private void doOpenProfile() {
        activity.selectTab(R.id.tabProfile);
        screenContext = ScreenContext.OTHER;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        StringBuilder sb = new StringBuilder("Đã mở trang cá nhân. ");
        if (user != null) {
            String name = user.getDisplayName();
            if (name != null && !name.isEmpty()) {
                sb.append("Xin chào ").append(name).append(". ");
            }
        }
        sb.append("Tại đây bạn có thể xem thông tin tài khoản, bảo mật và mật khẩu, "
                + "phương thức thanh toán, và cài đặt ứng dụng. "
                + "Các mục này thao tác trên màn hình");
        deliver(AssistantResult.ok(sb.toString()));
    }

    /** "Màn hình này có gì" — mô tả lại nội dung theo ngữ cảnh hiện tại. */
    private void doDescribeScreen() {
        switch (screenContext) {
            case EVENT_DETAIL:
                if (currentEvent != null) {
                    deliver(AssistantResult.ok("Bạn đang ở trang chi tiết sự kiện. "
                            + buildDetailSpeech(currentEvent)));
                    return;
                }
                break;
            case EVENT_LIST:
                if (!lastSearchResults.isEmpty()) {
                    StringBuilder sb = new StringBuilder("Bạn đang ở danh sách sự kiện, gồm: ");
                    for (int i = 0; i < lastSearchResults.size(); i++) {
                        sb.append("Số ").append(i + 1).append(": ")
                          .append(lastSearchResults.get(i).getTitle()).append(". ");
                    }
                    sb.append("Nói: mở số 1, để xem chi tiết");
                    deliver(AssistantResult.ok(sb.toString()));
                    return;
                }
                break;
            case MY_TICKETS:
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && !activity.isTicketsPinEnabled()) {
                    readMyTickets(user.getUid(), "Bạn đang ở trang vé của tôi. ");
                    return;
                }
                break;
            case TICKET_SELECTION:
                deliver(AssistantResult.ok("Bạn đang ở màn chọn vé"
                        + (currentEvent != null ? " cho sự kiện " + currentEvent.getTitle() : "")
                        + ". Vui lòng chọn loại vé và số lượng trên màn hình"));
                return;
            default:
                break;
        }
        // HOME hoặc không xác định → mô tả trang chủ
        deliver(AssistantResult.ok("Bạn đang ở trong ứng dụng mua vé sự kiện VibeTix. "
                + "Hãy nói: tìm sự kiện kèm từ khóa, mở vé của tôi, mở trang sự kiện, "
                + "hoặc trợ giúp, để nghe tất cả các lệnh"));
    }

    /**
     * Tải danh sách sự kiện (nổi bật nếu featuredOnly) rồi đọc lên,
     * đồng thời lưu vào lastSearchResults để user nói "mở số 1" được luôn.
     */
    private void fetchAndReadEvents(String intro, boolean featuredOnly) {
        FirebaseFirestore.getInstance()
                .collection(FirebaseCollections.EVENTS)
                .whereIn("status", Arrays.asList(Event.APPROVED, Event.ONGOING))
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    lastSearchResults.clear();
                    List<Event> fallback = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Event e = FirestoreHelper.docToEvent(doc);
                        if (e == null) continue;
                        if (e.getId() == null) e.setId(doc.getId());
                        if (!featuredOnly || e.isFeatured()) {
                            if (lastSearchResults.size() < MAX_SEARCH_RESULTS)
                                lastSearchResults.add(e);
                        } else if (fallback.size() < MAX_SEARCH_RESULTS) {
                            fallback.add(e);
                        }
                        if (lastSearchResults.size() >= MAX_SEARCH_RESULTS) break;
                    }
                    // Không có sự kiện nổi bật nào → đọc sự kiện thường
                    if (lastSearchResults.isEmpty()) lastSearchResults.addAll(fallback);

                    if (lastSearchResults.isEmpty()) {
                        deliver(AssistantResult.ok(intro
                                + "Hiện chưa có sự kiện nào đang mở bán"));
                        return;
                    }
                    StringBuilder sb = new StringBuilder(intro);
                    sb.append(featuredOnly ? "Sự kiện nổi bật gồm: " : "Các sự kiện đang mở bán gồm: ");
                    for (int i = 0; i < lastSearchResults.size(); i++) {
                        Event e = lastSearchResults.get(i);
                        sb.append("Số ").append(i + 1).append(": ").append(e.getTitle());
                        if (e.getStartTime() != null) sb.append(", ").append(e.getStartTime());
                        sb.append(". ");
                    }
                    sb.append("Nói: mở số 1, để xem chi tiết. "
                            + "Hoặc nói: tìm sự kiện kèm từ khóa, để tìm kiếm");
                    deliver(AssistantResult.ok(sb.toString()));
                })
                .addOnFailureListener(e ->
                        deliver(AssistantResult.ok(intro
                                + "Nói: tìm sự kiện kèm từ khóa, để tìm kiếm")));
    }

    private void doRepeat() {
        String last = tts.getLastSpoken();
        if (last.isEmpty()) {
            deliver(AssistantResult.error("Chưa có nội dung nào để nhắc lại"));
        } else {
            resumeListenAfterSpeech = conversationMode;
            tts.speak(last);
        }
    }

    private String helpText() {
        return "Bạn có thể nói các lệnh sau. "
                + "Tìm sự kiện, kèm từ khóa, để tìm kiếm. "
                + "Mở số 1, để mở sự kiện trong kết quả tìm kiếm. "
                + "Đọc chi tiết, để nghe thông tin sự kiện đang mở. "
                + "Mô tả poster, để nghe AI mô tả ảnh sự kiện. "
                + "Đặt vé, để mở màn chọn vé. "
                + "Mở vé của tôi, để nghe danh sách vé đã mua. "
                + "Mở trang sự kiện, để nghe các sự kiện đang mở bán. "
                + "Mở trang cá nhân, để vào tài khoản. "
                + "Màn hình này có gì, để nghe mô tả màn hình hiện tại. "
                + "Quay lại, hoặc, về trang chủ, để điều hướng. "
                + "Dừng lại, để tắt trợ lý. Nhắc lại, để nghe lại câu vừa rồi";
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    public TtsManager getTts() { return tts; }

    /**
     * Tắt mềm trợ lý (user gạt tắt trong Cài đặt): dừng nghe + dừng đọc,
     * không announce, không giải phóng tài nguyên — bật lại được ngay.
     */
    public void disable() {
        conversationMode = false;
        resumeListenAfterSpeech = false;
        mainHandler.removeCallbacksAndMessages(null);
        voiceInput.stopListening();
        tts.stop();
    }

    public void destroy() {
        conversationMode = false;
        mainHandler.removeCallbacksAndMessages(null);
        voiceInput.destroy();
        tts.shutdown();
    }
}
