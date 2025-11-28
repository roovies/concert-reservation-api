package com.roovies.concertreservation.payments.application.port.out;

/**
 * 결제 이벤트 발행 포트.
 * <p>
 * 결제 완료 시 외부 시스템(랭킹 등)에 이벤트를 발행한다.
 */
public interface PaymentEventPort {

    /**
     * 결제 완료 이벤트를 발행한다.
     *
     * @param paymentId  결제 ID
     * @param scheduleId 스케줄 ID
     * @param userId     사용자 ID
     */
    void publishPaymentCompleted(Long paymentId, Long scheduleId, Long userId);
}