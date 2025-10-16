package com.roovies.concertreservation.waiting.domain.vo;

public record WaitingQueueStatus(
        Integer rank,
        Integer totalWaiting
) {
}
