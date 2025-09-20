package com.roovies.concertreservation.payments.infra.adapter.out.external;

import com.roovies.concertreservation.payments.application.port.out.PaymentReservationQueryPort;
import com.roovies.concertreservation.payments.domain.external.query.HeldSeatsQuery;
import com.roovies.concertreservation.payments.domain.external.snapshot.PaymentHeldSeatsSnapShot;
import com.roovies.concertreservation.reservations.application.dto.query.GetHeldSeatsQuery;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.in.GetHeldSeatsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class PaymentReservationQueryAdapter implements PaymentReservationQueryPort {

    private final GetHeldSeatsUseCase getHeldSeatsUseCase;

    @Override
    public PaymentHeldSeatsSnapShot findHeldSeats(HeldSeatsQuery query) {

        GetHeldSeatsQuery externalQuery = GetHeldSeatsQuery.builder()
                .scheduleId(query.scheduleId())
                .seatIds(query.seatIds())
                .userId(query.userId())
                .build();

        HoldSeatResult result = getHeldSeatsUseCase.execute(externalQuery);
        return PaymentHeldSeatsSnapShot.of(
                result.scheduleId(),
                result.seatIds(),
                result.userId(),
                result.totalPrice()
        );
    }
}
