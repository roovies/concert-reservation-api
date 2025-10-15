package com.roovies.concertreservation.waiting.application.port.out;

import java.time.Duration;

public interface WaitingCachePort {

    boolean tryAcquireSemaphore(Long resourceId, int maxPermits);
    void saveToken(Long scheduleId, String userKey, String admittedToken);
}
