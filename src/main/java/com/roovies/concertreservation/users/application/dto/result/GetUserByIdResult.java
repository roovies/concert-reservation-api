package com.roovies.concertreservation.users.application.dto.result;

import com.roovies.concertreservation.users.domain.enums.UserStatus;

import java.time.LocalDateTime;

public record GetUserByIdResult(
        Long id,
        String email,
        String name,
        String nickname,
        UserStatus status,
        LocalDateTime createdAt
) {
    public static GetUserByIdResult of(Long id, String email, String name, String nickname, UserStatus status, LocalDateTime createdAt) {
        return new GetUserByIdResult(id, email, name, nickname, status, createdAt);
    }
}
