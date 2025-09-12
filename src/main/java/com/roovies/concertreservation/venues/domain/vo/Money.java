package com.roovies.concertreservation.venues.domain.vo;

public record Money(
        long amount
) {
    public Money {
        if (amount < 0)
            throw new IllegalArgumentException("금액은 음수일 수 없습니다.");
    }

    public Money add(Money other) {
        return new Money(this.amount + other.amount);
    }

    public Money subtract(Money other) {
        if (this.amount < other.amount) {
            throw new IllegalArgumentException("차감 결과가 음수가 될 수 없습니다");
        }
        return new Money(this.amount - other.amount);
    }
}
