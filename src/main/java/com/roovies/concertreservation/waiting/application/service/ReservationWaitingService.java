package com.roovies.concertreservation.waiting.application.service;

import com.roovies.concertreservation.shared.util.security.JwtUtils;
import com.roovies.concertreservation.waiting.application.dto.result.EnterQueueResult;
import com.roovies.concertreservation.waiting.application.port.in.WaitingUseCase;
import com.roovies.concertreservation.waiting.application.port.out.EmitterRepositoryPort;
import com.roovies.concertreservation.waiting.application.port.out.WaitingCachePort;
import com.roovies.concertreservation.waiting.domain.vo.WaitingQueueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Qualifier("reservationWaitingService")
@Transactional
@RequiredArgsConstructor
public class ReservationWaitingService implements WaitingUseCase {

    private static final int MAX_PERMITS = 100;
    private static final long SSE_TIMOUT = 600000L; // 10분

    private final JwtUtils jwtUtils;

    @Qualifier("reservationWaitingRedis")
    private final WaitingCachePort waitingCachePort;

    @Qualifier("reservationWaitingEmitterRepository")
    private final EmitterRepositoryPort emitterRepositoryPort;

    @Override
    public EnterQueueResult enterOrWaitQueue(Long userId, Long scheduleId) {
        // 세마포어 획득 시도
        boolean acquired = waitingCachePort.tryAcquireSemaphore(scheduleId, MAX_PERMITS);
        if (acquired) {
            // 세마포어 획득 시 즉시 입장 - 입장 토큰 발급
            String admittedToken = issueAdmittedToken(userId, scheduleId);
            return EnterQueueResult.builder()
                    .admitted(true)
                    .admittedToken(admittedToken)
                    .build();
        } else {
            WaitingQueueStatus status = enterQueue(userId, scheduleId);
            Integer rank = status.rank();
            Integer totalWaiting = status.totalWaiting();

            return EnterQueueResult.builder()
                    .admitted(false)
                    .rank(rank != null ? rank + 1 : null)  // ZRANK는 0부터 시작하므로 +1
                    .totalWaiting(totalWaiting)
                    .userKey(status.userKey())
                    .build();
        }
    }

    @Override
    public SseEmitter subscribeToQueue(Long userId, Long scheduleId, String userKey) {
        SseEmitter emitter = new SseEmitter(SSE_TIMOUT);
        validateUserKey(userId, userKey);

        emitterRepositoryPort.saveEmitterByUserKey(userKey, emitter);

        // 이벤트 등록
        // 타임아웃 시
        emitter.onTimeout(() -> {
            log.info("SSE 타임아웃: {}", userKey);
            waitingCachePort.removeWaitingQueue(scheduleId, userKey);
            emitterRepositoryPort.removeEmitterByUserKey(userKey);
        });

        // 정상 완료 시
        emitter.onCompletion(() -> {
            log.info("SSE 정상 완료: {}", userKey);
            emitterRepositoryPort.removeEmitterByUserKey(userKey);
        });

        // 에러 발생 시
        emitter.onError(e -> {
            log.error("SSE 에러: {}: {}", userKey, e.getMessage());
            waitingCachePort.removeWaitingQueue(scheduleId, userKey);
            emitterRepositoryPort.removeEmitterByUserKey(userKey);
        });

        // 초기 연결 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established"));
        } catch (Exception e) {
            log.error("Failed to send initial SSE message", e);
        }

        return emitter;
    }

    /**
     * 입장 토큰 발급
     */
    private String issueAdmittedToken(Long userId, Long scheduleId) {
        String userKey = generateUserKey(userId);

        // Claims 구성
        Map<String, String> claims = new HashMap<>();
        claims.put("userKey", userKey);
        claims.put("scheduleId", String.valueOf(scheduleId));
        claims.put("type", "ADMITTED");

        String admittedToken = jwtUtils.generateToken(String.valueOf(userId), claims, Duration.ofMinutes(10).toMillis());

        // 캐싱
        waitingCachePort.saveAdmittedToken(scheduleId, userKey, admittedToken);
        return admittedToken;
    }

    private String generateUserKey(Long userId) {
        return userId + ":" + UUID.randomUUID();
    }

    /**
     * 대기열 진입
     */
    private WaitingQueueStatus enterQueue(Long userId, Long scheduleId) {
        String userKey = generateUserKey(userId);
        waitingCachePort.enterQueue(scheduleId, userKey);

        return waitingCachePort.getRankAndTotalWaitingCount(scheduleId, userKey);
    }

    /**
     * userId와 userKey가 일치하는지 검증
     */
    private void validateUserKey(Long userId, String userKey) {
        String[] parts = userKey.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("유효하지 않은 userKey입니다.");
        }

        Long userIdFromKey = Long.parseLong(parts[0]);

        // userId 일치 여부 검증
        if (!userId.equals(userIdFromKey)) {
            throw new IllegalArgumentException("userId가 일치하지 않습니다.");
        }
    }
}
