package com.roovies.concertreservation.concerts.infra.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

@Schema(description = "콘서트 목록 조회 요청 DTO")
public record GetConcertsRequest(
        @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
        @PositiveOrZero
        int page,

        @Schema(description = "페이지 크기", example = "20")
        @Min(1)
        int size,

        @Schema(description = "정렬 기준 (예: startDate,desc / price,asc)", example = "startDate,desc")
        String sort,

        @Schema(description = "개최일", example = "2025-01-01")
        LocalDate startDate,

        @Schema(description = "종료일", example = "2025-08-25")
        LocalDate endDate
) {
    public GetConcertsRequest {
        // 기본값 처리
        page = page < 0 ? 0 : page;
        size = size < 1 ? 20 : size;
        sort = (sort == null || sort.isBlank()) ? "timestamp,desc" : sort;
    }
}
