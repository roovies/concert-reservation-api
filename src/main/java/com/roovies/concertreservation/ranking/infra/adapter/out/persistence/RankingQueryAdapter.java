package com.roovies.concertreservation.ranking.infra.adapter.out.persistence;

import com.roovies.concertreservation.ranking.application.dto.query.GetWeeklyPaymentCountQuery;
import com.roovies.concertreservation.ranking.application.port.out.RankingQueryRepositoryPort;
import com.roovies.concertreservation.ranking.domain.entity.ConcertRanking;
import com.roovies.concertreservation.reservations.domain.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 랭킹 조회 레포지토리 어댑터.
 * <p>
 * Spring Data JPA Repository를 사용하여 Port 인터페이스를 구현한다.
 */
@Slf4j
@Component
@Qualifier("rankingQueryRepository")
@RequiredArgsConstructor
public class RankingQueryAdapter implements RankingQueryRepositoryPort {

    private final RankingQueryRepository rankingQueryRepository;

    /**
     * 주간 결제 건수를 기준으로 상위 5개 스케줄을 조회한다.
     *
     * @param query 주간 결제 건수 조회 쿼리
     * @return 주간 랭킹 목록 (1-5위)
     */
    @Override
    public List<ConcertRanking> findWeeklyTop5(GetWeeklyPaymentCountQuery query) {
        List<Object[]> results = rankingQueryRepository.findWeeklyTop5RawData(
                ReservationStatus.CONFIRMED,
                query.startDate(),
                query.endDate()
        );

        // 결과를 ConcertRanking 도메인 엔티티로 변환
        List<ConcertRanking> rankings = new ArrayList<>();
        int rank = 1;

        for (Object[] row : results) {
            if (rank > 5) break; // TOP 5만

            ConcertRanking ranking = ConcertRanking.createWeekly(
                    (Long) row[0],          // scheduleId
                    (Long) row[1],          // concertId
                    (String) row[2],        // concertTitle
                    (Long) row[3],          // paymentCount
                    rank                    // rank
            );

            log.debug("[RankingQueryAdapter] 주간 랭킹 조회 - rank: {}, scheduleId: {}, count: {}",
                    rank, ranking.getScheduleId(), ranking.getPaymentCount());

            rankings.add(ranking);
            rank++;
        }

        return rankings;
    }

    /**
     * 스케줄 ID로 콘서트 정보를 조회한다.
     *
     * @param scheduleId 스케줄 ID
     * @return 콘서트 ID와 제목
     */
    @Override
    public ScheduleConcertInfo findConcertInfoByScheduleId(Long scheduleId) {
        Object[] row = rankingQueryRepository.findConcertInfoByScheduleIdRaw(scheduleId)
                .orElseThrow(() -> new NoSuchElementException("스케줄을 찾을 수 없습니다. scheduleId: " + scheduleId));

        return new ScheduleConcertInfo(
                (Long) row[0],      // scheduleId
                (Long) row[1],      // concertId
                (String) row[2]     // concertTitle
        );
    }
}