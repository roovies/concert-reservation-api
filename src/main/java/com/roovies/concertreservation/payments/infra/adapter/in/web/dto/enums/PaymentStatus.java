package com.roovies.concertreservation.payments.infra.adapter.in.web.dto.enums;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 상태")
public enum PaymentStatus {
    CONFIRMED,
    CANCELLED,
    FAILED,
}
