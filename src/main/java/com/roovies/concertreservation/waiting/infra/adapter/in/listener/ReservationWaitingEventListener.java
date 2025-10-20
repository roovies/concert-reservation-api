package com.roovies.concertreservation.waiting.infra.adapter.in.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roovies.concertreservation.waiting.application.port.in.WaitingUseCase;
import com.roovies.concertreservation.waiting.domain.event.WaitingQueueStatusUpdateEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationWaitingEventListener {

    private static final String CHANNEL_STATUS = "channel:status";
    private static final String CHANNEL_ADMIT = "channel:admit";
    public static final String KEY_EXPIRE_CHANNEL = "__keyevent@0__:expired";

    private static final String SEMAPHORE_PREFIX = "semaphore:reservation:";        // ReservationWaitingRedisAdapter.SEMAPHORE_PREFIX
    private static final String ADMITTED_TOKEN_PREFIX = "admitted:reservation:";    // ReservationWaitingRedisAdapter.ADMITTED_TOKEN_PREFIX
    /**
     * 키 만료 이벤트는 해당 DB에서만 발생한다.
     * Redis는 클라이언트 단위로 현재 선택된 DB를 기억한다.
     * 기본적으로 Redis는 별도 설정 없으면 0번을 사용하지만,
     * application.yml 같은 곳에서 redis의 database 번호를 변경할 경우에는 변경한 설정값으로 맞춰줘야 한다.
     */

    private final WaitingUseCase waitingUseCase;

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        RTopic statusTopic = redisson.getTopic(CHANNEL_STATUS);
        statusTopic.addListener(String.class, (channel, message) -> {
            handleNotifyWaitingQueueStatusEvent(message);
        });

        RTopic admissionTopic = redisson.getTopic(CHANNEL_ADMIT);
        admissionTopic.addListener(String.class, (channel, message) -> {
            handleNotifyAdmittedUsersEvent(message);
        });

        // 입장 토큰 만료 이벤트 리스너
        RTopic expirationTopic = redisson.getTopic(KEY_EXPIRE_CHANNEL);
        /**
         * Redis는 기본적으로 키 만료 이벤트를 발행하지 않는다. 직접 설정을 활성화해야 함.
         * # Redis CLI 또는 redis.conf에서 설정
         *   CONFIG SET notify-keyspace-events Ex
         */
        expirationTopic.addListener(String.class, (channel, expiredKey) -> {
            /* 만료된 키별로 로직 수행 */

            // 입장 토큰 만료
            if (expiredKey.startsWith(ADMITTED_TOKEN_PREFIX)) {
                handleAdmitTokenExpiration(expiredKey);
            }
        });
    }

    /**
     *  실시간 대기 순번 갱신 처리
     */
    private void handleNotifyWaitingQueueStatusEvent(String message) {
        try {
            WaitingQueueStatusUpdateEvent event = objectMapper.readValue(message, WaitingQueueStatusUpdateEvent.class);
            log.info("대기자 실시간 순번 갱신 이벤트 수신: scheduleId = {}", event.scheduleId());
            waitingUseCase.notifyWaitingQueueStatus(event.scheduleId());
        } catch (JsonProcessingException e) {
            log.error("대기자 실시간 순번 갱신 이벤트 처리 실패", e);
        }
    }

    /**
     * 입장 처리 완료 이벤트 처리
     */
    private void handleNotifyAdmittedUsersEvent(String message) {
        try {
            Map<String, String> admitTokenByUserKey = objectMapper.readValue(message, new TypeReference<>() {});
            log.info("입장 처리 완료 이벤트 수신: userKeys = {}", admitTokenByUserKey.keySet());
            waitingUseCase.notifyAdmittedUsers(admitTokenByUserKey);
        } catch (JsonProcessingException e) {
            log.error("입장 처리 완료 이벤트 처리 실패", e);
        }
    }

    /**
     * 입장 토큰 만료 시 해당 스케줄ID의 Permit 반환 처리
     */
    private void handleAdmitTokenExpiration(String expiredKey) {
        String keyWithoutPrefix = expiredKey.replace(ADMITTED_TOKEN_PREFIX, "");

        // scheduleId, userKey(userId:uuid) 분리 (첫 번째 콜론 전까지)
        String[] parts = keyWithoutPrefix.split(":", 2);
        if (parts.length < 2) {
            log.warn("입장 토큰 키 형식이 올바르지 않음: {}", expiredKey);
            return;
        }

        String scheduleId = parts[0];
        String userKey = parts[1]; // {userId}:{uuid}

        // Redis Semaphore Permit 반환
        String semaphoreKey = SEMAPHORE_PREFIX + scheduleId;
        RSemaphore semaphore = redisson.getSemaphore(semaphoreKey);
        semaphore.release();

        log.info("입장 토큰 만료로 Permit 회수: scheduleId = {}, userKey = {}", scheduleId, userKey);
    }
}
