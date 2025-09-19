package com.roovies.concertreservation.payments.domain;

import com.roovies.concertreservation.payments.domain.entity.PaymentIdempotency;
import com.roovies.concertreservation.payments.domain.enums.PaymentIdempotencyStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class PaymentIdempotencyTest {

    @Test
    void 결제_멱등성_정보_생성시_PROCESSING_상태여야_한다() {
        // given
        String idempotencyKey = "payment_key_12345";
        Long userId = 1L;
        Long paymentId = 12345L;
        PaymentIdempotencyStatus status = PaymentIdempotencyStatus.PROCESSING;
        String resultData = "test";
        LocalDateTime createdAt = LocalDateTime.of(2025, 9, 18, 14, 30, 25);
        LocalDateTime completedAt = LocalDateTime.of(2025, 9, 18, 14, 30, 25);

        // when
        PaymentIdempotency paymentIdempotency = PaymentIdempotency.create(idempotencyKey, userId, paymentId, status, resultData, createdAt, completedAt);

        // then
        assertThat(paymentIdempotency)
                .extracting("key", "userId", "status")
                .containsExactly(idempotencyKey, userId, PaymentIdempotencyStatus.PROCESSING);

        assertThat(paymentIdempotency.isProcessing()).isTrue();
    }

    @Test
    void 결제_멱등성_정보_생성시_결제ID와_응답데이터는_null이어야_한다() {
        // given
        String idempotencyKey = "payment_key_12345";
        Long userId = 1L;
        Long paymentId = null;
        PaymentIdempotencyStatus status = PaymentIdempotencyStatus.PROCESSING;
        String resultData = null;
        LocalDateTime createdAt = LocalDateTime.of(2025, 9, 18, 14, 30, 25);
        LocalDateTime completedAt = LocalDateTime.of(2025, 9, 18, 14, 30, 25);

        // when
        PaymentIdempotency paymentIdempotency = PaymentIdempotency.create(idempotencyKey, userId, paymentId, status, resultData, createdAt, completedAt);

        // then
        assertThat(paymentIdempotency.getPaymentId()).isNull();
        assertThat(paymentIdempotency.getResultData()).isNull();
    }

    @Test
    void 결제_멱등성_정보에_응답용_데이터를_적재하면_SUCCESS_상태여야_한다() {
        // given
        String idempotencyKey = "payment_key_12345";
        Long userId = 1L;
        Long paymentId = 12345L;
        PaymentIdempotencyStatus status = PaymentIdempotencyStatus.PROCESSING;
        String resultData = "test";
        LocalDateTime createdAt = LocalDateTime.of(2025, 9, 18, 14, 30, 25);
        LocalDateTime completedAt = LocalDateTime.of(2025, 9, 18, 14, 30, 25);
        // when
        PaymentIdempotency paymentIdempotency = PaymentIdempotency.create(idempotencyKey, userId, paymentId, status, resultData, createdAt, completedAt);
        paymentIdempotency.setResult(paymentId, resultData);

        // then
        assertThat(paymentIdempotency.getResultData()).isNotNull();
        assertThat(paymentIdempotency.getStatus()).isEqualTo(PaymentIdempotencyStatus.SUCCESS);
        assertThat(paymentIdempotency.getCompletedAt()).isNotNull();
    }

    @Test
    void 결제_멱등성_정보가_SUCCESS일_경우_결제ID가_매핑되어야_한다() {
        // given
        String idempotencyKey = "payment_key_12345";
        Long userId = 1L;
        Long paymentId = 12345L;
        PaymentIdempotencyStatus status = PaymentIdempotencyStatus.PROCESSING;
        String resultData = "test";
        LocalDateTime createdAt = LocalDateTime.of(2025, 9, 18, 14, 30, 25);
        LocalDateTime completedAt = LocalDateTime.of(2025, 9, 18, 14, 30, 25);

        // when
        PaymentIdempotency paymentIdempotency = PaymentIdempotency.create(idempotencyKey, userId, paymentId, status, resultData, createdAt, completedAt);
        paymentIdempotency.setResult(paymentId, resultData);

        // then
        assertThat(paymentIdempotency.getStatus()).isEqualTo(PaymentIdempotencyStatus.SUCCESS);
        assertThat(paymentIdempotency.getPaymentId()).isNotNull();
    }
}
