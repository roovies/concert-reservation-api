package com.roovies.concertreservation.payments.application.port.out;

public interface PaymentPointGatewayPort {
    Long getUserPoints(Long userId);
    Long deductPoint(Long userId, Long amount);
    Long refundPoint(Long userId, Long amount);
}
