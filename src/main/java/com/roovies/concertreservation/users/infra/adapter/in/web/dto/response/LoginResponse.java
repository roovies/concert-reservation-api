package com.roovies.concertreservation.users.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "로그인 응답 DTO")
public record LoginResponse(
        @Schema(description = "액세스 토큰", example = "액세스 토큰 문자열")
        String accessToken,

        @Schema(description = "리프레시 토큰", example = "리프레시 토큰 문자열")
        String refreshToken,

        @Schema(description = "액세스 토큰 만료일", example = "1800 (30분)")
        long accessTokenExpiresIn, // 남은 시간 (초)

        @Schema(description = "리프레시 토큰 만료일", example = "604800 (1주)")
        long refreshTokenExpiresIn,

        @Schema(description = "회원 정보(클라이언트 캐싱용)", example = "MemberInfo 객체")
        MemberInfo memberInfo

) {
    @Builder
    @Schema(description = "회원 정보 DTO")
    public record MemberInfo(
            @Schema(description = "회원 ID", example = "1")
            Long id,

            @Schema(description = "회원 이메일", example = "user@example.com")
            String email,

            @Schema(description = "회원 닉네임", example = "루비즈")
            String nickname
    ){}
}
