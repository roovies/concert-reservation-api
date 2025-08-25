package com.roovies.concertreservation.balance.infra.adapter.in.web.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record GetBalanceResponse(
        Long balance,
        LocalDateTime responseTime
) {
}
