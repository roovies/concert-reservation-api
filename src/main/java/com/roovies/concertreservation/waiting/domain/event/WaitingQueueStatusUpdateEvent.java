package com.roovies.concertreservation.waiting.domain.event;

public record WaitingQueueStatusUpdateEvent(
        Long scheduleId
        // 추후 확장성을 위해 객체로 수행
) {
}
