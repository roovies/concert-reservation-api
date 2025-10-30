package com.roovies.concertreservation.waiting.infra.adapter.out.redis;

import com.roovies.concertreservation.waiting.application.port.out.WaitingCachePort;
import com.roovies.concertreservation.waiting.domain.vo.WaitingQueueEntry;
import com.roovies.concertreservation.waiting.domain.vo.WaitingQueueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository("reservationWaitingRedis")
@RequiredArgsConstructor
public class ReservationWaitingRedisAdapter implements WaitingCachePort {

    private static final String SEMAPHORE_PREFIX = "semaphore:reservation:";
    private static final String WAITING_PREFIX = "waiting:reservation:";
    private static final String ACTIVE_WAITING_PREFIX = "active:waiting:reservations";
    private static final String ADMITTED_TOKEN_PREFIX = "admitted:reservation:";        // admitted:reservation:{scheduleId}:{userId}:{uuid}
    private static final String ADMIT_LOCK_PREFIX = "admit:lock:reservation:";

    private static final int MAX_PERMITS = 100;

    private final RedissonClient redisson;

    @Override
    public boolean tryAcquirePermit(Long scheduleId) {
        String key = SEMAPHORE_PREFIX + scheduleId;
        RSemaphore semaphore = redisson.getSemaphore(key);

        // Semaphore 초기화 (최초 1회)
        if (!semaphore.isExists())
            semaphore.trySetPermits(MAX_PERMITS);

        // Permit 획득 시도
        return semaphore.tryAcquire();
    }

    @Override
    public boolean tryAcquirePermits(Long scheduleId, int count) {
        String key = SEMAPHORE_PREFIX + scheduleId;
        RSemaphore semaphore = redisson.getSemaphore(key);
        return semaphore.tryAcquire(count);
    }

    @Override
    public int getAvailablePermits(Long scheduleId) {
        String key = SEMAPHORE_PREFIX + scheduleId;
        RSemaphore semaphore = redisson.getSemaphore(key);
        return semaphore.availablePermits();
    }

    @Override
    public void releasePermits(Long scheduleId, int count) {
        String key = SEMAPHORE_PREFIX + scheduleId;
        RSemaphore semaphore = redisson.getSemaphore(key);
        semaphore.release(count);
    }

    @Override
    public boolean tryAcquireAdmitLock(Long scheduleId) {
        String key = ADMIT_LOCK_PREFIX + scheduleId;
        RLock lock = redisson.getLock(key);

        try {
            return lock.tryLock(0, 10,  TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 획득 중 인터럽트 발생: scheduleId = {}", scheduleId, e);
            return false;
        }
    }

    @Override
    public void releaseAdmitLock(Long scheduleId) {
        String key = ADMIT_LOCK_PREFIX + scheduleId;
        RLock lock = redisson.getLock(key);

        // 현재 스레드가 락을 보유하고 있을 때만 해제
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("분산락 해제: scheduleId = {}", scheduleId);
        }
    }

    @Override
    public void saveAdmittedToken(Long scheduleId, String userKey, String admittedToken) {
        String key = ADMITTED_TOKEN_PREFIX + scheduleId + ":" + userKey;
        RBucket<String> bucket = redisson.getBucket(key);
        bucket.set(admittedToken, Duration.ofMinutes(10));

        log.debug("[ReservationWaitingRedisAdapter] 입장 토큰 발급 완료 - scheduleId: {}, userKey: {}", scheduleId, userKey);
    }

    @Override
    public void enterQueue(Long scheduleId, String userKey) {
        String key = WAITING_PREFIX + scheduleId;
        RScoredSortedSet<String> waitingQueue = redisson.getScoredSortedSet(key);
        long timestamp = System.currentTimeMillis();
        waitingQueue.add(timestamp, userKey);

        addActiveQueue(scheduleId);
    }

    @Override
    public WaitingQueueStatus getRankAndTotalWaitingCount(Long scheduleId, String userKey) {
        String key = WAITING_PREFIX + scheduleId;
        RScoredSortedSet<String> waitingQueue = redisson.getScoredSortedSet(key);

        Integer rank = waitingQueue.rank(userKey);
        Integer totalWaiting = waitingQueue.size();

        return new WaitingQueueStatus(userKey, rank, totalWaiting);
    }

    @Override
    public boolean removeWaitingQueue(Long scheduleId, String userKey) {
        String key = WAITING_PREFIX + scheduleId;
        RScoredSortedSet<String> waitingQueue = redisson.getScoredSortedSet(key);
        boolean result = waitingQueue.remove(userKey);
        if (result) {
            log.debug("대기열 큐에서 제거됨: scheduleId = {}, userKey = {}", scheduleId, userKey);

            // 제거한 후 더이상 대기자가 없을 경우 활성화된 대기열 큐 목록에서 해당 스케줄 삭제
            int remainingSize = waitingQueue.size();
            if (remainingSize == 0) {
                RSet<String> activeWaitingQueueScheduleIds = redisson.getSet(ACTIVE_WAITING_PREFIX);
                boolean removedActiveWaitingQueue = activeWaitingQueueScheduleIds.remove(scheduleId.toString());
                if (removedActiveWaitingQueue)
                    log.debug("대기자가 없어 대기열 큐에서 제거됨: scheduleId = {}", scheduleId);
            } else {
                log.debug("대기자가 아직 존재하여 대기열 큐 유지: scheduleId = {}", scheduleId);
            }
        }

        return result;
    }

    @Override
    public Set<String> getActiveWaitingScheduleIds() {
        RSet<String> activeWaitingScheduleIds = redisson.getSet(ACTIVE_WAITING_PREFIX);
        return activeWaitingScheduleIds.readAll();
    }

    @Override
    public boolean hasActiveWaitingQueue(Long resourceId) {
        RSet<String> activeWaitingScheduleIds = redisson.getSet(ACTIVE_WAITING_PREFIX);
        return activeWaitingScheduleIds.contains(resourceId.toString());
    }

    @Override
    public Collection<String> getActiveWaitingUserKeys(Long scheduleId) {
        String key = WAITING_PREFIX + scheduleId;
        RScoredSortedSet<String> waitingQueue = redisson.getScoredSortedSet(key);
        return waitingQueue.readAll();
    }

    @Override
    public void removeActiveWaitingScheduleId(Long scheduleId) {
        RSet<String> activeWaitingScheduleIds = redisson.getSet(ACTIVE_WAITING_PREFIX);
        activeWaitingScheduleIds.remove(scheduleId);
    }

    @Override
    public int getWaitingQueueSize(Long scheduleId) {
        String key = WAITING_PREFIX + scheduleId;
        RScoredSortedSet<String> waitingQueue = redisson.getScoredSortedSet(key);
        return waitingQueue.size();
    }

    @Override
    public List<WaitingQueueEntry> admitUsers(Long scheduleId, int count) {
        String key = WAITING_PREFIX + scheduleId;
        RScoredSortedSet<String> waitingQueue = redisson.getScoredSortedSet(key);

        Collection<ScoredEntry<String>> entries = waitingQueue.pollFirstEntries(count);
        return entries.stream()
                .map(entry -> new WaitingQueueEntry(
                        entry.getValue(),
                        entry.getScore()
                ))
                .toList();
    }

    @Override
    public void addUserToWaitingQueue(Long scheduleId, String userKey, double score) {
        String key = WAITING_PREFIX + scheduleId;
        RScoredSortedSet<String> waitingQueue = redisson.getScoredSortedSet(key);
        waitingQueue.add(score, userKey);
    }

    private void addActiveQueue(Long scheduleId) {
        RSet<String> activeWaitingQueueSchedules = redisson.getSet(ACTIVE_WAITING_PREFIX);
        activeWaitingQueueSchedules.add(scheduleId.toString());
    }
}
