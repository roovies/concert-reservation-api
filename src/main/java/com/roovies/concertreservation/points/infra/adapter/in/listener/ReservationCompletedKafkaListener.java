package com.roovies.concertreservation.points.infra.adapter.in.listener;

import com.roovies.concertreservation.points.application.dto.command.RewardPointCommand;
import com.roovies.concertreservation.points.application.dto.result.RewardPointResult;
import com.roovies.concertreservation.points.application.port.in.RewardPointUseCase;
import com.roovies.concertreservation.points.application.port.out.PointKafkaEventPort;
import com.roovies.concertreservation.shared.domain.event.CompensatePaymentEvent;
import com.roovies.concertreservation.shared.domain.event.PointRewardCompletedEvent;
import com.roovies.concertreservation.shared.domain.event.PointRewardFailedEvent;
import com.roovies.concertreservation.shared.domain.event.ReservationCompletedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 예약 완료 이벤트 Kafka 리스너 (Points 모듈).
 * <p>
 * 결제가 완료되고 예약이 확정된 이벤트를 수신하여 리워드 포인트를 적립한다.
 * SAGA Orchestration 패턴의 일부로, 포인트 적립 성공/실패 이벤트를 발행한다.
 */
@Slf4j
@Component("pointsReservationCompletedKafkaListener")
@RequiredArgsConstructor
public class ReservationCompletedKafkaListener {

    @Qualifier("rewardPointService")
    private final RewardPointUseCase rewardPointUseCase;

    private final PointKafkaEventPort pointKafkaEventPort;

    /**
     * 예약 완료 이벤트를 수신하여 포인트 적립을 처리한다.
     * <p>
     * Consumer Group: points-service
     * Topic: reservation-completed
     * <p>
     * 수동 커밋(MANUAL ACK) 모드를 사용하여 이벤트 처리 성공 시에만 오프셋을 커밋한다.
     * <p>
     * 포인트 적립 규칙:
     * - 원 금액(originalAmount)의 10%를 적립
     * - 100원 단위로 내림 (예: 1,250원 → 1,200원)
     *
     * @param event 예약 완료 이벤트
     * @param ack   Kafka 수동 커밋 핸들러
     */
    @KafkaListener(
            topics = "reservation-completed",
            groupId = "points-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleReservationCompleted(
            ReservationCompletedKafkaEvent event,
            Acknowledgment ack
    ) {
        Long paymentId = event.paymentId();
        Long userId = event.userId();
        Long originalAmount = event.originalAmount();

        log.info("========================================");
        log.info("[Points-ReservationCompletedKafkaListener] 예약 완료 이벤트 수신");
        log.info("  - Payment ID: {}", paymentId);
        log.info("  - User ID: {}", userId);
        log.info("  - Original Amount: {}원", originalAmount);
        log.info("========================================");

        try {
            // 원 금액의 10%를 계산하고 100원 단위로 내림
            long rewardAmount = calculateRewardAmount(originalAmount);

            // 포인트 적립
            RewardPointCommand command = RewardPointCommand.of(userId, rewardAmount, paymentId);
            RewardPointResult result = rewardPointUseCase.reward(command);

            // 포인트 적립 성공 이벤트 발행
            PointRewardCompletedEvent completedEvent = PointRewardCompletedEvent.of(
                    paymentId,
                    userId,
                    result.rewardAmount(),
                    result.totalAmount(),
                    result.updatedAt()
            );
            pointKafkaEventPort.publishPointRewardCompleted(completedEvent);

            // 처리 성공 시 수동으로 오프셋 커밋
            ack.acknowledge();

            log.info("[Points-ReservationCompletedKafkaListener] 포인트 적립 성공 - paymentId: {}, userId: {}, rewardAmount: {}원, totalAmount: {}원",
                    paymentId, userId, rewardAmount, result.totalAmount());

        } catch (Exception e) {
            log.error("[Points-ReservationCompletedKafkaListener] 포인트 적립 실패 - paymentId: {}, userId: {}",
                    paymentId, userId, e);

            try {
                // 포인트 적립 실패 이벤트 발행
                long attemptedRewardAmount = calculateRewardAmount(originalAmount);
                PointRewardFailedEvent failedEvent = PointRewardFailedEvent.of(
                        paymentId,
                        userId,
                        attemptedRewardAmount,
                        e.getMessage()
                );
                pointKafkaEventPort.publishPointRewardFailed(failedEvent);

                // 보상 트랜잭션 트리거: 결제 취소 이벤트 발행
                CompensatePaymentEvent compensateEvent = CompensatePaymentEvent.of(
                        paymentId,
                        userId,
                        event.paidAmount(),
                        "포인트 적립 실패로 인한 결제 보상: " + e.getMessage()
                );
                pointKafkaEventPort.publishCompensatePayment(compensateEvent);

                log.info("[Points-ReservationCompletedKafkaListener] 보상 트랜잭션 이벤트 발행 완료 - paymentId: {}, userId: {}",
                        paymentId, userId);

            } catch (Exception publishException) {
                log.error("[Points-ReservationCompletedKafkaListener] 보상 이벤트 발행 실패 - paymentId: {}, userId: {}",
                        paymentId, userId, publishException);
            }

            // 예외 발생 시 ack를 호출하지 않아 재처리됨
            // ErrorHandler에서 재시도 로직 수행
            throw e;
        }
    }

    /**
     * 원 금액의 10%를 계산하고 100원 단위로 내림한다.
     *
     * @param originalAmount 원 금액
     * @return 적립할 포인트 금액 (100원 단위)
     */
    private long calculateRewardAmount(Long originalAmount) {
        // 10% 계산
        long tenPercent = (long) (originalAmount * 0.1);

        // 100원 단위로 내림
        return (tenPercent / 100) * 100;
    }
}