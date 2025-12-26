package com.roovies.concertreservation.ranking.infra.adapter.in.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roovies.concertreservation.ranking.application.port.in.UpdateRankingUseCase;
import com.roovies.concertreservation.ranking.domain.event.PaymentCompletedEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 결제 완료 이벤트 리스너.
 * <p>
 * Redis Pub/Sub을 통해 결제 완료 이벤트를 구독하고,
 * 실시간 랭킹을 업데이트한다.
 */
@Slf4j
@Component("rankingPaymentEventListener")
public class PaymentEventListener {

    private static final String PAYMENT_COMPLETED_CHANNEL = "channel:payment:completed";

    private final UpdateRankingUseCase updateRankingUseCase;
    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;

    public PaymentEventListener(
            @Qualifier("realtimeRankingService") UpdateRankingUseCase updateRankingUseCase,
            RedissonClient redisson,
            ObjectMapper objectMapper
    ) {
        this.updateRankingUseCase = updateRankingUseCase;
        this.redisson = redisson;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        RTopic paymentTopic = redisson.getTopic(PAYMENT_COMPLETED_CHANNEL);
        paymentTopic.addListener(String.class, (channel, message) -> {
            handlePaymentCompletedEvent(message);
        });

        log.info("[PaymentEventListener] 결제 완료 이벤트 리스너 등록 완료");
    }

    /**
     * 결제 완료 이벤트를 처리한다.
     * <p>
     * 실시간 랭킹을 업데이트한다.
     */
    private void handlePaymentCompletedEvent(String message) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
            log.info("[PaymentEventListener] 결제 완료 이벤트 수신 - paymentId: {}, scheduleId: {}, userId: {}",
                    event.paymentId(), event.scheduleId(), event.userId());

            // 실시간 랭킹 업데이트
            updateRankingUseCase.updateRealtimeRanking(event.scheduleId());

            log.info("[PaymentEventListener] 실시간 랭킹 업데이트 완료 - scheduleId: {}", event.scheduleId());

        } catch (JsonProcessingException e) {
            log.error("[PaymentEventListener] 결제 완료 이벤트 처리 실패", e);
        }
    }
}