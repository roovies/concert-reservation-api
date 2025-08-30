package com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "예약 가능 좌석 목록 조회 응답 DTO")
public record GetAvailableSeatsResponse(
        @Schema(description = "콘서트 ID", example = "101")
        Long concertId,

        @Schema(description = "조회 날짜", example = "2025-09-01")
        LocalDate date,

        @Schema(description = "좌석 목록")
        List<SeatItemDto> seats
) {
        @Builder
        @Schema(description = "예약 가능 좌석 아이템 DTO")
        public record SeatItemDto(
                @Schema(description = "좌석 ID", example = "101")
                Long seatId,

                @Schema(description = "좌석 행번호", example = "A")
                int row,

                @Schema(description = "좌석 열번호", example = "10")
                int seatNumber,

                @Schema(description = "좌석 유형", example = "STANDARD")
                String seatType, // TODO: Enum 처리

                @Schema(description = "이미 예약되어있는지 여부", example = "true")
                boolean isReserved
        ) {
        }
}
