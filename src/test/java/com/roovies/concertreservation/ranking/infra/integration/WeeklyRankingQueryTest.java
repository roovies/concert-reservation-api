package com.roovies.concertreservation.ranking.infra.integration;

import com.roovies.concertreservation.ranking.infra.adapter.out.persistence.RankingQueryRepository;
import com.roovies.concertreservation.reservations.domain.enums.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class WeeklyRankingQueryTest {
    @Autowired
    private RankingQueryRepository repository;

    @Test
    void 주간_TOP5_조회() {
        // Given: 테스트 데이터 생성
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        // When
        List<Object[]> results = repository.findWeeklyTop5RawData(
                ReservationStatus.CONFIRMED, startDate, endDate
        );

        // Then
        assertThat(results).hasSizeLessThanOrEqualTo(5);
        // 내림차순 확인
        if (results.size() > 1) {
            Long firstCount = (Long) results.get(0)[3];
            Long secondCount = (Long) results.get(1)[3];
            assertThat(firstCount).isGreaterThanOrEqualTo(secondCount);
        }
    }
}
