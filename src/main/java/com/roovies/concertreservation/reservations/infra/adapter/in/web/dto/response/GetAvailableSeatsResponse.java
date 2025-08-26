package com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.response;

import com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.vo.SeatItemDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record GetAvailableSeatsResponse(
        @Schema(description = "콘서트 ID", example = "101")
        Long concertId,

        @Schema(description = "조회 날짜", example = "2025-09-01")
        LocalDate date,

        @Schema(description = "좌석 목록")
        List<SeatItemDto> seats
) {
}
