package com.roovies.concertreservation.users.infra.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "액세스 및 리프레시 토큰 재발급 요청 DTO")
public record ReissueRequest(
        @Schema(description = "리프레시 토큰", example = "리프레시 토큰 문자열")
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        String refreshToken
) {
}
