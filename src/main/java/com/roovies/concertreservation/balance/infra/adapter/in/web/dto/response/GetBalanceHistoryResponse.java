package com.roovies.concertreservation.balance.infra.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "잔액 충전/사용/환불 내역 응답 DTO")
public record GetBalanceHistoryResponse(
        @Schema(description = "내역 리스트")
        List<BalanceHistoryItem> items, // TODO: BalanceHistoryItem과 같은 controller dto 필요

        @Schema(description = "현재 페이지 번호")
        int page,

        @Schema(description = "페이지 크기")
        int size,

        @Schema(description = "총 페이지 수")
        int totalPages,

        @Schema(description = "총 내역 수")
        long totalElements
) {
        @Builder
        @Schema(description = "잔액 내역 DTO")
        public record BalanceHistoryItem(
                @Schema(description = "내역 ID", example = "123")
                Long id,

                @Schema(description = "거래 타입", example = "CHARGE / USE / REFUND")
                String type,

                @Schema(description = "거래 금액", example = "10000")
                long amount,

                @Schema(description = "거래 후 잔액", example = "50000")
                long balanceAfter,

                @Schema(description = "거래 일시", example = "2025-08-26T21:00:00")
                LocalDateTime createdAt,

                @Schema(description = "관련 결제/예약 ID", example = "456")
                Long referenceId
        ){}
}
