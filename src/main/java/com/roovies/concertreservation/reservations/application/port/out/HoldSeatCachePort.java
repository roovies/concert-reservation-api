package com.roovies.concertreservation.reservations.application.port.out;

import java.util.List;

public interface HoldSeatCachePort {
    boolean holdSeatList(Long scheduleId, List<Long> seatIds, Long userId);
    boolean deleteHoldSeatList(Long scheduleId, List<Long> seatIds, Long userId);
    boolean validateHoldSeatList(Long scheduleId, List<Long> seatIds, Long userId);
    long getHoldTTLSeconds(Long scheduleId, List<Long> seatIds, Long userId);
}
