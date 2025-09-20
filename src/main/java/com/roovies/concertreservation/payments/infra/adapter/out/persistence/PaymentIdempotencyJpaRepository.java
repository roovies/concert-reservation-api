package com.roovies.concertreservation.payments.infra.adapter.out.persistence;

import com.roovies.concertreservation.payments.infra.adapter.out.persistence.entity.PaymentIdempotencyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentIdempotencyJpaRepository extends JpaRepository<PaymentIdempotencyJpaEntity, String> {
}
