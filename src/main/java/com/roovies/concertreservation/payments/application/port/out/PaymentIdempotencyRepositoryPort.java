package com.roovies.concertreservation.payments.application.port.out;

import com.roovies.concertreservation.payments.application.dto.result.PayReservationResult;
import com.roovies.concertreservation.payments.domain.entity.PaymentIdempotency;

import java.util.Optional;

public interface PaymentIdempotencyRepositoryPort {
    boolean tryLock(PaymentIdempotency paymentIdempotency);
    Optional<PaymentIdempotency> findByKey(String key);
    void setResult(String idempotencyKey, Long paymentId, String result);
    void setFailed(String idempotencyKey, String reason);
}
