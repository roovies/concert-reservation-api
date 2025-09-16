package com.roovies.concertreservation.shared.vo;

import com.roovies.concertreservation.shared.domain.vo.Amount;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class AmountTest {

    @Test
    void 금액은_음수일_수_없다() {
        // given
        long value = -1;

        // when & then
        assertThatThrownBy(() -> Amount.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 0원 이상이어야 합니다.");
    }

    @Test
    void 현재_보유한_금액보다_큰_금액을_차감할_수_없다() {
        // given
        Amount currentAmount = Amount.of(1000);
        Amount subtractAmount = Amount.of(1001);

        // when
        assertThatThrownBy(() -> currentAmount.subtract(subtractAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("차감 결과가 음수가 될 수 없습니다.");
    }

    @Test
    void 추가하려는_금액은_1원_이상이어야_한다() {
        // given
        Amount amount = Amount.of(0);
        Amount currentAmount = Amount.of(1000);

        // when & then
        assertThatThrownBy(() -> currentAmount.add(amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("0원 이하는 추가할 수 없습니다.");
    }

    @Test
    void isGreaterThanOrEqual_메서드는_보유한_금액이_다른_금액보다_크거나_같으면_true를_반환한다() {
        // given
        Amount currentAmount = Amount.of(1000);
        Amount otherAmount = Amount.of(1000);
        Amount otherAmount2 = Amount.of(900);
        Amount otherAmount3 = Amount.of(1001);

        // when
        boolean result1 = currentAmount.isGreaterThanOrEqual(otherAmount);
        boolean result2 = currentAmount.isGreaterThanOrEqual(otherAmount2);
        boolean result3 = currentAmount.isGreaterThanOrEqual(otherAmount3);

        // then
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
        assertThat(result3).isFalse();
    }

    @Test
    void isZero_메서드는_보유한_금액이_0일_경우_true를_반환한다() {
        // given
        Amount currentAmount = Amount.of(0);
        Amount otherAmount = Amount.of(1000);

        // when
        boolean result = currentAmount.isZero();
        boolean result2 = otherAmount.isZero();

        // then
        assertThat(result).isTrue();
        assertThat(result2).isFalse();
    }

}
