package com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.vo;

import io.swagger.v3.oas.annotations.media.Schema;

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
