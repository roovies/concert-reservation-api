package com.roovies.concertreservation.payments.infra.adapter.out.external;

import com.roovies.concertreservation.payments.application.port.out.PaymentReservationGatewayPort;
import com.roovies.concertreservation.payments.application.dto.query.GetHeldSeatListQuery;
import com.roovies.concertreservation.payments.domain.external.ExternalHeldSeatList;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.in.GetHeldSeatListUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class PaymentReservationGatewayAdapter implements PaymentReservationGatewayPort {

    private final GetHeldSeatListUseCase getHeldSeatListUseCase;

    @Override
    public ExternalHeldSeatList findHeldSeats(GetHeldSeatListQuery query) {

        com.roovies.concertreservation.reservations.application.dto.query.GetHeldSeatListQuery externalQuery = com.roovies.concertreservation.reservations.application.dto.query.GetHeldSeatListQuery.builder()
                .scheduleId(query.scheduleId())
                .seatIds(query.seatIds())
                .userId(query.userId())
                .build();

        HoldSeatResult result = getHeldSeatListUseCase.getHeldSeatList(externalQuery);
        return ExternalHeldSeatList.of(
                result.scheduleId(),
                result.seatIds(),
                result.userId(),
                result.totalPrice()
        );
    }
}
