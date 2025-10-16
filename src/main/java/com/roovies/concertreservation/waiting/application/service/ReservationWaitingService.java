package com.roovies.concertreservation.waiting.application.service;

import com.roovies.concertreservation.shared.util.security.JwtUtils;
import com.roovies.concertreservation.waiting.application.dto.result.EnterQueueResult;
import com.roovies.concertreservation.waiting.application.port.in.WaitingUseCase;
import com.roovies.concertreservation.waiting.application.port.out.WaitingCachePort;
import com.roovies.concertreservation.waiting.domain.vo.WaitingQueueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final JwtUtils jwtUtils;
    private final WaitingCachePort waitingCachePort;

    @Override
    public EnterQueueResult enterOrWaitQueue(Long userId, Long scheduleId) {
        // 세마포어 획득 시도
        boolean acquired = waitingCachePort.tryAcquireSemaphore(scheduleId, 100);
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
}
