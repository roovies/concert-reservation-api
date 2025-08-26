package com.roovies.concertreservation.payments.infra.adapter.in.web.dto.response;

import com.roovies.concertreservation.payments.infra.adapter.in.web.dto.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "콘서트 예약 결제 응답 DTO")
public record CreatePaymentResponse(
        @Schema(description = "결제 ID", example = "101")
        Long paymentId,

        @Schema(description = "결제 금액", example = "120000")
        BigDecimal amount,

        @Schema(description = "결제 상태", example = "CONFIRMED")
        PaymentStatus status
) {
}
