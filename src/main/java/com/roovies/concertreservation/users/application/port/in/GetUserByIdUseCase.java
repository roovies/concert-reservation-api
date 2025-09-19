package com.roovies.concertreservation.users.application.port.in;

import com.roovies.concertreservation.users.application.dto.result.GetUserByIdResult;

public interface GetUserByIdUseCase {
    GetUserByIdResult findById(Long id);
}
