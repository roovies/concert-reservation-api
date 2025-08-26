package com.roovies.concertreservation.payments.infra.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "콘서트 예약 결제 요청 DTO")
public record CreatePaymentRequest(
        @Schema(description = "예약 ID", example = "101")
        Long reservationId
) {
}
