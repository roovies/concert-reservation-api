package com.roovies.concertreservation.points.application;

import com.roovies.concertreservation.points.application.dto.command.ChargePointCommand;
import com.roovies.concertreservation.points.application.port.out.PointRepositoryPort;
import com.roovies.concertreservation.points.application.service.ChargePointService;
import com.roovies.concertreservation.points.domain.entity.Point;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class) // Testcontainers 설정 포함
public class ChargePointConcurrencyTest {

    @Autowired
    private ChargePointService chargePointService;

    @Autowired
    private PointRepositoryPort pointRepositoryPort;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 데이터 정리
        pointRepositoryPort.deleteAll();
    }

    @Test
    void 동일한_회원의_포인트를_동시에_충전하더라도_결과값이_정상적이어야_한다() throws InterruptedException {
        // given
        // 초기 회원 데이터 저장
        Long userId = 1L;
        Point initialPoint = Point.create(userId, Amount.of(1000L), LocalDateTime.now());
        pointRepositoryPort.save(initialPoint);

        int threadCount = 50;
        long chargeAmount = 100L;

        // when
        List<CompletableFuture<Void>> futures =
                IntStream.range(0, threadCount)
                        .mapToObj(i -> CompletableFuture.runAsync(() -> {
                            try {
                                chargePointService.execute(ChargePointCommand.builder()
                                                .userId(userId)
                                                .amount(chargeAmount)
                                                .build());
                            } catch (Exception e) {
                                System.err.println("Thread " + i + " failed: " + e.getMessage());
                                // 예외 발생해도 테스트는 계속 진행
                            }
                        }))
                        .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // then
        Optional<Point> result = pointRepositoryPort.findById(userId);
        if (result.isPresent()) {
            Point point = result.get();
            long expectedAmount = 1000L + (threadCount * chargeAmount); // 6000L

            System.out.println("=== 동시성 테스트 결과 ===");
            System.out.println("예상 금액: " + expectedAmount);
            System.out.println("실제 금액: " + point.getAmount().value());

            if (point.getAmount().value() < expectedAmount) {
                System.out.println("❌ Race Condition 발생! 동시성 문제 있음");
                System.out.println("손실된 금액: " + (expectedAmount - point.getAmount().value()));
            } else {
                System.out.println("✅ 동시성 처리 정상");
            }

            assertThat(point.getAmount().value()).isEqualTo(expectedAmount);
        }
    }
}
