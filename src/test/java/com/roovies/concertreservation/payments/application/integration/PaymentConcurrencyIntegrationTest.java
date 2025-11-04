package com.roovies.concertreservation.payments.application.integration;


import com.roovies.concertreservation.payments.application.dto.command.PayReservationCommand;
import com.roovies.concertreservation.payments.application.dto.result.PayReservationResult;
import com.roovies.concertreservation.payments.application.port.in.PayReservationUseCase;
import com.roovies.concertreservation.reservations.application.dto.command.HoldSeatCommand;
import com.roovies.concertreservation.reservations.application.port.in.HoldSeatUseCase;
import com.roovies.concertreservation.testcontainers.MySQLTestContainer;
import com.roovies.concertreservation.testcontainers.RedisTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(MySQLTestContainer.class)
public class PaymentConcurrencyIntegrationTest extends RedisTestContainer {
    @Autowired
    private PayReservationUseCase payReservationUseCase;

    @Autowired
    private HoldSeatUseCase holdSeatUseCase;  // 추가

    @Test
    void 동일한_멱등성키로_동시에_결제_요청시_한건만_성공해야_한다() {
        // Given: 먼저 좌석 홀딩 수행
        String holdIdempotencyKey = "hold-" + System.currentTimeMillis();
        Long userId = 1L;
        Long scheduleId = 1L;
        List<Long> seatIds = Arrays.asList(1L, 2L);

        HoldSeatCommand holdCommand = HoldSeatCommand.builder()
                .idempotencyKey(holdIdempotencyKey)
                .scheduleId(scheduleId)
                .seatIds(seatIds)
                .userId(userId)
                .build();

        holdSeatUseCase.holdSeat(holdCommand);  // 좌석 홀딩 먼저 실행

        // Given: 결제 요청 준비
        String paymentIdempotencyKey = "payment-" + System.currentTimeMillis();
        int threadCount = 100;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateRequestCount = new AtomicInteger(0);

        PayReservationCommand command = PayReservationCommand.builder()
                .idempotencyKey(paymentIdempotencyKey)
                .userId(userId)
                .scheduleId(scheduleId)
                .seatIds(seatIds)
                .payForAmount(10000L)
                .discountAmount(0L)
                .build();

        // When: 100개 스레드가 동시에 결제 시도
        List<CompletableFuture<Void>> futures =
                IntStream.range(0, threadCount)
                        .mapToObj(i -> CompletableFuture.runAsync(() -> {
                            try {
                                PayReservationResult result = payReservationUseCase.payReservation(command);
                                successCount.incrementAndGet();
                            } catch (IllegalArgumentException e) {
                                if (e.getMessage().contains("중복된 요청입니다")) {
                                    duplicateRequestCount.incrementAndGet();
                                }
                            } catch (IllegalStateException e) {
                                // 좌석 관련 예외는 무시 (테스트 목적과 무관)
                            }
                        }))
                        .toList();

        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        all.join();

        // Then: 1건 성공, 99건 "중복 요청" 예외
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(duplicateRequestCount.get()).isEqualTo(threadCount - 1);
    }
}
