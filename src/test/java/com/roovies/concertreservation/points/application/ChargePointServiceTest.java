package com.roovies.concertreservation.points.application;

import com.roovies.concertreservation.points.application.dto.command.ChargePointCommand;
import com.roovies.concertreservation.points.application.port.out.PointRepositoryPort;
import com.roovies.concertreservation.points.application.port.out.PointUserQueryPort;
import com.roovies.concertreservation.points.application.service.ChargePointService;
import com.roovies.concertreservation.points.domain.entity.Point;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class ChargePointServiceTest {

    @Mock
    private PointUserQueryPort pointUserQueryPort;

    @Mock
    private PointRepositoryPort pointRepositoryPort;

    @InjectMocks
    private ChargePointService chargePointService;

    private ChargePointCommand command;

    @BeforeEach
    void setUp() {
        command = ChargePointCommand.of(1L, 1000L);
    }

    @Test
    void 존재하지_않는_회원일_경우_예외가_발생해야_한다() {
        // given
        given(pointUserQueryPort.getUser(command.userId()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chargePointService.excute(command))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("존재하지 않는 회원입니다.");
    }

    @Test
    void

    @Test
    void 정상적인_포인트_충전_요청시_성공해야_한다() {
        // given
        Point point = Point.create(1L, Amount.of(0), null);
        given(pointRepositoryPort.findById(1L))
                .willReturn(Optional.of(point));

        // when



    }
}
