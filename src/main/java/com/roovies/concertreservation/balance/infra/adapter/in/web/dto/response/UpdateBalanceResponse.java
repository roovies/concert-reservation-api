package com.roovies.concertreservation.balance.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "잔액 충전 응답 DTO")
public record UpdateBalanceResponse(
        @Schema(description = "충전 후 잔액", example = "20000")
        Long balance,

        @Schema(description = "응답 시간", example = "2025-08-26T21:00:00")
        LocalDateTime responseTime
) {
}
