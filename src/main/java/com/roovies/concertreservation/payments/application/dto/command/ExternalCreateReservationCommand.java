package com.roovies.concertreservation.payments.application.dto.command;

import com.roovies.concertreservation.reservations.domain.enums.ReservationStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record ExternalCreateReservationCommand(
        Long paymentId,
        Long userId,
        ReservationStatus status,
        Long scheduleId,
        List<Long> seatIds
) {}
