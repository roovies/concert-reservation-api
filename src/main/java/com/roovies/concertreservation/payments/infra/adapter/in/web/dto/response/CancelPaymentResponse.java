package com.roovies.concertreservation.payments.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "콘서트 예약 결제 응답 DTO")
public record CancelPaymentResponse(
        @Schema(description = "환불 ID", example = "101")
        Long refundId,

        @Schema(description = "환불 금액", example = "120000")
        BigDecimal amount
) {
}
