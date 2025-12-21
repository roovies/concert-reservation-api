# SAGA Choreography 패턴 기반 포인트 적립 및 보상 트랜잭션 구현 보고서

## 1. 개요

### 1.1 패턴 분석 결과

**현재 구현된 SAGA 패턴: Choreography (코레오그래피)**

### 1.2 핵심 근거

1. **중앙 오케스트레이터 부재**: 전체 워크플로우를 관리하는 중앙 조정자가 존재하지 않음
2. **이벤트 기반 통신**: 각 서비스가 Kafka 이벤트를 발행하고 구독하여 독립적으로 동작
3. **분산된 의사결정**: 각 서비스가 자체적으로 성공/실패를 판단하고 다음 이벤트를 발행
4. **자율적 보상 결정**: 포인트 서비스가 실패 시 자체적으로 보상 트랜잭션 이벤트를 발행

---

## 2. SAGA 패턴 개념

### 2.1 SAGA 패턴이란?

분산 트랜잭션 환경에서 데이터 일관성을 보장하기 위한 패턴으로, 여러 서비스에 걸친 긴 트랜잭션(Long-Lived Transaction)을 일련의 로컬 트랜잭션으로 분해하고, 각 단계의 실패 시 보상 트랜잭션(Compensating Transaction)을 실행하여 데이터 일관성을 유지합니다.

### 2.2 Orchestration vs Choreography

#### Orchestration
- **중앙 집중식**: 오케스트레이터가 워크플로우를 관리하고 각 서비스에 명령 전달
- **명시적 흐름**: 워크플로우 로직이 한 곳에 집중되어 가시성 높음
- **강한 결합**: 오케스트레이터와 각 서비스 간 의존성 존재

#### Choreography (현재 구현)
- **분산형**: 각 서비스가 이벤트를 구독하고 자율적으로 반응
- **암묵적 흐름**: 워크플로우가 이벤트 체인으로 분산되어 전체 흐름 파악 어려움
- **느슨한 결합**: 서비스 간 직접적인 의존성 없음

---

## 3. 아키텍처 다이어그램

### 3.1 서비스 구성

```
┌─────────────────┐         ┌─────────────────┐
│  Payments       │         │  Points         │
│  Service        │         │  Service        │
└────────┬────────┘         └────────┬────────┘
         │                           │
         │    ┌─────────────────┐    │
         └───▶│  Kafka Broker   │◀───┘
              │                 │
              │  Topics:        │
              │  • reservation- │
              │    completed    │
              │  • point-reward-│
              │    completed    │
              │  • point-reward-│
              │    failed       │
              │  • compensate-  │
              │    payment      │
              └─────────────────┘
```

### 3.2 이벤트 흐름도

#### 정상 흐름 (Happy Path)
```
[Payments Service]                    [Kafka]                    [Points Service]
       │                                  │                              │
       │ 1. 결제 성공                      │                              │
       │    예약 생성                       │                              │
       │                                  │                              │
       │ 2. ReservationCompletedKafkaEvent                              │
       ├─────────────────────────────────▶│                              │
       │    (reservation-completed)        │                              │
       │                                  │ 3. 이벤트 수신                 │
       │                                  ├─────────────────────────────▶│
       │                                  │                              │ 4. 포인트 적립
       │                                  │                              │    (원금의 10%)
       │                                  │                              │
       │                                  │ 5. PointRewardCompletedEvent  │
       │                                  │◀─────────────────────────────┤
       │                                  │    (point-reward-completed)   │
       │                                  │                              │
```

#### 실패 및 보상 흐름 (Compensation Path)
```
[Payments Service]                    [Kafka]                    [Points Service]
       │                                  │                              │
       │ 1. 결제 성공                      │                              │
       │    예약 생성                       │                              │
       │                                  │                              │
       │ 2. ReservationCompletedKafkaEvent                              │
       ├─────────────────────────────────▶│                              │
       │                                  │ 3. 이벤트 수신                 │
       │                                  ├─────────────────────────────▶│
       │                                  │                              │ 4. 포인트 적립 실패
       │                                  │                              │    (Exception)
       │                                  │ 5. PointRewardFailedEvent     │
       │                                  │◀─────────────────────────────┤
       │                                  │    (point-reward-failed)      │
       │                                  │                              │
       │                                  │ 6. CompensatePaymentEvent     │
       │                                  │◀─────────────────────────────┤
       │                                  │    (compensate-payment)       │
       │ 7. 보상 이벤트 수신                 │                              │
       │◀─────────────────────────────────┤                              │
       │                                  │                              │
       │ 8. 포인트 환불                     │                              │
       │    (RefundPaymentService)        │                              │
       │                                  │                              │
```

---

## 4. 상세 구현 분석

### 4.1 Payments 모듈

#### 4.1.1 발행하는 이벤트

**이벤트**: `ReservationCompletedKafkaEvent`
**Topic**: `reservation-completed`
**발행 시점**: 결제 성공 및 예약 완료 후

**구현 위치**:
- `payments/application/service/PayReservationService.java`
- `payments/application/listener/PaymentEventListener.java`
- `payments/infra/adapter/out/kafka/PaymentEventKafkaAdapter.java`

**발행 메커니즘** (트랜잭션 보장):
```java
// PayReservationService.java (라인 104-116)
// Step 1: 트랜잭션 내부에서 인메모리 이벤트 발행
final ReservationCompletedKafkaEvent kafkaEvent = ReservationCompletedKafkaEvent.of(
    payment.getPaymentId(),
    userId,
    reservation.getReservationId(),
    payment.getAmount()
);
eventPublisher.publishEvent(kafkaEvent);

// Step 2: PaymentEventListener.java (라인 18-25)
// 트랜잭션 커밋 완료 후에만 Kafka로 발행
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handlePaymentCompleted(final ReservationCompletedKafkaEvent event) {
    paymentKafkaEventPort.publishReservationCompleted(event);
}
```

**트랜잭션 보장 전략**:
1. Spring의 `ApplicationEventPublisher`로 트랜잭션 내부에서 이벤트 발행
2. `@TransactionalEventListener(AFTER_COMMIT)`로 커밋 성공 후에만 Kafka 발행
3. DB 트랜잭션 실패 시 이벤트 미발행으로 데이터 무결성 보장

#### 4.1.2 구독하는 이벤트

**이벤트**: `CompensatePaymentEvent`
**Topic**: `compensate-payment`
**Consumer Group**: `payment-compensation-service`

**구현 위치**:
- `payments/infra/adapter/in/listener/CompensatePaymentKafkaListener.java`
- `payments/application/service/RefundPaymentService.java`

**처리 로직**:
```java
// CompensatePaymentKafkaListener.java (라인 44-69)
@KafkaListener(topics = "compensate-payment", groupId = "payment-compensation-service")
public void handleCompensatePayment(
    @Payload final CompensatePaymentEvent event,
    final Acknowledgment ack
) {
    log.info("보상 트랜잭션 수신 - paymentId: {}, userId: {}, amount: {}, reason: {}",
        event.paymentId(), event.userId(), event.refundAmount(), event.reason());

    final RefundPaymentCommand command = RefundPaymentCommand.of(
        event.userId(),
        event.paymentId(),
        event.refundAmount(),
        event.reason()
    );

    // 보상 트랜잭션 실행: 포인트 환불
    refundPaymentUseCase.refund(command);

    log.info("보상 트랜잭션 완료 - paymentId: {}", event.paymentId());
    ack.acknowledge(); // 수동 커밋
}
```

**보상 트랜잭션 실행**:
```java
// RefundPaymentService.java (라인 22-49)
@Override
@Transactional
public RefundPaymentResult refund(final RefundPaymentCommand command) {
    // 1. 결제 정보 조회
    final Payment payment = paymentQueryRepositoryPort.findByIdWithLock(
        command.paymentId()
    ).orElseThrow(() -> new NotFoundException("결제 정보를 찾을 수 없습니다"));

    // 2. 포인트 환불 (외부 게이트웨이 호출)
    paymentPointGatewayPort.refund(command.userId(), command.refundAmount());

    log.info("포인트 환불 완료 - userId: {}, amount: {}, reason: {}",
        command.userId(), command.refundAmount(), command.reason());

    return RefundPaymentResult.of(payment.getPaymentId(), command.refundAmount());
}
```

---

### 4.2 Points 모듈

#### 4.2.1 구독하는 이벤트

**이벤트**: `ReservationCompletedKafkaEvent`
**Topic**: `reservation-completed`
**Consumer Group**: `points-service`

**구현 위치**:
- `points/infra/adapter/in/listener/ReservationCompletedKafkaListener.java`
- `points/application/service/RewardPointService.java`

**처리 로직**:
```java
// ReservationCompletedKafkaListener.java (라인 49-91)
@KafkaListener(topics = "reservation-completed", groupId = "points-service")
public void handleReservationCompleted(
    @Payload final ReservationCompletedKafkaEvent event,
    final Acknowledgment ack
) {
    log.info("예약 완료 이벤트 수신 - reservationId: {}, userId: {}, paidAmount: {}",
        event.reservationId(), event.userId(), event.paidAmount());

    try {
        // 포인트 적립액 계산 (원금의 10%, 100원 단위 내림)
        final long originalAmount = event.paidAmount();
        final long rewardAmount = calculateRewardAmount(originalAmount);

        // 포인트 적립 실행
        final RewardPointCommand command = RewardPointCommand.of(
            event.userId(),
            event.paymentId(),
            rewardAmount,
            "예약 완료 적립 (예약 ID: " + event.reservationId() + ")"
        );
        final RewardPointResult result = rewardPointUseCase.reward(command);

        // 성공 이벤트 발행
        final PointRewardCompletedEvent completedEvent = PointRewardCompletedEvent.of(
            result.pointId(),
            event.userId(),
            result.rewardedAmount(),
            event.reservationId()
        );
        pointKafkaEventPort.publishPointRewardCompleted(completedEvent);

        log.info("포인트 적립 성공 - userId: {}, rewardedAmount: {}",
            event.userId(), result.rewardedAmount());

        ack.acknowledge(); // 수동 커밋
    } catch (Exception e) {
        // 실패 처리 로직 (아래 섹션 참조)
    }
}

// 포인트 적립액 계산 로직
private long calculateRewardAmount(final long paidAmount) {
    final double rewardRate = 0.1; // 10%
    final long rewardAmount = (long) (paidAmount * rewardRate);
    return (rewardAmount / 100) * 100; // 100원 단위 내림
}
```

**적립 로직 상세**:
```java
// RewardPointService.java
@Override
@Transactional
public RewardPointResult reward(final RewardPointCommand command) {
    // 1. 사용자 조회
    final User user = userQueryRepositoryPort.findById(command.userId())
        .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

    // 2. 포인트 적립 (도메인 로직)
    final Point point = Point.reward(
        user.getUserId(),
        command.rewardAmount(),
        command.description()
    );

    // 3. 포인트 저장
    final Point savedPoint = pointCommandRepositoryPort.save(point);

    // 4. 결과 반환
    return RewardPointResult.of(savedPoint.getPointId(), savedPoint.getAmount());
}
```

#### 4.2.2 발행하는 이벤트 (성공 케이스)

**이벤트**: `PointRewardCompletedEvent`
**Topic**: `point-reward-completed`
**발행 시점**: 포인트 적립 성공 후

**이벤트 정의**:
```java
// shared/domain/event/PointRewardCompletedEvent.java
public record PointRewardCompletedEvent(
    Long pointId,
    Long userId,
    Long rewardedAmount,
    Long reservationId
) {
    public static PointRewardCompletedEvent of(
        final Long pointId,
        final Long userId,
        final Long rewardedAmount,
        final Long reservationId
    ) {
        return new PointRewardCompletedEvent(
            pointId,
            userId,
            rewardedAmount,
            reservationId
        );
    }
}
```

#### 4.2.3 발행하는 이벤트 (실패 케이스)

**이벤트 1**: `PointRewardFailedEvent`
**Topic**: `point-reward-failed`

**이벤트 2** (보상 트랜잭션 트리거): `CompensatePaymentEvent`
**Topic**: `compensate-payment`

**실패 처리 로직**:
```java
// ReservationCompletedKafkaListener.java (라인 93-127)
catch (Exception e) {
    log.error("포인트 적립 실패 - reservationId: {}, userId: {}, error: {}",
        event.reservationId(), event.userId(), e.getMessage(), e);

    // 1. 포인트 적립 실패 이벤트 발행
    final PointRewardFailedEvent failedEvent = PointRewardFailedEvent.of(
        event.userId(),
        event.paymentId(),
        event.reservationId(),
        e.getMessage()
    );
    pointKafkaEventPort.publishPointRewardFailed(failedEvent);

    // 2. 보상 트랜잭션 트리거: 결제 취소 요청 이벤트 발행
    final CompensatePaymentEvent compensateEvent = CompensatePaymentEvent.of(
        event.paymentId(),
        event.userId(),
        event.paidAmount(),
        "포인트 적립 실패로 인한 결제 보상 트랜잭션: " + e.getMessage()
    );
    pointKafkaEventPort.publishCompensatePayment(compensateEvent);

    log.info("보상 트랜잭션 이벤트 발행 - paymentId: {}, refundAmount: {}",
        event.paymentId(), event.paidAmount());

    // ack를 호출하지 않음 → 메시지 재처리 가능 (재시도 로직)
    throw e;
}
```

**보상 이벤트 정의**:
```java
// shared/domain/event/CompensatePaymentEvent.java
public record CompensatePaymentEvent(
    Long paymentId,
    Long userId,
    Long refundAmount,
    String reason
) {
    public static CompensatePaymentEvent of(
        final Long paymentId,
        final Long userId,
        final Long refundAmount,
        final String reason
    ) {
        return new CompensatePaymentEvent(
            paymentId,
            userId,
            refundAmount,
            reason
        );
    }
}
```

---

## 5. 이벤트 발행/구독 매트릭스

| 서비스 | 발행하는 이벤트 | Topic | 구독하는 이벤트 | Topic | Consumer Group |
|--------|----------------|-------|----------------|-------|----------------|
| **Payments** | `ReservationCompletedKafkaEvent` | `reservation-completed` | `CompensatePaymentEvent` | `compensate-payment` | `payment-compensation-service` |
| **Points** | `PointRewardCompletedEvent`<br>`PointRewardFailedEvent`<br>`CompensatePaymentEvent` | `point-reward-completed`<br>`point-reward-failed`<br>`compensate-payment` | `ReservationCompletedKafkaEvent` | `reservation-completed` | `points-service` |

---

## 6. 트랜잭션 흐름 시나리오

### 6.1 정상 흐름 (Happy Path)

```
┌─────────┐         ┌─────────┐         ┌─────────┐
│  User   │         │ Payments│         │ Points  │
└────┬────┘         └────┬────┘         └────┬────┘
     │                   │                   │
     │ 1. 예약 결제 요청   │                   │
     ├──────────────────▶│                   │
     │                   │                   │
     │                   │ 2. 포인트 차감      │
     │                   │    예약 생성        │
     │                   │    결제 완료        │
     │                   │                   │
     │                   │ 3. Kafka 이벤트    │
     │                   │ (reservation-     │
     │                   │  completed)       │
     │                   ├──────────────────▶│
     │                   │                   │
     │                   │                   │ 4. 포인트 적립
     │                   │                   │    (원금의 10%)
     │                   │                   │
     │                   │ 5. Kafka 이벤트    │
     │                   │ (point-reward-    │
     │                   │◀──────────────────┤
     │                   │  completed)       │
     │                   │                   │
     │ 6. 응답 (성공)     │                   │
     │◀──────────────────┤                   │
     │                   │                   │
```

**단계별 설명**:
1. 사용자가 예약 결제를 요청
2. Payments 서비스가 포인트 차감, 예약 생성, 결제 완료 처리
3. DB 트랜잭션 커밋 후 `ReservationCompletedKafkaEvent` Kafka 발행
4. Points 서비스가 이벤트를 수신하여 포인트 적립 (원금의 10%, 100원 단위)
5. 적립 성공 시 `PointRewardCompletedEvent` 발행
6. 사용자에게 성공 응답 반환

### 6.2 실패 및 보상 흐름 (Compensation Path)

```
┌─────────┐         ┌─────────┐         ┌─────────┐
│  User   │         │ Payments│         │ Points  │
└────┬────┘         └────┬────┘         └────┬────┘
     │                   │                   │
     │ 1. 예약 결제 요청   │                   │
     ├──────────────────▶│                   │
     │                   │                   │
     │                   │ 2. 포인트 차감      │
     │                   │    예약 생성        │
     │                   │    결제 완료        │
     │                   │                   │
     │                   │ 3. Kafka 이벤트    │
     │                   │ (reservation-     │
     │                   │  completed)       │
     │                   ├──────────────────▶│
     │                   │                   │
     │                   │                   │ 4. 포인트 적립 실패
     │                   │                   │    (Exception)
     │                   │                   │
     │                   │ 5. Kafka 이벤트    │
     │                   │ (point-reward-    │
     │                   │◀──────────────────┤
     │                   │  failed)          │
     │                   │                   │
     │                   │ 6. Kafka 이벤트    │
     │                   │ (compensate-      │
     │                   │◀──────────────────┤
     │                   │  payment)         │
     │                   │                   │
     │                   │ 7. 포인트 환불      │
     │                   │    (보상 트랜잭션)  │
     │                   │                   │
     │ 8. 응답 (성공)     │                   │
     │   - 사용자는 성공   │                   │
     │   - 내부적으로      │                   │
     │     보상 처리 완료  │                   │
     │◀──────────────────┤                   │
     │                   │                   │
```

**단계별 설명**:
1. 사용자가 예약 결제를 요청
2. Payments 서비스가 포인트 차감, 예약 생성, 결제 완료 처리
3. DB 트랜잭션 커밋 후 `ReservationCompletedKafkaEvent` Kafka 발행
4. Points 서비스가 이벤트를 수신하여 포인트 적립 시도 → **실패 (Exception)**
5. Points 서비스가 `PointRewardFailedEvent` 발행 (실패 기록)
6. Points 서비스가 **자율적으로** `CompensatePaymentEvent` 발행 (보상 트리거)
7. Payments 서비스가 보상 이벤트를 수신하여 포인트 환불 실행
8. 사용자에게는 결제 성공 응답 반환 (비동기 보상은 백그라운드에서 처리)

**중요 포인트**:
- 포인트 적립 실패 시 사용자는 이미 성공 응답을 받은 상태
- 보상 트랜잭션은 비동기로 처리되어 차감된 포인트가 환불됨
- 최종적으로 사용자 포인트는 원래 상태로 복구됨 (결제 전 - 결제 차감 + 환불 = 결제 전)

---

## 7. 기술적 구현 세부사항

### 7.1 Kafka 설정

#### Consumer 설정
```yaml
# application-local.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: concert-reservation-consumer
      auto-offset-reset: earliest
      enable-auto-commit: false  # 수동 커밋 활성화
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

**수동 커밋 사용 이유**:
- 비즈니스 로직 완료 후 명시적으로 커밋하여 At-Least-Once 보장
- 처리 실패 시 재시도 가능 (ack 미호출)

#### Producer 설정
```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all  # 모든 replica 확인 후 응답
      retries: 3
```

### 7.2 트랜잭션 보장 전략

#### Payments 서비스: Transactional Outbox Pattern 변형

**목표**: DB 트랜잭션과 Kafka 이벤트 발행의 원자성 보장

**구현**:
```java
// 1. @Transactional 내부에서 DB 작업 + 인메모리 이벤트 발행
@Transactional
public PayReservationResult pay(PayReservationCommand command) {
    // DB 작업
    Payment payment = ...;
    Reservation reservation = ...;

    // 인메모리 이벤트 발행 (트랜잭션 범위 내)
    eventPublisher.publishEvent(kafkaEvent);

    return result;
}

// 2. @TransactionalEventListener로 커밋 후 Kafka 발행
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handlePaymentCompleted(ReservationCompletedKafkaEvent event) {
    // DB 커밋 성공 후에만 실행
    paymentKafkaEventPort.publishReservationCompleted(event);
}
```

**장점**:
- DB 트랜잭션 실패 시 Kafka 이벤트 미발행
- DB 커밋 성공 시에만 Kafka 발행으로 데이터 일관성 보장

**제한사항**:
- Kafka 발행 실패 시 DB는 이미 커밋된 상태 (메시지 유실 가능)
- 향후 개선: Outbox Table + CDC(Change Data Capture) 패턴 고려

### 7.3 멱등성 보장

#### Payments 서비스: 멱등성 키 패턴

**목적**: 중복 결제 방지

**구현**:
```java
// payments/application/service/PayReservationService.java
@Override
@Transactional
public PayReservationResult pay(final PayReservationCommand command) {
    // 멱등성 체크: 이미 처리된 요청인지 확인
    if (paymentIdempotencyPort.exists(command.userId(), command.scheduleId())) {
        throw new AlreadyProcessedException("이미 처리된 결제 요청입니다");
    }

    // 비즈니스 로직 실행
    // ...

    // 멱등성 키 저장 (중복 방지)
    paymentIdempotencyPort.save(
        PaymentIdempotency.of(command.userId(), command.scheduleId())
    );

    return result;
}
```

#### Points 서비스: 멱등성 미구현

**현재 상태**: Points 서비스는 멱등성 키가 없음
**리스크**: Kafka 재처리 시 중복 적립 가능

**개선 방안**:
```java
// 제안: PointIdempotency 엔티티 추가
@Entity
public class PointIdempotency {
    @Id
    @GeneratedValue
    private Long id;

    private Long userId;
    private Long paymentId;  // 멱등성 키

    private LocalDateTime createdAt;
}

// RewardPointService에 멱등성 체크 추가
if (pointIdempotencyPort.exists(command.userId(), command.paymentId())) {
    log.warn("이미 처리된 적립 요청 - paymentId: {}", command.paymentId());
    return; // 중복 적립 방지
}
```

### 7.4 에러 핸들링 및 재시도 전략

#### Kafka Consumer 재시도

**현재 구현**:
```java
@KafkaListener(topics = "reservation-completed", groupId = "points-service")
public void handleReservationCompleted(
    @Payload final ReservationCompletedKafkaEvent event,
    final Acknowledgment ack
) {
    try {
        // 비즈니스 로직
        rewardPointUseCase.reward(command);
        ack.acknowledge(); // 성공 시 커밋
    } catch (Exception e) {
        log.error("포인트 적립 실패", e);
        // ack 미호출 → 메시지 재처리 (재시도)
        throw e;
    }
}
```

**재시도 동작**:
- `enable-auto-commit: false`로 수동 커밋 사용
- 예외 발생 시 ack 미호출 → Kafka가 메시지 재전송
- 무한 재시도 방지를 위한 DLQ(Dead Letter Queue) 필요

**개선 방안**:
```yaml
# Spring Kafka 재시도 설정 추가
spring:
  kafka:
    listener:
      ack-mode: manual  # 수동 커밋
    consumer:
      max-poll-records: 10
    retry:
      topic:
        enabled: true
        attempts: 3  # 최대 3회 재시도
        delay: 1000  # 1초 대기
        multiplier: 2.0  # 백오프 배수

# DLT (Dead Letter Topic) 설정
spring:
  kafka:
    listener:
      missing-topics-fatal: false
    template:
      default-topic: reservation-completed.DLT  # 실패 메시지 전송
```

---

## 8. Choreography 패턴의 장단점 분석

### 8.1 장점

#### 1. 느슨한 결합 (Loose Coupling)
- 각 서비스가 이벤트만 발행/구독하고 다른 서비스의 내부 구현을 모름
- 서비스 추가/제거 시 기존 서비스 수정 불필요

**예시**:
```
새로운 알림 서비스 추가 시:
- Payments/Points 서비스 수정 불필요
- reservation-completed 토픽만 구독하면 됨
```

#### 2. 확장성 (Scalability)
- 각 서비스를 독립적으로 확장 가능
- Kafka의 파티셔닝으로 수평 확장 용이

**예시**:
```
Points 서비스에 부하가 많을 경우:
- Points 서비스 인스턴스만 증설
- Kafka Consumer Group으로 부하 분산
```

#### 3. 단순한 아키텍처
- 중앙 오케스트레이터 관리 부담 없음
- 각 서비스가 자체적으로 책임 완결

### 8.2 단점

#### 1. 워크플로우 가시성 부족 (Low Visibility)
- 전체 트랜잭션 흐름을 한눈에 파악하기 어려움
- 어떤 서비스가 어떤 이벤트를 구독하는지 문서화 필수

**현재 문제**:
```
reservation-completed 이벤트 발행 시:
- Points 서비스가 구독하는지 코드를 직접 확인해야 함
- 이벤트 체인을 추적하려면 여러 파일을 탐색해야 함
```

**개선 방안**:
- 이벤트 카탈로그 문서 작성
- 분산 추적 도구(Zipkin, Jaeger) 도입

#### 2. 순환 의존성 위험 (Cyclic Dependency)
- 이벤트 체인이 복잡해지면 순환 참조 발생 가능

**예시 (주의)**:
```
Payments → reservation-completed → Points
Points → point-reward-failed → Payments
Payments → refund-completed → Points (위험!)
```

**현재 구현**:
- 현재는 단방향 흐름으로 안전함
- 향후 서비스 추가 시 주의 필요

#### 3. 디버깅 어려움
- 분산 환경에서 문제 원인 추적 곤란
- 로그가 여러 서비스에 분산되어 있음

**현재 문제**:
```
포인트 적립 실패 시:
1. Payments 서비스 로그 확인 (이벤트 발행)
2. Kafka 브로커 로그 확인 (이벤트 전달)
3. Points 서비스 로그 확인 (처리 실패)
4. Payments 서비스 로그 확인 (보상 실행)
→ 4개 위치의 로그를 조합해야 전체 흐름 파악 가능
```

**개선 방안**:
```java
// Correlation ID 패턴 적용
@Slf4j
public class ReservationCompletedKafkaListener {
    @KafkaListener(topics = "reservation-completed")
    public void handle(ReservationCompletedKafkaEvent event) {
        MDC.put("correlationId", event.getCorrelationId());
        log.info("Processing event with correlationId: {}", event.getCorrelationId());
        // ...
        MDC.clear();
    }
}
```

#### 4. 보상 로직 분산
- 각 서비스가 보상 결정을 내려 일관성 관리 어려움
- 보상 실패 시 처리 복잡도 증가

**현재 구현**:
```
Points 서비스가 보상 결정:
- 적립 실패 시 CompensatePaymentEvent 자체적으로 발행
- Payments 서비스는 무조건 보상 실행 (검증 로직 없음)
```

**리스크**:
```
Q: Payments 서비스에서 보상 트랜잭션도 실패하면?
A: 현재 구현에는 재보상 로직 없음 → 데이터 불일치 발생 가능
```

---

## 9. 개선 권장사항

### 9.1 단기 개선 (우선순위 높음)

#### 1. Points 서비스 멱등성 보장

**목적**: Kafka 재처리 시 중복 적립 방지

**구현 계획**:
```sql
-- PointIdempotency 테이블 생성
CREATE TABLE point_idempotency (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_payment (user_id, payment_id)
);
```

```java
// RewardPointService 수정
@Override
@Transactional
public RewardPointResult reward(final RewardPointCommand command) {
    // 멱등성 체크 추가
    if (pointIdempotencyPort.exists(command.userId(), command.paymentId())) {
        log.warn("중복 적립 방지 - paymentId: {}", command.paymentId());
        throw new AlreadyProcessedException("이미 처리된 적립 요청입니다");
    }

    // 포인트 적립
    final Point point = Point.reward(...);
    final Point savedPoint = pointCommandRepositoryPort.save(point);

    // 멱등성 키 저장
    pointIdempotencyPort.save(
        PointIdempotency.of(command.userId(), command.paymentId())
    );

    return RewardPointResult.of(savedPoint.getPointId(), savedPoint.getAmount());
}
```

#### 2. Dead Letter Queue (DLQ) 구현

**목적**: 재시도 실패 메시지 별도 저장 및 모니터링

**구현 계획**:
```java
// Kafka DLT 리스너 추가
@Component
@Slf4j
public class ReservationCompletedDltListener {

    @KafkaListener(
        topics = "reservation-completed.DLT",
        groupId = "points-service-dlt"
    )
    public void handleDlt(
        @Payload final ReservationCompletedKafkaEvent event,
        @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exception
    ) {
        log.error("DLT 메시지 수신 - reservationId: {}, exception: {}",
            event.reservationId(), exception);

        // 1. DLT 테이블에 저장
        dltRepository.save(DltMessage.of(
            "reservation-completed",
            event,
            exception,
            LocalDateTime.now()
        ));

        // 2. 알림 발송 (관리자)
        notificationService.sendAlert(
            "포인트 적립 최종 실패 알림",
            "reservationId: " + event.reservationId()
        );
    }
}
```

#### 3. 보상 트랜잭션 실패 핸들링

**목적**: 보상 실패 시 재보상 또는 수동 개입 트리거

**구현 계획**:
```java
// RefundPaymentService 수정
@Override
@Transactional
public RefundPaymentResult refund(final RefundPaymentCommand command) {
    try {
        // 포인트 환불
        paymentPointGatewayPort.refund(command.userId(), command.refundAmount());

        log.info("보상 트랜잭션 성공 - paymentId: {}", command.paymentId());
        return RefundPaymentResult.of(command.paymentId(), command.refundAmount());

    } catch (Exception e) {
        log.error("보상 트랜잭션 실패 - paymentId: {}, error: {}",
            command.paymentId(), e.getMessage(), e);

        // 실패 이벤트 발행 (관리자 알림 트리거)
        eventPublisher.publishEvent(CompensationFailedEvent.of(
            command.paymentId(),
            command.userId(),
            command.refundAmount(),
            e.getMessage()
        ));

        throw e;
    }
}
```

### 9.2 중기 개선 (우선순위 중간)

#### 4. 분산 추적 (Distributed Tracing) 도입

**목적**: 전체 트랜잭션 흐름 가시화 및 디버깅 용이

**도구**: Spring Cloud Sleuth + Zipkin

**구현 계획**:
```xml
<!-- pom.xml 또는 build.gradle -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  sleuth:
    sampler:
      probability: 1.0  # 100% 샘플링 (개발 환경)
  zipkin:
    base-url: http://localhost:9411
```

**효과**:
- Trace ID가 모든 로그와 Kafka 이벤트에 자동 삽입
- Zipkin UI에서 전체 트랜잭션 플로우 시각화

#### 5. Outbox Pattern 완전 구현

**목적**: Kafka 발행 실패 시에도 최종 일관성 보장

**구현 계획**:
```sql
-- Outbox 테이블 생성
CREATE TABLE outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSON NOT NULL,
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL
);
```

```java
// PayReservationService 수정
@Override
@Transactional
public PayReservationResult pay(final PayReservationCommand command) {
    // 1. 비즈니스 로직 실행
    Payment payment = ...;
    Reservation reservation = ...;

    // 2. Outbox에 이벤트 저장 (트랜잭션 범위 내)
    OutboxMessage outboxMessage = OutboxMessage.of(
        "Payment",
        payment.getPaymentId(),
        "ReservationCompleted",
        toJson(kafkaEvent)
    );
    outboxRepository.save(outboxMessage);

    // 3. 트랜잭션 커밋
    return result;
}

// 별도 스케줄러: 미발행 이벤트 폴링 및 Kafka 발행
@Scheduled(fixedDelay = 5000)
@Transactional
public void publishPendingEvents() {
    List<OutboxMessage> pending = outboxRepository.findByPublishedFalse();

    for (OutboxMessage message : pending) {
        try {
            kafkaTemplate.send(message.getEventType(), message.getPayload());
            message.markAsPublished();
            outboxRepository.save(message);
        } catch (Exception e) {
            log.error("Outbox 발행 실패 - id: {}", message.getId(), e);
        }
    }
}
```

**장점**:
- DB와 Kafka 발행의 원자성 완전 보장
- Kafka 장애 시에도 메시지 유실 방지

#### 6. 이벤트 버전 관리 (Schema Registry)

**목적**: 이벤트 스키마 변경 시 하위 호환성 보장

**도구**: Confluent Schema Registry

**구현 계획**:
```java
// 이벤트에 버전 정보 추가
public record ReservationCompletedKafkaEvent(
    String eventVersion,  // 추가: "1.0", "1.1", ...
    Long paymentId,
    Long userId,
    Long reservationId,
    Long paidAmount
) {
    public static ReservationCompletedKafkaEvent of(...) {
        return new ReservationCompletedKafkaEvent(
            "1.0",  // 버전 명시
            paymentId,
            userId,
            reservationId,
            paidAmount
        );
    }
}

// Consumer에서 버전별 처리
@KafkaListener(topics = "reservation-completed")
public void handle(ReservationCompletedKafkaEvent event) {
    switch (event.eventVersion()) {
        case "1.0" -> handleV1(event);
        case "1.1" -> handleV1_1(event);
        default -> throw new UnsupportedVersionException(event.eventVersion());
    }
}
```

### 9.3 장기 개선 (우선순위 낮음)

#### 7. Saga Orchestration으로 전환 고려

**시나리오**: 워크플로우가 더 복잡해질 경우

**장점**:
- 중앙에서 워크플로우 관리로 가시성 향상
- 보상 로직 중앙화로 일관성 관리 용이

**도구**: Netflix Conductor, Temporal, Camunda

**전환 후 아키텍처**:
```
┌────────────────────────┐
│  Saga Orchestrator     │
│  (Conductor/Temporal)  │
└───────┬────────────────┘
        │
        ├──▶ Payments Service
        ├──▶ Points Service
        └──▶ Notification Service
```

**전환 기준**:
- 서비스 수가 5개 이상으로 증가
- 보상 로직이 3단계 이상으로 복잡해짐
- 워크플로우 변경이 빈번함

#### 8. Event Sourcing 도입

**목적**: 모든 상태 변경을 이벤트로 저장하여 감사 추적 및 재현 가능

**도구**: Axon Framework, EventStore

**장점**:
- 완벽한 감사 로그
- 과거 시점의 상태 재구성 가능
- 디버깅 및 분석 용이

**단점**:
- 복잡도 증가
- 쿼리 성능 저하 (CQRS 필수)

---

## 10. 모니터링 및 운영 가이드

### 10.1 핵심 메트릭

#### Kafka 메트릭
```
1. Consumer Lag
   - 각 토픽별 미처리 메시지 수
   - 임계값: 100건 초과 시 알림

2. 이벤트 처리 시간
   - Payments: reservation-completed 발행 시간
   - Points: 포인트 적립 완료 시간
   - 임계값: P95 > 5초 시 알림

3. 이벤트 실패율
   - Points: 포인트 적립 실패 건수
   - Payments: 보상 트랜잭션 실패 건수
   - 임계값: 1% 초과 시 알림
```

#### 비즈니스 메트릭
```
1. 포인트 적립 성공률
   - (적립 성공 / 전체 결제) * 100
   - 목표: 99.9% 이상

2. 보상 트랜잭션 실행률
   - (보상 실행 / 포인트 적립 실패) * 100
   - 목표: 100%

3. 평균 적립 지연 시간
   - (적립 완료 시간 - 결제 완료 시간)
   - 목표: 5초 이내
```

### 10.2 알림 설정

```yaml
# Prometheus Alerting Rules 예시
groups:
  - name: saga-alerts
    rules:
      - alert: HighConsumerLag
        expr: kafka_consumer_lag{topic="reservation-completed"} > 100
        for: 5m
        annotations:
          summary: "포인트 서비스 Consumer Lag 높음"

      - alert: HighCompensationRate
        expr: rate(compensate_payment_total[5m]) > 0.01
        for: 5m
        annotations:
          summary: "보상 트랜잭션 실행률 높음 (1% 초과)"
```

### 10.3 장애 대응 플레이북

#### 시나리오 1: Points 서비스 다운

**증상**:
- `reservation-completed` 토픽의 Consumer Lag 증가
- 포인트 적립 완료 알림 미발송

**대응**:
1. Points 서비스 재시작
2. Consumer Lag 확인 (자동 재처리)
3. 적립 누락 건수 확인 (Kafka Consumer Offset 기반)

**복구 시간**: 5분 이내 (재시작 후 자동 재처리)

#### 시나리오 2: Kafka 브로커 장애

**증상**:
- 이벤트 발행 실패 로그 급증
- Payments/Points 서비스 모두 정상이지만 이벤트 전달 안 됨

**대응**:
1. Kafka 브로커 상태 확인 및 재시작
2. Outbox 패턴 구현 시: 미발행 이벤트 자동 재발행
3. Outbox 미구현 시: 수동 재처리 스크립트 실행

```sql
-- 미적립 결제 건 조회 (수동 재처리용)
SELECT p.payment_id, p.user_id, p.amount, p.created_at
FROM payment p
LEFT JOIN point pt ON pt.user_id = p.user_id
    AND pt.created_at > p.created_at
    AND pt.description LIKE CONCAT('%', p.payment_id, '%')
WHERE p.created_at > NOW() - INTERVAL 1 HOUR
  AND pt.point_id IS NULL;
```

#### 시나리오 3: 보상 트랜잭션 실패

**증상**:
- `CompensatePaymentEvent` 발행됨
- Payments 서비스에서 환불 실패 로그

**대응**:
1. 실패 원인 확인 (포인트 잔액 부족, 외부 API 오류 등)
2. 수동 환불 처리
3. 관리자 대시보드에서 보상 실패 건 모니터링

```sql
-- 보상 실패 건 조회
SELECT *
FROM compensation_failed_event
WHERE created_at > NOW() - INTERVAL 1 DAY
ORDER BY created_at DESC;
```

---

## 11. 결론

### 11.1 현재 구현 평가

**패턴**: SAGA Choreography
**성숙도**: 초기 단계 (MVP 수준)

**잘 구현된 점**:
1. ✅ 이벤트 기반 비동기 통신 구현
2. ✅ Payments 서비스의 트랜잭션 보장 (TransactionalEventListener)
3. ✅ 보상 트랜잭션 플로우 구현
4. ✅ Kafka 수동 커밋으로 At-Least-Once 보장

**개선 필요 사항**:
1. ❌ Points 서비스 멱등성 미구현 (중복 적립 위험)
2. ❌ Dead Letter Queue 미구현 (실패 메시지 추적 불가)
3. ❌ 보상 트랜잭션 실패 핸들링 부재
4. ❌ 분산 추적 미구현 (디버깅 어려움)
5. ❌ Outbox Pattern 미구현 (Kafka 장애 시 메시지 유실 위험)

### 11.2 향후 로드맵

#### Phase 1 (1-2주): 안정성 강화
- [ ] Points 서비스 멱등성 구현
- [ ] DLQ 구현 및 모니터링
- [ ] 보상 실패 알림 구현

#### Phase 2 (3-4주): 가시성 향상
- [ ] Spring Cloud Sleuth + Zipkin 도입
- [ ] 이벤트 카탈로그 문서 작성
- [ ] 메트릭 대시보드 구축 (Grafana)

#### Phase 3 (2-3개월): 완전성 보장
- [ ] Outbox Pattern 완전 구현
- [ ] Schema Registry 도입
- [ ] 자동 재처리 로직 고도화

#### Phase 4 (미정): 아키텍처 재평가
- [ ] Orchestration 전환 검토
- [ ] Event Sourcing 도입 검토

### 11.3 최종 의견

현재 구현된 SAGA Choreography 패턴은 **초기 MSA 전환에 적합한 구조**입니다. 서비스 간 결합도가 낮고 확장성이 우수하지만, **프로덕션 배포 전 멱등성과 DLQ 구현은 필수**입니다.

향후 서비스가 5개 이상으로 증가하거나 워크플로우가 복잡해질 경우, Orchestration 패턴으로의 전환을 고려해야 합니다. 현재 단계에서는 Choreography 패턴을 유지하면서 안정성과 가시성을 점진적으로 개선하는 것을 권장합니다.

---

## 12. 참고 자료

### 관련 파일 목록

#### Payments 모듈
- `payments/application/service/PayReservationService.java:104-116` - 이벤트 발행
- `payments/application/service/RefundPaymentService.java:22-49` - 보상 트랜잭션
- `payments/application/listener/PaymentEventListener.java:18-25` - TransactionalEventListener
- `payments/infra/adapter/in/listener/CompensatePaymentKafkaListener.java:44-69` - 보상 이벤트 구독
- `payments/infra/adapter/out/kafka/PaymentEventKafkaAdapter.java` - Kafka Producer

#### Points 모듈
- `points/application/service/RewardPointService.java` - 포인트 적립 로직
- `points/infra/adapter/in/listener/ReservationCompletedKafkaListener.java:49-127` - 이벤트 구독 및 실패 처리
- `points/infra/adapter/out/kafka/PointEventKafkaAdapter.java` - Kafka Producer

#### 이벤트 정의
- `shared/domain/event/ReservationCompletedKafkaEvent.java` - 예약 완료 이벤트
- `shared/domain/event/PointRewardCompletedEvent.java` - 적립 완료 이벤트
- `shared/domain/event/PointRewardFailedEvent.java` - 적립 실패 이벤트
- `shared/domain/event/CompensatePaymentEvent.java` - 보상 트랜잭션 이벤트

