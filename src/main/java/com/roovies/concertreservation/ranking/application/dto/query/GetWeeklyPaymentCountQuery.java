package com.roovies.concertreservation.ranking.application.dto.query;

import java.time.LocalDateTime;

/**
 * 주간 결제 건수 조회 쿼리.
 */
public record GetWeeklyPaymentCountQuery(
        LocalDateTime startDate,
        LocalDateTime endDate
) {
    public static GetWeeklyPaymentCountQuery of(LocalDateTime startDate, LocalDateTime endDate) {
        return new GetWeeklyPaymentCountQuery(startDate, endDate);
    }
}