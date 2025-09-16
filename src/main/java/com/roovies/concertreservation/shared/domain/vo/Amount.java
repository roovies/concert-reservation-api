package com.roovies.concertreservation.shared.domain.vo;

public record Amount(
        long value
) {
    public static Amount of(long value) {
        if (value < 0)
            throw new IllegalArgumentException("금액은 0원 이상이어야 합니다.");

        return new Amount(value);
    }

    public Amount add(Amount other) {
        if (other.value <= 0)
            throw new IllegalArgumentException("0원 이하는 추가할 수 없습니다.");

        return new Amount(this.value + other.value);
    }

    public Amount subtract(Amount other) {
        if (this.value < other.value) {
            throw new IllegalArgumentException("차감 결과가 음수가 될 수 없습니다.");
        }
        return new Amount(this.value - other.value);
    }

    public boolean isGreaterThanOrEqual(Amount other) {
        return this.value >= other.value;
    }

    public boolean isZero() {
        return this.value == 0;
    }
}
