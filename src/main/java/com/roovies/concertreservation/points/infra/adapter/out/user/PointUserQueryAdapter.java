package com.roovies.concertreservation.points.infra.adapter.out.user;

import com.roovies.concertreservation.points.application.port.out.PointUserQueryPort;
import com.roovies.concertreservation.points.domain.vo.external.PointUserSnapShot;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PointUserQueryAdapter implements PointUserQueryPort {

    @Override
    public Optional<PointUserSnapShot> getUser(Long userId) {
        return Optional.empty();
    }
}
