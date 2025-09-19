package com.roovies.concertreservation.payments.application.port.out;

import com.roovies.concertreservation.payments.domain.external.query.HeldSeatsQuery;
import com.roovies.concertreservation.payments.domain.external.snapshot.PaymentHeldSeatsSnapShot;

public interface PaymentReservationQueryPort {
    PaymentHeldSeatsSnapShot findHeldSeats(HeldSeatsQuery query);
}
