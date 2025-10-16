package com.roovies.concertreservation.waiting.application.port.in;

import com.roovies.concertreservation.waiting.application.dto.result.EnterQueueResult;

public interface WaitingUseCase {
    /**
     * 대기열 진입 또는 즉시 입장
     */
    EnterQueueResult enterOrWaitQueue(Long userId, Long resourceId);
}
