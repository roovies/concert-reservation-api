package com.roovies.concertreservation.payments.infra.adapter.out.external;

import com.roovies.concertreservation.payments.application.port.out.PaymentPointQueryPort;
import com.roovies.concertreservation.points.application.port.in.GetPointUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentPointQueryAdapter implements PaymentPointQueryPort {

    private final GetPointUseCase getPointUseCase;

    @Override
    public Long getUserPoints(Long userId) {
        return getPointUseCase.execute(userId);
    }
}
