package com.roovies.concertreservation.payments.domain.entity;

import com.roovies.concertreservation.payments.domain.enums.PaymentStatus;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Payment {
    private final Long id;
    private final Amount originalAmount;
    private Amount discountAmount;
    private Amount paidAmount;
    private PaymentStatus status;
    private final LocalDateTime createdAt;

    public static Payment create(Long id, Amount originalAmount, Amount paidAmount) {
        return new Payment(id, originalAmount, paidAmount, PaymentStatus.SUCCESS);
    }

    public static Payment restore(Long id, Amount originalAmount, Amount discountAmount, Amount paidAmount, PaymentStatus status, LocalDateTime createdAt) {
        return new Payment(id, originalAmount, discountAmount, paidAmount, status, createdAt);
    }

    private Payment(Long id, Amount originalAmount, Amount paidAmount, PaymentStatus status) {
        this.id = id;
        this.originalAmount = originalAmount;
        this.paidAmount = paidAmount;
        this.status = status;
        this.createdAt = LocalDateTime.now();

        validateAmount();
    }

    private Payment(Long id, Amount originalAmount, Amount discountAmount, Amount paidAmount, PaymentStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.paidAmount = paidAmount;
        this.status = status;
        this.createdAt = createdAt;
        validateAmount();
    }

    private void validateAmount() {
        validateOriginalAmount();
        validatePaidAmount();
    }

    private void validateOriginalAmount() {
        if (this.originalAmount == null)
            throw new IllegalArgumentException("정가는 null일 수 없습니다.");

        if (this.originalAmount.value() % 100 != 0)
            throw new IllegalArgumentException("정가는 100원 단위어야 합니다.");
    }

    private void validatePaidAmount() {
        if (this.paidAmount == null)
            throw new IllegalArgumentException("결제 금액은 null일 수 없습니다.");

        if (this.paidAmount.value() == 0)
            throw new IllegalArgumentException("결제 금액은 0원일 수 없습니다.");

        if (this.paidAmount.value() % 100 != 0)
            throw new IllegalArgumentException("결제 금액은 100원 단위어야 합니다.");

        if (this.originalAmount.value() < this.paidAmount.value())
            throw new IllegalArgumentException("결제 금액이 정가보다 많습니다.");
    }

    public void discount(Amount discountAmount) {
        this.discountAmount = discountAmount;
        this.paidAmount = this.paidAmount.subtract(discountAmount);
    }


}
