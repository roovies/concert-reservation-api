package com.roovies.concertreservation.balance.infra.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

@Schema(description = "잔액 충전/사용/환불 내역 요청 DTO")
public record GetBalanceHistoryRequest(
        @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
        @PositiveOrZero
        int page,

        @Schema(description = "페이지 크기", example = "20")
        @Min(1)
        int size,

        @Schema(description = "정렬 기준 (예: timestamp,desc / amount,asc)", example = "timestamp,desc")
        String sort,

        @Schema(description = "내역 종류 필터 (charge/use/refund)", example = "charge")
        String type,

        @Schema(description = "조회 시작 일시", example = "2025-01-01")
        LocalDate startDate,

        @Schema(description = "조회 종료 일시", example = "2025-08-25")
        LocalDate endDate
) {
    public GetBalanceHistoryRequest {
        // 기본값 처리
        page = page < 0 ? 0 : page;
        size = size < 1 ? 20 : size;
        sort = (sort == null || sort.isBlank()) ? "timestamp,desc" : sort;
    }
}

