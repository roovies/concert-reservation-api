package com.roovies.concertreservation.waiting.domain.vo;

public record WaitingQueueStatus(
        String userKey,
        Integer rank,
        Integer totalWaiting
) {
}
