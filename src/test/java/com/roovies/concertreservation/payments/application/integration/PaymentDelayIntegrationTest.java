package com.roovies.concertreservation.payments.application.integration;

import com.roovies.concertreservation.payments.application.dto.command.PayReservationCommand;
import com.roovies.concertreservation.payments.application.port.in.PayReservationUseCase;
import com.roovies.concertreservation.reservations.application.dto.command.HoldSeatCommand;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.in.HoldSeatUseCase;
import com.roovies.concertreservation.testcontainers.RedisTestContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@DisplayName("예약 지연 결제 테스트")
public class PaymentDelayIntegrationTest extends RedisTestContainer {

    @Autowired
    private HoldSeatUseCase holdSeatUseCase;

    @Autowired
    private PayReservationUseCase payReservationUseCase;

    @Autowired
    private RedissonClient redissonClient;

    @Test
    void 좌석_홀딩_후_TTL_만료시_결제가_실패해야_한다() throws Exception {
        // given: 좌석 홀딩
        HoldSeatCommand holdCommand = HoldSeatCommand.builder()
                .idempotencyKey(UUID.randomUUID().toString())
                .scheduleId(1L)
                .seatIds(List.of(1L, 2L))
                .userId(100L)
                .build();

        HoldSeatResult holdResult = holdSeatUseCase.holdSeat(holdCommand);

        // when: Redis TTL을 1초로 단축
        redissonClient.getBucket("hold:1:1").expire(Duration.ofSeconds(1));
        redissonClient.getBucket("hold:1:2").expire(Duration.ofSeconds(1));
        Thread.sleep(1100); // 1.1초 대기

        // then: 결제 시도 시 실패
        PayReservationCommand payCommand = PayReservationCommand.builder()
                .idempotencyKey(UUID.randomUUID().toString())
                .scheduleId(1L)
                .seatIds(List.of(1L, 2L))
                .userId(100L)
                .payForAmount(50000L)
                .discountAmount(0L)
                .build();

        assertThatThrownBy(() -> payReservationUseCase.payReservation(payCommand))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("예약 대기 중인 좌석이 없습니다.");
    }

}
