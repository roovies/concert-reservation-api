package com.roovies.concertreservation.payments.application.port.out;

import com.roovies.concertreservation.shared.domain.event.ReservationCompletedKafkaEvent;

/**
 * 결제 Kafka 이벤트 발행 포트.
 * <p>
 * 결제 완료 시 Kafka를 통해 외부 시스템(알림 등)에 이벤트를 발행한다.
 */
public interface PaymentKafkaEventPort {

    /**
     * 예약 완료 이벤트를 Kafka로 발행한다.
     *
     * @param event 예약 완료 이벤트
     */
    void publishReservationCompleted(ReservationCompletedKafkaEvent event);
}