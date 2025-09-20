package com.roovies.concertreservation.users.domain.entity;

import com.roovies.concertreservation.users.domain.enums.UserStatus;
import com.roovies.concertreservation.users.domain.vo.Email;
import com.roovies.concertreservation.users.domain.vo.Name;
import com.roovies.concertreservation.users.domain.vo.Nickname;
import com.roovies.concertreservation.users.domain.vo.Password;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class User {
    // TODO: 의미있는 값은 VO 처리 해야 함
    private Long id;
    private String email;
    private String password;
    private String name;
    private String nickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private UserStatus status;
    // 이하 생략

    public static User create(Long id, String email, String name, String nickname, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt, UserStatus status) {
        return new User(id, email, name, nickname, createdAt, updatedAt, deletedAt, status);
    }

    private User(Long id, String email, String name, String nickname, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt, UserStatus status) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.status = status;
    }
}
