package com.roovies.concertreservation.points.domain.entity;

import com.roovies.concertreservation.shared.domain.vo.Amount;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Point {
    private Long userId;
    private Amount amount;
    private LocalDateTime updatedAt;

    public static Point create(Long userId, Amount amount, LocalDateTime updatedAt) {
        return new Point(userId, amount, updatedAt);
    }

    private Point(Long userId, Amount amount, LocalDateTime updatedAt) {
        if (userId == null)
            throw new IllegalArgumentException("userId는 null일 수 없습니다.");

        if (amount == null)
            throw new IllegalArgumentException("amount는 null일 수 없습니다.");

        this.userId = userId;
        this.amount = amount;
        this.updatedAt = updatedAt;
    }

    public void charge(Amount other) {
        if (other.value() % 100 != 0)
            throw new IllegalArgumentException("포인트는 100원 단위로만 충전할 수 있습니다.");
        this.amount = this.amount.add(other);
        this.updatedAt = LocalDateTime.now();
    }

    public void use(Amount other) {
        if (!this.amount.isGreaterThanOrEqual(other))
            throw new IllegalArgumentException("포인트가 부족합니다.");

        this.amount = this.amount.subtract(other);
    }

    public void refund(Amount other) {
        if (other.value() < 1)
            throw new IllegalArgumentException("환불 금액은 1원 이상이어야 합니다.");
        this.amount = this.amount.add(other);
    }

}

