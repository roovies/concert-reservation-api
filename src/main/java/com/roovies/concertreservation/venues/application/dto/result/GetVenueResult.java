package com.roovies.concertreservation.venues.application.dto.result;

import com.roovies.concertreservation.venues.domain.entity.Venue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공연장 상세 정보 응답 DTO")
public record GetVenueResult(
        @Schema(description = "공연장 ID", example = "101")
        Long id,

        @Schema(description = "공연장명", example = "인천 아시아드 주경기장")
        String name,

        @Schema(description = "총좌석 수", example = "1000")
        int totalSeats,

        @Schema(description = "생성일", example = "2025-08-26T21:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정일", example = "2025-08-26T21:00:00")
        LocalDateTime updatedAt
) {
        public static GetVenueResult from(Venue venue) {
                return new GetVenueResult(
                        venue.getId(),
                        venue.getName(),
                        venue.getTotalSeats(),
                        venue.getCreatedAt(),
                        venue.getUpdatedAt()
                );
        }
}
