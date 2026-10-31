package com.pharmacy.demo.dto.common;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class ListPager {

    private static final Collator VIETNAMESE = Collator.getInstance(Locale.forLanguageTag("vi"));

    private ListPager() {}

    public static <T> PageResponse<T> paginate(
            List<T> items, PageQuery query, Map<String, Comparator<T>> sortable, String defaultSort) {
        List<T> sorted = items.stream()
                .sorted(resolve(query.sort(), sortable, defaultSort))
                .toList();
        int totalPages = Math.max(1, (int) Math.ceil(sorted.size() / (double) query.size()));
        int page = Math.min(query.page(), totalPages);
        int from = Math.min((page - 1) * query.size(), sorted.size());
        int to = Math.min(from + query.size(), sorted.size());
        return new PageResponse<>(sorted.subList(from, to), page, query.size(), sorted.size(), totalPages);
    }

    public static <T> Comparator<T> text(Function<T, String> extractor) {
        return Comparator.comparing(extractor, Comparator.nullsFirst(VIETNAMESE::compare));
    }

    public static <T, V extends Comparable<? super V>> Comparator<T> comparable(Function<T, V> extractor) {
        return Comparator.comparing(extractor, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    private static <T> Comparator<T> resolve(String sort, Map<String, Comparator<T>> sortable, String defaultSort) {
        String[] requested = split(sort);
        if (requested == null || !sortable.containsKey(requested[0])) {
            requested = split(defaultSort);
        }
        Comparator<T> comparator = sortable.get(requested[0]);
        return "desc".equalsIgnoreCase(requested[1]) ? comparator.reversed() : comparator;
    }

    private static String[] split(String sort) {
        if (sort == null || sort.isBlank()) {
            return null;
        }
        String[] tokens = sort.split(",");
        return new String[] {tokens[0].trim(), tokens.length > 1 ? tokens[1].trim() : "asc"};
    }
}
