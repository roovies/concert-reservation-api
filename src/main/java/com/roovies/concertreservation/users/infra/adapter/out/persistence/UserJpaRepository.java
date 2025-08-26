package com.roovies.concertreservation.users.infra.adapter.out.persistence;

import com.roovies.concertreservation.users.infra.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
}
