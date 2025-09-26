package com.roovies.concertreservation.concerts.application.service.command;

import com.roovies.concertreservation.concerts.application.port.in.CreateConcertUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CreateConcertService implements CreateConcertUseCase {
    @Override
    public void create() {

    }
}
