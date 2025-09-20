package com.roovies.concertreservation.payments.application.port.out;

import com.roovies.concertreservation.payments.domain.entity.Payment;

public interface PaymentRepositoryPort {

    Payment save(Payment payment);
}
