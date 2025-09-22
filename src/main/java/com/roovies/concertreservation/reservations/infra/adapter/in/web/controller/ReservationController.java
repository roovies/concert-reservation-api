package com.roovies.concertreservation.reservations.infra.adapter.in.web.controller;

import com.roovies.concertreservation.reservations.application.dto.command.HoldSeatCommand;
import com.roovies.concertreservation.reservations.application.dto.query.GetAvailableSeatsQuery;
import com.roovies.concertreservation.reservations.application.dto.result.GetAvailableSeatListResult;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.in.GetAvailableSeatsUseCase;
import com.roovies.concertreservation.reservations.application.port.in.HoldSeatUseCase;
import com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.request.CreateReservationRequest;
import com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.request.GetReservationHistoryRequest;
import com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.response.CreateReservationResponse;
import com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.response.GetAvailableSeatsResponse;
import com.roovies.concertreservation.reservations.infra.adapter.in.web.dto.response.GetReservationHistoryResponse;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservation API", description = "콘서트 예약 관련 명세서")
public class ReservationController {

    private final GetAvailableSeatsUseCase getAvailableSeatsUseCase;
    private final HoldSeatUseCase holdSeatUseCase;

    @Operation(
            summary = "예약 가능 좌석 목록 조회",
            description = "선택한 콘서트 일정의 예약 가능한 좌석 목록을 조회한다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "예약 가능 좌석 목록 조회 성공",
                            content = @Content(schema = @Schema(implementation = GetAvailableSeatsResponse.class))
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
    @GetMapping("/{concertId}/schedules/{date}/seats")
    public ResponseEntity<GetAvailableSeatsResponse> getAvailableSeats(
            @PathVariable("concertId") final Long concertId,
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date
    ) {
        GetAvailableSeatsQuery query = GetAvailableSeatsQuery.builder()
                .concertId(concertId)
                .date(date)
                .build();

        GetAvailableSeatListResult result = getAvailableSeatsUseCase.execute(query);
        List<GetAvailableSeatsResponse.SeatItemDto> availableSeats = result.availableSeats().stream()
                .map(seat -> GetAvailableSeatsResponse.SeatItemDto.builder()
                        .seatId(seat.seatId())
                        .row(seat.row())
                        .seatNumber(seat.seatNumber())
                        .seatType(seat.seatType())
                        .price(seat.price())
                        .build()
                )
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(
                GetAvailableSeatsResponse.builder()
                        .concertId(result.concertId())
                        .concertScheduleId(result.concertScheduleId())
                        .date(result.date())
                        .availableSeats(availableSeats)
                        .isAllReserved(result.isAllReserved())
                        .build()
        );
    }

    @Operation(
            summary = "좌석 예약 신청",
            description = """
                        인증된 사용자가 콘서트의 특정 날짜와 좌석을 예약 신청한다.
                        이 단계에서 결제는 이루어지지 않으며, 좌석은 15분 동안 선점된다.
                        """
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "예약 신청 성공",
                            content = @Content(schema = @Schema(implementation = CreateReservationResponse.class))
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
                            responseCode = "409",
                            description = "이미 예약된 좌석"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류"
                    )
            }
    )
    @SecurityRequirement(name = "bearerAuth")  // Swagger UI에 자물쇠 표시
    @Parameters({
            @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true, example = "Bearer {token}")
    })
    @PostMapping
    public ResponseEntity<CreateReservationResponse> createReservation(
            @AuthenticationPrincipal UserDetails userDetails, // TODO: Custom UserDetails 구현 필요
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request
    ) {
        // TODO: 스프링 시큐리티 구현 후 회원 ID 넘기도록 수정해야 함
        HoldSeatCommand command = HoldSeatCommand.builder()
                .idempotencyKey(idempotencyKey)
                .scheduleId(request.scheduleId())
                .seatIds(request.seatIds())
                .userId(1L)
                .build();
        HoldSeatResult result = holdSeatUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CreateReservationResponse.builder()
                        .scheduleId(result.scheduleId())
                        .seatIds(result.seatIds())
                        .ttlSeconds(result.ttlSeconds())
                        .build()
        );
    }

    @Operation(
            summary = "예약 내역 조회",
            description = "인증된 사용자는 자신의 예약 내역을 조회할 수 있다."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "예약 내역 조회 성공",
                            content = @Content(schema = @Schema(implementation = GetReservationHistoryResponse.class))
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
    @SecurityRequirement(name = "bearerAuth")  // Swagger UI에 자물쇠 표시
    @Parameters({
            @Parameter(name = "Authorization", in = ParameterIn.HEADER, required = true, example = "Bearer {token}")
    })
    @GetMapping("/me")
    public ResponseEntity<GetReservationHistoryResponse> getMyReservations(
            @AuthenticationPrincipal UserDetails userDetails, // TODO: Custom UserDetails 구현 필요
            @Valid @RequestBody GetReservationHistoryRequest request
    ) {

        List<GetReservationHistoryResponse.ReservationHistoryItem> dummyItems = List.of(
                GetReservationHistoryResponse.ReservationHistoryItem.builder()
                        .id(201L)
                        .concertId(101L)
                        .concertTitle("K-Pop Concert 2025")
                        .seatNumber("A-12")
                        .status("CONFIRMED")
                        .reservedAt(LocalDateTime.of(2025, 8, 26, 21, 0))
                        .build(),
                GetReservationHistoryResponse.ReservationHistoryItem.builder()
                        .id(202L)
                        .concertId(102L)
                        .concertTitle("Jazz Night Live")
                        .seatNumber("B-05")
                        .status("CANCELLED")
                        .reservedAt(LocalDateTime.of(2025, 8, 25, 15, 30))
                        .build(),
                GetReservationHistoryResponse.ReservationHistoryItem.builder()
                        .id(203L)
                        .concertId(103L)
                        .concertTitle("Rock Festival 2025")
                        .seatNumber("C-20")
                        .status("CONFIRMED")
                        .reservedAt(LocalDateTime.of(2025, 8, 24, 10, 45))
                        .build()
        );

        GetReservationHistoryResponse response = GetReservationHistoryResponse.builder()
                .items(dummyItems)
                .page(1)
                .size(10)
                .totalPages(5)
                .totalElements(48)
                .build();

        return ResponseEntity.ok(response);
    }
}
