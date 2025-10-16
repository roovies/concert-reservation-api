package com.roovies.concertreservation.waiting.application.port.out;

import com.roovies.concertreservation.waiting.domain.vo.WaitingQueueStatus;

public interface WaitingCachePort {

    boolean tryAcquireSemaphore(Long resourceId, int maxPermits);
    void saveAdmittedToken(Long resourceId, String userKey, String admittedToken);
    void enterQueue(Long resourceId, String userKey);
    WaitingQueueStatus getRankAndTotalWaitingCount(Long resourceId, String userKey);
}
