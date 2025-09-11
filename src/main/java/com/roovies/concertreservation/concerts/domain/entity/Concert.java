package com.roovies.concertreservation.concerts.domain.entity;

import com.roovies.concertreservation.concerts.domain.enums.ConcertStatus;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
public class Concert { // Aggregate Root
    // 핵심 도메인 속성 (Concerts 테이블 - 불변)
    private final Long id;
    private final String title;
    private final String description;
    private final long minPrice;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    // Concert Aggregate Root로만 접근할 수 있는 ConcertSchedule Domain Entity
    private List<ConcertSchedule> schedules;

    public static Concert create(
            Long id, String title, String description, long minPrice,
            LocalDate startDate, LocalDate endDate, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        return new Concert(id, title, description, minPrice, startDate, endDate, createdAt, updatedAt);
    }

    private Concert(
            Long id, String title, String description, long minPrice,
            LocalDate startDate, LocalDate endDate, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.minPrice = minPrice;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 콘서트 상태 정보 반환
     * @param now
     * @return 콘서트 상태 정보
     */
    public ConcertStatus getStatus(LocalDate now) {
        if (now.isBefore(startDate)) return ConcertStatus.PREPARE;
        if (!now.isAfter(endDate)) return ConcertStatus.ONGOING;
        return ConcertStatus.ENDED;
    }

    /**
     * 특정 날짜의 콘서트 일정 정보 반환
     * @param date
     * @return 콘서트 일정 정보
     */
    public ConcertSchedule getSchedule(LocalDate date) {
        return schedules.stream()
                .filter(schedule -> schedule.getDate().equals(date))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 콘서트 일정이 존재하지 않습니다."));
    }

    /**
     * 콘서트 일정 목록을 반환
     * @return 읽기 전용 일정 목록 (수정 불가)
     */
    public List<ConcertSchedule> getSchedules() {
        return Collections.unmodifiableList(schedules);
    }

    /**
     * 콘서트의 스케줄을 일괄 설정
     * - 정렬되어 조회된 콘서트의 일정을 적재할 때 사용
     * @param schedules
     */
    public void setSchedules(List<ConcertSchedule> schedules) {
        // 스케줄 범위에 벗어난 일정이 있는지 확인
        boolean hasInvalid = schedules.stream()
                .anyMatch(schedule -> schedule.getDate().isBefore(startDate)
                        || schedule.getDate().isAfter(endDate));

        if (hasInvalid)
            throw new IllegalArgumentException("콘서트의 상세일정은 시작일과 종료일 범위 내에 있어야 합니다.");

        this.schedules = schedules;
    }

    /**
     * 콘서트 스케줄에 일정 추가
     * - 조회된 콘서트 일정에
     * @param schedule
     */
    public void addSchedule(ConcertSchedule schedule) {
        // 스케줄 범위에 벗어났는지 확인
        if (schedule.getDate().isBefore(startDate) || schedule.getDate().isAfter(endDate))
            throw new IllegalArgumentException("콘서트의 상세일정은 시작일과 종료일 범위 내에 있어야 합니다.");

        schedules.add(schedule);
        Collections.sort(schedules, (schedule1, schedule2) -> schedule1.getDate().compareTo(schedule2.getDate()) );
    }
}
