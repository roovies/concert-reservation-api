package com.roovies.concertreservation.reservations.application.integration;

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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("좌석 예약 타임아웃 테스트")
public class HoldSeatTimeoutIntegrationTest extends RedisTestContainer {

    @Autowired
    private HoldSeatUseCase holdSeatUseCase;

    @Autowired
    private RedissonClient redissonClient;

    @Test
    void 좌석_홀딩_TTL_만료_후_다른_사용자가_예약할_수_있어야_한다() throws Exception {
        // given: 사용자A가 좌석 홀딩
        Long userA = 100L;
        Long userB = 200L;
        List<Long> seatIds = List.of(1L, 2L);

        HoldSeatCommand holdCommandA = HoldSeatCommand.builder()
                .idempotencyKey(UUID.randomUUID().toString())
                .scheduleId(1L)
                .seatIds(seatIds)
                .userId(userA)
                .build();

        HoldSeatResult holdResultA = holdSeatUseCase.holdSeat(holdCommandA);
        assertThat(holdResultA.userId()).isEqualTo(userA);

        // when: TTL 만료 대기 (Redis EXPIRE 명령으로 1초로 단축)
        redissonClient.getBucket("hold:1:1").expire(Duration.ofSeconds(1));
        redissonClient.getBucket("hold:1:2").expire(Duration.ofSeconds(1));
        Thread.sleep(1100); // 1.1초 대기

        // then: 사용자B가 동일 좌석 홀딩 성공
        HoldSeatCommand holdCommandB = HoldSeatCommand.builder()
                .idempotencyKey(UUID.randomUUID().toString())
                .scheduleId(1L)
                .seatIds(seatIds)
                .userId(userB)
                .build();

        HoldSeatResult holdResultB = holdSeatUseCase.holdSeat(holdCommandB);
        assertThat(holdResultB.userId()).isEqualTo(userB);
        assertThat(holdResultB.seatIds()).containsExactlyElementsOf(seatIds);
    }
}
