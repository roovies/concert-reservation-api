package com.roovies.concertreservation.reservations.application.port.in;

import com.roovies.concertreservation.reservations.application.dto.command.HoldSeatCommand;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;

public interface HoldSeatUseCase {
    HoldSeatResult execute(HoldSeatCommand command);
}
