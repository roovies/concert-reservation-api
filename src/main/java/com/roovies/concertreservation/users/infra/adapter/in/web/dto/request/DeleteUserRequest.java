package com.roovies.concertreservation.users.infra.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원탈퇴 요청 DTO")
public record DeleteUserRequest(
        @Schema(description = "패스워드", example = "test111$%")
        @NotBlank(message = "패스워드는 필수입니다.")
        String password
) {
}
