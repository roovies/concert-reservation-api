package com.roovies.concertreservation.concerts.infra.adapter.in.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record GetConcertsResponse(
        @Schema(description = "내역 리스트")
        List<Object> items, // TODO: ConcertItem과 같은 controller dto 필요

        @Schema(description = "현재 페이지 번호")
        int page,

        @Schema(description = "페이지 크기")
        int size,

        @Schema(description = "총 페이지 수")
        int totalPages,

        @Schema(description = "총 콘서트 수")
        long totalElements
) {
}
