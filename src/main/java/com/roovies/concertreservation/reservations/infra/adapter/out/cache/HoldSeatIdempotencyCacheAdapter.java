package com.roovies.concertreservation.reservations.infra.adapter.out.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.out.HoldSeatIdempotencyCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
@Slf4j
public class HoldSeatIdempotencyCacheAdapter implements HoldSeatIdempotencyCachePort {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:hold-seat";
    private static final long IDEMPOTENCY_TTL_SECONDS = 3600L; // 1시간
    private static final String PROCESSING_STATUS = "PROCESSING";

    @Override
    public boolean tryProcess(String idempotencyKey) {
        try {
            String key = createIdempotencyKey(idempotencyKey);
            RBucket<String> bucket = redissonClient.getBucket(key);

            // setIfAbsent 사용 -> 원자 연산, 이미 있으면 false를 반환
            // - 동시에 여러 쓰레드가 같은 key를 저장하려 해도 최초로 하나만 성공함
            boolean result = bucket.setIfAbsent(PROCESSING_STATUS, Duration.ofSeconds(IDEMPOTENCY_TTL_SECONDS));
            if (result)
                log.debug("멱등성 키 선점 성공: idempotencyKey={}", idempotencyKey);
            else
                log.debug("멱등성 키 이미 존재: idempotencyKey={}", idempotencyKey);

            return result;
        } catch (Exception e) {
            log.error("멱등성 키 선점 실패: idempotencyKey={}", idempotencyKey, e);
            return false;
        }
    }

    @Override
    public boolean isProcessing(String idempotencyKey) {
        try {
            String key = createIdempotencyKey(idempotencyKey);
            RBucket<String> bucket = redissonClient.getBucket(key);

            if (!bucket.isExists())
                return false;

            String value = bucket.get();
            return PROCESSING_STATUS.equals(value);
        } catch (Exception e) {
            log.error("멱등성 처리 상태 확인 실패: idempotencyKey={}", idempotencyKey, e);
            return false;
        }
    }

    @Override
    public void removeProcessingStatus(String idempotencyKey) {
        try {
            String key = createIdempotencyKey(idempotencyKey);
            RBucket<String> bucket = redissonClient.getBucket(key);

            // 원자적 연산으로 PROCESSING 상태일 때만 삭제
            bucket.compareAndSet(PROCESSING_STATUS, null);
            log.debug("멱등성 처리 상태 제거: idempotencyKey={}", idempotencyKey);
        } catch (Exception e) {
            log.error("멱등성 처리 상태 제거 실패: idempotencyKey={}", idempotencyKey, e);
        }
    }

    @Override
    public void saveResult(String idempotencyKey, HoldSeatResult result) {
        try {
            String key = createIdempotencyKey(idempotencyKey);
            RBucket<String> bucket = redissonClient.getBucket(key);

            // 이미 tryProcess()에서 원자적 연산을 수행했으므로 set으로 저장해도 됨
            String jsonResult = objectMapper.writeValueAsString(result);
            boolean updated = bucket.compareAndSet(PROCESSING_STATUS, jsonResult);
            if (updated) {
                log.debug("멱등성 결과 저장: idempotencyKey={}, result={}", idempotencyKey, result);
            } else {
                log.warn("멱등성 결과 저장 실패 - 상태 불일치: idempotencyKey={}", idempotencyKey);
            }
        } catch (Exception e) {
            log.error("멱등성 결과 저장 실패: idempotencyKey={}", idempotencyKey, e);
            throw new RuntimeException("멱등성 결과 저장에 실패했습니다.", e);
        }
    }

    @Override
    public HoldSeatResult findByIdempotencyKey(String idempotencyKey) {
        try {
            String key = createIdempotencyKey(idempotencyKey);
            RBucket<String> bucket = redissonClient.getBucket(key);

            if (!bucket.isExists())
                return null;

            String jsonResult = bucket.get();
            HoldSeatResult result = objectMapper.readValue(jsonResult, HoldSeatResult.class);
            log.debug("멱등성 결과 조회: idempotencyKey={}, result={}", idempotencyKey, result);
            return result;
        } catch (Exception e) {
            log.error("멱등성 결과 조회 실패: idempotencyKey={}", idempotencyKey, e);
            return null;
        }
    }

    private String createIdempotencyKey(String idempotencyKey) {
        return String.format("%s:%s", IDEMPOTENCY_KEY_PREFIX, idempotencyKey);
    }
}
