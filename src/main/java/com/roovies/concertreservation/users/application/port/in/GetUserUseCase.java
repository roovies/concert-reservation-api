package com.roovies.concertreservation.users.application.port.in;

import com.roovies.concertreservation.users.application.dto.result.GetUserResult;

public interface GetUserUseCase {
    GetUserResult findById(Long id);
}
