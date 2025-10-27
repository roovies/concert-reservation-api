package com.roovies.concertreservation.waiting.unit.service;

import com.roovies.concertreservation.shared.util.security.JwtUtils;
import com.roovies.concertreservation.waiting.application.dto.result.EnterQueueResult;
import com.roovies.concertreservation.waiting.application.port.out.EmitterRepositoryPort;
import com.roovies.concertreservation.waiting.application.port.out.WaitingCachePort;
import com.roovies.concertreservation.waiting.application.port.out.WaitingEventPublisher;
import com.roovies.concertreservation.waiting.application.service.ReservationWaitingService;
import com.roovies.concertreservation.waiting.domain.vo.WaitingQueueEntry;
import com.roovies.concertreservation.waiting.domain.vo.WaitingQueueStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationWaitingService 단위 테스트")
public class ReservationWaitingServiceTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private WaitingCachePort waitingCachePort;

    @Mock
    private EmitterRepositoryPort emitterRepositoryPort;

    @Mock
    private WaitingEventPublisher waitingEventPublisher;

    @InjectMocks
    private ReservationWaitingService reservationWaitingService;

    @Test
    @DisplayName("대기열이 비활성화 상태이고 세마포어 획득 성공 시 즉시 입장해야 한다")
    void enterOrWaitQueue_즉시입장_성공() {
        // given
        Long userId = 1L;
        Long scheduleId = 100L;
        String expectedToken = "test-admit-token";

        given(waitingCachePort.hasActiveWaitingQueue(scheduleId)).willReturn(false); // 대기열 큐 존재X 상태 반환
        given(waitingCachePort.tryAcquirePermit(scheduleId)).willReturn(true); // 세마포어 획득 성공 상태 반환
        given(jwtUtils.generateToken(anyString(), anyMap(), anyLong())).willReturn(expectedToken); // 입장 토큰 발급

        // when
        EnterQueueResult result = reservationWaitingService.enterOrWaitQueue(userId, scheduleId);

        // then
        assertThat(result.admitted()).isTrue();
        assertThat(result.admittedToken()).isEqualTo(expectedToken);
        assertThat(result.rank()).isNull();
        assertThat(result.totalWaiting()).isNull();
        assertThat(result.userKey()).isNull();

        // 중요한 부수 효과만 행위 검증 수행
        // 입장 토큰 저장 (중요한 비즈니스 규칙)
        verify(waitingCachePort).saveAdmittedToken(eq(scheduleId), anyString(), eq(expectedToken));
        // 대기열에 들어가지 않았음을 확인 (중요한 비즈니스 규칙)
        verify(waitingCachePort, never()).enterQueue(anyLong(), anyString());
    }

    @Test
    @DisplayName("대기열이 활성화 상태이면 대기열에 진입해야 한다")
    void enterOrWaitQueue_대기열진입_활성화상태() {
        // given
        Long userId = 1L;
        Long scheduleId = 100L;
        String userKey = "1:test-uuid";
        WaitingQueueStatus status = new WaitingQueueStatus(userKey, 9, 50); // rank는 0-indexed

        given(waitingCachePort.hasActiveWaitingQueue(scheduleId)).willReturn(true);
        given(waitingCachePort.getRankAndTotalWaitingCount(eq(scheduleId), anyString())).willReturn(status);

        // when
        EnterQueueResult result = reservationWaitingService.enterOrWaitQueue(userId, scheduleId);

        // then
        assertThat(result.admitted()).isFalse();
        assertThat(result.admittedToken()).isNull();
        assertThat(result.rank()).isEqualTo(10); // ZRANK는 0부터 시작하므로 +1
        assertThat(result.totalWaiting()).isEqualTo(50);
        assertThat(result.userKey()).isEqualTo(userKey);

        verify(waitingCachePort).hasActiveWaitingQueue(scheduleId);
        verify(waitingCachePort, never()).tryAcquirePermit(scheduleId);
        verify(waitingCachePort).enterQueue(eq(scheduleId), anyString());
    }

    @Test
    @DisplayName("세마포어 획득 실패 시 대기열에 진입해야 한다")
    void enterOrWaitQueue_대기열진입_세마포어실패() {
        // given
        Long userId = 1L;
        Long scheduleId = 100L;
        String userKey = "1:test-uuid";
        WaitingQueueStatus status = new WaitingQueueStatus(userKey, 0, 1);

        given(waitingCachePort.hasActiveWaitingQueue(scheduleId)).willReturn(false);
        given(waitingCachePort.tryAcquirePermit(scheduleId)).willReturn(false);
        given(waitingCachePort.getRankAndTotalWaitingCount(eq(scheduleId), anyString())).willReturn(status);

        // when
        EnterQueueResult result = reservationWaitingService.enterOrWaitQueue(userId, scheduleId);

        // then
        assertThat(result.admitted()).isFalse();
        assertThat(result.rank()).isEqualTo(1);
        assertThat(result.totalWaiting()).isEqualTo(1);

        verify(waitingCachePort).tryAcquirePermit(scheduleId);
        verify(waitingCachePort).enterQueue(eq(scheduleId), anyString());
    }

    @Test
    @DisplayName("유효한 userKey로 SSE 구독 시 emitter가 반환되어야 한다")
    void subscribeToQueue_정상구독() {
        // given
        Long userId = 1L;
        Long scheduleId = 100L;
        String userKey = "1:test-uuid";

        // when
        SseEmitter emitter = reservationWaitingService.subscribeToQueue(userId, scheduleId, userKey);

        // then
        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(600000L); // 10분

        verify(emitterRepositoryPort).saveEmitterByUserKey(eq(userKey), any(SseEmitter.class));
    }

    @Test
    @DisplayName("userKey의 userId가 일치하지 않으면 예외가 발생해야 한다")
    void subscribeToQueue_userId불일치_예외발생() {
        // given
        Long userId = 1L;
        Long scheduleId = 100L;
        String invalidUserKey = "999:test-uuid"; // 다른 userId

        // when & then
        assertThatThrownBy(() -> reservationWaitingService.subscribeToQueue(userId, scheduleId, invalidUserKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId가 일치하지 않습니다.");

        verify(emitterRepositoryPort, never()).saveEmitterByUserKey(anyString(), any());
    }

    @Test
    @DisplayName("잘못된 형식의 userKey는 예외가 발생해야 한다")
    void subscribeToQueue_잘못된형식_예외발생() {
        // given
        Long userId = 1L;
        Long scheduleId = 100L;
        String invalidUserKey = "invalid-format"; // 콜론이 없음

        // when & then
        assertThatThrownBy(() -> reservationWaitingService.subscribeToQueue(userId, scheduleId, invalidUserKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 userKey입니다.");
    }

    @Test
    @DisplayName("활성 대기열이 없으면 입장 처리를 수행하지 않아야 한다")
    void admitUsersInActiveWaitingSchedules_활성대기열없음() {
        // given
        given(waitingCachePort.getActiveWaitingScheduleIds()).willReturn(Set.of());

        // when
        reservationWaitingService.admitUsersInActiveWaitingSchedules();

        // then
        verify(waitingCachePort).getActiveWaitingScheduleIds();
        verify(waitingCachePort, never()).tryAcquireAdmitLock(anyLong());
    }

    @Test
    @DisplayName("분산락 획득 실패 시 입장 처리를 건너뛰어야 한다")
    void admitUsersInActiveWaitingSchedules_분산락실패() {
        // given
        given(waitingCachePort.getActiveWaitingScheduleIds()).willReturn(Set.of("100"));
        given(waitingCachePort.tryAcquireAdmitLock(100L)).willReturn(false);

        // when
        reservationWaitingService.admitUsersInActiveWaitingSchedules();

        // then
        verify(waitingCachePort).getActiveWaitingScheduleIds();
        verify(waitingCachePort).tryAcquireAdmitLock(100L);
        verify(waitingCachePort, never()).getAvailablePermits(anyLong());
        verify(waitingCachePort, never()).releaseAdmitLock(anyLong());
    }

    @Test
    @DisplayName("토큰 발급 실패 시 대기열에 재추가하고 Permit을 반환해야 한다")
    void admitUsers_토큰발급실패_보상트랜잭션() {
        // given
        Long scheduleId = 100L;
        WaitingQueueEntry entry = new WaitingQueueEntry("1:uuid-1", 1000.0);
        List<WaitingQueueEntry> admittedEntries = List.of(entry);

        given(waitingCachePort.getActiveWaitingScheduleIds()).willReturn(Set.of("100"));
        given(waitingCachePort.tryAcquireAdmitLock(scheduleId)).willReturn(true);
        given(waitingCachePort.getAvailablePermits(scheduleId)).willReturn(50);
        given(waitingCachePort.getWaitingQueueSize(scheduleId)).willReturn(1);
        given(waitingCachePort.tryAcquirePermits(scheduleId, 1)).willReturn(true);
        given(waitingCachePort.admitUsers(scheduleId, 1)).willReturn(admittedEntries);

        // JWT 생성 실패 시뮬레이션
        given(jwtUtils.generateToken(anyString(), anyMap(), anyLong()))
                .willThrow(new RuntimeException("JWT generation failed"));

        // when
        reservationWaitingService.admitUsersInActiveWaitingSchedules();

        // then
        verify(waitingCachePort).addUserToWaitingQueue(scheduleId, "1:uuid-1", 1000.0);
        verify(waitingCachePort).releasePermits(scheduleId, 1);
        verify(waitingCachePort).releaseAdmitLock(scheduleId);
    }

    @Test
    @DisplayName("입장 처리 수가 가용 Permit과 대기자 수 중 작은 값으로 결정되어야 한다")
    void admitUsers_입장처리수_결정로직() {
        // given - 가용 Permit 10개, 대기자 5명
        Long scheduleId = 100L;
        List<WaitingQueueEntry> admittedEntries = List.of(
                new WaitingQueueEntry("1:uuid-1", 1000.0),
                new WaitingQueueEntry("2:uuid-2", 1001.0),
                new WaitingQueueEntry("3:uuid-3", 1002.0),
                new WaitingQueueEntry("4:uuid-4", 1003.0),
                new WaitingQueueEntry("5:uuid-5", 1004.0)
        );

        given(waitingCachePort.getActiveWaitingScheduleIds()).willReturn(Set.of("100"));
        given(waitingCachePort.tryAcquireAdmitLock(scheduleId)).willReturn(true);
        given(waitingCachePort.getAvailablePermits(scheduleId)).willReturn(10);
        given(waitingCachePort.getWaitingQueueSize(scheduleId)).willReturn(5);
        given(waitingCachePort.tryAcquirePermits(scheduleId, 5)).willReturn(true);
        given(waitingCachePort.admitUsers(scheduleId, 5)).willReturn(admittedEntries);
        given(jwtUtils.generateToken(anyString(), anyMap(), anyLong())).willReturn("test-token");
        given(emitterRepositoryPort.containsEmitterByUserKey(anyString())).willReturn(true);

        // when
        reservationWaitingService.admitUsersInActiveWaitingSchedules();

        // then
        verify(waitingCachePort).tryAcquirePermits(scheduleId, 5); // Math.min(10, 5) = 5
        verify(waitingCachePort).admitUsers(scheduleId, 5);
        verify(waitingCachePort, never()).releasePermits(eq(scheduleId), anyInt());
        verify(waitingCachePort).releaseAdmitLock(scheduleId);
    }

    @Test
    @DisplayName("parallelStream으로 여러 스케줄을 동시에 처리해야 한다")
    void admitUsers_병렬처리() {
        // given
        Set<String> scheduleIds = Set.of("100", "200", "300");
        given(waitingCachePort.getActiveWaitingScheduleIds()).willReturn(scheduleIds);
        given(waitingCachePort.tryAcquireAdmitLock(anyLong())).willReturn(true);
        given(waitingCachePort.getAvailablePermits(anyLong())).willReturn(10);
        given(waitingCachePort.getWaitingQueueSize(anyLong())).willReturn(5);
        given(waitingCachePort.tryAcquirePermits(anyLong(), anyInt())).willReturn(true);
        given(waitingCachePort.admitUsers(anyLong(), anyInt())).willReturn(List.of());

        // when
        reservationWaitingService.admitUsersInActiveWaitingSchedules();

        // then
        verify(waitingCachePort).tryAcquireAdmitLock(100L);
        verify(waitingCachePort).tryAcquireAdmitLock(200L);
        verify(waitingCachePort).tryAcquireAdmitLock(300L);
        verify(waitingCachePort, times(3)).releaseAdmitLock(anyLong());
    }

    @Test
    @DisplayName("활성 대기열이 있으면 각 스케줄별로 이벤트를 발행해야 한다")
    void publishActiveWaitingScheduleStatus_이벤트발행() {
        // given
        Set<String> scheduleIds = Set.of("100", "200", "300");
        given(waitingCachePort.getActiveWaitingScheduleIds()).willReturn(scheduleIds);

        // when
        reservationWaitingService.publishActiveWaitingScheduleStatus();

        // then
        verify(waitingEventPublisher).notifyWaitingQueueStatusEvent(100L);
        verify(waitingEventPublisher).notifyWaitingQueueStatusEvent(200L);
        verify(waitingEventPublisher).notifyWaitingQueueStatusEvent(300L);
        verify(waitingEventPublisher, times(3)).notifyWaitingQueueStatusEvent(anyLong());
    }

    @Test
    @DisplayName("대기자가 없는 스케줄은 활성 목록에서 제거되어야 한다")
    void notifyWaitingQueueStatus_대기자없음_활성목록제거() {
        // given
        Long scheduleId = 100L;
        given(waitingCachePort.getActiveWaitingUserKeys(scheduleId)).willReturn(List.of());

        // when
        reservationWaitingService.notifyWaitingQueueStatus(scheduleId);

        // then
        verify(waitingCachePort).getActiveWaitingUserKeys(scheduleId);
        verify(waitingCachePort).removeActiveWaitingScheduleId(scheduleId);
        verify(emitterRepositoryPort, never()).getEmitterByUserKey(anyString());
    }

    @Test
    @DisplayName("로컬 인스턴스에 연결된 대기자만 SSE 알림을 받아야 한다")
    void notifyWaitingQueueStatus_로컬대기자만_알림전송() throws Exception {
        // given
        Long scheduleId = 100L;
        String localUserKey = "1:local-uuid";
        String remoteUserKey = "2:remote-uuid";
        Collection<String> allUserKeys = List.of(localUserKey, remoteUserKey);

        SseEmitter localEmitter = new SseEmitter();
        WaitingQueueStatus localStatus = new WaitingQueueStatus(localUserKey, 0, 2);

        given(waitingCachePort.getActiveWaitingUserKeys(scheduleId)).willReturn(allUserKeys);
        given(emitterRepositoryPort.containsEmitterByUserKey(localUserKey)).willReturn(true);
        given(emitterRepositoryPort.containsEmitterByUserKey(remoteUserKey)).willReturn(false);
        given(emitterRepositoryPort.getEmitterByUserKey(localUserKey)).willReturn(localEmitter);
        given(waitingCachePort.getRankAndTotalWaitingCount(scheduleId, localUserKey)).willReturn(localStatus);

        // when
        reservationWaitingService.notifyWaitingQueueStatus(scheduleId);

        // then
        verify(emitterRepositoryPort).getEmitterByUserKey(localUserKey);
        verify(emitterRepositoryPort, never()).getEmitterByUserKey(remoteUserKey);
        verify(waitingCachePort).getRankAndTotalWaitingCount(scheduleId, localUserKey);
        verify(waitingCachePort, never()).getRankAndTotalWaitingCount(scheduleId, remoteUserKey);
    }

    @Test
    @DisplayName("순번 조회 실패 시 해당 사용자는 건너뛰어야 한다")
    void notifyWaitingQueueStatus_순번조회실패_건너뛰기() {
        // given
        Long scheduleId = 100L;
        String userKey = "1:test-uuid";
        SseEmitter emitter = new SseEmitter();
        WaitingQueueStatus statusWithNullRank = new WaitingQueueStatus(userKey, null, 10);

        given(waitingCachePort.getActiveWaitingUserKeys(scheduleId)).willReturn(List.of(userKey));
        given(emitterRepositoryPort.containsEmitterByUserKey(userKey)).willReturn(true);
        given(emitterRepositoryPort.getEmitterByUserKey(userKey)).willReturn(emitter);
        given(waitingCachePort.getRankAndTotalWaitingCount(scheduleId, userKey)).willReturn(statusWithNullRank);

        // when
        reservationWaitingService.notifyWaitingQueueStatus(scheduleId);

        // then
        verify(waitingCachePort).getRankAndTotalWaitingCount(scheduleId, userKey);
        // SSE 전송이 시도되지 않아야 함 (rank가 null이므로)
    }
}
