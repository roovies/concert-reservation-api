package com.roovies.concertreservation.waiting.application.service;

import com.roovies.concertreservation.shared.util.security.JwtUtils;
import com.roovies.concertreservation.waiting.application.dto.result.EnterQueueResult;
import com.roovies.concertreservation.waiting.application.port.in.WaitingUseCase;
import com.roovies.concertreservation.waiting.application.port.out.EmitterRepositoryPort;
import com.roovies.concertreservation.waiting.application.port.out.WaitingCachePort;
import com.roovies.concertreservation.waiting.application.port.out.WaitingEventPublisher;
import com.roovies.concertreservation.waiting.domain.vo.WaitingQueueEntry;
import com.roovies.concertreservation.waiting.domain.vo.WaitingQueueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Qualifier("reservationWaitingService")
@Transactional
@RequiredArgsConstructor
public class ReservationWaitingService implements WaitingUseCase {

    private static final long SSE_TIMOUT = 600000L; // 10분

    private final JwtUtils jwtUtils;

    @Qualifier("reservationWaitingRedis")
    private final WaitingCachePort waitingCachePort;

    @Qualifier("reservationWaitingEmitterRepository")
    private final EmitterRepositoryPort emitterRepositoryPort;

    @Qualifier("reservationRedisEventPublisher")
    private final WaitingEventPublisher waitingEventPublisher;


    @Override
    public EnterQueueResult enterOrWaitQueue(Long userId, Long scheduleId) {
        // 대기열이 활성화된 스케줄인지 확인
        boolean isActivate = waitingCachePort.hasActiveWaitingQueue(scheduleId);

        // 대기열이 비활성화 상태일 때만 세마포어 획득 시도
        if (!isActivate && waitingCachePort.tryAcquirePermit(scheduleId)) {
            // 세마포어 획득 시 즉시 입장 - 입장 토큰 발급
            String admittedToken = issueAdmittedToken(userId, scheduleId);
            return EnterQueueResult.builder()
                    .admitted(true)
                    .admittedToken(admittedToken)
                    .build();
        }

        // 대기열 입장 (대기열 활성화 상태이거나 세마포어 획득 실패 시)
        WaitingQueueStatus status = enterQueue(userId, scheduleId);
        Integer rank = status.rank();
        Integer totalWaiting = status.totalWaiting();

        return EnterQueueResult.builder()
                .admitted(false)
                .rank(rank != null ? rank + 1 : null)  // ZRANK는 0부터 시작하므로 +1
                .totalWaiting(totalWaiting)
                .userKey(status.userKey())
                .build();
    }

    @Override
    public SseEmitter subscribeToQueue(Long userId, Long scheduleId, String userKey) {
        SseEmitter emitter = new SseEmitter(SSE_TIMOUT);
        validateUserKey(userId, userKey);

        emitterRepositoryPort.saveEmitterByUserKey(userKey, emitter);

        // 이벤트 등록
        // 타임아웃 시
        emitter.onTimeout(() -> {
            log.info("SSE 타임아웃: {}", userKey);
            waitingCachePort.removeWaitingQueue(scheduleId, userKey);
            emitterRepositoryPort.removeEmitterByUserKey(userKey);
        });

        // 정상 완료 시
        emitter.onCompletion(() -> {
            log.info("SSE 정상 완료: {}", userKey);
            emitterRepositoryPort.removeEmitterByUserKey(userKey);
        });

        // 에러 발생 시
        emitter.onError(e -> {
            log.error("SSE 에러: {}: {}", userKey, e.getMessage());
            waitingCachePort.removeWaitingQueue(scheduleId, userKey);
            emitterRepositoryPort.removeEmitterByUserKey(userKey);
        });

        // 초기 연결 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established"));
        } catch (Exception e) {
            log.error("Failed to send initial SSE message", e);
        }

        return emitter;
    }

    @Override
    public void publishActiveWaitingScheduleStatus() {
        // 대기열이 활성화 되어 있는 스케줄ID 목록 조회
        Set<String> scheduleIds = waitingCachePort.getActiveWaitingScheduleIds();
        if (scheduleIds.isEmpty()) {
            log.debug("대기열이 발생한 스케줄이 존재하지 않음");
            return;
        }

        for (String scheduleId : scheduleIds) {
            waitingEventPublisher.notifyWaitingQueueStatusEvent(Long.parseLong(scheduleId));
            log.debug("대기자 실시간 순번 갱신 이벤트 발행 요청: scheduleId = {}", scheduleId);
        }
    }

    @Override
    public void notifyWaitingQueueStatus(Long scheduleId) {
        // 해당 스케줄ID의 전체 대기자 목록 조회
        Collection<String> userKeys = waitingCachePort.getActiveWaitingUserKeys(scheduleId);
        if (userKeys.isEmpty()) {
            log.debug("스케줄에 대기중인 사용자가 없음: scheduleId = {}", scheduleId);

            // 대기자가 없는데 활성화된 스케줄이므로 활성화 목록에서 제거
            waitingCachePort.removeActiveWaitingScheduleId(scheduleId);
            return;
        }

        // Local emitterRepository에만 존재하는 userKeys만 필터링
        List<String> localUserKeys = filterUserKeys(userKeys);
        if (localUserKeys.isEmpty()) {
            log.debug("현재 인스턴스에 연결된 대기자가 없음: scheduleId = {}, userKey = {}", scheduleId, localUserKeys);
            return;
        }

        log.debug("실시간 순번 알림 전송: scheduleId = {}, 대상자 수 = {}", scheduleId, localUserKeys.size());

        for (String userKey : localUserKeys) {
            SseEmitter emitter = null;
            try {
                emitter = emitterRepositoryPort.getEmitterByUserKey(userKey);
                if (emitter == null)
                    continue;

                // 각 사용자별 대기 정보 조회
                WaitingQueueStatus status = waitingCachePort.getRankAndTotalWaitingCount(scheduleId, userKey);
                Integer rank = status.rank();
                Integer totalWaiting = status.totalWaiting();

                if (rank == null) {
                    log.warn("사용자의 순번을 찾을 수 없음 (ZRANK): scheduleId = {}, userKey = {}", userKey, scheduleId);
                    continue;
                }

                int currentPosition = rank + 1; // ZRANK는 0부터 시작하므로 +1

                // TODO: SSE 전송 시 DTO를 만들면 단일 사용에 많은 클래스 파일을 만들 것 같아 Map을 써봤는데 괜찮을까?
                Map<String, Object> response = Map.of(
                        "scheduleId", scheduleId,
                        "rank", currentPosition,
                        "totalWaiting", totalWaiting,
                        "userKey", userKey,
                        "timestamp", LocalDateTime.now()
                );

                emitter.send(
                        SseEmitter.event()
                                .name("reservation-waiting-status-update")
                                .data(response)
                );

                log.debug("순번 알림 전송 완료: userKey = {}, position = {}/{}", userKey, currentPosition, totalWaiting);
            } catch (IOException e) {
                log.warn("SSE 전송 실패: userKey = {}", userKey, e);
                emitterRepositoryPort.removeEmitterByUserKey(userKey);
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("순번 알림 처리 중 오류 발생: userKey = {}", userKey, e);
            }
        }
    }

    @Override
    public void notifyAdmittedUsers(Map<String, String> userKeyToAdmittedToken) {
        for (Map.Entry<String, String> entry : userKeyToAdmittedToken.entrySet()) {
            String userKey = entry.getKey();
            String admitToken = entry.getValue();

            SseEmitter emitter = emitterRepositoryPort.getEmitterByUserKey(userKey);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("admit")
                            .data(admitToken));

                    log.info("입장처리 SSE 알림 전송 완료: userKey = {}", userKey);
                    emitterRepositoryPort.removeEmitterByUserKey(userKey);
                } catch (Exception e) {
                    log.error("입장처리 SSE 알림 전송 실패: userKey = {}", userKey, e);
                    // 재시도 로직 구현 커스터마이징 필요
                    // 우선 현재 로직은 재시도 로직 없이 바로 Local Emitter Map에서 삭제하도록 함
                    emitterRepositoryPort.removeEmitterByUserKey(userKey);
                }
            }
        }
    }

    @Override
    public void admitUsersInActiveWaitingSchedules() {
        Set<String> scheduleIds = waitingCachePort.getActiveWaitingScheduleIds();
        if (scheduleIds.isEmpty()) {
            log.debug("대기열이 발생한 스케줄이 존재하지 않음");
            return;
        }

        // 분산락으로 다중 인스턴스 및 멀티스레드(병렬처리)에서 동시성 제어
        scheduleIds.parallelStream().forEach(scheduleIdStr -> {
            Long scheduleId = Long.parseLong(scheduleIdStr);
            boolean admitLockAcquired = waitingCachePort.tryAcquireAdmitLock(scheduleId);
            if (admitLockAcquired) {
                Map<String, String> localUserToAdmittedToken = new HashMap<>();
                Map<String, String> remoteUserToAdmittedToken = new HashMap<>();

                try {
                    // 가용 가능한 Permit 수 확인
                    final int availablePermits = waitingCachePort.getAvailablePermits(scheduleId);
                    if (availablePermits <= 0) {
                        log.debug("해당 스케줄에 사용 가능한 Permit이 없음: scheduleId = {}", scheduleId);
                        return;
                    }

                    // 해당 스케줄 대기자 수 조회
                    final int waitingQueueSize = waitingCachePort.getWaitingQueueSize(scheduleId);
                    if (waitingQueueSize <= 0) {
                        log.debug("해당 스케줄에 대기자가 없음: scheduleId = {}", scheduleId);

                        // 대기자가 존재하지 않으니 활성 대기열 목록에서 해당 스케줄ID를 제거
                        waitingCachePort.removeActiveWaitingScheduleId(scheduleId);
                        log.debug("스케줄에 실제 대기자가 없어 활성 대기열 목록에서 제거됨: scheduleId = {}", scheduleId);
                        return;
                    }

                    // 입장 처리할 대기자 수 결정
                    final int countToAdmit = Math.min(availablePermits, waitingQueueSize);

                    // 결정된 수만큼 Permit 획득 (Atomic)
                    boolean permitLockAcquired = waitingCachePort.tryAcquirePermits(scheduleId, countToAdmit);
                    if (!permitLockAcquired) {
                        log.debug("해당 스케줄 Permit 획득 실패({}개): scheduleId = {}", countToAdmit, scheduleId);
                        return;
                    }

                    log.debug("해당 스케줄 Permit 획득 성공({}개): scheduleId = {}", countToAdmit, scheduleId);

                    // 대기열에서 획득한 Permit 수만큼 사용자 추출(ZPOPMIN)
                    // 입장 허용 처리 실패 시 다시 대기열에 넣어줘야 하므로 기존 score 정보도 함께 조회
                    List<WaitingQueueEntry> admittedEntries = waitingCachePort.admitUsers(scheduleId, countToAdmit);

                    // 보상 트랜잭션: 추출된 사용자 < 획득한 Permit 수 => 잔여 Permit 반환
                    int admittedSize = admittedEntries.size();
                    if (admittedSize < countToAdmit) {
                        int countToRelease = countToAdmit - admittedSize;
                        waitingCachePort.releasePermits(scheduleId, countToRelease);
                        log.debug("추출된 대기자가 획득한 Permit 수보다 적어 잔여 Permit 반납: scheduleId = {}, permitsToReturn = {}", scheduleId, countToRelease);
                    }

                    if (admittedSize == 0) {
                        // admittedSize < countToAdmit 뒤에 넣은 이유는 잔여 Permit 수만큼 락 해제를 먼저 수행하기 위해서다.
                        log.debug("실제 입장 처리된 대기자가 없음: scheduleId = {}, admittedSize = {}", scheduleId, admittedSize);
                    }

                    // 입장 토큰 발급
                    int failCount = 0;
                    for (WaitingQueueEntry entry : admittedEntries) {
                        String userKey = entry.userKey();
                        double originalScore = entry.score();

                        String[] parts = userKey.split(":");

                        try {
                            // Claims 구성
                            Map<String, String> claims = new HashMap<>();
                            claims.put("userKey", userKey);
                            claims.put("scheduleId", scheduleIdStr);
                            claims.put("type", "ADMITTED");

                            String userIdStr = parts[0];
                            String admittedToken = jwtUtils.generateToken(userIdStr, claims, Duration.ofMinutes(10).toMillis());

                            // Redis에 입장 토큰 저장 (TTL: 10분)
                            waitingCachePort.saveAdmittedToken(scheduleId, userKey, admittedToken);


                            // 로컬 EmitterMap에 존재하는지 확인 (분산락 내부에서 확인만)
                            if (emitterRepositoryPort.containsEmitterByUserKey(userKey))
                                localUserToAdmittedToken.put(userKey, admittedToken);
                            else
                                remoteUserToAdmittedToken.put(userKey, admittedToken);

                            log.debug("입장 토큰 발급 완료: userKey = {}", userKey);
                        } catch (Exception e) {
                            log.error("입장 토큰 발급 실패: userKey = {}", userKey, e);

                            // 입장 토큰 발급 실패되면 Permit 반납을 위해 실패 횟수 카운팅
                            failCount++;

                            // 보상 트랜잭션: 입장 토큰 발급 실패 시 해당 사용자를 다시 대기열 맨 앞에 추가
                            waitingCachePort.addUserToWaitingQueue(scheduleId, userKey, originalScore);
                            log.info("입장 토큰 발급 실패로 대기열에 재추가: userKey = {}, originalScore = {}", userKey, originalScore);
                        }
                    }

                    // 보상 트랜잭션: 토큰 발급 실패 건수만큼 Permit 반납
                    if (failCount > 0) {
                        waitingCachePort.releasePermits(scheduleId, failCount);
                        log.info("입장 토큰 발급 실패로 Permit 반납: scheduleId = {}, failCount = {}", scheduleId, failCount);
                    }
                } finally {
                    // 분산락 해제
                    waitingCachePort.releaseAdmitLock(scheduleId);
                }

                if (!localUserToAdmittedToken.isEmpty())
                    notifyAdmittedUsers(localUserToAdmittedToken);

                if (!remoteUserToAdmittedToken.isEmpty())
                    waitingEventPublisher.notifyAdmittedUsersEvent(remoteUserToAdmittedToken);
            }
        });
    }

    /**
     * 입장 토큰 발급
     */
    private String issueAdmittedToken(Long userId, Long scheduleId) {
        String userKey = generateUserKey(userId);

        // Claims 구성
        Map<String, String> claims = new HashMap<>();
        claims.put("userKey", userKey);
        claims.put("scheduleId", String.valueOf(scheduleId));
        claims.put("type", "ADMITTED");

        String admittedToken = jwtUtils.generateToken(String.valueOf(userId), claims, Duration.ofMinutes(10).toMillis());

        // 캐싱
        waitingCachePort.saveAdmittedToken(scheduleId, userKey, admittedToken);
        return admittedToken;
    }

    private String generateUserKey(Long userId) {
        return userId + ":" + UUID.randomUUID();
    }

    /**
     * 대기열 진입
     */
    private WaitingQueueStatus enterQueue(Long userId, Long scheduleId) {
        String userKey = generateUserKey(userId);
        waitingCachePort.enterQueue(scheduleId, userKey);

        return waitingCachePort.getRankAndTotalWaitingCount(scheduleId, userKey);
    }

    /**
     * userId와 userKey가 일치하는지 검증
     */
    private void validateUserKey(Long userId, String userKey) {
        String[] parts = userKey.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("유효하지 않은 userKey입니다.");
        }

        Long userIdFromKey = Long.parseLong(parts[0]);

        // userId 일치 여부 검증
        if (!userId.equals(userIdFromKey)) {
            throw new IllegalArgumentException("userId가 일치하지 않습니다.");
        }
    }

    /**
     * 현재 인스턴스 인메모리 emitterMap에 있는 userKey만 필터링
     */
    private List<String> filterUserKeys(Collection<String> userKeys) {
        return userKeys.stream()
                .filter(emitterRepositoryPort::containsEmitterByUserKey)
                .toList();
    }
}
