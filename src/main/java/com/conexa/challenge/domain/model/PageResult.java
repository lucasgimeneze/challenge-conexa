package com.conexa.challenge.domain.model;

import java.util.List;

public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public PageResult {
        if (page < 1 || size < 1 || totalElements < 0) {
            throw new IllegalArgumentException("page and size must be positive and totalElements not negative");
        }
        content = List.copyOf(content);
    }

    public static <T> PageResult<T> of(List<T> items, int page, int size) {
        int from = Math.min((page - 1) * size, items.size());
        int to = Math.min(from + size, items.size());
        return new PageResult<>(items.subList(from, to), page, size, items.size());
    }

    public static <T> PageResult<T> empty(int page, int size, long totalElements) {
        return new PageResult<>(List.of(), page, size, totalElements);
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / size);
    }
}
