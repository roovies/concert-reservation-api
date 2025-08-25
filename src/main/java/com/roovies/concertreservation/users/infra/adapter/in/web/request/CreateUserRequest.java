package com.roovies.concertreservation.users.infra.adapter.in.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @Schema(description = "회원 이메일", example = "test@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @Schema(description = "패스워드", example = "roovies1234@@")
        @NotBlank(message = "패스워드는 필수입니다.")
        String password,

        @Schema(description = "이름", example = "이지환")
        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @Schema(description = "닉네임", example = "루비즈")
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname
) {
}
