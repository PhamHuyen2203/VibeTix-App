package com.example.vibetix.Accessibility;

import java.text.Normalizer;

/**
 * SpeechCommandParser — chuyển text (từ SpeechRecognizer) thành AssistantCommand
 * bằng keyword matching tiếng Việt, không phân biệt dấu.
 *
 * Cách hoạt động: chuẩn hóa câu nói về dạng không dấu (giữ nguyên độ dài chuỗi)
 * rồi so khớp prefix/contains với danh sách keyword. Nhờ giữ nguyên độ dài,
 * phần tham số (từ khóa tìm kiếm...) được cắt ra từ câu GỐC còn nguyên dấu.
 */
public class SpeechCommandParser {

    /** Chuẩn hóa 1 ký tự về không dấu — giữ nguyên độ dài chuỗi. */
    private static char stripAccent(char c) {
        if (c == 'đ') return 'd';
        if (c == 'Đ') return 'D';
        String s = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
        return s.charAt(0);
    }

    /** Chuẩn hóa cả chuỗi về lowercase không dấu, độ dài không đổi. */
    public static String normalize(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            sb.append(Character.toLowerCase(stripAccent(text.charAt(i))));
        }
        return sb.toString();
    }

    // Prefix cho lệnh tìm kiếm — thứ tự dài trước ngắn sau để cắt đúng tham số
    private static final String[] SEARCH_PREFIXES = {
            "tim kiem su kien", "tim su kien", "tim kiem", "tim"
    };

    // Prefix cho lệnh mở sự kiện
    private static final String[] OPEN_PREFIXES = {
            "mo su kien so", "mo su kien", "chon su kien", "mo so", "chon so", "mo", "chon", "so"
    };

    public AssistantCommand parse(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return AssistantCommand.unknown(rawText);
        }
        String raw  = rawText.trim();
        String norm = normalize(raw);

        // ── Lệnh không tham số — check trước để không nhầm với search/open ──
        if (containsAny(norm, "dung lai", "im lang", "dung noi", "ngung doc", "ngung lai")
                || norm.equals("dung") || norm.equals("thoi") || norm.equals("stop")) {
            return new AssistantCommand(AssistantCommand.Type.STOP, "", raw);
        }
        if (containsAny(norm, "nhac lai", "noi lai", "lap lai")) {
            return new AssistantCommand(AssistantCommand.Type.REPEAT, "", raw);
        }
        if (containsAny(norm, "tro giup", "huong dan", "giup toi", "co the noi gi", "lenh nao")
                || norm.equals("giup")) {
            return new AssistantCommand(AssistantCommand.Type.HELP, "", raw);
        }
        if (containsAny(norm, "quay lai", "tro lai", "quay ve", "tro ve", "back")) {
            return new AssistantCommand(AssistantCommand.Type.GO_BACK, "", raw);
        }
        if (norm.contains("trang chu")) {
            return new AssistantCommand(AssistantCommand.Type.GO_HOME, "", raw);
        }

        // ── Điều hướng tab — check TRƯỚC book/open để không nhầm keyword ──
        // "vé của tôi" phải đứng trước "đặt vé"/"mua vé"
        if (containsAny(norm, "ve cua toi", "ve da mua", "ve toi da mua")
                || norm.startsWith("xem ve") || norm.equals("mo ve")) {
            return new AssistantCommand(AssistantCommand.Type.OPEN_MY_TICKETS, "", raw);
        }
        // "trang sự kiện" phải đứng trước "mở sự kiện <tên>" (mở 1 sự kiện cụ thể)
        if (containsAny(norm, "trang su kien", "tab su kien", "danh sach su kien",
                "tat ca su kien", "cac su kien")) {
            return new AssistantCommand(AssistantCommand.Type.OPEN_EVENTS_TAB, "", raw);
        }
        if (containsAny(norm, "ca nhan", "ho so", "tai khoan cua toi", "trang toi")) {
            return new AssistantCommand(AssistantCommand.Type.OPEN_PROFILE, "", raw);
        }
        // "mô tả màn hình" phải đứng trước "mô tả (poster)"
        if (containsAny(norm, "man hinh", "dang o dau", "o trang nao", "co gi")) {
            return new AssistantCommand(AssistantCommand.Type.DESCRIBE_SCREEN, "", raw);
        }

        if (containsAny(norm, "dat ve", "mua ve", "book ve")) {
            return new AssistantCommand(AssistantCommand.Type.BOOK_TICKET, "", raw);
        }
        if (containsAny(norm, "mo ta", "poster", "ta anh", "ta hinh")) {
            return new AssistantCommand(AssistantCommand.Type.DESCRIBE_POSTER, "", raw);
        }
        if (norm.startsWith("doc") || containsAny(norm, "chi tiet", "thong tin su kien")) {
            return new AssistantCommand(AssistantCommand.Type.READ_DETAIL, "", raw);
        }

        // ── Tìm sự kiện: "tìm [kiếm] [sự kiện] <từ khóa>" ──
        for (String prefix : SEARCH_PREFIXES) {
            if (norm.startsWith(prefix)) {
                String arg = raw.substring(Math.min(prefix.length(), raw.length())).trim();
                if (!arg.isEmpty()) {
                    return new AssistantCommand(AssistantCommand.Type.SEARCH_EVENT, arg, raw);
                }
            }
        }

        // ── Mở sự kiện: "mở số 1" / "mở sự kiện <tên>" / "số một" ──
        for (String prefix : OPEN_PREFIXES) {
            if (norm.startsWith(prefix)) {
                String arg = raw.substring(Math.min(prefix.length(), raw.length())).trim();
                if (!arg.isEmpty() || prefix.length() > 3) {
                    return new AssistantCommand(AssistantCommand.Type.OPEN_EVENT, arg, raw);
                }
            }
        }

        // Nếu câu chỉ là một con số ("một", "2"...) → hiểu là chọn kết quả đó
        if (parseIndex(raw) > 0) {
            return new AssistantCommand(AssistantCommand.Type.OPEN_EVENT, raw, raw);
        }

        return AssistantCommand.unknown(raw);
    }

    /**
     * Parse số thứ tự từ tham số của lệnh OPEN_EVENT.
     * Hỗ trợ chữ số ("1", "số 2") và số đếm tiếng Việt ("một", "hai"...).
     * @return số thứ tự 1-based, hoặc -1 nếu không phải số.
     */
    public static int parseIndex(String argument) {
        if (argument == null) return -1;
        String norm = normalize(argument).replace("so ", "").replace("thu ", "").trim();
        try {
            return Integer.parseInt(norm);
        } catch (NumberFormatException ignored) { }
        switch (norm) {
            case "mot": case "nhat": case "dau tien": return 1;
            case "hai": case "nhi":                    return 2;
            case "ba":                                 return 3;
            case "bon": case "tu":                     return 4;
            case "nam":                                return 5;
            case "sau":                                return 6;
            case "bay":                                return 7;
            case "tam":                                return 8;
            case "chin":                               return 9;
            case "muoi":                               return 10;
            default:                                   return -1;
        }
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }
}
