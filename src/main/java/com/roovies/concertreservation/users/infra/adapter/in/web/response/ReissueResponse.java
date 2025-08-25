package com.roovies.concertreservation.users.infra.adapter.in.web.response;

import lombok.Builder;

@Builder
public record ReissueResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn, // 남은 시간 (초)
        long refreshTokenExpiresIn
) {
}
