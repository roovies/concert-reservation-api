package com.roovies.concertreservation.points.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "포인트 조회 응답 DTO")
public record GetPointResponse(
        @Schema(description = "보유 잔액", example = "20000")
        Long point,

        @Schema(description = "응답 시간", example = "2025-08-26T21:00:00")
        LocalDateTime responseTime
) {
}
