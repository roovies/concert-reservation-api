package com.roovies.concertreservation.payments.domain.enums;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 상태")
public enum PaymentStatus {
    SUCCESS,
    FAILED,
    REFUNDED
}
