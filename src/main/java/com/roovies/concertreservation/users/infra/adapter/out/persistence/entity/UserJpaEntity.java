package com.roovies.concertreservation.users.infra.adapter.out.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class UserJpaEntity {
    @Id
    private Long id;
    private String email;
    private String nickname;
    private String password;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

}
