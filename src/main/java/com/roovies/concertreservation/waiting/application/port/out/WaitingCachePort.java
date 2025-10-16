package com.roovies.concertreservation.waiting.application.port.out;

import com.roovies.concertreservation.waiting.domain.vo.WaitingQueueStatus;

import java.util.Collection;
import java.util.Set;

public interface WaitingCachePort {

    boolean tryAcquireSemaphore(Long resourceId, int maxPermits);
    void saveAdmittedToken(Long resourceId, String userKey, String admittedToken);
    void enterQueue(Long resourceId, String userKey);
    WaitingQueueStatus getRankAndTotalWaitingCount(Long resourceId, String userKey);
    boolean removeWaitingQueue(Long resourceId, String userKey);
    Set<String> getActiveWaitingScheduleIds();
    Collection<String> getActiveWaitingUserKeys(Long resourceId);
    void removeActiveWaitingScheduleId(Long resourceId);
}
