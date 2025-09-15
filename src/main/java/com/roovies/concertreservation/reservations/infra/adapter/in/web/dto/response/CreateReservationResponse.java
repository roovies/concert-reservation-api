package com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "좌석 예약 응답 DTO")
public record CreateReservationResponse(
        @Schema(description = "스케줄 ID", example = "101")
        Long scheduleId,

        @Schema(description = "예약한 좌석ID 목록", example = "[1, 2, 3]")
        List<Long> seatIds,

        @Schema(description = "회원 ID", example = "1")
        Long userId,

        @Schema(description = "점유 만료 시간(초)", example = "1")
        long ttlSeconds
) {
}
