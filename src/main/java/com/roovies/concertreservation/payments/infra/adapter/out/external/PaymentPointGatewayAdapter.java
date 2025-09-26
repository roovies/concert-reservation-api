package com.roovies.concertreservation.payments.infra.adapter.out.external;

import com.roovies.concertreservation.payments.application.port.out.PaymentPointGatewayPort;
import com.roovies.concertreservation.points.application.port.in.GetPointUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentPointGatewayAdapter implements PaymentPointGatewayPort {

    private final GetPointUseCase getPointUseCase;

    @Override
    public Long getUserPoints(Long userId) {
        return getPointUseCase.findById(userId);
    }
}
