package com.roovies.concertreservation.reservations.application;

import com.roovies.concertreservation.reservations.application.dto.command.HoldSeatCommand;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.out.HoldSeatCachePort;
import com.roovies.concertreservation.reservations.application.port.out.HoldSeatIdempotencyCachePort;
import com.roovies.concertreservation.reservations.application.service.HoldSeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class
HoldSeatUseCaseTest {

    @Mock
    private HoldSeatCachePort holdSeatCachePort;

    @Mock
    private HoldSeatIdempotencyCachePort holdSeatIdempotencyCachePort;

    @InjectMocks
    private HoldSeatService holdSeatService;

    private Long scheduleId;
    private Long userId;
    private List<Long> seatIds;
    private HoldSeatCommand command;

    @BeforeEach
    void setUp() {
        scheduleId = 1L;
        userId = 100L;
        seatIds = Arrays.asList(1L, 2L, 3L);
        command = new HoldSeatCommand("idempotencyKey", scheduleId, seatIds, userId);
    }

    @Test
    void 정상적인_좌석_예약_요청시_성공해야_한다() {
        // given
        long expectedTTL = 300L;

        // 멱등성 정보가 없음
        given(holdSeatIdempotencyCachePort.tryProcess(anyString()))
                .willReturn(true);

        // 현재 홀딩된 정보가 없음
        given(holdSeatCachePort.validateHoldSeatList(scheduleId, seatIds, userId))
                .willReturn(false);
        given(holdSeatCachePort.holdSeatList(scheduleId, seatIds, userId))
                .willReturn(true);
        given(holdSeatCachePort.getHoldTTLSeconds(scheduleId, seatIds, userId))
                .willReturn(expectedTTL);

        // when
        HoldSeatResult result = holdSeatService.holdSeat(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.scheduleId()).isEqualTo(scheduleId);
        assertThat(result.seatIds()).containsExactlyElementsOf(seatIds);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.ttlSeconds()).isEqualTo(expectedTTL);
    }

    @Test
    void 좌석_목록이_null이면_예외가_발생해야_한다() {
        // given
        HoldSeatCommand nullSeatIdsCommand = HoldSeatCommand.builder()
                .idempotencyKey("idempotencyKey")
                .scheduleId(scheduleId)
                .seatIds(null)
                .userId(userId)
                .build();
        given(holdSeatIdempotencyCachePort.tryProcess(anyString())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> holdSeatService.holdSeat(nullSeatIdsCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약할 좌석이 없습니다.");
    }

    @Test
    void 좌석_목록이_비어있을_때_예외가_발생해야_한다() {
        // given
        HoldSeatCommand emptySeatIdsCommand = HoldSeatCommand.builder()
                .idempotencyKey("idempotencyKey")
                .scheduleId(scheduleId)
                .seatIds(new ArrayList<>())
                .userId(userId)
                .build();
        given(holdSeatIdempotencyCachePort.tryProcess(anyString())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> holdSeatService.holdSeat(emptySeatIdsCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약할 좌석이 없습니다.");
    }

    @Test
    void 중복된_좌석ID가_있을_경우_중복_제거_후_처리해야_한다() {
        // given
        List<Long> duplicateSeatIds = Arrays.asList(101L, 102L, 103L, 102L, 101L);
        List<Long> uniqueSeatIds = Arrays.asList(101L, 102L, 103L);
        HoldSeatCommand duplicateCommand = HoldSeatCommand.builder()
                .idempotencyKey("idempotencyKey")
                .scheduleId(scheduleId)
                .seatIds(duplicateSeatIds)
                .userId(userId)
                .build();

        long expectedTTL = 300L;
        given(holdSeatIdempotencyCachePort.tryProcess(anyString()))
                .willReturn(true);
        given(holdSeatCachePort.validateHoldSeatList(scheduleId, uniqueSeatIds, userId))
                .willReturn(false);
        given(holdSeatCachePort.holdSeatList(scheduleId, uniqueSeatIds, userId))
                .willReturn(true);
        given(holdSeatCachePort.getHoldTTLSeconds(scheduleId, uniqueSeatIds, userId))
                .willReturn(expectedTTL);

        // when
        HoldSeatResult result = holdSeatService.holdSeat(duplicateCommand);

        // then
        assertThat(result.seatIds()).containsExactlyElementsOf(uniqueSeatIds);
    }

    @Test
    void 동일한_요청을_재시도할_경우_기존_결과를_반환한다() {
        // given
        long expectedTTL = 300L;
        given(holdSeatIdempotencyCachePort.tryProcess(anyString()))
                .willReturn(true);
        given(holdSeatCachePort.validateHoldSeatList(scheduleId, seatIds, userId))
                .willReturn(true);
        given(holdSeatCachePort.getHoldTTLSeconds(scheduleId, seatIds, userId))
                .willReturn(expectedTTL);

        // when
        HoldSeatResult result = holdSeatService.holdSeat(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.ttlSeconds()).isEqualTo(expectedTTL);
    }

    @Test
    void 다른_사용자가_이미_예약중일_경우_예외가_발생해야_한다() {
        // given
        given(holdSeatIdempotencyCachePort.tryProcess(anyString()))
                .willReturn(true);
        given(holdSeatCachePort.validateHoldSeatList(scheduleId, seatIds, userId))
                .willReturn(false);
        given(holdSeatCachePort.holdSeatList(scheduleId, seatIds, userId))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> holdSeatService.holdSeat(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("다른 사용자가 이미 예약 중인 좌석입니다.");
    }

    @Test
    void 멀티쓰레드_환경에서_동시에_같은_예약을_수행할_경우_한명만_예약되어야_한다() {
        // given
        int threadCount = 10;
        List<Long> seatIds = Arrays.asList(1L, 2L, 3L);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 첫 번 째 호출만 성공하도록 설정
        given(holdSeatIdempotencyCachePort.tryProcess(anyString()))
                .willReturn(true);
        given(holdSeatCachePort.validateHoldSeatList(eq(scheduleId), eq(seatIds), anyLong()))
                .willReturn(false);
        given(holdSeatCachePort.holdSeatList(eq(scheduleId), eq(seatIds), anyLong()))
                .willReturn(true)   // 첫 번째 호출만 성공
                .willReturn(false); // 이후 호출들은 실패
        given(holdSeatCachePort.getHoldTTLSeconds(eq(scheduleId), eq(seatIds), anyLong()))
                .willReturn(300L);

        // when
        List<CompletableFuture<Void>> futures =
                IntStream.range(0, threadCount)
                        .mapToObj(num -> CompletableFuture.runAsync(() -> {
                            Long currentUserId = 1000L + num;
                            HoldSeatCommand currentCommand = new HoldSeatCommand("idempotencyKey", scheduleId, seatIds, currentUserId);
                            try {
                                HoldSeatResult result = holdSeatService.holdSeat(currentCommand);
                                successCount.incrementAndGet();
                            } catch (IllegalStateException e) {
                                if ("다른 사용자가 이미 예약 중인 좌석입니다.".equals(e.getMessage())) {
                                    failCount.incrementAndGet();
                                }
                            }
                        }))
                        .toList();

        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        all.join(); // 모든 작업 완료 대기

        // then
        assertThat(successCount.get()).isEqualTo(1); // 하나만 성공
        assertThat(failCount.get()).isEqualTo(threadCount - 1); // 나머지는 실패
    }

    @Test
    void 멀티쓰레드_환경에서_동시에_각자_다른_예약을_수행할_경우_전부_다_예약되어야_한다() {
        // given
        int threadCount = 10;
        AtomicInteger successCount = new AtomicInteger(0);

        given(holdSeatIdempotencyCachePort.tryProcess(anyString()))
                .willReturn(true);
        given(holdSeatCachePort.validateHoldSeatList(anyLong(), anyList(), anyLong()))
                .willReturn(false);
        given(holdSeatCachePort.holdSeatList(anyLong(), anyList(), anyLong()))
                .willReturn(true);
        given(holdSeatCachePort.getHoldTTLSeconds(anyLong(), anyList(), anyLong()))
                .willReturn(300L);

        // when
        List<CompletableFuture<Void>> futures =
                IntStream.range(0, threadCount)
                        .mapToObj(num -> CompletableFuture.runAsync(() -> {
                            Long currentUserId = 1000L + num;
                            List<Long> uniqueSeats = Arrays.asList(100L + num); // 각 쓰레드마다 다른 좌석
                            // HoldSeatCommand currentCommand = new HoldSeatCommand(scheduleId, seatIds, currentUserId);
                            HoldSeatCommand concurrentCommand = new HoldSeatCommand("idempotencyKey", scheduleId, uniqueSeats, 1000L + num);
                            HoldSeatResult result = holdSeatService.holdSeat(concurrentCommand);
                            successCount.incrementAndGet();
                        }))
                        .toList();

        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        all.join();

        // then
        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    @Test
    void 멱등성키가_null일_경우_예외가_발생해야_한다() {
        // given
        List<Long> seatIds = Arrays.asList(101L, 102L, 103L, 102L, 101L);
        HoldSeatCommand command = HoldSeatCommand.builder()
                .idempotencyKey(null)
                .scheduleId(scheduleId)
                .seatIds(seatIds)
                .userId(userId)
                .build();

        // when & then
        assertThatThrownBy(() -> holdSeatService.holdSeat(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("요청 식별자(Idempotency Key)가 필요합니다.");
    }

    @Test
    void 멱등성키가_빈값일_경우_예외가_발생해야_한다() {
        // given
        List<Long> seatIds = Arrays.asList(101L, 102L, 103L, 102L, 101L);
        HoldSeatCommand command = HoldSeatCommand.builder()
                .idempotencyKey("")
                .scheduleId(scheduleId)
                .seatIds(seatIds)
                .userId(userId)
                .build();

        // when & then
        assertThatThrownBy(() -> holdSeatService.holdSeat(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("요청 식별자(Idempotency Key)가 필요합니다.");
    }

    @Test
    void 이미_멱등성키가_존재할_경우_캐싱된_결과를_반환해야_한다() {
        // given
        List<Long> seatIds = Arrays.asList(101L, 102L, 103L, 102L, 101L);
        HoldSeatResult cached = HoldSeatResult.builder()
                .scheduleId(1L)
                .seatIds(seatIds)
                .userId(5L)
                .totalPrice(5L)
                .ttlSeconds(500)
                .build();

        given(holdSeatIdempotencyCachePort.findByIdempotencyKey(command.idempotencyKey()))
                .willReturn(cached);

        // when
        HoldSeatResult result = holdSeatService.holdSeat(command);

        // then
        assertThat(result.scheduleId()).isEqualTo(cached.scheduleId());
        assertThat(result.seatIds()).containsExactlyElementsOf(cached.seatIds());
        assertThat(result.userId()).isEqualTo(cached.userId());
        assertThat(result.ttlSeconds()).isEqualTo(cached.ttlSeconds());
    }

    @Test
    void 멱등성_저장에_실패하면_비즈니스도_실패해야_한다() {
        // given
        given(holdSeatIdempotencyCachePort.tryProcess(anyString()))
                .willReturn(true);
        given(holdSeatCachePort.validateHoldSeatList(command.scheduleId(), command.seatIds(), command.userId()))
                .willReturn(false);
        given(holdSeatCachePort.holdSeatList(command.scheduleId(), command.seatIds(), command.userId()))
                .willReturn(true);
        given(holdSeatCachePort.getHoldTTLSeconds(command.scheduleId(), command.seatIds(), command.userId()))
                .willReturn(300L);

        // 멱등성 저장 실패
        doThrow(new RuntimeException("동시성 문제가 발생했습니다."))
                .when(holdSeatIdempotencyCachePort).saveResult(anyString(), any(HoldSeatResult.class));

        // when & then
        assertThatThrownBy(() -> holdSeatService.holdSeat(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("동시성 문제가 발생했습니다.");
    }
}
