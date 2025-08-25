package com.roovies.concertreservation.users.infra.adapter.in.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(
        @Schema(description = "현재 패스워드", example = "test111$%")
        @NotBlank(message = "현재 패스워드는 필수입니다.")
        String currentPassword,

        @Schema(description = "변경할 패스워드", example = "gogo111!!")
        @NotBlank(message = "변경할 패스워드는 필수입니다.")
        String newPassword
) {
}
