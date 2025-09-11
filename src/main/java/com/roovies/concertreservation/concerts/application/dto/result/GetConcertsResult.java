package com.roovies.concertreservation.concerts.application.dto.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "콘서트 목록 조회 응답 DTO")
public record GetConcertsResult(
        @Schema(description = "내역 리스트")
        List<ConcertItem> items,

        @Schema(description = "현재 페이지 번호")
        int page,

        @Schema(description = "페이지 크기")
        int size,

        @Schema(description = "총 페이지 수")
        int totalPages,

        @Schema(description = "총 콘서트 수")
        long totalElements
) {
        @Builder
        @Schema(description = "콘서트 아이템 DTO")
        public record ConcertItem(
                @Schema(description = "콘서트 ID", example = "101")
                Long id,

                @Schema(description = "콘서트 제목", example = "K-Pop Concert 2025")
                String title,

                @Schema(description = "콘서트 시작일", example = "2025-09-01")
                LocalDate startDate,

                @Schema(description = "콘서트 종료일", example = "2025-09-05")
                LocalDate endDate,

                @Schema(description = "총 좌석 수", example = "1000")
                int totalSeats // TODO: 총 좌석 수가 아니라 상태로 바꾸는 게 좋을 듯 => 시작일/종료일 관련된
        ) {}
}
