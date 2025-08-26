package com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CreateReservationResponse(
        @Schema(description = "예약 ID", example = "101")
        Long reservationId,

        @Schema(description = "만료 시간 (epoch seconds)", example = "1752151800")
        Long expiresAt
) {
}
