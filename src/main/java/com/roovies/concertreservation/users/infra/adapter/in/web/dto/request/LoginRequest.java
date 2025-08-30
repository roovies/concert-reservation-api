package com.roovies.concertreservation.users.infra.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청 DTO")
public record LoginRequest(
        @Schema(description = "회원 이메일", example = "test@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @Schema(description = "패스워드", example = "pwd1111!!")
        @NotBlank(message = "패스워드는 필수입니다.")
        String password
) {
}
