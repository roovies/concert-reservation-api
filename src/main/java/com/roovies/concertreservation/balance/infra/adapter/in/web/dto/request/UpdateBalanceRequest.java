package com.roovies.concertreservation.balance.infra.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "잔액 충전 요청 DTO")
public record UpdateBalanceRequest(
        @Schema(description = "충전 금액", example = "10000")
        @NotNull(message = "충전 금액은 필수입니다.")
        Long balance
) {
}
