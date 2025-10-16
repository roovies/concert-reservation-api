package com.roovies.concertreservation.waiting.infra.adapter.in.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roovies.concertreservation.waiting.application.port.in.WaitingUseCase;
import com.roovies.concertreservation.waiting.domain.event.WaitingQueueStatusUpdateEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationWaitingEventListener {

    private static final String CHANNEL_STATUS = "channel:status";

    private final WaitingUseCase waitingUseCase;

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        RTopic statusTopic = redisson.getTopic(CHANNEL_STATUS);
        statusTopic.addListener(String.class, (channel, message) -> {
            handleNotifyWaitingQueueStatusEvent(message);
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
}
