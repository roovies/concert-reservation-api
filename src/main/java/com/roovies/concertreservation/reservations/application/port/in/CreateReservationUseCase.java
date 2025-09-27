package com.roovies.concertreservation.reservations.application.port.in;

import com.roovies.concertreservation.reservations.application.dto.command.CreateReservationCommand;

public interface CreateReservationUseCase {
    void createReservation(CreateReservationCommand command);
}
