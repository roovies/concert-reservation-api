package com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "콘서트 예약 가능 일정 목록 조회 응답 DTO")
public record GetAvailableSchedulesResponse(
        @Schema(description = "콘서트 ID", example = "101")
        Long concertId,

        @Schema(description = "예약 가능 날짜 목록", example = "[\"2025-09-01\", \"2025-09-02\"]")
        List<LocalDate> availableDates
) {
}
