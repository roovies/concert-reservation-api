package com.roovies.concertreservation.waiting.infra.adapter.out.redis;

import com.roovies.concertreservation.waiting.application.port.out.WaitingCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
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
    public void saveToken(Long scheduleId, String userKey, String admittedToken) {
        String key = ADMITTED_TOKEN_PREFIX + scheduleId + ":" + userKey;
        RBucket<String> bucket = redisson.getBucket(key);
        bucket.set(admittedToken, Duration.ofMinutes(10));
    }
}
