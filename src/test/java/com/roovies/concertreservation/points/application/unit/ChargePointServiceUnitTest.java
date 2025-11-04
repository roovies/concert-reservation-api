package com.roovies.concertreservation.points.application.unit;

import com.roovies.concertreservation.points.application.dto.command.ChargePointCommand;
import com.roovies.concertreservation.points.application.dto.result.ChargePointResult;
import com.roovies.concertreservation.points.application.port.out.PointCommandRepositoryPort;
import com.roovies.concertreservation.points.application.service.ChargePointService;
import com.roovies.concertreservation.points.domain.entity.Point;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class ChargePointServiceUnitTest {

    @Mock
    private PointCommandRepositoryPort pointCommandRepositoryPort;

    @InjectMocks
    private ChargePointService chargePointService;

    private ChargePointCommand command;

    @BeforeEach
    void setUp() {
        command = ChargePointCommand.builder()
                .userId(1L)
                .amount(1000L)
                .build();
    }

    @Test
    void 존재하지_않는_회원일_경우_예외가_발생해야_한다() {
        // given
        given(pointCommandRepositoryPort.findById(command.userId()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chargePointService.charge(command))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("존재하지 않는 회원입니다.");
    }

    @Test
    void 보유_포인트가_0일_때_포인트_충전_시_결과는_충전금액이어야_한다() {
        // given
        Point storedPoint = Point.create(command.userId(), Amount.of(0), null);
        given(pointCommandRepositoryPort.findById(command.userId()))
                .willReturn(Optional.of(storedPoint));

        Point resultPoint = Point.create(command.userId(), Amount.of(command.amount()), LocalDateTime.of(2025, 9, 16, 17, 30));
        given(pointCommandRepositoryPort.save(storedPoint))
                .willReturn(resultPoint);

        // when
        ChargePointResult result = chargePointService.charge(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(command.userId());
        assertThat(result.totalAmount()).isEqualTo(command.amount());
        assertThat(result.updatedAt()).isNotNull();
    }
}
