package com.roovies.concertreservation.waiting.infra.adapter.out.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roovies.concertreservation.waiting.application.port.out.WaitingEventPublisher;
import com.roovies.concertreservation.waiting.domain.event.WaitingQueueStatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Qualifier("reservationRedisEventPublisher")
public class ReservationRedisEventPublisher implements WaitingEventPublisher {

    private static final String CHANNEL_STATUS = "channel:status";
    private static final String CHANNEL_ADMIT = "channel:admit";

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;

    /**
     * 실시간 순번 알림 이벤트를 채널을 구독(리스닝)하고 있는 모든 인스턴스에 브로드캐스트
     */
    public void notifyWaitingQueueStatusEvent(Long scheduleId) {
        try {
            // 발행할 메시지
            String message = objectMapper.writeValueAsString(new WaitingQueueStatusUpdateEvent(scheduleId));
            // 발행할 채널
            RTopic topic = redisson.getTopic(CHANNEL_STATUS);
            // 해당 채널에 메시지 발행
            topic.publish(message);
            log.info("대기자 순번 갱신 이벤트 발행 완료: scheduleId = {}", scheduleId);
        } catch (JsonProcessingException e) {
            log.error("대기자 순번 갱신 이벤트 발행 실패: {}", e.getMessage());
        }
    }

    /**
     * 현재 인스턴스에서 처리 불가한 입장 처리가 완료된 userKey와 입장토큰을 모든 인스턴스에 브로드캐스트
     */
    @Override
    public void notifyAdmittedUsersEvent(Map<String, String> userKeyToAdmittedToken) {
        try {
            // 발행할 메시지
            String message = objectMapper.writeValueAsString(userKeyToAdmittedToken);
            // 발행할 채널
            RTopic topic = redisson.getTopic(CHANNEL_ADMIT);
            // 해당 채널에 메시지 발행
            topic.publish(message);
            log.info("입장 처리 완료 이벤트 발행 완료: userKeys = {}", userKeyToAdmittedToken.keySet());
        } catch (JsonProcessingException e) {
            log.error("입장 처리 완료 이벤트 발행 실패", e);
        }
    }
}
