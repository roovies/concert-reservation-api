package com.roovies.concertreservation.waiting.application.service;

import com.roovies.concertreservation.shared.util.security.JwtUtils;
import com.roovies.concertreservation.waiting.application.dto.result.EnterQueueResult;
import com.roovies.concertreservation.waiting.application.port.in.WaitingUseCase;
import com.roovies.concertreservation.waiting.application.port.out.WaitingCachePort;
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
    public EnterQueueResult enterQueue(Long userId, Long scheduleId) {
        // 세마포어 획득 시도
        boolean acquired = waitingCachePort.tryAcquireSemaphore(scheduleId, 100);
        if (acquired) {
            // 세마포어 획득 시 즉시 입장 - 입장 토큰
            String admittedToken = issueAdmittedToken(userId, scheduleId);
            return EnterQueueResult.builder()
                    .admitted(true)
                    .admittedToken(admittedToken)
                    .build();
        } else {

        }
        return null;
    }

    private String issueAdmittedToken(Long userId, Long scheduleId) {
        String userKey = generateUserKey(userId);

        // Claims 구성
        Map<String, String> claims = new HashMap<>();
        claims.put("userKey", userKey);
        claims.put("scheduleId", String.valueOf(scheduleId));
        claims.put("type", "ADMITTED");

        String admittedToken = jwtUtils.generateToken(String.valueOf(userId), claims, Duration.ofMinutes(10).toMillis());

        // 캐싱
        waitingCachePort.saveToken(scheduleId, userKey, admittedToken);
        return admittedToken;
    }


    private String generateUserKey(Long userId) {
        return userId + ":" + UUID.randomUUID();
    }
}
