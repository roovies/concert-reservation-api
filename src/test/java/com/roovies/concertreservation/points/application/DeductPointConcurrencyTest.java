package com.roovies.concertreservation.points.application;


import com.roovies.concertreservation.points.application.dto.command.DeductPointCommand;
import com.roovies.concertreservation.points.application.port.out.PointCommandRepositoryPort;
import com.roovies.concertreservation.points.application.service.DeductPointService;
import com.roovies.concertreservation.points.domain.entity.Point;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import com.roovies.concertreservation.testcontainers.MySQLTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(MySQLTestContainer.class)
@DisplayName("포인트 차감 동시성 테스트")
public class DeductPointConcurrencyTest {

    @Autowired
    private DeductPointService deductPointService;

    @Autowired
    private PointCommandRepositoryPort pointCommandRepositoryPort;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 데이터 정리
        pointCommandRepositoryPort.deleteAll();
    }

    @Test
    void 동일한_회원의_포인트를_동시에_차감하더라도_결과값이_정상적이어야_한다() {
        // given
        // 초기 회원 데이터 저장
        Long userId = 1L;
        long initialAmount = 10_000L;
        Point initialPoint = Point.create(userId, Amount.of(initialAmount), LocalDateTime.now());
        pointCommandRepositoryPort.save(initialPoint);

        int threadCount = 50;
        long deductAmount = 100L;
        long expectedAmount = initialAmount - (threadCount * deductAmount); // 5000L

        // when
        List<CompletableFuture<Void>> futures =
                IntStream.range(0, threadCount)
                        .mapToObj(i -> CompletableFuture.runAsync(() -> {
                            try {
                                deductPointService.deduct(DeductPointCommand.builder()
                                                .userId(userId)
                                                .amount(deductAmount)
                                                .build());
                            } catch (Exception e) {
                                System.err.println("Thread " + i + " failed: " + e.getMessage());
                                // 예외 발생해도 테스트는 계속 진행
                            }
                        }))
                        .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // then
        Point result = pointCommandRepositoryPort.findById(userId)
                .orElseThrow(() -> new AssertionError("포인트를 찾을 수 없습니다."));

        System.out.println("=== 포인트 차감 동시성 테스트 결과 ===");
        System.out.println("초기 금액: " + initialAmount);
        System.out.println("예상 최종 금액: " + expectedAmount);
        System.out.println("실제 최종 금액: " + result.getAmount().value());

        if (result.getAmount().value() > expectedAmount) {
            System.out.println("❌ Race Condition 발생! 차감이 누락됨");
            System.out.println("누락된 차감 금액: " + (result.getAmount().value() - expectedAmount));
        } else {
            System.out.println("✅ 동시성 처리 정상");
        }

        assertThat(result.getAmount().value()).isEqualTo(expectedAmount);
    }

    @Test
    void 잔액_부족_시_일부_요청만_성공하고_나머지는_실패해야_한다() {
        // given
        // 초기 회원 데이터 저장
        Long userId = 1L;
        long initialAmount = 1000L;
        Point initialPoint = Point.create(userId, Amount.of(initialAmount), LocalDateTime.now());
        pointCommandRepositoryPort.save(initialPoint);

        int threadCount = 20;
        long deductAmount = 100L; // 총 2000L 차감 시도 (잔액 1000L)

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        List<CompletableFuture<Void>> futures =
                IntStream.range(0, threadCount)
                        .mapToObj(i -> CompletableFuture.runAsync(() -> {
                            try {
                                deductPointService.deduct(DeductPointCommand.builder()
                                        .userId(userId)
                                        .amount(deductAmount)
                                        .build());
                                successCount.incrementAndGet();
                            } catch (IllegalArgumentException e) {
                                if ("포인트가 부족합니다.".equals(e.getMessage())) {
                                    failCount.incrementAndGet();
                                }
                            } catch (Exception e) {
                                System.err.println("Unexpected error: " + e.getMessage());
                            }
                        }))
                        .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // then
        Point result = pointCommandRepositoryPort.findById(userId)
                .orElseThrow(() -> new AssertionError("포인트를 찾을 수 없습니다."));

        System.out.println("=== 잔액 부족 동시성 테스트 결과 ===");
        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());
        System.out.println("최종 잔액: " + result.getAmount().value());

        // 10번만 성공해야 함 (1000L / 100L = 10)
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(10);
        assertThat(result.getAmount().value()).isEqualTo(0L);
    }

    @Test
    void 여러_사용자가_동시에_각자의_포인트를_차감해도_정상적으로_처리되어야_한다() {
        // given
        int userCount = 10;
        long initialAmount = 5000L;
        long deductAmount = 1000L;

        // 각 사용자별 초기 포인트 설정
        IntStream.range(0, userCount).forEach(i -> {
            Point point = Point.create((long) i, Amount.of(initialAmount), LocalDateTime.now());
            pointCommandRepositoryPort.save(point);
        });

        // when - 각 사용자가 동시에 차감
        List<CompletableFuture<Void>> futures =
                IntStream.range(0, userCount)
                        .mapToObj(i -> CompletableFuture.runAsync(() -> {
                            deductPointService.deduct(DeductPointCommand.builder()
                                    .userId((long) i)
                                    .amount(deductAmount)
                                    .build());
                        }))
                        .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // then - 모든 사용자의 잔액 확인
        IntStream.range(0, userCount).forEach(i -> {
            Point result = pointCommandRepositoryPort.findById((long) i)
                    .orElseThrow(() -> new AssertionError("사용자 " + i + "의 포인트를 찾을 수 없습니다."));

            assertThat(result.getAmount().value())
                    .as("사용자 %d의 최종 잔액", i)
                    .isEqualTo(initialAmount - deductAmount);
        });

        System.out.println("✅ 다중 사용자 동시 차감 테스트 성공");
    }

}
