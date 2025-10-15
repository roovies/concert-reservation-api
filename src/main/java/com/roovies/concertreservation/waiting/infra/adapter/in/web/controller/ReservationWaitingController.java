package com.roovies.concertreservation.waiting.infra.adapter.in.web.controller;

import com.roovies.concertreservation.waiting.infra.adapter.in.web.controller.response.ReservationEnterResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/waiting/reservation")
@Tag(name = "Reservation Waiting API", description = "예약 대기열 명세서")
public class ReservationWaitingController {

    /**
     * 1. 대기열 진입 또는 즉시 입장
     */
    @PostMapping("/enter")
    public ResponseEntity<ReservationEnterResponse> enterQueue(
            @AuthenticationPrincipal UserDetails userDetails, // TODO: Custom UserDetails 구현 필요
            @RequestBody Long scheduleId
    ) {
        return null;
    }
}
