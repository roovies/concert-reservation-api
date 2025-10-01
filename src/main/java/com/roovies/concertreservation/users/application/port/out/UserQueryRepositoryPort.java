package com.roovies.concertreservation.users.application.port.out;

import com.roovies.concertreservation.users.domain.entity.User;

import java.util.Optional;

public interface UserQueryRepositoryPort {
    Optional<User> findById(Long id);
}
