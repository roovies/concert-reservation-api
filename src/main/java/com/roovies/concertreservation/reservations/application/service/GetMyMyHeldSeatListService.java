package com.roovies.concertreservation.reservations.application.service;

import com.roovies.concertreservation.reservations.application.dto.query.GetHeldSeatListQuery;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.in.GetMyHeldSeatListUseCase;
import com.roovies.concertreservation.reservations.application.port.out.HoldSeatCachePort;
import com.roovies.concertreservation.reservations.application.port.out.ReservationVenueGatewayPort;
import com.roovies.concertreservation.reservations.domain.entity.HoldSeat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 홀딩 좌석 조회 서비스 구현체.
 * <p>
 * 사용자가 특정 콘서트 스케줄에서 임시로 홀딩한 좌석 목록과 총 금액, TTL 정보를 조회한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyMyHeldSeatListService implements GetMyHeldSeatListUseCase {

    private final HoldSeatCachePort holdSeatCachePort;
    private final ReservationVenueGatewayPort reservationVenueGatewayPort;

    /**
     * 사용자가 홀딩한 좌석 목록을 조회한다.
     * <p>
     * - 캐시에서 사용자의 홀딩 좌석 목록을 가져온다.<br>
     * - 좌석이 존재하지 않으면 빈 결과를 반환한다.<br>
     * - 좌석이 존재하면 TTL과 총 금액을 포함한 결과를 반환한다.
     *
     * @param query 홀딩 좌석 조회 요청 객체
     * @return 홀딩 좌석 조회 결과
     */
    @Override
    public HoldSeatResult getMyHeldSeatList(GetHeldSeatListQuery query) {
        log.info("[GetHeldSeatListService] 사용자의 홀딩 좌석 목록 조회 - userId: {}, scheduleId: {}, seatIds: {}",
                query.userId(), query.scheduleId(), query.seatIds());
        
        List<HoldSeat> heldSeats = holdSeatCachePort.getHoldSeatList(query.scheduleId(), query.seatIds(), query.userId());
        if (heldSeats.isEmpty())
            return createEmptyResult(query);

        return createHoldSeatResult(query, heldSeats);
    }

    /**
     * 홀딩 좌석이 없는 경우 빈 결과를 생성한다.
     *
     * @param query 홀딩 좌석 조회 요청 객체
     * @return 빈 홀딩 좌석 결과
     */
    private HoldSeatResult createEmptyResult(GetHeldSeatListQuery query) {
        return HoldSeatResult.builder()
                .scheduleId(query.scheduleId())
                .seatIds(Collections.emptyList())
                .userId(query.userId())
                .totalPrice(0L)
                .ttlSeconds(0L)
                .build();
    }


    /**
     * 홀딩 좌석이 존재하는 경우 TTL과 총 금액을 포함한 결과를 생성한다.
     *
     * @param query     홀딩 좌석 조회 요청 객체
     * @param heldSeats 캐시에서 조회한 홀딩 좌석 목록
     * @return 홀딩 좌석 결과
     */
    private HoldSeatResult createHoldSeatResult(GetHeldSeatListQuery query, List<HoldSeat> heldSeats) {
        long ttl = getTTL(query);
        List<Long> seatIds = extractSeatIds(heldSeats);
        Long scheduleId = heldSeats.get(0).getScheduleId();
        Long totalPrice = reservationVenueGatewayPort.getTotalSeatPrice(seatIds);

        return HoldSeatResult.builder()
                .scheduleId(scheduleId)
                .seatIds(seatIds)
                .userId(query.userId())
                .totalPrice(totalPrice)
                .ttlSeconds(ttl)
                .build();
    }

    /**
     * 홀딩 좌석의 TTL(남은 유효 시간)을 조회한다.
     *
     * @param query 홀딩 좌석 조회 요청 객체
     * @return TTL(초)
     */
    private long getTTL(GetHeldSeatListQuery query) {
        return holdSeatCachePort.getHoldTTLSeconds(
                query.scheduleId(), query.seatIds(), query.userId());
    }

    /**
     * 홀딩 좌석 목록에서 좌석 ID만 추출한다.
     *
     * @param heldSeats 홀딩 좌석 목록
     * @return 좌석 ID 목록
     */
    private List<Long> extractSeatIds(List<HoldSeat> heldSeats) {
        return heldSeats.stream()
                .map(HoldSeat::getSeatId)
                .toList();
    }
}
