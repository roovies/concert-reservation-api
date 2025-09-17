package com.roovies.concertreservation.reservations.application.port.out;

import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;

public interface HoldSeatIdempotencyCachePort {
    boolean tryProcess(String idempotencyKey);
    boolean isProcessing(String idempotencyKey);
    void removeProcessingStatus(String idempotencyKey);
    void saveResult(String idempotencyKey, HoldSeatResult result);
    HoldSeatResult findByIdempotencyKey(String idempotencyKey);
}
