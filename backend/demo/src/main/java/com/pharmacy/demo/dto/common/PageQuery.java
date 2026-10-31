package com.pharmacy.demo.dto.common;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PageQuery(int page, int size, String sort) {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 200;

    public static PageQuery of(Integer page, Integer size, String sort) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new PageQuery(safePage, safeSize, sort);
    }

    public Pageable toPageable(Set<String> sortable, String defaultSort) {
        return PageRequest.of(page - 1, size, resolveSort(sortable, defaultSort));
    }

    private Sort resolveSort(Set<String> sortable, String defaultSort) {
        List<String> parts = parse(sort);
        if (parts.isEmpty() || !sortable.contains(parts.get(0))) {
            parts = parse(defaultSort);
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(parts.get(1)) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, parts.get(0));
    }

    private List<String> parse(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] tokens = value.split(",");
        return List.of(tokens[0].trim(), tokens.length > 1 ? tokens[1].trim() : "asc");
    }
}
