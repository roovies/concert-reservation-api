package com.roovies.concertreservation.waiting.application.dto.result;

import lombok.Builder;

@Builder
public record EnterQueueResult(
        boolean admitted,       // 즉시 입장 여부
        String admittedToken,   // 입장 토큰 (즉시 입장 시에만)
        Integer rank,           // 대기 순번 (대기열 진입 시에만)
        Integer totalWaiting,      // 전체 대기자 수
        String userKey          // 대기열 식별자
) {
}
