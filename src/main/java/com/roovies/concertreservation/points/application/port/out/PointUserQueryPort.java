package com.roovies.concertreservation.points.application.port.out;

import com.roovies.concertreservation.points.domain.vo.external.PointUserSnapShot;

import java.util.Optional;

public interface PointUserQueryPort {
    Optional<PointUserSnapShot> getUser(Long userId);
}
