package com.roovies.concertreservation.payments.infra.adapter.in.listener;

import com.roovies.concertreservation.payments.application.dto.command.RefundPaymentCommand;
import com.roovies.concertreservation.payments.application.dto.result.RefundPaymentResult;
import com.roovies.concertreservation.payments.application.port.in.RefundPaymentUseCase;
import com.roovies.concertreservation.shared.domain.event.CompensatePaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 결제 보상 이벤트 Kafka 리스너.
 * <p>
 * SAGA Orchestration 패턴의 보상 트랜잭션으로,
 * 포인트 적립 실패 등의 이유로 결제를 롤백(포인트 환불)하는 이벤트를 수신한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompensatePaymentKafkaListener {

    @Qualifier("refundPaymentService")
    private final RefundPaymentUseCase refundPaymentUseCase;

    /**
     * 결제 보상 이벤트를 수신하여 환불을 처리한다.
     * <p>
     * Consumer Group: payment-compensation-service
     * Topic: compensate-payment
     * <p>
     * 수동 커밋(MANUAL ACK) 모드를 사용하여 이벤트 처리 성공 시에만 오프셋을 커밋한다.
     *
     * @param event 결제 보상 이벤트
     * @param ack   Kafka 수동 커밋 핸들러
     */
    @KafkaListener(
            topics = "compensate-payment",
            groupId = "payment-compensation-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCompensatePayment(
            CompensatePaymentEvent event,
            Acknowledgment ack
    ) {
        Long paymentId = event.paymentId();
        Long userId = event.userId();
        Long refundAmount = event.refundAmount();
        String reason = event.reason();

        log.info("========================================");
        log.info("[CompensatePaymentKafkaListener] 결제 보상 이벤트 수신");
        log.info("  - Payment ID: {}", paymentId);
        log.info("  - User ID: {}", userId);
        log.info("  - Refund Amount: {}원", refundAmount);
        log.info("  - Reason: {}", reason);
        log.info("========================================");

        try {
            // 결제 환불 (포인트 환불)
            RefundPaymentCommand command = RefundPaymentCommand.of(
                    paymentId,
                    userId,
                    refundAmount,
                    reason
            );
            RefundPaymentResult result = refundPaymentUseCase.refund(command);

            // 처리 성공 시 수동으로 오프셋 커밋
            ack.acknowledge();

            log.info("[CompensatePaymentKafkaListener] 결제 보상 처리 성공 - paymentId: {}, userId: {}, refundAmount: {}원, totalPointAmount: {}원",
                    paymentId, userId, refundAmount, result.totalPointAmount());

        } catch (Exception e) {
            log.error("[CompensatePaymentKafkaListener] 결제 보상 처리 실패 - paymentId: {}, userId: {}",
                    paymentId, userId, e);

            // 예외 발생 시 ack를 호출하지 않아 재처리됨
            // ErrorHandler에서 재시도 로직 수행
            throw e;
        }
    }
}