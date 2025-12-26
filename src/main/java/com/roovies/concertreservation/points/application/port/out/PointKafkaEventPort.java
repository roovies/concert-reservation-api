package com.roovies.concertreservation.points.application.port.out;

import com.roovies.concertreservation.shared.domain.event.CompensatePaymentEvent;
import com.roovies.concertreservation.shared.domain.event.PointRewardCompletedEvent;
import com.roovies.concertreservation.shared.domain.event.PointRewardFailedEvent;

/**
 * 포인트 모듈의 Kafka 이벤트 발행 포트.
 * <p>
 * SAGA Orchestration 패턴에서 포인트 적립 결과를 다른 바운디드 컨텍스트로 전파한다.
 */
public interface PointKafkaEventPort {

    /**
     * 포인트 적립 완료 이벤트를 발행한다.
     *
     * @param event 포인트 적립 완료 이벤트
     */
    void publishPointRewardCompleted(PointRewardCompletedEvent event);

    /**
     * 포인트 적립 실패 이벤트를 발행한다.
     *
     * @param event 포인트 적립 실패 이벤트
     */
    void publishPointRewardFailed(PointRewardFailedEvent event);

    /**
     * 결제 보상 이벤트를 발행한다.
     *
     * @param event 결제 보상 이벤트
     */
    void publishCompensatePayment(CompensatePaymentEvent event);
}