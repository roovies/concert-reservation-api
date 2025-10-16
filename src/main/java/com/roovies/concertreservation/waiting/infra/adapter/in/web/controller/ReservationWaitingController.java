package com.roovies.concertreservation.waiting.infra.adapter.in.web.controller;

import com.roovies.concertreservation.waiting.application.dto.result.EnterQueueResult;
import com.roovies.concertreservation.waiting.application.port.in.WaitingUseCase;
import com.roovies.concertreservation.waiting.infra.adapter.in.web.controller.response.ReservationEnterResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/waiting/reservation")
@Tag(name = "Reservation Waiting API", description = "예약 대기열 명세서")
public class ReservationWaitingController {

    private final WaitingUseCase waitingUseCase;

    public ReservationWaitingController(@Qualifier("reservationWaitingService") WaitingUseCase waitingUseCase) {
        this.waitingUseCase = waitingUseCase;
    }
    /**
     * 1. 대기열 진입 또는 즉시 입장
     */
    @PostMapping("/enter")
    public ResponseEntity<ReservationEnterResponse> enterQueue(
            @AuthenticationPrincipal UserDetails userDetails, // TODO: Custom UserDetails 구현 필요
            @RequestBody Long scheduleId
    ) {
        // TODO: Security Context에서 가져오도록 해야함
        Long tmpUserId = 1L;

        EnterQueueResult result = waitingUseCase.enterOrWaitQueue(tmpUserId, scheduleId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ReservationEnterResponse.builder()
                        .admitted(result.admitted())
                        .admittedToken(result.admittedToken())
                        .rank(result.rank())
                        .totalWaiting(result.totalWaiting())
                        .userKey(result.userKey())
                        .build()
        );
    }
}
