package com.pharmacy.demo.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextSearch {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private TextSearch() {}

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return DIACRITICS
                .matcher(decomposed)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    public static boolean matchesAny(String query, String... fields) {
        String needle = normalize(query);
        if (needle.isEmpty()) {
            return true;
        }
        for (String field : fields) {
            if (normalize(field).contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
