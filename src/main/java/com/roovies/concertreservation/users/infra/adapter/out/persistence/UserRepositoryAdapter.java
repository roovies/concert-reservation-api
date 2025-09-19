package com.roovies.concertreservation.users.infra.adapter.out.persistence;

import com.roovies.concertreservation.users.application.port.out.UserRepositoryPort;
import com.roovies.concertreservation.users.domain.entity.User;
import com.roovies.concertreservation.users.domain.vo.Email;
import com.roovies.concertreservation.users.infra.adapter.out.persistence.entity.UserJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {
    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findById(Long id) {
        Optional<UserJpaEntity> entity = userJpaRepository.findById(id);
        if (entity.isPresent()) {
            UserJpaEntity userEntity = entity.get();
            return Optional.of(
                    User.create(
                        userEntity.getId(),
                        userEntity.getEmail(),
                        userEntity.getName(),
                        userEntity.getNickname(),
                        userEntity.getCreatedAt(),
                        userEntity.getUpdatedAt(),
                        userEntity.getDeletedAt(),
                        userEntity.getStatus()
                    )
            );
        }
        return Optional.empty();
    }
}
