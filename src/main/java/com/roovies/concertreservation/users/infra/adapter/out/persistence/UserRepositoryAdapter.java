package com.roovies.concertreservation.users.infra.adapter.out.persistence;

import com.roovies.concertreservation.users.application.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {
    private final UserJpaRepository userJpaRepository;
}
