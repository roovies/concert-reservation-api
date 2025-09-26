package com.roovies.concertreservation.payments.application.port.in;

import com.roovies.concertreservation.payments.application.dto.command.PayReservationCommand;
import com.roovies.concertreservation.payments.application.dto.result.PayReservationResult;

public interface PayReservationUseCase {
    PayReservationResult payReservation(PayReservationCommand command);
}
