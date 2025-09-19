package com.roovies.concertreservation.payments.infra.adapter.out.persistence;

import com.roovies.concertreservation.payments.application.port.out.PaymentIdempotencyRepositoryPort;
import com.roovies.concertreservation.payments.domain.entity.PaymentIdempotency;
import com.roovies.concertreservation.payments.infra.adapter.out.persistence.entity.PaymentIdempotencyJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PaymentIdempotencyRepositoryAdapter implements PaymentIdempotencyRepositoryPort {

    private final PaymentIdempotencyJpaRepository paymentIdempotencyJpaRepository;

    @Override
    public boolean tryLock(PaymentIdempotency paymentIdempotency) {
        try {
            PaymentIdempotencyJpaEntity entity = PaymentIdempotencyJpaEntity.from(paymentIdempotency);
            paymentIdempotencyJpaRepository.save(entity);
            log.info("멱등성 키 처리 시작: idempotencyKey={}", paymentIdempotency.getKey());
            return true;

        } catch (DataIntegrityViolationException e) {
            // 중복 키 - 이미 처리 중이거나 완료
            log.info("중복 멱등성 키 감지: idempotencyKey={}", paymentIdempotency.getKey());
            return false;
        }
    }

    @Override
    public Optional<PaymentIdempotency> findByKey(String key) {
        Optional<PaymentIdempotencyJpaEntity> entity = paymentIdempotencyJpaRepository.findById(key);
        if (entity.isPresent()) {
            PaymentIdempotencyJpaEntity idempotencyJpaEntity = entity.get();
            return Optional.of(
                    PaymentIdempotency.create(
                            idempotencyJpaEntity.getKey(),
                            idempotencyJpaEntity.getUserId(),
                            idempotencyJpaEntity.getPaymentId(),
                            idempotencyJpaEntity.getStatus(),
                            idempotencyJpaEntity.getResultData(),
                            idempotencyJpaEntity.getCreatedAt(),
                            idempotencyJpaEntity.getCompletedAt()
                    )
            );
        }

        return Optional.empty();
    }

    @Override
    public void setResult(PaymentIdempotency idempotency, Long paymentId, String result) {
        // 멱등성 엔티티 업데이트
        PaymentIdempotencyJpaEntity entity = paymentIdempotencyJpaRepository.findById(idempotency.getKey())
                .orElseThrow(() -> new IllegalArgumentException("멱등성 키를 찾을 수 없습니다: " + idempotency.getKey()));

        entity.updateResult(paymentId, result);

        paymentIdempotencyJpaRepository.save(entity);
        log.info("멱등성 키 처리 완료: idempotencyKey={}, paymentId={}",
                idempotency.getKey(), paymentId);
    }

    @Override
    public void setFailed(PaymentIdempotency idempotency, String failureReason) {
        PaymentIdempotencyJpaEntity entity = paymentIdempotencyJpaRepository.findById(idempotency.getKey())
                .orElseThrow(() -> new IllegalArgumentException("멱등성 키를 찾을 수 없습니다: " + idempotency.getKey()));

        entity.updateFailed(failureReason);

        paymentIdempotencyJpaRepository.save(entity);
        log.warn("멱등성 키 처리 실패: idempotencyKey={}, reason={}",
                idempotency.getKey(), failureReason);
    }
}
