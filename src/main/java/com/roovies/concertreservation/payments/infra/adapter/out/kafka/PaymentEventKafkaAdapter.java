package com.roovies.concertreservation.payments.infra.adapter.out.kafka;

import com.roovies.concertreservation.payments.application.port.out.PaymentKafkaEventPort;
import com.roovies.concertreservation.shared.domain.event.ReservationCompletedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 결제 이벤트 Kafka Producer 어댑터.
 * <p>
 * Kafka를 통해 결제 완료 이벤트를 발행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventKafkaAdapter implements PaymentKafkaEventPort {

    private static final String TOPIC = "reservation-completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 예약 완료 이벤트를 Kafka로 발행한다.
     *
     * @param event 예약 완료 이벤트
     */
    public void publishReservationCompleted(ReservationCompletedKafkaEvent event) {
        try {
            // Kafka로 비동기 전송
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(TOPIC, event.paymentId().toString(), event);

            // 전송 결과 처리
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[PaymentEventKafkaAdapter] 예약 완료 이벤트 발행 성공 - topic: {}, partition: {}, offset: {}, paymentId: {}, userId: {}",
                            TOPIC,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            event.paymentId(),
                            event.userId());
                } else {
                    log.error("[PaymentEventKafkaAdapter] 예약 완료 이벤트 발행 실패 - paymentId: {}, userId: {}, error: {}",
                            event.paymentId(),
                            event.userId(),
                            ex.getMessage(),
                            ex);
                }
            });

        } catch (Exception e) {
            log.error("[PaymentEventKafkaAdapter] 예약 완료 이벤트 발행 중 예외 발생 - paymentId: {}, userId: {}",
                    event.paymentId(),
                    event.userId(),
                    e);
        }
    }
}