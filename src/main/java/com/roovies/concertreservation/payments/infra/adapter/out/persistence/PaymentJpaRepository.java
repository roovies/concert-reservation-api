package com.roovies.concertreservation.payments.infra.adapter.out.persistence;

import com.roovies.concertreservation.payments.infra.adapter.out.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {
}
