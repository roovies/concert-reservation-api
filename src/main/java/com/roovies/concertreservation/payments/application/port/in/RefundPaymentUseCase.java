package com.roovies.concertreservation.payments.application.port.in;

import com.roovies.concertreservation.payments.application.dto.command.RefundPaymentCommand;
import com.roovies.concertreservation.payments.application.dto.result.RefundPaymentResult;

/**
 * 결제 환불 유스케이스.
 * <p>
 * SAGA Orchestration 패턴의 보상 트랜잭션으로,
 * 하위 단계 실패 시 결제를 롤백(포인트 환불)하는 기능을 제공한다.
 */
public interface RefundPaymentUseCase {
    /**
     * 결제를 환불한다 (포인트 환불).
     *
     * @param command 결제 환불 요청 커맨드
     * @return 결제 환불 결과
     */
    RefundPaymentResult refund(RefundPaymentCommand command);
}