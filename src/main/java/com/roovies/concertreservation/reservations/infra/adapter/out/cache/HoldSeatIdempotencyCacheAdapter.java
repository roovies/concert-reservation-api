package com.roovies.concertreservation.reservations.infra.adapter.out.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.out.HoldSeatIdempotencyCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class HoldSeatIdempotencyCacheAdapter implements HoldSeatIdempotencyCachePort {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:hold-seat";
    private static final long IDEMPOTENCY_TTL_SECONDS = 3600L; // 1시간

    @Override
    public void saveResult(String idempotencyKey, HoldSeatResult result) {
        try {
            String key = createIdempotencyKey(idempotencyKey);
            RBucket<String> bucket = redissonClient.getBucket(key);

            String jsonResult = objectMapper.writeValueAsString(result);
            // setIfAbsent 사용으로 동시성 문제 해결
            // - setIfAbsent는 키가 존재하지 않을 때만 값을 저장하는 원자적 연산(setnx)
            // - 따라서 동시에 여러 요청이 들어와도 첫 요청만 성공하고 나머지는 false를 반환함
            // - 왜 분산락을 적용하지 않았는가?
            //   -> 멱등성은 실제 처리 로직에서 첫 요청만 성공되면 되고, 나머지는 첫 요청의 결과를 그대로 재사용해야 함 (동일 요청 동일 결과)
            //   -> 멱등성이 실제 발생하는 케이스는 동일 사용작 동시에 보내는 요청 수가 3~5회 정도 수준이 평균이므로, 이미 저장된 결과를 재사용하면 됨
            //   -> 따라서 분산락 같은 거로 처리 순서를 제어할 필요 없이, 동일 요청 중 아무 요청만 먼저 원자적으로 성공시킨 후 나머지를 캐싱해서 쓰면 됨
            boolean saved = bucket.setIfAbsent(jsonResult, Duration.ofSeconds(IDEMPOTENCY_TTL_SECONDS));

            if (saved) {
                log.debug("멱등성 결과 저장 성공: idempotencyKey={}, result={}", idempotencyKey, result);
            } else {
                log.debug("멱등성 결과 이미 존재: idempotencyKey={}", idempotencyKey);
            }

            log.debug("멱등성 결과 저장: idempotencyKey={}, result={}", idempotencyKey, result);
        } catch (Exception e) {
            log.error("멱등성 결과 저장 실패: idempotencyKey={}", idempotencyKey, e);
            throw new RuntimeException("멱등성 결과 저장에 실패했습니다.", e);
        }
    }

    @Override
    public Optional<HoldSeatResult> findByIdempotencyKey(String idempotencyKey) {
        try {
            String key = createIdempotencyKey(idempotencyKey);
            RBucket<String> bucket = redissonClient.getBucket(key);

            if (!bucket.isExists())
                return Optional.empty();

            String jsonResult = bucket.get();
            HoldSeatResult result = objectMapper.readValue(jsonResult, HoldSeatResult.class);

            log.debug("멱등성 결과 조회: idempotencyKey={}, result={}", idempotencyKey, result);
            return Optional.of(result);
        } catch (Exception e) {
            log.error("멱등성 결과 조회 실패: idempotencyKey={}", idempotencyKey, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String idempotencyKey) {
        try {
            String key = createIdempotencyKey(idempotencyKey);
            RBucket<String> bucket = redissonClient.getBucket(key);
            return bucket.isExists();
        } catch (Exception e) {
            log.error("멱등성 키 존재 확인 실패: idempotencyKey={}", idempotencyKey, e);
            return false;
        }
    }

    private String createIdempotencyKey(String idempotencyKey) {
        return String.format("%s:%s", IDEMPOTENCY_KEY_PREFIX, idempotencyKey);
    }
}
