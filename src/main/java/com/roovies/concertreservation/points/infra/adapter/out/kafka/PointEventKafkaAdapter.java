package com.roovies.concertreservation.points.infra.adapter.out.kafka;

import com.roovies.concertreservation.points.application.port.out.PointKafkaEventPort;
import com.roovies.concertreservation.shared.domain.event.CompensatePaymentEvent;
import com.roovies.concertreservation.shared.domain.event.PointRewardCompletedEvent;
import com.roovies.concertreservation.shared.domain.event.PointRewardFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 포인트 이벤트 Kafka Producer 어댑터.
 * <p>
 * Kafka를 통해 포인트 적립 결과 및 보상 이벤트를 발행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointEventKafkaAdapter implements PointKafkaEventPort {

    private static final String TOPIC_POINT_REWARD_COMPLETED = "point-reward-completed";
    private static final String TOPIC_POINT_REWARD_FAILED = "point-reward-failed";
    private static final String TOPIC_COMPENSATE_PAYMENT = "compensate-payment";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishPointRewardCompleted(PointRewardCompletedEvent event) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(TOPIC_POINT_REWARD_COMPLETED, event.paymentId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[PointEventKafkaAdapter] 포인트 적립 완료 이벤트 발행 성공 - topic: {}, partition: {}, offset: {}, paymentId: {}, userId: {}",
                            TOPIC_POINT_REWARD_COMPLETED,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            event.paymentId(),
                            event.userId());
                } else {
                    log.error("[PointEventKafkaAdapter] 포인트 적립 완료 이벤트 발행 실패 - paymentId: {}, userId: {}, error: {}",
                            event.paymentId(),
                            event.userId(),
                            ex.getMessage(),
                            ex);
                }
            });

        } catch (Exception e) {
            log.error("[PointEventKafkaAdapter] 포인트 적립 완료 이벤트 발행 중 예외 발생 - paymentId: {}, userId: {}",
                    event.paymentId(),
                    event.userId(),
                    e);
        }
    }

    @Override
    public void publishPointRewardFailed(PointRewardFailedEvent event) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(TOPIC_POINT_REWARD_FAILED, event.paymentId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[PointEventKafkaAdapter] 포인트 적립 실패 이벤트 발행 성공 - topic: {}, partition: {}, offset: {}, paymentId: {}, userId: {}",
                            TOPIC_POINT_REWARD_FAILED,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            event.paymentId(),
                            event.userId());
                } else {
                    log.error("[PointEventKafkaAdapter] 포인트 적립 실패 이벤트 발행 실패 - paymentId: {}, userId: {}, error: {}",
                            event.paymentId(),
                            event.userId(),
                            ex.getMessage(),
                            ex);
                }
            });

        } catch (Exception e) {
            log.error("[PointEventKafkaAdapter] 포인트 적립 실패 이벤트 발행 중 예외 발생 - paymentId: {}, userId: {}",
                    event.paymentId(),
                    event.userId(),
                    e);
        }
    }

    @Override
    public void publishCompensatePayment(CompensatePaymentEvent event) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(TOPIC_COMPENSATE_PAYMENT, event.paymentId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[PointEventKafkaAdapter] 결제 보상 이벤트 발행 성공 - topic: {}, partition: {}, offset: {}, paymentId: {}, userId: {}, reason: {}",
                            TOPIC_COMPENSATE_PAYMENT,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            event.paymentId(),
                            event.userId(),
                            event.reason());
                } else {
                    log.error("[PointEventKafkaAdapter] 결제 보상 이벤트 발행 실패 - paymentId: {}, userId: {}, error: {}",
                            event.paymentId(),
                            event.userId(),
                            ex.getMessage(),
                            ex);
                }
            });

        } catch (Exception e) {
            log.error("[PointEventKafkaAdapter] 결제 보상 이벤트 발행 중 예외 발생 - paymentId: {}, userId: {}",
                    event.paymentId(),
                    event.userId(),
                    e);
        }
    }
}