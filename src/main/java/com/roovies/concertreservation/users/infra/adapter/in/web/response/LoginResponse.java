package com.roovies.concertreservation.users.infra.adapter.in.web.response;

import lombok.Builder;

@Builder
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn, // 남은 시간 (초)
        long refreshTokenExpiresIn,
        MemberInfo memberInfo

) {
    @Builder
    public record MemberInfo(
            Long id,
            String email,
            String nickname
    ){}
}
