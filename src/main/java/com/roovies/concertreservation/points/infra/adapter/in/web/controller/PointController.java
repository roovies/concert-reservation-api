package com.roovies.concertreservation.points.infra.adapter.in.web.controller;

import com.roovies.concertreservation.points.infra.adapter.in.web.dto.request.GetPointHistoryRequest;
import com.roovies.concertreservation.points.infra.adapter.in.web.dto.response.GetPointHistoryResponse;
import com.roovies.concertreservation.points.infra.adapter.in.web.dto.response.GetPointResponse;
import com.roovies.concertreservation.points.infra.adapter.in.web.dto.response.UpdatePointResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/point")
@Tag(name = "Point API", description = "포인트 관련 기능 명세서")
@SecurityRequirement(name = "bearerAuth")  // Swagger UI에 자물쇠 표시
public class PointController {

    @Operation(
            summary = "포인트 조회",
            description = "인증된 사용자는 자신이 현재 보유한 포인트를 조회할 수 있다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "포인트 조회 성공",
                            content = @Content(schema = @Schema(implementation = GetPointResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
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
    @Parameters({
            @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true, example = "Bearer {token}")
    })
    @GetMapping
    public ResponseEntity<GetPointResponse> getBalance(@AuthenticationPrincipal UserDetails userDetails) { // TODO: Custom User Details 구현 필요
        return ResponseEntity.status(HttpStatus.CREATED).body(
                GetPointResponse.builder()
                        .point(100000L)
                        .responseTime(LocalDateTime.now())
                        .build()
        );
    }

    @Operation(
            summary = "포인트 충전",
            description = "인증된 사용자는 포인트를 충전할 수 있다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "포인트 충전 성공",
                            content = @Content(schema = @Schema(implementation = UpdatePointResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
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
    @Parameters({
            @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true, example = "Bearer {token}")
    })
    @PostMapping("/charge")
    public ResponseEntity<UpdatePointResponse> updateBalance(@AuthenticationPrincipal UserDetails userDetails) { // TODO: Custom User Details 구현 필요
        return ResponseEntity.status(HttpStatus.CREATED).body(
                UpdatePointResponse.builder()
                        .point(100000L)
                        .responseTime(LocalDateTime.now())
                        .build()
        );
    }

    @Operation(
            summary = "포인트 충전/사용/환불 내역 조회",
            description = "인증된 사용자는 자신의 포인트 충전 및 사용 내역을 조회할 수 있다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "포인트 이력 조회 성공",
                            content = @Content(schema = @Schema(implementation = GetPointHistoryResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 데이터"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
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
    @Parameters({
            @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true, example = "Bearer {token}")
    })
    @GetMapping("/history")
    public ResponseEntity<GetPointHistoryResponse> getBalanceHistory(
            @AuthenticationPrincipal UserDetails userDetails, // TODO: Custom User Details 구현 필요
            @Valid @RequestBody GetPointHistoryRequest request
    ) {
        List<GetPointHistoryResponse.PointHistoryItem> dummyItems = List.of(
                GetPointHistoryResponse.PointHistoryItem.builder()
                        .id(1L)
                        .type("CHARGE")
                        .amount(10000)
                        .pointAfter(50000)
                        .createdAt(LocalDateTime.of(2025, 8, 26, 21, 0))
                        .referenceId(101L)
                        .build(),
                GetPointHistoryResponse.PointHistoryItem.builder()
                        .id(2L)
                        .type("USE")
                        .amount(15000)
                        .pointAfter(35000)
                        .createdAt(LocalDateTime.of(2025, 8, 25, 15, 30))
                        .referenceId(102L)
                        .build(),
                GetPointHistoryResponse.PointHistoryItem.builder()
                        .id(3L)
                        .type("REFUND")
                        .amount(5000)
                        .pointAfter(40000)
                        .createdAt(LocalDateTime.of(2025, 8, 24, 10, 45))
                        .referenceId(103L)
                        .build()
        );

        GetPointHistoryResponse response = GetPointHistoryResponse.builder()
                .items(dummyItems)
                .page(1)
                .size(10)
                .totalPages(5)
                .totalElements(48)
                .build();

        return ResponseEntity.ok(response);
    }
}
