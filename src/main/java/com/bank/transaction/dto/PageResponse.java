package com.bank.transaction.dto;

import java.util.List;

/**
 * Page Response DTO (Record)
 * 
 * JDK 21 Record class for paginated responses.
 * Contains the data list along with pagination metadata.
 *
 * @param <T> the type of elements in the page
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    /**
     * Create a PageResponse from a list and pagination parameters
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PageResponse<>(
            content,
            page,
            size,
            totalElements,
            totalPages,
            page == 0,
            page >= totalPages - 1
        );
    }
}
