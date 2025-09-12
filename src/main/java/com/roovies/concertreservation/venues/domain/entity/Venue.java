package com.roovies.concertreservation.venues.domain.entity;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
public class Venue { // Aggregate Root
    private final Long id;
    private final String name;
    private final int totalSeats;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Venue Aggregate Root로만 접근할 수 있는 VenueSeats Domain Entity
    private List<VenueSeat> seats;

    public static Venue create(Long id, String name, int totalSeats, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Venue(id, name, totalSeats, createdAt, updatedAt);
    }

    private Venue(Long id, String name, int totalSeats, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.totalSeats = totalSeats;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 공연장 좌석 목록을 반환
     * @return 읽기 전용 좌석 목록 (수정 불가)
     */
    public List<VenueSeat> getSeats() {
        return Collections.unmodifiableList(seats);
    }

    /**
     * 공연장의 좌석을 일괄 설정
     * - 정렬되어 조회된 콘서트의 일정을 적재할 때 사용
     * @param seats
     */
    public void setSeats(List<VenueSeat> seats) {
        this.seats = seats;
    }
}
