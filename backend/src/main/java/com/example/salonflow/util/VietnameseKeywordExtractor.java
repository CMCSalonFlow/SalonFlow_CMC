package com.example.salonflow.util;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tách từ khoá đơn giản cho tiếng Việt (không dùng thư viện NLP nặng).
 * Chiến lược: tách theo khoảng trắng/dấu câu, giữ nguyên dấu tiếng Việt,
 * loại bỏ từ dừng (stop words) và từ quá ngắn.
 *
 * LƯU Ý: đây là cách tách đơn giản theo TỪ ĐƠN (unigram), không ghép được
 * từ ghép có nghĩa (ví dụ "dịch vụ" sẽ tách thành "dịch" và "vụ" riêng biệt).
 * Nếu cần độ chính xác cao hơn, cân nhắc dùng thư viện tách từ tiếng Việt
 * (ví dụ VnCoreNLP) ở giai đoạn sau — hiện tại ưu tiên đơn giản, không thêm
 * dependency nặng.
 */
public final class VietnameseKeywordExtractor {

    private VietnameseKeywordExtractor() {
    }

private static final Set<String> STOP_WORDS = Set.of(
        "và", "là", "của", "có", "được", "rất", "cũng", "này", "đã", "cho",
        "mình", "tôi", "em", "anh", "chị", "khi", "thì", "nên", "vì", "nếu",
        "một", "các", "những", "để", "với", "từ", "tại", "trong", "ngoài",
        "nhưng", "mà", "hay", "hoặc", "không", "chưa", "sẽ", "đang", "vẫn",
        "lại", "nữa", "quá", "hơi", "khá", "ạ", "nhé", "ơi", "vậy", "thế",
        "the", "ở", "ra", "vào", "lên", "xuống", "đây", "đó", "kia",
        "họ", "nó", "ta", "bạn", "mọi", "nào", "gì", "ai", "sao", "bởi"
);

    private static final Pattern SPLIT_PATTERN = Pattern.compile("[\\s,.!?;:()\\[\\]\"'/\\\\\\-_]+");

    /**
     * Trả về map keyword -> tần suất xuất hiện trong 1 đoạn text.
     */
    public static Map<String, Integer> extractFrequency(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyMap();
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase();
        String[] tokens = SPLIT_PATTERN.split(normalized);

        Map<String, Integer> freq = new HashMap<>();
        for (String token : tokens) {
            String word = token.trim();
            if (word.length() < 2) {
                continue; // bỏ từ quá ngắn (1 ký tự, thường không có nghĩa)
            }
            if (STOP_WORDS.contains(word)) {
                continue;
            }
            if (!word.chars().allMatch(c -> Character.isLetter(c))) {
                continue; // bỏ token có số/ký tự đặc biệt lẫn vào
            }
            freq.merge(word, 1, Integer::sum);
        }
        return freq;
    }

    /**
     * Gộp nhiều map tần suất lại thành 1 (dùng khi xử lý nhiều review trong cùng 1 branch/tháng).
     */
    public static Map<String, Integer> mergeFrequencies(List<Map<String, Integer>> maps) {
        Map<String, Integer> result = new HashMap<>();
        for (Map<String, Integer> m : maps) {
            m.forEach((k, v) -> result.merge(k, v, Integer::sum));
        }
        return result;
    }
}
