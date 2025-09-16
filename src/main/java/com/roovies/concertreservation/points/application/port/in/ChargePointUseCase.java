package com.roovies.concertreservation.points.application.port.in;

import com.roovies.concertreservation.points.application.dto.command.ChargePointCommand;
import com.roovies.concertreservation.points.application.dto.result.ChargePointResult;

public interface ChargePointUseCase {
    ChargePointResult execute(ChargePointCommand command);
}
