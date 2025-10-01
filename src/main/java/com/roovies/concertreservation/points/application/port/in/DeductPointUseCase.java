package com.roovies.concertreservation.points.application.port.in;

import com.roovies.concertreservation.points.application.dto.command.DeductPointCommand;
import com.roovies.concertreservation.points.application.dto.result.DeductPointResult;

public interface DeductPointUseCase {
    DeductPointResult deduct(DeductPointCommand command);
}
