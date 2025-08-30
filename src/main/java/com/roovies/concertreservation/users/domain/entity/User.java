package com.roovies.concertreservation.users.domain.entity;

import com.roovies.concertreservation.users.domain.enums.UserStatus;
import com.roovies.concertreservation.users.domain.vo.Email;
import com.roovies.concertreservation.users.domain.vo.Name;
import com.roovies.concertreservation.users.domain.vo.Nickname;
import com.roovies.concertreservation.users.domain.vo.Password;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private Email email;
    private Password password;
    private Name name;
    private Nickname nickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private UserStatus status;
    // 이하 생략
}
