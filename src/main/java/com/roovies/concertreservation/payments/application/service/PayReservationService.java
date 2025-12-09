package com.roovies.concertreservation.payments.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roovies.concertreservation.payments.application.dto.command.ExternalCreateReservationCommand;
import com.roovies.concertreservation.payments.application.dto.command.PayReservationCommand;
import com.roovies.concertreservation.payments.application.dto.result.PayReservationResult;
import com.roovies.concertreservation.payments.application.port.in.PayReservationUseCase;
import com.roovies.concertreservation.payments.application.port.out.*;
import com.roovies.concertreservation.payments.domain.entity.Payment;
import com.roovies.concertreservation.payments.domain.entity.PaymentIdempotency;
import com.roovies.concertreservation.payments.application.dto.query.GetHeldSeatListQuery;
import com.roovies.concertreservation.payments.domain.external.ExternalHeldSeatList;
import com.roovies.concertreservation.reservations.domain.enums.ReservationStatus;
import com.roovies.concertreservation.shared.domain.event.ReservationCompletedKafkaEvent;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 결제 서비스 구현체.
 * <p>
 * 공연 예약 서비스에서 사용자의 좌석 결제 요청을 처리한다.
 * 멱등성 키를 기반으로 중복 요청을 방지하고,
 * 결제 처리 및 예약 확정을 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayReservationService implements PayReservationUseCase {

    private final PaymentCommandRepositoryPort paymentCommandRepositoryPort;
    private final PaymentIdempotencyRepositoryPort paymentIdempotencyRepositoryPort;

    private final PaymentReservationGatewayPort paymentReservationGatewayPort;
    private final PaymentPointGatewayPort paymentPointGatewayPort;
    private final PaymentUserGatewayPort paymentUserGatewayPort;
    private final PaymentEventPort paymentEventPort;
    private final PaymentKafkaEventPort paymentKafkaEventPort;

    private final ObjectMapper objectMapper;

    /**
     * 홀딩된 좌석에 대해 결제를 처리한다.
     *
     * @param command 결제 요청 명령 객체
     * @return 결제 처리 결과
     * @throws IllegalStateException 포인트 부족, 좌석 없음 등 비즈니스 로직 실패 시
     * @throws RuntimeException      기술적 오류 발생 시
     */
    @Override
    public PayReservationResult payReservation(PayReservationCommand command) {
        String idempotencyKey = command.idempotencyKey();
        Long scheduleId = command.scheduleId();
        List<Long> seatIds = command.seatIds();
        Long userId = command.userId();
        Long payForAmount = command.payForAmount();
        Long discountAmount = command.discountAmount();

        log.info("[PayReservationService] 홀딩된 좌석 결제 시작 - userId: {}, scheduleId: {}, seatIds: {}",
                userId, scheduleId, seatIds);

        // 예약 대기 검증
        ExternalHeldSeatList heldSeats = validateAndGetHeldSeats(scheduleId, seatIds, userId);

        // 멱등성 키 락 선점 시도
        boolean lockAcquired = tryLock(idempotencyKey, userId);
        if (!lockAcquired)
            return handleDuplicateRequest(idempotencyKey);

        // 락 획득한 경우 결제 로직 수행
        try {
            Payment result = payReservation(userId, payForAmount, discountAmount, heldSeats.totalPrice());
            // 결과 객체 생성
            PayReservationResult payReservationResult = PayReservationResult.builder()
                    .paymentId(result.getId())
                    .scheduleId(scheduleId)
                    .seatIds(seatIds)
                    .userId(userId)
                    .originalAmount(result.getOriginalAmount().value())
                    .discountAmount(result.getDiscountAmount().value())
                    .paidAmount(result.getPaidAmount().value())
                    .status(result.getStatus())
                    .completedAt(result.getCreatedAt())
                    .build();

            // 결제 성공 시 예약 정보 적재
            saveNewReservation(payReservationResult);

            // 성공한 경우 멱등성 정보 갱신
            paymentIdempotencyRepositoryPort.setResult(idempotencyKey, result.getId(), objectMapper.writeValueAsString(payReservationResult));

            // 결제 완료 이벤트 발행 (랭킹 계산용 - Redis Pub/Sub)
            paymentEventPort.publishPaymentCompleted(result.getId(), scheduleId, userId);

            // 예약 완료 이벤트 발행 (알림 등 다른 서비스용 - Kafka)
            ReservationCompletedKafkaEvent kafkaEvent = ReservationCompletedKafkaEvent.of(
                    payReservationResult.paymentId(),
                    payReservationResult.scheduleId(),
                    payReservationResult.seatIds(),
                    payReservationResult.userId(),
                    payReservationResult.originalAmount(),
                    payReservationResult.discountAmount(),
                    payReservationResult.paidAmount(),
                    payReservationResult.status(),
                    payReservationResult.completedAt()
            );
            paymentKafkaEventPort.publishReservationCompleted(kafkaEvent);

            log.info("[PayReservationService] 홀딩된 좌석 결제 성공 - userId: {}, scheduleId: {}, seatIds: {}",
                    userId, scheduleId, seatIds);

            return payReservationResult;
        } catch (IllegalStateException e) {
            // 비즈니스 로직 실패 (포인트 부족, 좌석 없음 등)
            log.error("[PayReservationService] 홀딩된 좌석 결제 실패- userId: {}, scheduleId: {}, seatIds: {}, reason={}",
                    command.userId(), command.scheduleId(), command.seatIds(), e.getMessage());
            paymentIdempotencyRepositoryPort.setFailed(idempotencyKey, e.getMessage());
            throw e;

        } catch (Exception e) {
            // 예상치 못한 기술적 실패
            log.error("[PayReservationService] 홀딩된 좌석 결제 실패 - userId: {}, scheduleId: {}, seatIds: {}, reason={}",
                    command.userId(), command.scheduleId(), command.seatIds(), e.getMessage());
            paymentIdempotencyRepositoryPort.setFailed(idempotencyKey, "시스템 오류");
            throw new RuntimeException("결제 처리에 실패했습니다.", e);
        }
    }

    /**
     * 예약 대기 중인 좌석을 검증하고 가져온다.
     *
     * @param scheduleId 스케줄 ID
     * @param seatIds    좌석 ID 목록
     * @param userId     사용자 ID
     * @return 예약 대기 좌석 정보
     * @throws IllegalStateException 예약 대기 중인 좌석이 없는 경우
     */
    private ExternalHeldSeatList validateAndGetHeldSeats(Long scheduleId, List<Long> seatIds, Long userId) {
        GetHeldSeatListQuery query = GetHeldSeatListQuery.of(
                scheduleId,
                seatIds,
                userId
        );

        ExternalHeldSeatList heldSeats = paymentReservationGatewayPort.findHeldSeats(query);
        if (heldSeats.seatIds().isEmpty())
            throw new IllegalStateException("예약 대기 중인 좌석이 없습니다.");

        return heldSeats;
    }

    /**
     * 멱등성 키를 기반으로 락을 획득한다.
     *
     * @param idempotencyKey 멱등성 키
     * @param userId         사용자 ID
     * @return 락 획득 성공 여부
     */
    private boolean tryLock(String idempotencyKey, Long userId) {
        // 멱등성 처리
        PaymentIdempotency idempotency = PaymentIdempotency.tryProcess(
                idempotencyKey,
                userId
        );

        // 멱등성 키 선점 시도 (RDB INSERT)
        return paymentIdempotencyRepositoryPort.tryLock(idempotency);
    }

    /**
     * 멱등성 키가 이미 존재하는 경우 중복 요청을 처리한다.
     *
     * @param idempotencyKey 멱등성 키
     * @return 이전 요청의 성공 결과
     * @throws IllegalArgumentException 중복 요청이 진행 중이거나 실패한 경우
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
     * 저장된 JSON 데이터를 {@link PayReservationResult} 객체로 역직렬화한다.
     *
     * @param idempotency 멱등성 엔티티
     * @return 역직렬화된 결제 결과
     * @throws NoSuchElementException 저장된 결과가 없을 경우
     * @throws RuntimeException       역직렬화 실패 시
     */
    private PayReservationResult deserializeResult(PaymentIdempotency idempotency) {
        if (idempotency.getResultData() == null || idempotency.getResultData().trim().isEmpty()) {
            log.error("[PayReservationService] 멱등성 키 역직렬화 실패 - idempotencyKey: {}",
                    idempotency.getKey());

            throw new NoSuchElementException("저장된 결제 결과를 찾을 수 없습니다.");
        }

        try {
            PayReservationResult result = objectMapper.readValue(
                    idempotency.getResultData(),
                    PayReservationResult.class
            );

            return result;

        } catch (JsonProcessingException e) {
            log.error("[PayReservationService] 멱등성 키 역직렬화 실패 - idempotencyKey: {}",
                    idempotency.getKey());
            throw new RuntimeException("저장된 결제 결과를 읽는데 실패했습니다.", e);
        }
    }

    /**
     * 실제 결제 로직을 수행한다.
     *
     * @param userId        사용자 ID
     * @param payForAmount  사용자가 결제하려는 금액
     * @param discountAmount 할인 금액
     * @param totalPrice    총 결제 금액
     * @return 저장된 결제 엔티티
     * @throws IllegalStateException 결제 포인트 부족 시
     */
    private Payment payReservation(Long userId, Long payForAmount, Long discountAmount, Long totalPrice) {
        // 결제 객체 만들기
        Payment payment = Payment.create(
                userId,
                Amount.of(totalPrice),
                Amount.of(payForAmount)
        );

        // 할인 금액 있으면 적용
        if (discountAmount > 0)
            payment.discount(Amount.of(discountAmount));

        // 포인트 차감
        Long resultAmount = paymentPointGatewayPort.deductPoint(userId, totalPrice);

        // 결제 정보 저장
        return paymentCommandRepositoryPort.save(payment);
    }

    /**
     * 결제 성공 후 예약 정보를 저장한다.
     *
     * @param payReservationResult 결제 결과
     */
    private void saveNewReservation(PayReservationResult payReservationResult) {
        ExternalCreateReservationCommand command = ExternalCreateReservationCommand.builder()
                .paymentId(payReservationResult.paymentId())
                .userId(payReservationResult.userId())
                .status(ReservationStatus.CONFIRMED)
                .scheduleId(payReservationResult.scheduleId())
                .seatIds(payReservationResult.seatIds())
                .build();

        paymentReservationGatewayPort.saveReservation(command);
    }
}
