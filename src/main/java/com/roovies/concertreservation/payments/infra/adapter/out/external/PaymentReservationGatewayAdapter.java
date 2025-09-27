package com.roovies.concertreservation.payments.infra.adapter.out.external;

import com.roovies.concertreservation.payments.application.dto.command.ExternalCreateReservationCommand;
import com.roovies.concertreservation.payments.application.port.out.PaymentReservationGatewayPort;
import com.roovies.concertreservation.payments.application.dto.query.GetHeldSeatListQuery;
import com.roovies.concertreservation.payments.domain.external.ExternalHeldSeatList;
import com.roovies.concertreservation.reservations.application.dto.command.CreateReservationCommand;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.in.CreateReservationUseCase;
import com.roovies.concertreservation.reservations.application.port.in.GetHeldSeatListUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class PaymentReservationGatewayAdapter implements PaymentReservationGatewayPort {

    private final GetHeldSeatListUseCase getHeldSeatListUseCase;
    private final CreateReservationUseCase createReservationUseCase;

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

    @Override
    public void saveReservation(ExternalCreateReservationCommand command) {
        CreateReservationCommand requestCommand = CreateReservationCommand.builder()
                .paymentId(command.paymentId())
                .userId(command.userId())
                .status(command.status())
                .scheduleId(command.scheduleId())
                .seatIds(command.seatIds())
                .build();
        createReservationUseCase.createReservation(requestCommand);
    }
}
