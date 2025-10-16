package com.roovies.concertreservation.waiting.application.port.out;

public interface WaitingEventPublisher {
    void notifyWaitingQueueStatusEvent(Long resourceId);
}
