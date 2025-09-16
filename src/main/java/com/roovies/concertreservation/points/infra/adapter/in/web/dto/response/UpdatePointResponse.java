package com.roovies.concertreservation.points.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "포인트 충전 응답 DTO")
public record UpdatePointResponse(
        @Schema(description = "회원 ID", example = "1")
        Long userId,

        @Schema(description = "충전 후 포인트", example = "20000")
        Long point,

        @Schema(description = "응답 시간", example = "2025-08-26T21:00:00")
        LocalDateTime responseTime
) {
}
