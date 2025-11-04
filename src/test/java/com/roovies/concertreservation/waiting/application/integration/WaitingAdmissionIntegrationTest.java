package com.roovies.concertreservation.waiting.application.integration;

import com.roovies.concertreservation.testcontainers.RedisTestContainer;
import com.roovies.concertreservation.waiting.application.dto.result.EnterQueueResult;
import com.roovies.concertreservation.waiting.application.port.in.WaitingUseCase;
import com.roovies.concertreservation.waiting.application.port.out.WaitingCachePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(RedisTestContainer.class)
@DisplayName("대기열 입장 처리 통합 테스트")
public class WaitingAdmissionIntegrationTest {

    @Autowired
    @Qualifier("reservationWaitingService")
    private WaitingUseCase waitingUseCase;

    @Autowired
    @Qualifier("reservationWaitingRedis")
    private WaitingCachePort waitingCachePort;

    private static final Long TEST_SCHEDULE_ID = 888L;

    @BeforeEach
    void setUp() {
        // 테스트 수행 전 대기열에서 테스트 스케줄 제거
        waitingCachePort.removeActiveWaitingScheduleId(TEST_SCHEDULE_ID);
    }

    @Test
    void 대기열에_있는_사용자를_입장처리_할_수_있어야_한다() {
        // given - 세마포어 가득 채움 (100명)
        IntStream.range(0, 100)
                .forEach(i -> waitingUseCase.enterOrWaitQueue((long) i, TEST_SCHEDULE_ID));

        // 대기열에 20명 추가
        List<EnterQueueResult> results = IntStream.range(100, 120)
                .mapToObj(i -> waitingUseCase.enterOrWaitQueue((long) i, TEST_SCHEDULE_ID))
                .toList();

        // when & then
        // 모두 대기 상태인지 확인
        assertThat(results).allMatch(r -> !r.admitted());

        // 입장처리 실행 후 대기열 크기 확인
        waitingUseCase.admitUsersInActiveWaitingSchedules();
        int queueSize = waitingCachePort.getWaitingQueueSize(TEST_SCHEDULE_ID);
        assertThat(queueSize).isEqualTo(20);
    }
}
