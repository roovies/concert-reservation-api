package com.roovies.concertreservation.concerts.infra.adapter.in.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record GetConcertResponse(
        @Schema(description = "콘서트 ID")
        Long id,

        @Schema(description = "콘서트명")
        String title,

        @Schema(description = "상세설명")
        String description,

        @Schema(description = "개최일")
        LocalDate startDate,

        @Schema(description = "종료일")
        LocalDate endDate,

        @Schema(description = "총 좌석 수")
        int totalSeats,

        @Schema(description = "생성일")
        LocalDateTime createdAt,

        @Schema(description = "수정일")
        LocalDateTime updatedAt
) {
}
