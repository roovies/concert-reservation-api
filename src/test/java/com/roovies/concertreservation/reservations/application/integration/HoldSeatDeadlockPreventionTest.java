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
@DisplayName("좌석 예약 데드락 방지 테스트")
public class HoldSeatDeadlockPreventionTest extends RedisTestContainer {

    @Autowired
    private HoldSeatUseCase holdSeatUseCase;

    @Autowired
    private HoldSeatCachePort holdSeatCachePort;

    private static final Long SCHEDULE_ID = 3001L;
    private static final long DEADLOCK_TIMEOUT_MS = 10000L;

    @BeforeEach
    void setUp() {
        clearSeats(SCHEDULE_ID, List.of(1L, 2L, 3L));
    }

    @AfterEach
    void tearDown() {
        clearSeats(SCHEDULE_ID, List.of(1L, 2L, 3L));
    }

    @Test
    @DisplayName("정렬되지 않은 좌석 ID로 요청해도 데드락이 발생하지 않아야 한다")
    void 정렬되지_않은_좌석_요청시_데드락_방지() throws Exception {
        // given: 같은 좌석을 다른 순서로 요청
        Long userA = 100L;
        Long userB = 200L;

        HoldSeatCommand commandA = HoldSeatCommand.builder()
                .idempotencyKey(UUID.randomUUID().toString())
                .scheduleId(SCHEDULE_ID)
                .seatIds(List.of(3L, 1L, 2L))  // 역순
                .userId(userA)
                .build();

        HoldSeatCommand commandB = HoldSeatCommand.builder()
                .idempotencyKey(UUID.randomUUID().toString())
                .scheduleId(SCHEDULE_ID)
                .seatIds(List.of(1L, 2L, 3L))  // 정순
                .userId(userB)
                .build();

        // when: 동시 요청
        CountDownLatch latch = new CountDownLatch(2);
        long startTime = System.currentTimeMillis();

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

        HoldSeatResult resultA = futureA.get(DEADLOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        HoldSeatResult resultB = futureB.get(DEADLOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        long executionTime = System.currentTimeMillis() - startTime;

        // then: 한 명만 성공, 데드락 없음
        long successCount = Stream.of(resultA, resultB)
                .filter(Objects::nonNull)
                .count();

        assertThat(successCount).isEqualTo(1);
        assertThat(executionTime).isLessThan(DEADLOCK_TIMEOUT_MS);
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