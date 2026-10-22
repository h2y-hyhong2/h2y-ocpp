package com.lselink.elvis.control.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * CSMS Lite 관제 어드민 REST API 표준 페이징 응답 DTO (Sprint 25: S25-API-0010-CO)
 *
 * @param <T> 목록 데이터 타입
 */
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean isLast;

    public PageResponse(List<T> content, int page, int size, long totalElements, int totalPages, boolean isLast) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.isLast = isLast;
    }

    /**
     * Spring Data JPA Page<T> 객체로부터 PageResponse 생성
     */
    public static <T> PageResponse<T> of(Page<T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isLast()
        );
    }

    /**
     * 수동 리스트 페이징 생성
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = 0;
        if (size > 0) {
            totalPages = (int) Math.ceil((double) totalElements / size);
        }
        boolean isLast = false;
        if (page >= totalPages - 1) {
            isLast = true;
        }
        return new PageResponse<>(content, page, size, totalElements, totalPages, isLast);
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isLast() {
        return isLast;
    }
}
