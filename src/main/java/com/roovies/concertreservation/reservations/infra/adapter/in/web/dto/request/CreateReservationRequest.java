package com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "좌석 예약 요청 DTO")
public record CreateReservationRequest(
        @Schema(description = "콘서트 스케줄 ID", example = "1")
        @NotNull(message = "콘서트 스케줄 ID는 필수입니다.")
        Long scheduleId,

        @Schema(description = "좌석 ID", example = "1")
        @NotNull(message = "좌석 ID는 필수입니다.")
        List<Long> seatIds
) {
}
