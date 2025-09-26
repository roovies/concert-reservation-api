package com.roovies.concertreservation.payments.application.port.out;

import com.roovies.concertreservation.payments.application.dto.query.GetHeldSeatListQuery;
import com.roovies.concertreservation.payments.domain.external.ExternalHeldSeatList;

public interface PaymentReservationGatewayPort {
    ExternalHeldSeatList findHeldSeats(GetHeldSeatListQuery query);
}
