package com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateReservationRequest(
        @Schema(description = "콘서트 ID", example = "1001")
        @NotNull(message = "콘서트 ID는 필수입니다.")
        Long concertId,

        @Schema(description = "예약 날짜 (yyyy-MM-dd)", example = "2025-07-10")
        @NotBlank(message = "예약 날짜는 필수입니다.")
        LocalDate reservationDate,

        @Schema(description = "좌석 ID", example = "12L")
        @NotNull(message = "좌석 ID는 필수입니다.")
        Long seatId
) {
}
