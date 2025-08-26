package com.roovies.concertreservation.users.infra.adapter.out.persistence;

import com.roovies.concertreservation.users.application.port.out.UserRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;
}
