package com.roovies.concertreservation.payments.application.service;

import com.roovies.concertreservation.payments.application.dto.command.RefundPaymentCommand;
import com.roovies.concertreservation.payments.application.dto.result.RefundPaymentResult;
import com.roovies.concertreservation.payments.application.port.in.RefundPaymentUseCase;
import com.roovies.concertreservation.payments.application.port.out.PaymentPointGatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 결제 환불 서비스 구현체.
 * <p>
 * SAGA Orchestration 패턴의 보상 트랜잭션으로,
 * 포인트 적립 실패 등의 이유로 결제를 롤백(포인트 환불)한다.
 */
@Slf4j
@Service("refundPaymentService")
@RequiredArgsConstructor
@Transactional
public class RefundPaymentService implements RefundPaymentUseCase {

    private final PaymentPointGatewayPort paymentPointGatewayPort;

    /**
     * 결제를 환불한다 (포인트 환불).
     * <p>
     * - 사용자의 포인트를 환불 금액만큼 증가시킨다.<br>
     * - 환불 결과는 DB에 반영되며, 환불 금액과 총 포인트 잔액을 반환한다.
     *
     * @param command 결제 환불 요청 커맨드
     * @return 환불 후 결과 객체
     * @throws RuntimeException 환불 처리 실패 시
     */
    @Override
    public RefundPaymentResult refund(RefundPaymentCommand command) {
        Long paymentId = command.paymentId();
        Long userId = command.userId();
        Long refundAmount = command.refundAmount();
        String reason = command.reason();

        log.info("[RefundPaymentService] 결제 환불 시작 - paymentId: {}, userId: {}, refundAmount: {}원, reason: {}",
                paymentId, userId, refundAmount, reason);

        try {
            // 포인트 환불
            Long totalPointAmount = paymentPointGatewayPort.refundPoint(userId, refundAmount);

            log.info("[RefundPaymentService] 결제 환불 성공 - paymentId: {}, userId: {}, refundAmount: {}원, totalPointAmount: {}원",
                    paymentId, userId, refundAmount, totalPointAmount);

            return RefundPaymentResult.builder()
                    .paymentId(paymentId)
                    .userId(userId)
                    .refundAmount(refundAmount)
                    .totalPointAmount(totalPointAmount)
                    .refundedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("[RefundPaymentService] 결제 환불 실패 - paymentId: {}, userId: {}, refundAmount: {}원",
                    paymentId, userId, refundAmount, e);
            throw new RuntimeException("결제 환불에 실패했습니다.", e);
        }
    }
}