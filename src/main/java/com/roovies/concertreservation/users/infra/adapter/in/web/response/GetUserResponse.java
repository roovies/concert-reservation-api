package com.roovies.concertreservation.users.infra.adapter.in.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record GetUserResponse(
        @Schema(description = "회원 이메일", example = "test@example.com")
        String email,

        @Schema(description = "이름", example = "이지환")
        String name,

        @Schema(description = "닉네임", example = "루비즈")
        String nickname,

        @Schema(description = "가입일", example = "2025-08-24")
        LocalDate createdAt
) {
}
