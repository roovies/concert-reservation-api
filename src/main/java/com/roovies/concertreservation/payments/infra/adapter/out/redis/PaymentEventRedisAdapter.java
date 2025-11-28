package com.roovies.concertreservation.payments.infra.adapter.out.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roovies.concertreservation.payments.application.port.out.PaymentEventPort;
import com.roovies.concertreservation.ranking.domain.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * 결제 이벤트 Redis Pub/Sub 어댑터.
 * <p>
 * Redis Pub/Sub을 통해 결제 완료 이벤트를 발행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventRedisAdapter implements PaymentEventPort {

    private static final String PAYMENT_COMPLETED_CHANNEL = "channel:payment:completed";

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;

    /**
     * 결제 완료 이벤트를 Redis Pub/Sub으로 발행한다.
     *
     * @param paymentId  결제 ID
     * @param scheduleId 스케줄 ID
     * @param userId     사용자 ID
     */
    @Override
    public void publishPaymentCompleted(Long paymentId, Long scheduleId, Long userId) {
        try {
            PaymentCompletedEvent event = PaymentCompletedEvent.of(paymentId, scheduleId, userId);
            String message = objectMapper.writeValueAsString(event);

            RTopic topic = redisson.getTopic(PAYMENT_COMPLETED_CHANNEL);
            topic.publish(message);

            log.info("[PaymentEventRedisAdapter] 결제 완료 이벤트 발행 - paymentId: {}, scheduleId: {}, userId: {}",
                    paymentId, scheduleId, userId);

        } catch (JsonProcessingException e) {
            log.error("[PaymentEventRedisAdapter] 결제 완료 이벤트 발행 실패", e);
        }
    }
}