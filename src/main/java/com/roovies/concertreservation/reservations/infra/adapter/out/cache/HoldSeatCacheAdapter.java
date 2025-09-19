package com.roovies.concertreservation.reservations.infra.adapter.out.cache;

import com.roovies.concertreservation.reservations.application.port.out.HoldSeatCachePort;
import com.roovies.concertreservation.reservations.domain.entity.HoldSeat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
@Slf4j
public class HoldSeatCacheAdapter implements HoldSeatCachePort {

    private final RedissonClient redissonClient;

    private static final String HOLD_KEY_PREFIX = "hold";
    private static final String LOCK_KEY_PREFIX = "lock:seat";

    private static final long HOLD_TTL_SECONDS = 900L; // 15분
    private static final long LOCK_WAIT_SECONDS = 3L;
    private static final long LOCK_LEASE_SECONDS = 10L;

    /**
     * 주어진 좌석 목록을 사용자의 홀딩 상태로 Redis에 등록.
     * <p>
     * 1. 락 키 정렬 후 순차적으로 락 획득 (데드락 방지)
     * 2. 이미 홀딩된 좌석이 없는지 확인
     * 3. 모든 좌석 홀딩 처리
     *
     * @param scheduleId 홀딩 대상 스케줄 ID
     * @param seatIds 중복 제거된 홀딩할 좌석 ID 목록
     * @param userId 요청한 사용자 ID
     * @return 모든 좌석 홀딩 성공 시 true, 하나라도 실패 시 false
     */
    @Override
    public boolean holdSeatList(Long scheduleId, List<Long> seatIds, Long userId) {
        // 락 키들을 정렬하여 데드락 방지
        // - 여러 좌석을 동시에 홀딩할 때, 락 획득 순서가 다르면 데드락(교착상태)이 발생할 수 있음
        //   예: 사용자 A가 좌석 1 → 2 순서로 락 시도, 사용자 B가 좌석 2 → 1 순서로 락 시도
        //       두 사용자가 서로 상대방 락을 기다리면서 무한 대기 상태가 발생
        // - 따라서 좌석 ID 또는 락 키를 항상 일정한 순서(예: 오름차순)로 정렬하고 락을 획득하면
        //   모든 요청이 동일한 순서로 락을 시도하게 되어 데드락을 방지할 수 있음
        List<String> sortedLockKeys = seatIds.stream()
                .map(seatId -> createLockKey(scheduleId, seatId))
                .sorted()
                .toList();

        // 모든 락을 순차적으로 획득
        List<RLock> acquiredLocks = new ArrayList<>();
        try {
            // 1. 모든 락 획득 시도
            for (String lockKey : sortedLockKeys) {
                RLock lock = redissonClient.getLock(lockKey); // Lock Key에 대한 분산락(RLock) 객체를 가져옴 (락획득X. 단순히 락을 조작할 수 있는 참조만 획득)
                // tryLock: 지정된 시간 내에 락 획득 시도 (wait: 락 대기 시간, lease: 락 점유 시간)
                boolean lockAcquired = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);

                if(!lockAcquired) {
                    log.warn("좌석 홀딩 락 획득 실패: lockKey={}, scheduleId={}, userId={}", lockKey, scheduleId, userId);
                    return false; // 하나라도 실패하면 전체 실패
                }
                acquiredLocks.add(lock);
            }
            log.info("모든 좌석 락 획득 성공: scheduleId={}, seatIds={}, userId={}",
                    scheduleId, seatIds, userId);

            // 2. 모든 좌석이 홀딩 가능한지 확인
            List<String> holdKeys = seatIds.stream()
                    .map(seatId -> createHoldKey(scheduleId, seatId))
                    .toList();

            for (String holdKey : holdKeys) {
                RBucket<String> bucket = redissonClient.getBucket(holdKey);
                // 2-1. 해당 좌석 키값이 이미 캐싱되고 있는지 확인
                if (bucket.isExists()) {
                    log.warn("이미 홀딩된 좌석 존재: holdKey={}, scheduleId={}, userId={}",
                            holdKey, scheduleId, userId);
                    return false; // 하나라도 이미 홀딩되어 있으면 전체 실패
                }

                // 2-2. 모든 좌석이 홀딩이 가능하다면, 모든 좌석 홀딩 처리
                bucket.set(userId.toString(), Duration.ofSeconds(HOLD_TTL_SECONDS));
            }

            log.info("모든 좌석 홀딩 성공: scheduleId={}, seatIds={}, userId={}",
                    scheduleId, seatIds, userId);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("좌석 홀딩 중 인터럽트 발생: scheduleId={}, seatIds={}, userId={}",
                    scheduleId, seatIds, userId, e);
            return false;
        } catch (Exception e) {
            log.error("좌석 홀딩 중 예외 발생: scheduleId={}, seatIds={}, userId={}",
                    scheduleId, seatIds, userId, e);
            return false;
        } finally {
            // 4. 모든 락 해제
            // - tryLock()으로 락을 획득한 후 좌석 보류 로직 실행 도중 예외가 발생하거나, lease time 내에 작업이 일찍 끝났더라도
            //   락을 해제하지 않았으므로 다른 쓰레드/서버에서 해당 좌석을 계속 점유 상태로 인식하게 됨 (일시적 데드락 발생 가능성)
            // - 따라서 finally에서 항상 락 해제를 호출하여 안전하게 다른 요청이 접근 가능하도록 함
            for (RLock lock : acquiredLocks) {
                try {
                    if (lock.isHeldByCurrentThread())
                        lock.unlock();
                } catch (Exception e) {
                    log.error("락 해제 중 오류 발생", e);
                }
            }
        }
    }

    /**
     * 주어진 좌석 목록의 홀딩 상태를 삭제.
     * <p>
     * 1. 락 키 정렬 후 순차적으로 락 획득 (데드락 방지)
     * 2. 홀딩 권한 검증 후 삭제
     *
     * @param scheduleId 삭제 대상 공연/스케줄 ID
     * @param seatIds 중복 제거된 삭제할 좌석 ID 목록
     * @param userId 요청한 사용자 ID
     * @return 모든 좌석 삭제 성공 시 true, 일부 좌석 삭제 실패/권한 없음 시 false
     */
    @Override
    public boolean deleteHoldSeatList(Long scheduleId, List<Long> seatIds, Long userId) {
        // 1. 락 키들을 정렬하여 데드락 방지
        List<String> sortedLockKeys = seatIds.stream()
                .map(seatId -> createLockKey(scheduleId, seatId))
                .sorted()
                .toList();

        // 2. 모든 락을 순차적으로 획득
        List<RLock> acquiredLocks = new ArrayList<>();
        try {
            for (String lockKey : sortedLockKeys) {
                RLock lock = redissonClient.getLock(lockKey);
                boolean lockAcquired = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);

                if (!lockAcquired) {
                    log.warn("좌석 홀딩 해제 락 획득 실패: lockKey={}", lockKey);
                    return false;
                }
                acquiredLocks.add(lock);
            }

            // 3. 권한 검증 및 삭제
            boolean allDeleted = true;
            for (Long seatId : seatIds) {
                String holdKey = createHoldKey(scheduleId, seatId);
                RBucket<String> bucket = redissonClient.getBucket(holdKey);

                if(!bucket.isExists()) {
                    log.warn("삭제할 홀딩이 존재하지 않음: scheduleId={}, seatId={}", scheduleId, seatId);
                    continue; // 이미 없으면 넘어감
                }

                String currentHoldUserId = bucket.get();
                if (currentHoldUserId == null || !currentHoldUserId.equals(userId.toString())) {
                    log.warn("홀딩 해제 권한 없음: scheduleId={}, seatId={}, requestUserId={}, holdUserId={}",
                            scheduleId, seatId, userId, currentHoldUserId);
                    allDeleted = false;
                    continue; // 권한이 없으면 해당 좌석은 삭제하지 않음
                }

                bucket.delete();
                log.debug("좌석 홀딩 삭제: scheduleId={}, seatId={}, userId={}", scheduleId, seatId, userId);
            }

            return allDeleted;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.error("좌석 홀딩 삭제 중 예외 발생: scheduleId={}, seatIds={}, userId={}",
                    scheduleId, seatIds, userId, e);
            return false;
        } finally {
            // 모든 락 해제
            for (RLock lock : acquiredLocks) {
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                } catch (Exception e) {
                    log.error("락 해제 중 오류 발생", e);
                }
            }
        }
    }

    /**
     * 주어진 좌석 목록이 현재 사용자에 의해 홀딩되어 있는지 검증.
     *
     * @param scheduleId 검증 대상 공연/스케줄 ID
     * @param seatIds 중복 제거된 검증할 좌석 ID 목록
     * @param userId 요청한 사용자 ID
     * @return 모든 좌석이 사용자의 홀딩 상태일 경우 true, 그렇지 않으면 false
     */
    @Override
    public boolean validateHoldSeatList(Long scheduleId, List<Long> seatIds, Long userId) {
        try {
            for (Long seatId : seatIds) {
                String holdKey = createHoldKey(scheduleId, seatId);
                RBucket<String> bucket = redissonClient.getBucket(holdKey);

                if (!bucket.isExists()) {
                    log.debug("홀딩 정보 없음: scheduleId={}, seatId={}", scheduleId, seatId);
                    return false;
                }

                String currentHoldUserId = bucket.get();
                if (currentHoldUserId == null || !currentHoldUserId.equals(userId.toString())) {
                    log.debug("홀딩 권한 없음: scheduleId={}, seatId={}, requestUserId={}, holdUserId={}",
                            scheduleId, seatId, userId, currentHoldUserId);
                    return false;
                }
            }

            log.debug("모든 좌석 홀딩 검증 성공: scheduleId={}, seatIds={}, userId={}",
                    scheduleId, seatIds, userId);
            return true;

        } catch (Exception e) {
            log.error("좌석 홀딩 검증 중 예외 발생: scheduleId={}, seatIds={}, userId={}",
                    scheduleId, seatIds, userId, e);
            return false;
        }
    }

    /**
     * 주어진 좌석 목록의 TTL(만료까지 남은 시간)을 조회.
     * 모든 좌석이 동일 TTL로 설정된다고 가정하고, 첫 번째 좌석 기준으로 TTL 반환.
     *
     * @param scheduleId 공연/스케줄 ID
     * @param seatIds 조회할 좌석 ID 목록
     * @param userId 요청한 사용자 ID
     * @return 남은 TTL 초 단위 (없으면 -1: expire 설정 안됨, -2: 키 없음)
     */
    public long getHoldTTLSeconds(Long scheduleId, List<Long> seatIds, Long userId) {
        try {
            for (Long seatId : seatIds) {
                String holdKey = createHoldKey(scheduleId, seatId);
                RBucket<String> bucket = redissonClient.getBucket(holdKey);

                if (!bucket.isExists()) {
                    log.debug("TTL 조회 실패 (키 없음): scheduleId={}, seatId={}", scheduleId, seatId);
                    return -2;
                }

                String currentHoldUserId = bucket.get();
                if (currentHoldUserId == null || !currentHoldUserId.equals(userId.toString())) {
                    log.debug("TTL 조회 권한 없음: scheduleId={}, seatId={}, requestUserId={}, holdUserId={}",
                            scheduleId, seatId, userId, currentHoldUserId);
                    return -2;
                }

                long remainMillis = bucket.remainTimeToLive();
                if (remainMillis < 0)
                    return remainMillis; // -1(expire: 없음), -2(키 없음)

                return TimeUnit.MILLISECONDS.toSeconds(remainMillis); // 초로 변환
            }
        } catch (Exception e) {
            log.error("좌석 TTL 조회 중 예외 발생: scheduleId={}, seatIds={}, userId={}",
                    scheduleId, seatIds, userId, e);
        }
        return -2;
    }

    /**
     * 주어진 좌석 목록의 홀딩 상태를 조회하여 Domain 객체로 반환.
     *
     * @param scheduleId 조회할 스케줄 ID
     * @param seatIds 조회할 좌석 ID 목록
     * @param userId 요청한 사용자 ID (권한 검증용)
     * @return 홀딩된 좌석 정보 목록 (Domain 객체)
     */
    @Override
    public List<HoldSeat> getHoldSeatList(Long scheduleId, List<Long> seatIds, Long userId) {
        try {
            List<HoldSeat> results = new ArrayList<>();

            for (Long seatId : seatIds) {
                String holdKey = createHoldKey(scheduleId, seatId);
                RBucket<String> bucket = redissonClient.getBucket(holdKey);

                if (!bucket.isExists()) {
                    // 홀딩되지 않은 좌석이 있으면 예외 발생
                    return Collections.emptyList();
                }

                String holdUserIdStr = bucket.get();
                if (holdUserIdStr == null) {
                    log.warn("홀딩 키는 존재하지만 값이 null: scheduleId={}, seatId={}", scheduleId, seatId);
                    return Collections.emptyList();
                }

                // TTL 조회 (남은 시간)
                long remainMillis = bucket.remainTimeToLive();
                long remainSeconds = remainMillis > 0 ? TimeUnit.MILLISECONDS.toSeconds(remainMillis) : -1;

                Long holdUserId = Long.valueOf(holdUserIdStr);
                // 현재 사용자가 홀딩한 좌석이 아니면 예외 발생
                if (!holdUserId.equals(userId)) {
                    return Collections.emptyList();
                }

                // Domain 객체 생성하여 추가
                HoldSeat holdSeat = HoldSeat.create(
                        scheduleId,
                        seatId,
                        holdUserId,
                        remainSeconds
                );
                results.add(holdSeat);
            }

            log.debug("홀딩된 좌석 조회 완료: scheduleId={}, 요청좌석수={}, 홀딩좌석수={}, userId={}",
                    scheduleId, seatIds.size(), results.size(), userId);

            return results;

        } catch (Exception e) {
            log.error("홀딩된 좌석 조회 중 예외 발생: scheduleId={}, seatIds={}, userId={}",
                    scheduleId, seatIds, userId, e);
            return Collections.emptyList();
        }
    }

    private String createLockKey(Long scheduleId, Long seatId) {
        return String.format("%s:%d:%d", LOCK_KEY_PREFIX, scheduleId, seatId);
    }

    private String createHoldKey(Long scheduleId, Long seatId) {
        return String.format("%s:%d:%d", HOLD_KEY_PREFIX, scheduleId, seatId);
    }
}
