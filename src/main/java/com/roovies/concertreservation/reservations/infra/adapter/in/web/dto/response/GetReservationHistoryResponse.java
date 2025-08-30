package com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "예약 내역 조회 응답 DTO")
public record GetReservationHistoryResponse(
        @Schema(description = "내역 리스트")
        List<ReservationHistoryItem> items,

        @Schema(description = "현재 페이지 번호")
        int page,

        @Schema(description = "페이지 크기")
        int size,

        @Schema(description = "총 페이지 수")
        int totalPages,

        @Schema(description = "총 내역 수")
        long totalElements
) {
        @Builder
        @Schema(description = "예약 내역 아이템 DTO")
        public record ReservationHistoryItem(
                @Schema(description = "예약 ID", example = "201")
                Long id,

                @Schema(description = "콘서트 ID", example = "101")
                Long concertId,

                @Schema(description = "콘서트 제목", example = "K-Pop Concert 2025")
                String concertTitle,

                @Schema(description = "예약 좌석 번호", example = "A-12")
                String seatNumber,

                @Schema(description = "예약 상태", example = "CONFIRMED / CANCELLED")
                String status, // TODO: enum처리

                @Schema(description = "예약 일시", example = "2025-08-26T21:00:00")
                LocalDateTime reservedAt
        ) {}
}
