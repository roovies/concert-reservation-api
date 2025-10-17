package com.roovies.concertreservation.waiting.application.port.out;

import com.roovies.concertreservation.waiting.domain.vo.WaitingQueueEntry;
import com.roovies.concertreservation.waiting.domain.vo.WaitingQueueStatus;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface WaitingCachePort {

    boolean tryAcquirePermit(Long resourceId);
    boolean tryAcquirePermits(Long scheduleId, int count);
    int getAvailablePermits(Long resourceId);
    void releasePermits(Long resourceId, int count);
    boolean tryAcquireAdmitLock(Long resourceId);
    void releaseAdmitLock(Long resourceId);
    void saveAdmittedToken(Long resourceId, String userKey, String admittedToken);
    void enterQueue(Long resourceId, String userKey);
    WaitingQueueStatus getRankAndTotalWaitingCount(Long resourceId, String userKey);
    boolean removeWaitingQueue(Long resourceId, String userKey);
    Set<String> getActiveWaitingScheduleIds();
    Collection<String> getActiveWaitingUserKeys(Long resourceId);
    void removeActiveWaitingScheduleId(Long resourceId);
    int getWaitingQueueSize(Long resourceId);
    List<WaitingQueueEntry> admitUsers(Long resourceId, int count);
    void addUserToWaitingQueue(Long resourceId, String userKey, double score);
}
