package com.roovies.concertreservation.payments.infra.adapter.out.external;

import com.roovies.concertreservation.payments.application.port.out.PaymentPointGatewayPort;
import com.roovies.concertreservation.points.application.dto.command.DeductPointCommand;
import com.roovies.concertreservation.points.application.dto.result.DeductPointResult;
import com.roovies.concertreservation.points.application.port.in.DeductPointUseCase;
import com.roovies.concertreservation.points.application.port.in.GetPointUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentPointGatewayAdapter implements PaymentPointGatewayPort {

    private final GetPointUseCase getPointUseCase;
    private final DeductPointUseCase deductPointUseCase;

    /**
     * Query 조회
     */
    @Override
    public Long getUserPoints(Long userId) {
        return getPointUseCase.findById(userId);
    }

    @Override
    public Long deductPoint(Long userId, Long amount) {
        DeductPointCommand command = DeductPointCommand.builder()
                .userId(userId)
                .amount(amount)
                .build();

        DeductPointResult result = deductPointUseCase.deduct(command);
        return result.resultAmount();
    }
}
