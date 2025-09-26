package com.roovies.concertreservation.payments.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roovies.concertreservation.payments.application.dto.command.PayReservationCommand;
import com.roovies.concertreservation.payments.application.dto.result.PayReservationResult;
import com.roovies.concertreservation.payments.application.port.in.PayReservationUseCase;
import com.roovies.concertreservation.payments.application.port.out.*;
import com.roovies.concertreservation.payments.domain.entity.Payment;
import com.roovies.concertreservation.payments.domain.entity.PaymentIdempotency;
import com.roovies.concertreservation.payments.application.dto.query.GetHeldSeatListQuery;
import com.roovies.concertreservation.payments.domain.external.ExternalHeldSeatList;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PayReservationService implements PayReservationUseCase {

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final PaymentIdempotencyRepositoryPort paymentIdempotencyRepositoryPort;

    private final PaymentReservationGatewayPort paymentReservationGatewayPort;
    private final PaymentPointGatewayPort paymentPointGatewayPort;
    private final PaymentUserGatewayPort paymentUserGatewayPort;

    private final ObjectMapper objectMapper;

    @Override
    public PayReservationResult payReservation(PayReservationCommand command) {
        GetHeldSeatListQuery query = GetHeldSeatListQuery.of(
                command.scheduleId(),
                command.seatIds(),
                command.userId()
        );

        ExternalHeldSeatList heldSeats = paymentReservationGatewayPort.findHeldSeats(query);
        if (heldSeats.seatIds().isEmpty())
            throw new IllegalStateException("예약 대기 중인 좌석이 없습니다.");

        // 멱등성 처리
        PaymentIdempotency idempotency = PaymentIdempotency.tryProcess(
                command.idempotencyKey(),
                command.userId()
        );

        // 멱등성 키 선점 시도 (RDB INSERT)
        boolean lockAquired = paymentIdempotencyRepositoryPort.tryLock(idempotency);
        if (!lockAquired)
            return handleDuplicateRequest(command.idempotencyKey());

        // 락 획득한 경우 결제 로직 수행
        try {
            Payment result = processPayment(command, heldSeats);
            // 결과 객체 생성
            PayReservationResult payReservationResult = PayReservationResult.builder()
                    .paymentId(result.getId())
                    .scheduleId(command.scheduleId())
                    .seatIds(command.seatIds())
                    .userId(command.userId())
                    .originalAmount(result.getOriginalAmount().value())
                    .discountAmount(result.getDiscountAmount().value())
                    .paidAmount(result.getPaidAmount().value())
                    .status(result.getStatus())
                    .completedAt(result.getCreatedAt())
                    .build();

            // 성공한 경우 멱등성 정보 갱신
            paymentIdempotencyRepositoryPort.setResult(idempotency, result.getId(), objectMapper.writeValueAsString(payReservationResult));
            return payReservationResult;
        } catch (IllegalStateException e) {
            // 비즈니스 로직 실패 (포인트 부족, 좌석 없음 등)
            log.warn("결제 비즈니스 로직 실패: idempotencyKey={}, reason={}",
                    idempotency.getKey(), e.getMessage());
            paymentIdempotencyRepositoryPort.setFailed(idempotency, e.getMessage());
            throw e;

        } catch (Exception e) {
            // 예상치 못한 기술적 실패
            log.error("결제 처리 중 예상치 못한 오류: idempotencyKey={}", idempotency.getKey(), e);
            paymentIdempotencyRepositoryPort.setFailed(idempotency, "시스템 오류");
            throw new RuntimeException("결제 처리에 실패했습니다.", e);
        }
    }

    /**
     * 중복 요청 처리 로직
     */
    private PayReservationResult handleDuplicateRequest(String idempotencyKey) {
        PaymentIdempotency storedIdempotency = paymentIdempotencyRepositoryPort.findByKey(idempotencyKey)
                .orElseThrow(() -> new IllegalArgumentException("동시성 문제가 발생했습니다."));

        if (storedIdempotency.isProcessing()) {
            throw new IllegalArgumentException("중복된 요청입니다. 처리 중입니다.");
        }

        if (storedIdempotency.isSuccess()) {
            return deserializeResult(storedIdempotency);
        }

        // 실패 상태인 경우
        throw new IllegalArgumentException("이전 요청이 실패했습니다. 새로운 멱등성 키로 다시 시도해주세요.");
    }

    /**
     * 저장된 결과를 PayReservationResult로 역직렬화
     */
    private PayReservationResult deserializeResult(PaymentIdempotency idempotency) {
        if (idempotency.getResultData() == null || idempotency.getResultData().trim().isEmpty()) {
            log.error("No result data found for successful idempotency. key: {}", idempotency.getKey());
            throw new NoSuchElementException("저장된 결제 결과를 찾을 수 없습니다.");
        }

        try {
            PayReservationResult result = objectMapper.readValue(
                    idempotency.getResultData(),
                    PayReservationResult.class
            );

            log.debug("Successfully deserialized result for idempotencyKey: {}", idempotency.getKey());
            return result;

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize payment result for idempotencyKey: {}", idempotency.getKey(), e);
            throw new RuntimeException("저장된 결제 결과를 읽는데 실패했습니다.", e);
        }
    }

    /**
     * 실제 결제 처리 로직
     */
    private Payment processPayment(PayReservationCommand command, ExternalHeldSeatList heldSeats) {
        // 실제 보유 포인트 조회 및 결제 가능한지 검증
        Long currentPoint = paymentPointGatewayPort.getUserPoints(command.userId());
        if (currentPoint < heldSeats.totalPrice())
            throw new IllegalStateException("결제 금액이 부족합니다.");

        // 결제 객체 만들기
        Payment payment = Payment.create(
                command.userId(),
                Amount.of(heldSeats.totalPrice()),
                Amount.of(command.payForAmount())
        );

        // 할인 금액 있으면 적용
        if (command.discountAmount() > 0)
            payment.discount(Amount.of(command.discountAmount()));

        // 최종 결제 후 남은 포인트 계산
        Long remainingAmount = currentPoint - payment.getPaidAmount().value();

        // 결제 정보 저장
        Payment result = paymentRepositoryPort.save(payment);

        // 남은 포인트 반영
        paymentUserGatewayPort.updateUserPoints(command.userId(), remainingAmount);
        return result;
    }
}
