package com.roovies.concertreservation.payments.infra.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "결제 환불 요청 DTO")
public record CancelPaymentRequest(
        @Schema(description = "결제 ID", example = "101")
        @NotNull(message = "결제 ID는 필수입니다.")
        Long paymentId
) {
}
