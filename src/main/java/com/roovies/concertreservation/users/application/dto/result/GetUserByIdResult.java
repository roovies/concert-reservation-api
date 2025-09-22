package com.roovies.concertreservation.users.application.dto.result;

import com.roovies.concertreservation.users.domain.enums.UserStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record GetUserByIdResult(
        Long id,
        String email,
        String name,
        String nickname,
        UserStatus status,
        LocalDateTime createdAt
) {}
