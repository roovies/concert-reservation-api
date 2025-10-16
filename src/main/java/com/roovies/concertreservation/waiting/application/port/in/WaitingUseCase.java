package com.roovies.concertreservation.waiting.application.port.in;

import com.roovies.concertreservation.waiting.application.dto.result.EnterQueueResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface WaitingUseCase {
    /**
     * 대기열 진입 또는 즉시 입장
     */
    EnterQueueResult enterOrWaitQueue(Long userId, Long resourceId);

    /**
     *  SSE 연결 수립 (대기열 진입 시 클라이언트에서 호출)
     */
    SseEmitter subscribeToQueue(Long userId, Long resourceId, String userKey);
}
