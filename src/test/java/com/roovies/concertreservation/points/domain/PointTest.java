package com.roovies.concertreservation.points.domain;

import com.roovies.concertreservation.points.domain.entity.Point;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

public class PointTest {

    private Long userId;
    private LocalDateTime updatedAt;

    @BeforeEach
    void setUp() {
        userId = 1L;
        updatedAt = LocalDateTime.of(2025, 9, 7, 10, 0);
    }

    @Test
    void 정상적으로_포인트를_생성할_수_있다() {
        // given
        Amount amount = Amount.of(1000);

        // when
        Point result = Point.create(userId, amount, updatedAt);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void 포인트_생성_시_금액이_null일_수_없다() {
        // given
        Amount amount = null;

        // when & then
        assertThatThrownBy(() -> Point.create(userId, amount, updatedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount는 null일 수 없습니다.");
    }

    @Test
    void 포인트_생성_시_회원ID가_null일_수_없다() {
        // given
        Long userId = null;
        Amount amount = Amount.of(1000);

        // when & then
        assertThatThrownBy(() -> Point.create(userId, amount, updatedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId는 null일 수 없습니다.");
    }

    @Test
    void 정상적으로_포인트를_충전할_수_있다() {
        // given
        Point point = Point.create(userId, Amount.of(0), updatedAt);
        Amount chargeAmount = Amount.of(1000);

        // when
        point.charge(chargeAmount);

        // then
        assertThat(point.getAmount()).isEqualTo(chargeAmount);
    }

    @Test
    void 보유_포인트가_1원_이상일_때_포인트_충전_시_최종_포인트는_충전_금액에_보유_포인트를_더한_값이어야_한다() {
        // given
        Point point = Point.create(userId, Amount.of(1000), updatedAt);
        Amount chargeAmount = Amount.of(1000);

        // when
        point.charge(chargeAmount);

        // then
        assertThat(point.getAmount()).isEqualTo(Amount.of(2000));
    }

    @Test
    void 포인트_충전은_100원_단위로만_충전할_수_있다() {
        // given
        Point point = Point.create(userId, Amount.of(1000), updatedAt);
        Amount chargeAmount = Amount.of(1001);

        // when & then
        assertThatThrownBy(() -> point.charge(chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트는 100원 단위로만 충전할 수 있습니다.");
    }

    @Test
    void 포인트_충전_시_0원은_충전할_수_없다() {
        // given
        Point point = Point.create(userId, Amount.of(1000), updatedAt);
        Amount chargeAmount = Amount.of(0);

        // when & then
        assertThatThrownBy(() -> point.charge(chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("0원 이하는 추가할 수 없습니다.");
    }

    @Test
    void 정상적으로_포인트를_사용할_수_있다() {
        // given
        Point point = Point.create(userId, Amount.of(1000), updatedAt);
        Amount useAmount = Amount.of(1000);

        // then
        point.deduct(useAmount);

        // then
        assertThat(point.getAmount()).isEqualTo(Amount.of(0));
    }

    @Test
    void 보유_포인트_보다_많은_포인트를_사용할_수_없다() {
        // given
        Point point = Point.create(userId, Amount.of(1000), updatedAt);
        Amount useAmount = Amount.of(1001);

        // when & then
        assertThatThrownBy(() -> point.deduct(useAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트가 부족합니다.");
    }

    @Test
    void 정상적으로_포인트를_환불_받을_수_있다() {
        // given
        Point point = Point.create(userId, Amount.of(1000), updatedAt);
        Amount refundAmount = Amount.of(1000);

        // when
        point.refund(refundAmount);

        // then
        assertThat(point.getAmount()).isEqualTo(Amount.of(2000));
    }

    @Test
    void 포인트_환불금액은_0원일_수_없다() {
        // given
        Point point = Point.create(userId, Amount.of(1000), updatedAt);
        Amount refundAmount = Amount.of(0);

        // when & then
        assertThatThrownBy(() -> point.refund(refundAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("환불 금액은 1원 이상이어야 합니다.");
    }
}
