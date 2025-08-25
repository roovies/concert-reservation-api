package com.roovies.concertreservation.concerts.infra.adapter.in.web;

import com.roovies.concertreservation.balance.infra.adapter.in.web.request.GetBalanceHistoryRequest;
import com.roovies.concertreservation.concerts.infra.adapter.in.web.request.GetConcertsRequest;
import com.roovies.concertreservation.concerts.infra.adapter.in.web.response.GetConcertResponse;
import com.roovies.concertreservation.concerts.infra.adapter.in.web.response.GetConcertsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/concerts")
@Tag(name = "Concert API", description = "콘서트 관련 기능 명세서")
public class ConcertController {

    @Operation(
            summary = "콘서트 목록 조회",
            description = "모든 사용자는 콘서트 목록을 조회할 수 있다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "콘서트 목록 조회 성공",
                            content = @Content(schema = @Schema(implementation = GetConcertsResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "리소스를 찾을 수 없음"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류"
                    )
            }
    )
    @GetMapping
    public ResponseEntity<GetConcertsResponse> getConcerts(@Valid @RequestBody GetConcertsRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(GetConcertsResponse.builder()
                        .items(Arrays.asList(1, 2, 3))
                        .page(1)
                        .size(10)
                        .totalPages(5)
                        .totalElements(48)
                        .build());
    }

    @Operation(
            summary = "콘서트 상세 정보 조회",
            description = "모든 사용자는 콘서트의 상세 정보를 조회할 수 있다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "콘서트 상세 정보 조회 성공",
                            content = @Content(schema = @Schema(implementation = GetConcertResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "리소스를 찾을 수 없음"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류"
                    )
            }
    )
    @GetMapping("/{concertId}")
    public ResponseEntity<GetConcertResponse> getConcert(@PathVariable Long concertId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                GetConcertResponse.builder()
                        .id(1L)
                        .title("흠뻑쇼")
                        .description("설명")
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .totalSeats(1000)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );
    }
}
