package com.roovies.concertreservation.concerts.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Schema(description = "콘서트 상세 정보 응답 DTO")
public record GetConcertResponse(
        @Schema(description = "콘서트 ID", example = "101")
        Long id,

        @Schema(description = "콘서트명", example = "인천 흠뻑쇼")
        String title,

        @Schema(description = "상세설명", example = "지상 최대 여름 페스티벌...")
        String description,

        @Schema(description = "최소 가격", example = "99000")
        long minPrice,

        @Schema(description = "개최일", example = "2025-08-26")
        LocalDate startDate,

        @Schema(description = "종료일", example = "2025-08-27")
        LocalDate endDate,

        @Schema(description = "총 좌석 수", example = "2000")
        int totalSeats,

        @Schema(description = "잔여 좌석 수 (현재 날짜 기준)", example = "500")
        int availableSeats,

        @Schema(description = "생성일", example = "2025-08-26T21:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정일", example = "2025-08-26T21:00:00")
        LocalDateTime updatedAt
) {
}
