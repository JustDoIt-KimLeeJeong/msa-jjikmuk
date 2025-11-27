package com.tradingsystem.order.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 공통 래퍼
 *  * - Spring Page<T>를 프론트엔드 친화적으로 변환
 *  * - 불필요한 필드 제거
 *  * - 제네릭으로 재사용 가능
 *  *
 *  * @param <T> 응답 데이터 타입 (예: OrderResponse)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString
public class PageResponse<T> {

    /**
     * 실제 데이터 목록
     */
    private List<T> content;

    /**
     * 현재 페이지 번호 (0부터 시작)
     */
    private int pageNumber;

    /**
     * 페이지당 데이터 개수
     */
    private int pageSize;

    /**
     * 전체 데이터 개수
     */
    private long totalElements;

    /**
     * 전체 페이지 수
     */
    private int totalPages;

    /**
     * 첫 페이지 여부
     */
    private boolean isFirst;

    /**
     * 마지막 페이지 여부
     */
    private boolean isLast;

    /**
     * 다음 페이지 존재 여부
     */
    private boolean hasNext;

    /**
     * 이전 페이지 존재 여부
     */
    private boolean hasPrevious;

    // === 정적 팩토리 메서드 ===

    /**
     * Spring Page<T> → PageResponse<T> 변환
     * - Java 8 스타일 (정적 팩토리 메서드)
     *
     * @param page Spring Data JPA Page 객체
     * @param <T> 데이터 타입
     * @return PageResponse 래퍼
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * 빈 페이지 응답 생성
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param <T> 데이터 타입
     * @return 빈 PageResponse
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return PageResponse.<T>builder()
                .content(List.of())
                .pageNumber(page)
                .pageSize(size)
                .totalElements(0L)
                .totalPages(0)
                .isFirst(true)
                .isLast(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    // === 편의 메서드 ===

    /**
     * 데이터 존재 여부
     */
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    /**
     * 실제 반환된 데이터 개수
     */
    public int numberOfElements() {
        return content != null ? content.size() : 0;
    }

}
