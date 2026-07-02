package com.example.vibetix.Utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Thuật toán tìm kiếm dựa trên khoảng cách vector ngắn nhất.
 *
 * Công thức tính điểm liên quan (relevance score) cho mỗi sự kiện:
 *
 *   score = w₁·exactMatch + w₂·prefixMatch + w₃·(1 - editDist/maxLen) + w₄·tokenOverlap
 *
 * Trong đó:
 *   - exactMatch ∈ {0,1}: chứa chính xác query (sau normalize)
 *   - prefixMatch ∈ {0,1}: title bắt đầu bằng query
 *   - editDist: khoảng cách Levenshtein giữa query và substring tốt nhất của title
 *   - tokenOverlap: tỷ lệ token query khớp với token title (Jaccard-like)
 *   - w₁=0.4, w₂=0.2, w₃=0.25, w₄=0.15 (trọng số)
 *
 * Normalize: loại bỏ dấu tiếng Việt (ă→a, ê→e, ơ→o, ư→u, đ→d, dấu thanh)
 * → so sánh "khong dau" và "có dấu" cho kết quả tương đương.
 *
 * Kết quả trả về sắp xếp theo score giảm dần (liên quan nhất trước).
 * Chỉ trả về kết quả có score > threshold.
 */
public class VectorSearch {

    private static final double W_EXACT   = 0.40;
    private static final double W_PREFIX  = 0.20;
    private static final double W_EDIT    = 0.25;
    private static final double W_TOKEN   = 0.15;
    private static final double THRESHOLD = 0.15;

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /** Kết quả tìm kiếm: item gốc + điểm liên quan. */
    public static class ScoredResult<T> {
        public final T item;
        public final double score;
        public ScoredResult(T item, double score) {
            this.item = item;
            this.score = score;
        }
    }

    /** Interface lấy text searchable từ item. */
    public interface TextExtractor<T> {
        String[] getSearchableTexts(T item);
    }

    /**
     * Tìm kiếm và xếp hạng danh sách items theo mức độ liên quan với query.
     *
     * @param query   từ khóa (có dấu hoặc không dấu)
     * @param items   danh sách items
     * @param extractor hàm lấy các text searchable từ item (title, location, organizer...)
     * @return danh sách items xếp theo score giảm dần, đã lọc threshold
     */
    public static <T> List<ScoredResult<T>> search(String query, List<T> items, TextExtractor<T> extractor) {
        if (query == null || query.trim().isEmpty() || items == null) return new ArrayList<>();

        String normQuery = normalize(query.trim());
        String[] queryTokens = normQuery.split("\\s+");

        List<ScoredResult<T>> results = new ArrayList<>();

        for (T item : items) {
            String[] texts = extractor.getSearchableTexts(item);
            double bestScore = 0;

            for (String text : texts) {
                if (text == null || text.isEmpty()) continue;
                double s = computeScore(normQuery, queryTokens, normalize(text));
                if (s > bestScore) bestScore = s;
            }

            if (bestScore >= THRESHOLD) {
                results.add(new ScoredResult<>(item, bestScore));
            }
        }

        // Sort: score cao nhất (liên quan nhất) lên trước
        Collections.sort(results, (a, b) -> Double.compare(b.score, a.score));
        return results;
    }

    /**
     * Tính điểm liên quan giữa query và một text.
     *
     * Vector 4 chiều: [exactMatch, prefixMatch, editSimilarity, tokenOverlap]
     * Score = dot product với weight vector [W_EXACT, W_PREFIX, W_EDIT, W_TOKEN]
     */
    private static double computeScore(String normQuery, String[] queryTokens, String normText) {
        // 1. Exact match: text chứa toàn bộ query
        double exact = normText.contains(normQuery) ? 1.0 : 0.0;

        // 2. Prefix match: text bắt đầu bằng query
        double prefix = normText.startsWith(normQuery) ? 1.0 : 0.0;

        // 3. Edit distance similarity: tìm substring tốt nhất trong text khớp query
        double editSim = computeEditSimilarity(normQuery, normText);

        // 4. Token overlap (Jaccard-like): bao nhiêu token query xuất hiện trong text
        double tokenOvl = computeTokenOverlap(queryTokens, normText);

        return W_EXACT * exact + W_PREFIX * prefix + W_EDIT * editSim + W_TOKEN * tokenOvl;
    }

    /**
     * Edit distance similarity = 1 - (minEditDist / max(|query|, |bestSubstring|))
     * Dùng sliding window: so sánh query với mỗi substring cùng length trong text.
     */
    private static double computeEditSimilarity(String query, String text) {
        int qLen = query.length();
        int tLen = text.length();
        if (qLen == 0 || tLen == 0) return 0;

        // Nếu query ngắn hơn text: tìm substring trong text khớp nhất
        int windowSize = Math.min(qLen, tLen);
        int minDist = Integer.MAX_VALUE;

        if (tLen >= qLen) {
            // Sliding window
            for (int i = 0; i <= tLen - qLen; i++) {
                String sub = text.substring(i, i + qLen);
                int dist = levenshtein(query, sub);
                if (dist < minDist) minDist = dist;
                if (dist == 0) return 1.0; // exact substring match
            }
        } else {
            minDist = levenshtein(query, text);
        }

        return 1.0 - ((double) minDist / Math.max(qLen, windowSize));
    }

    /**
     * Tỷ lệ token query khớp trong text (mỗi token query check contains trong text).
     */
    private static double computeTokenOverlap(String[] queryTokens, String text) {
        if (queryTokens.length == 0) return 0;
        int matched = 0;
        for (String token : queryTokens) {
            if (token.length() >= 2 && text.contains(token)) matched++;
        }
        return (double) matched / queryTokens.length;
    }

    /**
     * Khoảng cách Levenshtein (edit distance) giữa 2 chuỗi.
     * O(m*n) nhưng với query ngắn (<50 chars) thì OK.
     */
    private static int levenshtein(String a, String b) {
        int m = a.length(), n = b.length();
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];

        for (int j = 0; j <= n; j++) prev[j] = j;

        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            for (int j = 1; j <= n; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[n];
    }

    /**
     * Normalize tiếng Việt: bỏ dấu thanh + đổi ký tự đặc biệt.
     * "Sự kiện Đà Nẵng" → "su kien da nang"
     * "concert" → "concert" (giữ nguyên)
     */
    public static String normalize(String input) {
        if (input == null) return "";
        String lower = input.toLowerCase(Locale.ROOT);
        // Đ/đ → d (trước khi decompose vì đ không decompose)
        lower = lower.replace('đ', 'd').replace('Đ', 'd');
        // Unicode NFD decompose: ă → a + combining breve, ê → e + combining circumflex, etc.
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        // Loại bỏ combining diacritical marks
        return DIACRITICS.matcher(decomposed).replaceAll("");
    }
}
