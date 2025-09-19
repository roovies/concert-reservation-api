package com.roovies.concertreservation.payments.application.port.out;

public interface PaymentUserGatewayPort {

    void updateUserPoints(Long userId, Long points);

}
