package com.roovies.concertreservation.reservations.application;

import com.roovies.concertreservation.reservations.application.dto.command.HoldSeatCommand;
import com.roovies.concertreservation.reservations.application.port.in.HoldSeatUseCase;
import com.roovies.concertreservation.testcontainers.RedisTestContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("좌석 예약 동시성 테스트")
public class HoldSeatConcurrencyTest extends RedisTestContainer {

    @Autowired
    private HoldSeatUseCase holdSeatUseCase;

    @Test
    void 같은_좌석에_대해_동시_예약_요청시_한명만_성공해야_한다() {
        // given
        Long scheduleId = 1L;
        List<Long> seatIds = List.of(1L, 2L, 3L);

        int threadCount = 100;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    Long userId = 1000L + i;
                    String idempotencyKey = UUID.randomUUID().toString();
                    HoldSeatCommand command = HoldSeatCommand.builder()
                            .idempotencyKey(idempotencyKey)
                            .scheduleId(scheduleId)
                            .seatIds(seatIds)
                            .userId(userId)
                            .build();

                    try {
                        holdSeatUseCase.holdSeat(command);
                        successCount.incrementAndGet();
                    } catch (IllegalStateException e) {
                        if (e.getMessage().contains("다른 사용자가 이미 예약 중")) {
                            failCount.incrementAndGet();
                        }
                    }
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);
    }

}
