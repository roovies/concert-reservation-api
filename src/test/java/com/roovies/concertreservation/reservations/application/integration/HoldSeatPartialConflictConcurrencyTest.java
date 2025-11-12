package com.roovies.concertreservation.reservations.application.integration;

import com.roovies.concertreservation.reservations.application.dto.command.HoldSeatCommand;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.in.HoldSeatUseCase;
import com.roovies.concertreservation.reservations.application.port.out.HoldSeatCachePort;
import com.roovies.concertreservation.testcontainers.RedisTestContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("좌석 예약 부분 충돌 동시성 테스트")
public class HoldSeatPartialConflictConcurrencyTest extends RedisTestContainer {

    @Autowired
    private HoldSeatUseCase holdSeatUseCase;

    @Autowired
    private HoldSeatCachePort holdSeatCachePort;

    private static final Long SCHEDULE_ID = 2001L;

    @BeforeEach
    void setUp() {
        clearSeats(SCHEDULE_ID, List.of(1L, 2L, 3L, 4L));
    }

    @AfterEach
    void tearDown() {
        clearSeats(SCHEDULE_ID, List.of(1L, 2L, 3L, 4L));
    }

    @Test
    @DisplayName("겹치는 좌석 예약 시 먼저 획득한 사용자만 성공해야 한다")
    void 겹치는_좌석_예약시_한명만_성공() throws Exception {
        // given
        Long userA = 100L;
        Long userB = 200L;

        HoldSeatCommand commandA = HoldSeatCommand.builder()
                .idempotencyKey(UUID.randomUUID().toString())
                .scheduleId(SCHEDULE_ID)
                .seatIds(List.of(1L, 2L, 3L))
                .userId(userA)
                .build();

        HoldSeatCommand commandB = HoldSeatCommand.builder()
                .idempotencyKey(UUID.randomUUID().toString())
                .scheduleId(SCHEDULE_ID)
                .seatIds(List.of(2L, 3L, 4L))  // 좌석 2, 3이 겹침
                .userId(userB)
                .build();

        // when: 동시 요청
        CountDownLatch latch = new CountDownLatch(2);

        CompletableFuture<HoldSeatResult> futureA = CompletableFuture.supplyAsync(() -> {
            awaitLatch(latch);
            try {
                return holdSeatUseCase.holdSeat(commandA);
            } catch (Exception e) {
                return null;
            }
        });

        CompletableFuture<HoldSeatResult> futureB = CompletableFuture.supplyAsync(() -> {
            awaitLatch(latch);
            try {
                return holdSeatUseCase.holdSeat(commandB);
            } catch (Exception e) {
                return null;
            }
        });

        HoldSeatResult resultA = futureA.get(10, TimeUnit.SECONDS);
        HoldSeatResult resultB = futureB.get(10, TimeUnit.SECONDS);

        // then: 한 명만 성공
        long successCount = Stream.of(resultA, resultB)
                .filter(Objects::nonNull)
                .count();

        assertThat(successCount).isEqualTo(1);

        // 성공한 사용자의 좌석 검증
        if (resultA != null) {
            assertThat(resultA.seatIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        }
        if (resultB != null) {
            assertThat(resultB.seatIds()).containsExactlyInAnyOrder(2L, 3L, 4L);
        }
    }

    private void awaitLatch(CountDownLatch latch) {
        latch.countDown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void clearSeats(Long scheduleId, List<Long> seatIds) {
        try {
            holdSeatCachePort.deleteHoldSeatList(scheduleId, seatIds, 0L);
            Thread.sleep(50);
        } catch (Exception e) {
            // ignore
        }
    }
}