package com.example.vibetix.Utils;

import java.util.HashMap;
import java.util.Map;

public class CosineSimilarityUtils {

    /**
     * Tính độ tương đồng cosine giữa hai chuỗi dựa trên bigram character.
     * Trả về giá trị trong [0.0, 1.0]; 1.0 = giống nhau hoàn toàn.
     */
    public static double calculateSimilarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        a = a.toLowerCase().trim();
        b = b.toLowerCase().trim();
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        if (a.equals(b)) return 1.0;

        Map<String, Integer> vecA = buildBigramVector(a);
        Map<String, Integer> vecB = buildBigramVector(b);

        double dot = 0, magA = 0, magB = 0;
        for (Map.Entry<String, Integer> e : vecA.entrySet()) {
            int va = e.getValue();
            int vb = vecB.getOrDefault(e.getKey(), 0);
            dot += (double) va * vb;
            magA += (double) va * va;
        }
        for (int vb : vecB.values()) {
            magB += (double) vb * vb;
        }
        if (magA == 0 || magB == 0) return 0.0;
        return dot / (Math.sqrt(magA) * Math.sqrt(magB));
    }

    private static Map<String, Integer> buildBigramVector(String s) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < s.length() - 1; i++) {
            String bigram = s.substring(i, i + 2);
            map.put(bigram, map.getOrDefault(bigram, 0) + 1);
        }
        if (s.length() == 1) map.put(s, 1);
        return map;
    }
}
