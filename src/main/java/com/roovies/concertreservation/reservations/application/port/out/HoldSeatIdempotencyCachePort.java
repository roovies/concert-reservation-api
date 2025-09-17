package com.roovies.concertreservation.reservations.application.port.out;

import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;

import java.util.Optional;

public interface HoldSeatIdempotencyCachePort {
    void saveResult(String idempotencyKey, HoldSeatResult result);
    Optional<HoldSeatResult> findByIdempotencyKey(String idempotencyKey);
    boolean exists(String idempotencyKey);
}
