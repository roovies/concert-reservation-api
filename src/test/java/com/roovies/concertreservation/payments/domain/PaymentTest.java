package com.roovies.concertreservation.payments.domain;


import com.roovies.concertreservation.payments.domain.entity.Payment;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class PaymentTest {
    @Test
    void 결제_금액은_null일_수_없다() {
        // given
        Long paymentId = 1L;
        Amount originalAmount = Amount.of(1000L);
        Amount paidAmount = null;

        // when & then
        assertThatThrownBy(() -> Payment.create(paymentId, originalAmount, paidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("결제 금액은 null일 수 없습니다.");
    }

    @Test
    void 결제_금액은_0원일_수_없다() {
        // given
        Long paymentId = 1L;
        Amount originalAmount = Amount.of(1000L);
        Amount paidAmount = Amount.of(0L);

        // when & then
        assertThatThrownBy(() -> Payment.create(
                paymentId,
                originalAmount,
                paidAmount
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("결제 금액은 0원일 수 없습니다.");
    }

    @Test
    void 결제_금액은_100원_단위어야_한다() {
        // given
        Long paymentId = 1L;
        Long reservationId = 5L;
        Amount originalAmount = Amount.of(1000L);
        Amount paidAmount = Amount.of(110L);

        // when & then
        assertThatThrownBy(() -> Payment.create(paymentId, originalAmount, paidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("결제 금액은 100원 단위어야 합니다.");
    }

    @Test
    void 결제_금액은_정가보다_많을_수_없다() {
        // given
        Long paymentId = 1L;
        Amount originalAmount = Amount.of(1000L);
        Amount paidAmount = Amount.of(1100L);

        // when & then
        assertThatThrownBy(() -> Payment.create(paymentId, originalAmount, paidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("결제 금액이 정가보다 많습니다.");
    }

    @Test
    void 정가는_100원_단위로_책정되어야_한다() {
        // given
        Long paymentId = 1L;
        Amount originalAmount = Amount.of(110L);
        Amount paidAmount = Amount.of(1100L);

        // when & then
        assertThatThrownBy(() -> Payment.create(paymentId, originalAmount, paidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("정가는 100원 단위어야 합니다.");
    }

    @Test
    void discount_메서드를_통해_할인을_적용하면_결제_금액이_할인된_금액만큼_차감되어야_한다() {
        // given
        Long paymentId = 1L;
        Amount originalAmount = Amount.of(1000L);
        Amount paidAmount = Amount.of(1000L);

        Amount discoundAmount = Amount.of(500L);
        Long expectedPaidAmount = 500L;

        // when
        Payment payment = Payment.create(paymentId, originalAmount, paidAmount);
        payment.discount(discoundAmount);

        // then
        assertThat(payment.getDiscountAmount().value()).isEqualTo(discoundAmount.value());
        assertThat(payment.getPaidAmount().value()).isEqualTo(expectedPaidAmount);
    }

    @Test
    void 정가는_null일_수_없다() {
        // given
        Long paymentId = 1L;
        Amount originalAmount = null;
        Amount paidAmount = Amount.of(1000L);

        // when & then
        assertThatThrownBy(() -> Payment.create(paymentId, originalAmount, paidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("정가는 null일 수 없습니다.");
    }

}
