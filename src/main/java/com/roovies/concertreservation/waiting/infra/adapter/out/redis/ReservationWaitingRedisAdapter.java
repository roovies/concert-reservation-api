package com.roovies.concertreservation.waiting.infra.adapter.out.redis;

import com.roovies.concertreservation.waiting.application.port.out.WaitingCachePort;
import com.roovies.concertreservation.waiting.domain.vo.WaitingQueueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Slf4j
@Repository
@Qualifier("reservationWaitingRedis")
@RequiredArgsConstructor
public class ReservationWaitingRedisAdapter implements WaitingCachePort {

    private static final String SEMAPHORE_PREFIX = "semaphore:reservation:";
    private static final String WAITING_PREFIX = "waiting:reservation:";
    private static final String ACTIVE_WAITING_PREFIX = "active:waiting:reservations";
    private static final String ADMITTED_TOKEN_PREFIX = "admitted:reservation:";        // admitted:reservation:{scheduleId}:{userId}:{uuid}
    private static final String ADMIT_LOCK_PREFIX = "admit:lock:reservation:";

    private final RedissonClient redisson;

    @Override
    public boolean tryAcquireSemaphore(Long scheduleId, int maxPermits) {
        String key = SEMAPHORE_PREFIX + scheduleId;
        RSemaphore semaphore = redisson.getSemaphore(key);

        // Semaphore 초기화 (최초 1회)
        if (!semaphore.isExists())
            semaphore.trySetPermits(maxPermits);

        // Permit 획득 시도
        return semaphore.tryAcquire();
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

    private void addActiveQueue(Long scheduleId) {
        RSet<String> activeWaitingQueueSchedules = redisson.getSet(ACTIVE_WAITING_PREFIX);
        activeWaitingQueueSchedules.add(scheduleId.toString());
    }
}
