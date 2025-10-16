package com.roovies.concertreservation.waiting.infra.adapter.in.scheduler;

import com.roovies.concertreservation.waiting.application.port.in.WaitingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationWaitingScheduler {

    private final WaitingUseCase waitingUseCase;

    /**
     * 스케줄러1: 실시간 순번 정보 응답
     * - 5초마다 대기 중인 사용자에게 갱신된 순번 및 전체 대기자 수를 알려줌
     * - Redis Pub/Sub으로 브로드캐스트하여 모든 인스턴스에서 로컬에 가지고 있는 SSE 연결에만 전송할 수 있도록 수행
     * - Shed Lock을 적용하여 분산 환경에서 하나의 인스턴스에서만 실행되도록 수행
     */
    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(name = "notifyQueueStatus", lockAtMostFor = "4s", lockAtLeastFor = "2s")
    public void sendNotificationWaitingStatus() {
        log.debug("=== 대기열이 활성화된 스케줄별 대기자 순번 업데이트 수행 ===");
        waitingUseCase.publishActiveWaitingScheduleStatus();
        log.debug("=== 대기열이 활성화된 스케줄별 대기자 순번 업데이트 완료 ===");
    }
}
