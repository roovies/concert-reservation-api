package com.roovies.concertreservation.payments.infra.adapter.out.external;

import com.roovies.concertreservation.payments.application.port.out.PaymentUserGatewayPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentUserGatewayAdapter implements PaymentUserGatewayPort {


    @Override
    public void updateUserPoints(Long userId, Long points) {
        // TODO: 기능 구현 필요
    }
}
