package com.roovies.concertreservation.payments.infra.adapter.out.persistence;

import com.roovies.concertreservation.payments.application.port.out.PaymentRepositoryPort;
import com.roovies.concertreservation.payments.domain.entity.Payment;
import com.roovies.concertreservation.payments.infra.adapter.out.persistence.entity.PaymentJpaEntity;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity toSaveEntity = PaymentJpaEntity.from(payment);
        PaymentJpaEntity savedEntity = paymentJpaRepository.save(toSaveEntity);
        return Payment.restore(
                savedEntity.getId(),
                Amount.of(savedEntity.getOriginalAmount()),
                Amount.of(savedEntity.getDiscountAmount()),
                Amount.of(savedEntity.getPaidAmount()),
                savedEntity.getStatus(),
                savedEntity.getCreatedAt()
        );
    }
}
