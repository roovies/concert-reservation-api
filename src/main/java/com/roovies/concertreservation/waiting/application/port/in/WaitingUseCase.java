package com.roovies.concertreservation.waiting.application.port.in;

import com.roovies.concertreservation.waiting.application.dto.result.EnterQueueResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface WaitingUseCase {
    /**
     * 대기열 진입 또는 즉시 입장
     */
    EnterQueueResult enterOrWaitQueue(Long userId, Long resourceId);

    /**
     *  SSE 연결 수립 (대기열 진입 시 클라이언트에서 호출)
     */
    SseEmitter subscribeToQueue(Long userId, Long resourceId, String userKey);

    /**
     * 활성화된 리소스별 대기자 순번 갱신 이벤트 처리
     */
    void publishActiveWaitingScheduleStatus();

    /**
     * 리소스ID에 대기 중인 대기자들 순번 알림 전송
     */
    void notifyWaitingQueueStatus(Long resourceId);

    /**
     * 입장처리된 대기자들에게 입장 처리 알림 전송
     */
    void notifyAdmittedUsers(Map<String, String> userKeyToAdmittedToken);

    /**
     *
     */
    void admitUsersInActiveWaitingSchedules();
}
