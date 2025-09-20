package com.roovies.concertreservation.payments.application.port.out;

import com.roovies.concertreservation.payments.application.dto.result.PayReservationResult;
import com.roovies.concertreservation.payments.domain.entity.PaymentIdempotency;

import java.util.Optional;

public interface PaymentIdempotencyRepositoryPort {
    boolean tryLock(PaymentIdempotency paymentIdempotency);
    Optional<PaymentIdempotency> findByKey(String key);
    void setResult(PaymentIdempotency idempotency, Long paymentId, String result);
    void setFailed(PaymentIdempotency idempotency, String reason);
}
