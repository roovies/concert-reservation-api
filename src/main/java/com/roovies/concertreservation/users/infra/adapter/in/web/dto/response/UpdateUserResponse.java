package com.roovies.concertreservation.users.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "회원정보 수정 응답 DTO")
public record UpdateUserResponse(
        @Schema(description = "회원 이메일", example = "test@example.com")
        String email,

        @Schema(description = "이름", example = "이지환")
        String name,

        @Schema(description = "닉네임", example = "루비즈")
        String nickname
) {
}
