package com.roovies.concertreservation.ranking.infra.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roovies.concertreservation.concerts.infra.adapter.out.persistence.entity.ConcertJpaEntity;
import com.roovies.concertreservation.concerts.infra.adapter.out.persistence.entity.ConcertScheduleJpaEntity;
import com.roovies.concertreservation.concerts.infra.adapter.out.persistence.ConcertJpaRepository;
import com.roovies.concertreservation.concerts.infra.adapter.out.persistence.ConcertScheduleJpaRepository;
import com.roovies.concertreservation.ranking.application.port.out.RankingCachePort;
import com.roovies.concertreservation.ranking.domain.event.PaymentCompletedEvent;
import com.roovies.concertreservation.testcontainers.RedisTestContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 결제 완료 → Redis Pub/Sub → 실시간 랭킹 업데이트 전체 흐름 통합 테스트.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class PaymentToRankingIntegrationTest extends RedisTestContainer {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    @Qualifier("rankingRedisCache")
    private RankingCachePort rankingCachePort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConcertJpaRepository concertJpaRepository;

    @Autowired
    private ConcertScheduleJpaRepository concertScheduleJpaRepository;

    private static final String PAYMENT_COMPLETED_CHANNEL = "channel:payment:completed";
    private static final String REALTIME_RANKING_KEY = "ranking:realtime";

    private ConcertJpaEntity concert1;
    private ConcertJpaEntity concert2;
    private ConcertJpaEntity concert3;
    private ConcertScheduleJpaEntity schedule1;
    private ConcertScheduleJpaEntity schedule2;
    private ConcertScheduleJpaEntity schedule3;

    @BeforeEach
    void setUp() {
        // 콘서트 데이터 삽입
        concert1 = concertJpaRepository.save(
                ConcertJpaEntity.create("흠뻑쇼", "싸이")
        );
        concert2 = concertJpaRepository.save(
                ConcertJpaEntity.create("아이유 콘서트", "아이유")
        );
        concert3 = concertJpaRepository.save(
                ConcertJpaEntity.create("BTS 콘서트", "BTS")
        );

        // 스케줄 데이터 삽입
        schedule1 = concertScheduleJpaRepository.save(
                ConcertScheduleJpaEntity.create(concert1, LocalDate.now().plusDays(1), 100)
        );
        schedule2 = concertScheduleJpaRepository.save(
                ConcertScheduleJpaEntity.create(concert2, LocalDate.now().plusDays(2), 100)
        );
        schedule3 = concertScheduleJpaRepository.save(
                ConcertScheduleJpaEntity.create(concert3, LocalDate.now().plusDays(3), 100)
        );
    }

    @AfterEach
    void cleanup() {
        // 테스트 후 Redis 데이터 정리
        RScoredSortedSet<String> realtimeSet = redissonClient.getScoredSortedSet(REALTIME_RANKING_KEY);
        realtimeSet.clear();

        // DB 데이터 정리
        concertScheduleJpaRepository.deleteAll();
        concertJpaRepository.deleteAll();
    }

    @Test
    void 결제_완료_이벤트_발행시_실시간_랭킹이_업데이트되어야_한다() throws Exception {
        // given
        Long scheduleId = schedule1.getId();
        Long paymentId = 100L;
        Long userId = 1000L;

        // when
        PaymentCompletedEvent event = PaymentCompletedEvent.of(paymentId, scheduleId, userId);
        String message = objectMapper.writeValueAsString(event);

        RTopic topic = redissonClient.getTopic(PAYMENT_COMPLETED_CHANNEL);
        topic.publish(message);

        // then
        // 이벤트 리스너가 비동기로 처리하므로 잠시 대기
        await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(REALTIME_RANKING_KEY);
                    Double score = sortedSet.getScore(scheduleId.toString());
                    assertThat(score).isNotNull();
                    assertThat(score).isEqualTo(1.0);
                });
    }

    @Test
    void 동일_스케줄에_여러_결제_이벤트_발생시_스코어가_누적되어야_한다() throws Exception {
        // given
        Long scheduleId = schedule2.getId();

        // when
        RTopic topic = redissonClient.getTopic(PAYMENT_COMPLETED_CHANNEL);

        // 3개의 결제 이벤트 발행
        for (int i = 0; i < 3; i++) {
            PaymentCompletedEvent event = PaymentCompletedEvent.of((long) (100 + i), scheduleId, (long) (1000 + i));
            String message = objectMapper.writeValueAsString(event);
            topic.publish(message);
        }

        // then
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(REALTIME_RANKING_KEY);
                    Double score = sortedSet.getScore(scheduleId.toString());
                    assertThat(score).isNotNull();
                    assertThat(score).isEqualTo(3.0);
                });
    }

    private void publishPaymentEvent(RTopic topic, Long paymentId, Long scheduleId, Long userId) throws Exception {
        PaymentCompletedEvent event = PaymentCompletedEvent.of(paymentId, scheduleId, userId);
        String message = objectMapper.writeValueAsString(event);
        topic.publish(message);
    }
}