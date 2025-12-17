package com.roovies.concertreservation.alarm.infra.adapter.in.listener;

import com.roovies.concertreservation.shared.domain.event.ReservationCompletedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 예약 완료 이벤트 Kafka 리스너.
 * <p>
 * 결제가 완료되고 예약이 확정된 이벤트를 수신하여 알림을 처리한다.
 * 현재는 이벤트 수신 로깅만 수행하며, 향후 알림 발송 로직이 추가될 예정이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCompletedKafkaListener {

    /**
     * 예약 완료 이벤트를 수신하여 처리한다.
     * <p>
     * Consumer Group: alarm-service
     * Topic: reservation-completed
     * <p>
     * 수동 커밋(MANUAL ACK) 모드를 사용하여 이벤트 처리 성공 시에만 오프셋을 커밋한다.
     *
     * @param event 예약 완료 이벤트
     * @param ack   Kafka 수동 커밋 핸들러
     */
    @KafkaListener(
            topics = "reservation-completed",
            groupId = "alarm-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleReservationCompleted(
            ReservationCompletedKafkaEvent event,
            Acknowledgment ack
    ) {
        try {
            log.info("========================================");
            log.info("[ReservationCompletedKafkaListener] 예약 완료 이벤트 수신");
            log.info("  - Payment ID: {}", event.paymentId());
            log.info("  - User ID: {}", event.userId());
            log.info("  - Schedule ID: {}", event.scheduleId());
            log.info("  - Seat IDs: {}", event.seatIds());
            log.info("  - Original Amount: {}원", event.originalAmount());
            log.info("  - Discount Amount: {}원", event.discountAmount());
            log.info("  - Paid Amount: {}원", event.paidAmount());
            log.info("  - Status: {}", event.status());
            log.info("  - Completed At: {}", event.completedAt());
            log.info("  - Published At: {}", event.publishedAt());
            log.info("========================================");

            // TODO: 실제 알림 발송 로직 구현
            // 예: SMS, 이메일, 푸시 알림 등

            // 처리 성공 시 수동으로 오프셋 커밋
            ack.acknowledge();

            log.info("[ReservationCompletedKafkaListener] 예약 완료 이벤트 처리 성공 - paymentId: {}, userId: {}",
                    event.paymentId(), event.userId());

        } catch (Exception e) {
            log.error("[ReservationCompletedKafkaListener] 예약 완료 이벤트 처리 실패 - paymentId: {}, userId: {}",
                    event.paymentId(), event.userId(), e);
            // 예외 발생 시 ack를 호출하지 않아 재처리됨
            // ErrorHandler에서 재시도 로직 수행
            throw e;
        }
    }
}