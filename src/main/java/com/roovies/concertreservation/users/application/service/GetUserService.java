package com.roovies.concertreservation.users.application.service;

import com.roovies.concertreservation.users.application.dto.result.GetUserResult;
import com.roovies.concertreservation.users.application.port.in.GetUserUseCase;
import com.roovies.concertreservation.users.application.port.out.UserQueryRepositoryPort;
import com.roovies.concertreservation.users.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GetUserService implements GetUserUseCase {

    private final UserQueryRepositoryPort userQueryRepositoryPort;

    @Override
    public GetUserResult findById(Long id) {
        User user = userQueryRepositoryPort.findById(id)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));

        return null;
    }
}
