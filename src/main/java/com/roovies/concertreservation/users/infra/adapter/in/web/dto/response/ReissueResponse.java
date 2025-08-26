package com.roovies.concertreservation.users.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "토큰 재발급 응답 DTO")
public record ReissueResponse(
        @Schema(description = "새 액세스 토큰", example = "새 액세스 토큰 문자열")
        String accessToken,

        @Schema(description = "새 리프레시 토큰", example = "새 리프레시 토큰 문자열")
        String refreshToken,

        @Schema(description = "액세스 토큰 남은 만료 시간(초)", example = "1800")
        long accessTokenExpiresIn, // 남은 시간 (초)

        @Schema(description = "리프레시 토큰 남은 만료 시간(초)", example = "604800")
        long refreshTokenExpiresIn
) {
}
