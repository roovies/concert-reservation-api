package com.roovies.concertreservation.shared.domain.event;

import com.roovies.concertreservation.payments.domain.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 완료 Kafka 이벤트.
 * <p>
 * 결제가 성공적으로 완료되고 예약이 확정되었을 때 발행되는 이벤트로,
 * Kafka를 통해 alarm 등의 다른 바운디드 컨텍스트로 전파된다.
 *
 * @param paymentId 결제 ID
 * @param scheduleId 스케줄 ID
 * @param seatIds 예약한 좌석 ID 목록
 * @param userId 사용자 ID
 * @param originalAmount 원 금액 (할인 전)
 * @param discountAmount 할인 금액
 * @param paidAmount 실제 결제 금액
 * @param status 결제 상태
 * @param completedAt 결제 완료 시각
 * @param publishedAt 이벤트 발행 시각
 */
public record ReservationCompletedKafkaEvent(
        Long paymentId,
        Long scheduleId,
        List<Long> seatIds,
        Long userId,
        Long originalAmount,
        Long discountAmount,
        Long paidAmount,
        PaymentStatus status,
        LocalDateTime completedAt,
        LocalDateTime publishedAt
) {
    public static ReservationCompletedKafkaEvent of(
            Long paymentId,
            Long scheduleId,
            List<Long> seatIds,
            Long userId,
            Long originalAmount,
            Long discountAmount,
            Long paidAmount,
            PaymentStatus status,
            LocalDateTime completedAt
    ) {
        return new ReservationCompletedKafkaEvent(
                paymentId,
                scheduleId,
                seatIds,
                userId,
                originalAmount,
                discountAmount,
                paidAmount,
                status,
                completedAt,
                LocalDateTime.now()
        );
    }
}