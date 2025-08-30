package com.roovies.concertreservation.users.infra.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원정보 수정 요청 DTO")
public record UpdateUserRequest(
        @Schema(description = "회원 이메일", example = "test@example.com")
        String email,

        @Schema(description = "패스워드", example = "roovies1234@@")
        @NotBlank(message = "패스워드는 필수입니다.")
        String password,

        @Schema(description = "이름", example = "이지환")
        String name,

        @Schema(description = "닉네임", example = "루비즈")
        String nickname
) {
}
