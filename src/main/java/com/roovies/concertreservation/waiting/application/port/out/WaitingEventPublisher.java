package com.roovies.concertreservation.waiting.application.port.out;

import java.util.Map;

public interface WaitingEventPublisher {
    void notifyWaitingQueueStatusEvent(Long resourceId);
    void notifyAdmittedUsersEvent(Map<String, String> userKeyToAdmittedToken);
}
