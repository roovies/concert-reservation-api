# MSA 전환 및 분산 트랜잭션 처리 설계 문서

## 목차
1. [개요](#1-개요)
2. [현재 시스템 분석](#2-현재-시스템-분석)
3. [MSA 배포 단위 설계](#3-msa-배포-단위-설계)
4. [분산 트랜잭션 문제](#4-분산-트랜잭션-문제)
5. [해결방안](#5-해결방안)
6. [Production-Ready 구현](#6-production-ready-구현)
7. [모니터링 및 운영](#7-모니터링-및-운영)
8. [마이그레이션 전략](#8-마이그레이션-전략)

---

## 1. 개요

### 1.1 목적
현재 모놀리식 아키텍처의 콘서트 예약 시스템을 MSA(Microservices Architecture)로 전환하여:
- **독립적 배포 및 확장성** 확보
- **장애 격리** 및 시스템 안정성 향상
- **기술 스택 다양화** 및 팀 자율성 증대
- **비즈니스 도메인별** 개발 속도 향상

### 1.2 범위
- 도메인별 서비스 분리 설계
- 분산 트랜잭션 처리 방안
- 이벤트 기반 아키텍처 구축
- 데이터 일관성 보장 메커니즘

---

## 2. 현재 시스템 분석

### 2.1 현재 바운디드 컨텍스트
```
concerts/          - 콘서트 정보 관리
├── domain/
├── application/
└── infra/

reservations/      - 예약 관리 (좌석 홀딩, 예약 확정)
waiting/           - 대기열 관리 (Redis 기반)
payments/          - 결제 처리
points/            - 포인트 관리
users/             - 사용자 관리
venues/            - 공연장 관리
ranking/           - 랭킹 계산
alarm/             - 알림 발송
```

### 2.2 현재 트랜잭션 경계
```java
// 모놀리식: 단일 트랜잭션으로 처리
@Transactional
public PayReservationResult payReservation(PayReservationCommand command) {
    // 1. 좌석 홀딩 검증 (reservations)
    validateHeldSeats();

    // 2. 포인트 차감 (points)
    deductPoint();

    // 3. 결제 저장 (payments)
    savePayment();

    // 4. 예약 확정 (reservations)
    saveReservation();

    // 5. 이벤트 발행 (ranking, alarm)
    publishEvents();
}
```

**문제점:**
- 모든 작업이 하나의 트랜잭션에 묶임
- 부분 실패 시 전체 롤백
- 확장성 제약 (단일 DB)

---

## 3. MSA 배포 단위 설계

### 3.1 서비스 분리 기준

#### 원칙
1. **도메인 응집도**: 비즈니스 기능이 밀접하게 연관된 것끼리 그룹화
2. **데이터 소유권**: 각 서비스가 자신의 데이터를 완전히 소유
3. **독립 배포 가능성**: 다른 서비스에 영향 없이 독립 배포
4. **팀 구조**: 팀 단위로 소유 가능한 크기

### 3.2 최종 서비스 구성

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway                             │
│              (Kong, Spring Cloud Gateway)                    │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼────────┐  ┌──────▼───────┐  ┌───────▼────────┐
│ User Service   │  │Concert Service│  │Reservation Svc │
│                │  │                │  │                │
│ - users        │  │ - concerts     │  │ - reservations │
│                │  │ - venues       │  │ - waiting      │
│ DB: PostgreSQL │  │ DB: PostgreSQL │  │ DB: PostgreSQL │
│                │  │                │  │ Cache: Redis   │
└────────────────┘  └────────────────┘  └────────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼────────┐  ┌──────▼───────┐  ┌───────▼────────┐
│ Payment Svc    │  │Notification   │  │Event Store     │
│                │  │    Service    │  │   (Optional)   │
│ - payments     │  │                │  │                │
│ - points       │  │ - alarm        │  │ - Event Log    │
│ DB: PostgreSQL │  │ - ranking      │  │ DB: PostgreSQL │
│                │  │ Cache: Redis   │  │   or EventDB   │
└────────────────┘  └────────────────┘  └────────────────┘
        │                   │
        └───────────────────┘
                │
    ┌───────────▼──────────┐
    │   Message Broker     │
    │    (Kafka/RabbitMQ)  │
    └──────────────────────┘
```

### 3.3 서비스별 책임

#### 1️⃣ User Service
**책임:**
- 사용자 인증/인가
- 사용자 프로필 관리
- JWT 토큰 발급

**데이터:**
- users 테이블

**기술 스택:**
- Spring Boot 3.x
- PostgreSQL
- Redis (세션)

---

#### 2️⃣ Concert Service
**책임:**
- 콘서트 정보 관리 (CRUD)
- 공연 스케줄 관리
- 좌석 정보 관리
- 공연장 관리

**데이터:**
- concerts, schedules, seats, venues 테이블

**기술 스택:**
- Spring Boot 3.x
- PostgreSQL
- Elasticsearch (검색)

---

#### 3️⃣ Reservation Service
**책임:**
- 대기열 관리 (Redis Sorted Set)
- 좌석 홀딩 (5분 TTL)
- 예약 확정
- 예약 조회

**데이터:**
- reservations, held_seats 테이블
- Redis (대기열, 홀딩 정보)

**기술 스택:**
- Spring Boot 3.x
- PostgreSQL
- Redis (대기열, 캐시)

**특징:**
- 높은 동시성 처리 필요
- Redis 기반 분산 락

---

#### 4️⃣ Payment Service
**책임:**
- 결제 처리 (PG 연동)
- 포인트 관리 (충전, 사용, 환불)
- 결제 내역 조회
- 멱등성 보장

**데이터:**
- payments, points, payment_idempotency 테이블

**기술 스택:**
- Spring Boot 3.x
- PostgreSQL
- Redis (멱등성 키)

**특징:**
- 트랜잭션 보장 최우선
- Outbox Pattern 적용

---

#### 5️⃣ Notification Service
**책임:**
- 예약 확정 알림 (SMS, Email, Push)
- 실시간 랭킹 계산
- 주간 랭킹 집계

**데이터:**
- notification_logs, ranking 테이블
- Redis (실시간 랭킹)

**기술 스택:**
- Spring Boot 3.x
- PostgreSQL
- Redis (랭킹 캐시)
- Kafka Consumer

**특징:**
- 비동기 처리
- At-least-once 보장

---

### 3.4 서비스 간 통신 방식

#### Synchronous (동기)
```
User Service ──HTTP──> Concert Service (콘서트 정보 조회)
Reservation Service ──HTTP──> User Service (사용자 검증)
```

**사용 시점:**
- 즉시 응답이 필요한 조회 작업
- 사용자 검증 등 필수 검증

**문제점:**
- 서비스 간 결합도 증가
- 장애 전파 가능성
- 응답 시간 증가

**해결:**
- Circuit Breaker (Resilience4j)
- Timeout 설정
- Fallback 처리
- 캐싱

---

#### Asynchronous (비동기)
```
Payment Service ──Kafka──> Notification Service (결제 완료 이벤트)
Payment Service ──Kafka──> Reservation Service (예약 확정)
```

**사용 시점:**
- 결과 확인이 나중에 가능한 작업
- 이벤트 발행 및 처리
- 장기 실행 작업

**장점:**
- 서비스 간 결합도 감소
- 장애 격리
- 확장성 향상

---

## 4. 분산 트랜잭션 문제

### 4.1 문제 정의

#### 예제 시나리오: 예약 결제
```
[기존 모놀리식]
┌────────────────────────────────────────┐
│     Single Transaction (ACID)          │
│  1. 좌석 홀딩 검증                       │
│  2. 포인트 차감        ← 실패 시 전체 롤백 │
│  3. 결제 저장                            │
│  4. 예약 확정                            │
└────────────────────────────────────────┘

[MSA 환경]
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│Reservation Svc   │  │ Payment Service  │  │Notification Svc  │
│                  │  │                  │  │                  │
│ 1. 좌석 검증 ✓   │→ │ 2. 포인트 차감 ✗ │→ │ 3. 알림 발송 ?   │
│                  │  │                  │  │                  │
└──────────────────┘  └──────────────────┘  └──────────────────┘
        ↑                      ↑                      ↑
     각자의 DB            각자의 DB              각자의 DB

문제: 2번 실패 시 1번은 이미 커밋됨 → 데이터 불일치
```

### 4.2 분산 트랜잭션의 한계

#### 2PC (Two-Phase Commit) 문제점
```java
// ❌ 사용 불가: MSA에서 2PC는 안티패턴
@Transactional
public void reserveAndPay() {
    reservationDB.update();  // Phase 1: Prepare
    paymentDB.update();      // Phase 1: Prepare
    // Phase 2: Commit (모든 DB 동시 커밋)
}
```

**문제점:**
1. **성능 저하**: 모든 참여자가 락을 오래 유지
2. **가용성 저하**: 하나의 서비스 장애가 전체 블로킹
3. **확장성 제약**: 트랜잭션 코디네이터 병목
4. **복잡도 증가**: 분산 락, 데드락 처리

---

### 4.3 CAP 정리와 선택

```
   Consistency (일관성)
         /\
        /  \
       /    \
      /  CA  \
     /________\
    /  CP  AP  \
   /____________\
Availability    Partition
(가용성)       Tolerance
              (분할 내성)
```

**MSA 선택: AP (가용성 + 분할 내성)**
- 강한 일관성 포기
- 최종 일관성(Eventual Consistency) 채택
- 가용성과 성능 우선

---

## 5. 해결방안

### 5.1 Transactional Outbox Pattern

#### 개념
비즈니스 트랜잭션과 이벤트 발행을 **동일한 로컬 트랜잭션**에서 처리

```
┌────────────────────────────────────┐
│     Payment Service DB             │
│  ┌──────────────┐  ┌────────────┐ │
│  │  payments    │  │  outbox    │ │  ← 같은 트랜잭션
│  │   (결제)     │  │ (이벤트)   │ │
│  └──────────────┘  └────────────┘ │
└────────────────────────────────────┘
           │
           │ Message Relay (별도 프로세스)
           ▼
    ┌──────────────┐
    │    Kafka     │
    └──────────────┘
```

#### 장점
✅ **원자성 보장**: 결제 성공 = 이벤트 반드시 저장
✅ **At-least-once 전달**: 이벤트 유실 방지
✅ **트랜잭션 일관성**: 로컬 트랜잭션만 사용

#### 단점
⚠️ **복잡도 증가**: Message Relay 필요
⚠️ **지연 발생**: 폴링 주기에 따른 지연
⚠️ **중복 처리 가능**: Consumer에서 멱등성 필요

---

### 5.2 SAGA Pattern

#### 5.2.1 Choreography (안무 방식)

```
┌────────────┐       ┌────────────┐       ┌────────────┐
│Reservation │       │  Payment   │       │Notification│
│  Service   │       │  Service   │       │  Service   │
└──────┬─────┘       └──────┬─────┘       └──────┬─────┘
       │                    │                    │
       │ 1.ReservationCreated │                  │
       ├───────────────────>│                    │
       │                    │                    │
       │                    │ 2.PaymentProcessed │
       │                    ├───────────────────>│
       │                    │                    │
       │                    │ 3.NotificationSent │
       │<───────────────────┼────────────────────│
       │                    │                    │

실패 시:
       │                    │                    │
       │                    │ PaymentFailed      │
       │<───────────────────┤                    │
       │                    │                    │
       │ ReservationCancelled                    │
       │                    │                    │
```

**장점:**
- 중앙 조정자 불필요
- 서비스 간 결합도 낮음
- 확장성 우수

**단점:**
- 흐름 추적 어려움
- 순환 의존성 위험
- 디버깅 복잡

---

#### 5.2.2 Orchestration (지휘 방식)

```
                    ┌──────────────────┐
                    │ Saga Orchestrator│
                    │  (주문 생성)      │
                    └────────┬─────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
┌────────────┐       ┌────────────┐       ┌────────────┐
│Reservation │       │  Payment   │       │Notification│
│  Service   │       │  Service   │       │  Service   │
└────────────┘       └────────────┘       └────────────┘
     ↓                    ↓                    ↓
   Success              Success              Success
     │                    │                    │
     └────────────────────┴────────────────────┘
                          │
                    Saga Complete

실패 시:
     ↓                    ↓
   Success              Failed
     │                    │
     └────────────────────┘
              │
       Compensating Transaction
        (예약 취소 보상)
```

**장점:**
- 중앙에서 흐름 제어
- 모니터링 및 추적 용이
- 복잡한 비즈니스 로직 구현 가능

**단점:**
- 중앙 조정자가 SPOF
- 조정자 복잡도 증가
- 서비스 간 결합도 증가

---

### 5.3 보상 트랜잭션 (Compensating Transaction)

```java
// Forward Transaction
createReservation()  → reserveSeats()   → processPayment()
       ↓                     ↓                   ↓
    Success              Success              Failed
                                                 ↓
// Compensating Transaction (역순 실행)
                         unreserveSeats()  ← cancelReservation()
```

**설계 원칙:**
1. 모든 작업은 보상 가능해야 함
2. 보상 트랜잭션은 멱등해야 함
3. 역순으로 실행
4. 실패 로그 기록

---

### 5.4 이벤트 소싱 (Event Sourcing)

#### 개념
상태 변화를 이벤트 스트림으로 저장

```
기존 방식 (State-based):
┌─────────────────┐
│  reservations   │
│  id  | status   │
│  1   | PENDING  │ → UPDATE → CONFIRMED
└─────────────────┘

이벤트 소싱 (Event-based):
┌────────────────────────────────┐
│     event_store                │
│ id | event_type        | data  │
│ 1  | ReservationCreated| {...} │
│ 2  | PaymentCompleted  | {...} │
│ 3  | ReservationConfirmed|{...}│
└────────────────────────────────┘
         ↓ Projection
┌─────────────────┐
│  Read Model     │ (CQRS)
│  reservations   │
└─────────────────┘
```

**장점:**
- 완전한 감사 로그
- 시간 여행 가능 (과거 상태 재구성)
- 이벤트 재생으로 복구
- CQRS와 자연스러운 결합

**단점:**
- 복잡도 매우 높음
- 이벤트 스키마 관리 어려움
- 쿼리 성능 저하 (Projection 필요)

---

## 6. Production-Ready 구현

### 6.1 Outbox Pattern 구현

#### 6.1.1 Outbox 테이블 설계

```sql
CREATE TABLE outbox_events (
    id                  BIGSERIAL PRIMARY KEY,
    aggregate_type      VARCHAR(255) NOT NULL,  -- 'Payment', 'Reservation'
    aggregate_id        VARCHAR(255) NOT NULL,  -- 비즈니스 ID
    event_type          VARCHAR(255) NOT NULL,  -- 'PaymentCompleted'
    payload             JSONB NOT NULL,         -- 이벤트 데이터
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMP,              -- 처리 완료 시각
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, PROCESSED, FAILED
    retry_count         INTEGER NOT NULL DEFAULT 0,
    last_error_message  TEXT,
    version             INTEGER NOT NULL DEFAULT 1  -- Optimistic Locking
);

-- 인덱스
CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);
```

#### 6.1.2 Outbox Entity

```java
package com.roovies.concertreservation.shared.domain.outbox;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Transactional Outbox 패턴 엔티티.
 * <p>
 * 비즈니스 트랜잭션과 동일한 트랜잭션에서 이벤트를 저장하여
 * 이벤트 발행을 보장한다.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_status_created", columnList = "status, createdAt"),
    @Index(name = "idx_outbox_aggregate", columnList = "aggregateType, aggregateId")
})
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType;  // Payment, Reservation

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;  // PaymentCompleted, ReservationConfirmed

    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column(columnDefinition = "text")
    private String lastErrorMessage;

    @Version
    private Integer version;

    // 보호된 기본 생성자 (JPA)
    protected OutboxEvent() {}

    private OutboxEvent(
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload
    ) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.status = OutboxStatus.PENDING;
    }

    public static OutboxEvent create(
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload
    ) {
        return new OutboxEvent(aggregateType, aggregateId, eventType, payload);
    }

    /**
     * 처리 완료 표시 (멱등)
     */
    public void markAsProcessed() {
        if (this.status != OutboxStatus.PROCESSED) {
            this.status = OutboxStatus.PROCESSED;
            this.processedAt = Instant.now();
        }
    }

    /**
     * 실패 기록 및 재시도 카운트 증가
     */
    public void markAsFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.lastErrorMessage = errorMessage;
        this.retryCount++;
    }

    /**
     * 재시도 가능 여부 확인
     */
    public boolean canRetry(int maxRetries) {
        return this.retryCount < maxRetries && this.status == OutboxStatus.FAILED;
    }

    /**
     * 재시도 준비
     */
    public void prepareForRetry() {
        this.status = OutboxStatus.PENDING;
        this.lastErrorMessage = null;
    }

    // Getters
    public Long getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
    public OutboxStatus getStatus() { return status; }
    public Integer getRetryCount() { return retryCount; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public Integer getVersion() { return version; }
}
```

#### 6.1.3 Outbox Status Enum

```java
package com.roovies.concertreservation.shared.domain.outbox;

/**
 * Outbox 이벤트 처리 상태
 */
public enum OutboxStatus {
    /**
     * 처리 대기 중
     */
    PENDING,

    /**
     * Kafka로 발행 완료
     */
    PROCESSED,

    /**
     * 발행 실패 (재시도 대상)
     */
    FAILED
}
```

#### 6.1.4 Outbox Repository

```java
package com.roovies.concertreservation.shared.infra.persistence.outbox;

import com.roovies.concertreservation.shared.domain.outbox.OutboxEvent;
import com.roovies.concertreservation.shared.domain.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;

/**
 * Outbox 이벤트 Repository
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * 처리 대기 중인 이벤트를 생성 시각 순으로 조회 (비관적 락)
     * <p>
     * PESSIMISTIC_WRITE 락으로 여러 Message Relay가 동시에 조회해도
     * 각자 다른 레코드를 처리하도록 보장
     *
     * @param status 상태
     * @param limit 조회 개수
     * @return 이벤트 목록
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status " +
           "ORDER BY e.createdAt ASC " +
           "LIMIT :limit")
    List<OutboxEvent> findPendingEvents(
            @Param("status") OutboxStatus status,
            @Param("limit") int limit
    );

    /**
     * 재시도 가능한 실패 이벤트 조회
     *
     * @param status 상태 (FAILED)
     * @param maxRetries 최대 재시도 횟수
     * @param retryAfter 재시도 대기 시간 이후
     * @param limit 조회 개수
     * @return 재시도 대상 이벤트
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status " +
           "AND e.retryCount < :maxRetries " +
           "AND e.createdAt < :retryAfter " +
           "ORDER BY e.createdAt ASC " +
           "LIMIT :limit")
    List<OutboxEvent> findRetryableEvents(
            @Param("status") OutboxStatus status,
            @Param("maxRetries") int maxRetries,
            @Param("retryAfter") Instant retryAfter,
            @Param("limit") int limit
    );

    /**
     * 처리 완료된 오래된 이벤트 삭제 (정리 작업)
     *
     * @param status 상태 (PROCESSED)
     * @param before 기준 시각 (예: 7일 전)
     */
    void deleteByStatusAndProcessedAtBefore(OutboxStatus status, Instant before);
}
```

#### 6.1.5 결제 서비스에서 Outbox 저장

```java
package com.roovies.concertreservation.payments.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roovies.concertreservation.payments.application.dto.command.PayReservationCommand;
import com.roovies.concertreservation.payments.application.dto.result.PayReservationResult;
import com.roovies.concertreservation.payments.application.port.in.PayReservationUseCase;
import com.roovies.concertreservation.shared.domain.event.ReservationCompletedKafkaEvent;
import com.roovies.concertreservation.shared.domain.outbox.OutboxEvent;
import com.roovies.concertreservation.shared.infra.persistence.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox Pattern이 적용된 결제 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayReservationWithOutboxService implements PayReservationUseCase {

    private final PaymentCommandRepositoryPort paymentCommandRepositoryPort;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * 결제 처리 + Outbox 이벤트 저장 (동일 트랜잭션)
     */
    @Override
    @Transactional
    public PayReservationResult payReservation(PayReservationCommand command) {
        try {
            // 1. 결제 처리
            Payment payment = processPayment(command);

            // 2. 결제 저장 (DB)
            Payment savedPayment = paymentCommandRepositoryPort.save(payment);

            // 3. 결제 결과 생성
            PayReservationResult result = createResult(savedPayment, command);

            // 4. Outbox 이벤트 저장 (같은 트랜잭션)
            saveOutboxEvent(result);

            log.info("[PayReservationWithOutboxService] 결제 및 Outbox 저장 성공 - paymentId: {}",
                    savedPayment.getId());

            return result;

        } catch (Exception e) {
            log.error("[PayReservationWithOutboxService] 결제 처리 실패", e);
            throw new RuntimeException("결제 처리에 실패했습니다.", e);
        }
    }

    /**
     * Outbox 이벤트 저장
     * <p>
     * 결제 트랜잭션과 동일한 트랜잭션에서 저장되므로
     * 결제 성공 = 이벤트 반드시 저장 보장
     */
    private void saveOutboxEvent(PayReservationResult result) {
        try {
            // 이벤트 객체 생성
            ReservationCompletedKafkaEvent event = ReservationCompletedKafkaEvent.of(
                    result.paymentId(),
                    result.scheduleId(),
                    result.seatIds(),
                    result.userId(),
                    result.originalAmount(),
                    result.discountAmount(),
                    result.paidAmount(),
                    result.status(),
                    result.completedAt()
            );

            // JSON 직렬화
            String payload = objectMapper.writeValueAsString(event);

            // Outbox 엔티티 생성
            OutboxEvent outboxEvent = OutboxEvent.create(
                    "Payment",                           // aggregateType
                    result.paymentId().toString(),       // aggregateId
                    "ReservationCompleted",              // eventType
                    payload                              // payload (JSON)
            );

            // 저장 (결제와 같은 트랜잭션)
            outboxEventRepository.save(outboxEvent);

            log.debug("[PayReservationWithOutboxService] Outbox 이벤트 저장 - eventId: {}, paymentId: {}",
                    outboxEvent.getId(), result.paymentId());

        } catch (JsonProcessingException e) {
            log.error("[PayReservationWithOutboxService] Outbox 이벤트 직렬화 실패 - paymentId: {}",
                    result.paymentId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }

    // ... 기타 메서드
}
```

---

#### 6.1.6 Message Relay (Outbox Publisher)

```java
package com.roovies.concertreservation.shared.infra.outbox;

import com.roovies.concertreservation.shared.domain.outbox.OutboxEvent;
import com.roovies.concertreservation.shared.domain.outbox.OutboxStatus;
import com.roovies.concertreservation.shared.infra.persistence.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Outbox Message Relay (Publisher)
 * <p>
 * 주기적으로 Outbox 테이블을 폴링하여 Kafka로 이벤트 발행
 * <p>
 * Thread-Safety: @SchedulerLock으로 분산 환경에서 단일 인스턴스만 실행 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxMessageRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;
    private static final String TOPIC = "outbox-events";

    /**
     * PENDING 상태 이벤트 발행
     * <p>
     * - 매 5초마다 실행
     * - ShedLock으로 클러스터 내 단일 인스턴스만 실행
     * - 배치 단위로 처리 (메모리 효율성)
     */
    @Scheduled(fixedDelay = 5000)  // 5초
    @SchedulerLock(
            name = "outboxMessageRelay_publishPending",
            lockAtMostFor = "4s",
            lockAtLeastFor = "2s"
    )
    public void publishPendingEvents() {
        try {
            // 1. PENDING 이벤트 조회 (비관적 락)
            List<OutboxEvent> events = outboxEventRepository.findPendingEvents(
                    OutboxStatus.PENDING,
                    BATCH_SIZE
            );

            if (events.isEmpty()) {
                return;
            }

            log.info("[OutboxMessageRelay] PENDING 이벤트 발행 시작 - count: {}", events.size());

            // 2. 배치 발행
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            events.parallelStream().forEach(event -> {
                try {
                    publishEvent(event);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("[OutboxMessageRelay] 이벤트 발행 실패 - eventId: {}, error: {}",
                            event.getId(), e.getMessage(), e);
                }
            });

            log.info("[OutboxMessageRelay] PENDING 이벤트 발행 완료 - success: {}, failure: {}",
                    successCount.get(), failureCount.get());

        } catch (Exception e) {
            log.error("[OutboxMessageRelay] PENDING 이벤트 발행 중 예외 발생", e);
        }
    }

    /**
     * FAILED 상태 이벤트 재시도
     * <p>
     * - 매 30초마다 실행
     * - 최대 3회 재시도
     * - 30초 후 재시도 (exponential backoff)
     */
    @Scheduled(fixedDelay = 30000)  // 30초
    @SchedulerLock(
            name = "outboxMessageRelay_retryFailed",
            lockAtMostFor = "25s",
            lockAtLeastFor = "10s"
    )
    public void retryFailedEvents() {
        try {
            // 1. 재시도 대상 이벤트 조회
            Instant retryAfter = Instant.now().minus(30, ChronoUnit.SECONDS);

            List<OutboxEvent> events = outboxEventRepository.findRetryableEvents(
                    OutboxStatus.FAILED,
                    MAX_RETRIES,
                    retryAfter,
                    BATCH_SIZE
            );

            if (events.isEmpty()) {
                return;
            }

            log.info("[OutboxMessageRelay] FAILED 이벤트 재시도 시작 - count: {}", events.size());

            // 2. 재시도 준비 및 발행
            events.forEach(event -> {
                event.prepareForRetry();
                try {
                    publishEvent(event);
                    log.info("[OutboxMessageRelay] 재시도 성공 - eventId: {}, retryCount: {}",
                            event.getId(), event.getRetryCount());
                } catch (Exception e) {
                    log.error("[OutboxMessageRelay] 재시도 실패 - eventId: {}, retryCount: {}",
                            event.getId(), event.getRetryCount(), e);
                }
            });

        } catch (Exception e) {
            log.error("[OutboxMessageRelay] FAILED 이벤트 재시도 중 예외 발생", e);
        }
    }

    /**
     * 처리 완료된 오래된 이벤트 정리
     * <p>
     * - 매일 자정 실행
     * - 7일 이상 된 PROCESSED 이벤트 삭제
     */
    @Scheduled(cron = "0 0 0 * * *")  // 매일 자정
    @SchedulerLock(
            name = "outboxMessageRelay_cleanup",
            lockAtMostFor = "10m",
            lockAtLeastFor = "1m"
    )
    @Transactional
    public void cleanupProcessedEvents() {
        try {
            Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);

            outboxEventRepository.deleteByStatusAndProcessedAtBefore(
                    OutboxStatus.PROCESSED,
                    sevenDaysAgo
            );

            log.info("[OutboxMessageRelay] 처리 완료 이벤트 정리 완료");

        } catch (Exception e) {
            log.error("[OutboxMessageRelay] 이벤트 정리 중 예외 발생", e);
        }
    }

    /**
     * 개별 이벤트 Kafka 발행
     */
    @Transactional
    protected void publishEvent(OutboxEvent event) {
        try {
            // 1. Kafka 전송 (비동기)
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(
                            TOPIC,
                            event.getAggregateId(),  // Key: aggregateId로 파티셔닝
                            event.getPayload()        // Value: JSON payload
                    );

            // 2. 전송 완료 대기 및 처리
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    // 성공: PROCESSED로 업데이트
                    event.markAsProcessed();
                    outboxEventRepository.save(event);

                    log.debug("[OutboxMessageRelay] Kafka 발행 성공 - eventId: {}, partition: {}, offset: {}",
                            event.getId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());

                } else {
                    // 실패: FAILED로 업데이트
                    event.markAsFailed(ex.getMessage());
                    outboxEventRepository.save(event);

                    log.error("[OutboxMessageRelay] Kafka 발행 실패 - eventId: {}, error: {}",
                            event.getId(), ex.getMessage());
                }
            }).get();  // 동기 대기 (배치 처리이므로 허용)

        } catch (Exception e) {
            // 예외 발생 시 FAILED 처리
            event.markAsFailed(e.getMessage());
            outboxEventRepository.save(event);
            throw new RuntimeException("Kafka 발행 실패", e);
        }
    }
}
```

이 문서는 계속 작성 중입니다. 다음 섹션을 작성하겠습니다.
---

### 6.2 SAGA Orchestration Pattern 구현

#### 6.2.1 SAGA 상태 관리

```java
package com.roovies.concertreservation.shared.domain.saga;

/**
 * SAGA 실행 상태
 */
public enum SagaStatus {
    /**
     * 실행 시작
     */
    STARTED,

    /**
     * 정상 완료
     */
    COMPLETED,

    /**
     * 실패 - 보상 트랜잭션 필요
     */
    FAILED,

    /**
     * 보상 트랜잭션 실행 중
     */
    COMPENSATING,

    /**
     * 보상 완료 (롤백 성공)
     */
    COMPENSATED,

    /**
     * 보상 실패 (수동 개입 필요)
     */
    COMPENSATION_FAILED
}
```

#### 6.2.2 SAGA 인스턴스 엔티티

```java
package com.roovies.concertreservation.shared.domain.saga;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * SAGA 인스턴스 (Orchestrator State)
 * <p>
 * 분산 트랜잭션의 상태를 추적하고 복구를 위한 정보 저장
 */
@Entity
@Table(name = "saga_instances", indexes = {
    @Index(name = "idx_saga_status", columnList = "status"),
    @Index(name = "idx_saga_created", columnList = "createdAt")
})
public class SagaInstance {

    @Id
    private String sagaId;  // UUID

    @Column(nullable = false)
    private String sagaType;  // "ReservationPaymentSaga"

    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;  // SAGA 실행 데이터 (JSON)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    @Column(nullable = false)
    private Integer currentStep = 0;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    private Instant completedAt;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Version
    private Integer version;

    protected SagaInstance() {}

    private SagaInstance(String sagaType, String payload) {
        this.sagaId = UUID.randomUUID().toString();
        this.sagaType = sagaType;
        this.payload = payload;
        this.status = SagaStatus.STARTED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static SagaInstance start(String sagaType, String payload) {
        return new SagaInstance(sagaType, payload);
    }

    public void advanceStep() {
        this.currentStep++;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        this.status = SagaStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    public void startCompensation() {
        this.status = SagaStatus.COMPENSATING;
        this.updatedAt = Instant.now();
    }

    public void compensate() {
        this.status = SagaStatus.COMPENSATED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void compensationFailed(String errorMessage) {
        this.status = SagaStatus.COMPENSATION_FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    // Getters
    public String getSagaId() { return sagaId; }
    public String getSagaType() { return sagaType; }
    public String getPayload() { return payload; }
    public SagaStatus getStatus() { return status; }
    public Integer getCurrentStep() { return currentStep; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public Integer getVersion() { return version; }
}
```

#### 6.2.3 SAGA Orchestrator 인터페이스

```java
package com.roovies.concertreservation.shared.application.saga;

/**
 * SAGA Orchestrator 인터페이스
 * <p>
 * 분산 트랜잭션을 관리하고 실패 시 보상 트랜잭션을 실행
 */
public interface SagaOrchestrator<T> {

    /**
     * SAGA 실행
     *
     * @param command 입력 데이터
     * @return SAGA ID
     */
    String execute(T command);

    /**
     * SAGA 상태 조회
     *
     * @param sagaId SAGA ID
     * @return SAGA 상태
     */
    SagaStatus getStatus(String sagaId);

    /**
     * SAGA 보상 (수동 실행)
     *
     * @param sagaId SAGA ID
     */
    void compensate(String sagaId);
}
```

#### 6.2.4 예약-결제 SAGA 구현 예시

```java
package com.roovies.concertreservation.reservations.application.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roovies.concertreservation.shared.application.saga.SagaOrchestrator;
import com.roovies.concertreservation.shared.domain.saga.SagaInstance;
import com.roovies.concertreservation.shared.domain.saga.SagaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약-결제 SAGA Orchestrator
 * <p>
 * 흐름:
 * 1. 좌석 예약 (Reservation Service)
 * 2. 포인트 차감 (Payment Service)
 * 3. 알림 발송 (Notification Service)
 * <p>
 * 실패 시 역순으로 보상:
 * - 포인트 환불 → 예약 취소
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationPaymentSagaOrchestrator 
        implements SagaOrchestrator<ReservationPaymentCommand> {

    private final SagaInstanceRepository sagaInstanceRepository;
    private final ReservationServiceClient reservationServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public String execute(ReservationPaymentCommand command) {
        String sagaId = null;

        try {
            // 1. SAGA 시작
            String payload = objectMapper.writeValueAsString(command);
            SagaInstance saga = SagaInstance.start("ReservationPaymentSaga", payload);
            saga = sagaInstanceRepository.save(saga);
            sagaId = saga.getSagaId();

            log.info("[ReservationPaymentSaga] SAGA 시작 - sagaId: {}, userId: {}", 
                    sagaId, command.getUserId());

            // 2. Step 1: 좌석 예약
            ReservationResult reservationResult = executeStep1_CreateReservation(saga, command);

            // 3. Step 2: 포인트 차감 및 결제
            PaymentResult paymentResult = executeStep2_ProcessPayment(saga, command, reservationResult);

            // 4. Step 3: 알림 발송
            executeStep3_SendNotification(saga, command, reservationResult, paymentResult);

            // 5. SAGA 완료
            saga.complete();
            sagaInstanceRepository.save(saga);

            log.info("[ReservationPaymentSaga] SAGA 완료 - sagaId: {}", sagaId);

            return sagaId;

        } catch (Exception e) {
            log.error("[ReservationPaymentSaga] SAGA 실패 - sagaId: {}", sagaId, e);

            // 실패 시 보상 트랜잭션 실행
            if (sagaId != null) {
                compensate(sagaId);
            }

            throw new SagaExecutionException("SAGA 실행 실패", e);
        }
    }

    /**
     * Step 1: 좌석 예약
     */
    private ReservationResult executeStep1_CreateReservation(
            SagaInstance saga,
            ReservationPaymentCommand command
    ) {
        try {
            log.info("[ReservationPaymentSaga] Step 1 시작 - 좌석 예약");

            ReservationResult result = reservationServiceClient.createReservation(
                    command.getScheduleId(),
                    command.getSeatIds(),
                    command.getUserId()
            );

            saga.advanceStep();
            sagaInstanceRepository.save(saga);

            log.info("[ReservationPaymentSaga] Step 1 완료 - reservationId: {}", 
                    result.getReservationId());

            return result;

        } catch (Exception e) {
            saga.fail("좌석 예약 실패: " + e.getMessage());
            sagaInstanceRepository.save(saga);
            throw e;
        }
    }

    /**
     * Step 2: 포인트 차감 및 결제
     */
    private PaymentResult executeStep2_ProcessPayment(
            SagaInstance saga,
            ReservationPaymentCommand command,
            ReservationResult reservationResult
    ) {
        try {
            log.info("[ReservationPaymentSaga] Step 2 시작 - 결제 처리");

            PaymentResult result = paymentServiceClient.processPayment(
                    reservationResult.getReservationId(),
                    command.getUserId(),
                    reservationResult.getTotalAmount()
            );

            saga.advanceStep();
            sagaInstanceRepository.save(saga);

            log.info("[ReservationPaymentSaga] Step 2 완료 - paymentId: {}", 
                    result.getPaymentId());

            return result;

        } catch (Exception e) {
            saga.fail("결제 처리 실패: " + e.getMessage());
            sagaInstanceRepository.save(saga);
            throw e;
        }
    }

    /**
     * Step 3: 알림 발송
     */
    private void executeStep3_SendNotification(
            SagaInstance saga,
            ReservationPaymentCommand command,
            ReservationResult reservationResult,
            PaymentResult paymentResult
    ) {
        try {
            log.info("[ReservationPaymentSaga] Step 3 시작 - 알림 발송");

            notificationServiceClient.sendReservationConfirmation(
                    command.getUserId(),
                    reservationResult.getReservationId(),
                    paymentResult.getPaymentId()
            );

            saga.advanceStep();
            sagaInstanceRepository.save(saga);

            log.info("[ReservationPaymentSaga] Step 3 완료");

        } catch (Exception e) {
            // 알림 실패는 치명적이지 않으므로 로그만 기록
            log.warn("[ReservationPaymentSaga] Step 3 실패 (무시) - {}", e.getMessage());
        }
    }

    /**
     * 보상 트랜잭션 실행 (역순)
     */
    @Override
    @Transactional
    public void compensate(String sagaId) {
        SagaInstance saga = sagaInstanceRepository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("SAGA not found: " + sagaId));

        try {
            saga.startCompensation();
            sagaInstanceRepository.save(saga);

            log.info("[ReservationPaymentSaga] 보상 트랜잭션 시작 - sagaId: {}, currentStep: {}", 
                    sagaId, saga.getCurrentStep());

            ReservationPaymentCommand command = objectMapper.readValue(
                    saga.getPayload(), 
                    ReservationPaymentCommand.class
            );

            // Step 2까지 완료된 경우: 포인트 환불
            if (saga.getCurrentStep() >= 2) {
                compensateStep2_RefundPayment(command);
            }

            // Step 1까지 완료된 경우: 예약 취소
            if (saga.getCurrentStep() >= 1) {
                compensateStep1_CancelReservation(command);
            }

            saga.compensate();
            sagaInstanceRepository.save(saga);

            log.info("[ReservationPaymentSaga] 보상 트랜잭션 완료 - sagaId: {}", sagaId);

        } catch (Exception e) {
            saga.compensationFailed("보상 실패: " + e.getMessage());
            sagaInstanceRepository.save(saga);

            log.error("[ReservationPaymentSaga] 보상 트랜잭션 실패 - sagaId: {} (수동 개입 필요)", 
                    sagaId, e);

            throw new SagaCompensationException("보상 트랜잭션 실패", e);
        }
    }

    /**
     * 보상: 포인트 환불
     */
    private void compensateStep2_RefundPayment(ReservationPaymentCommand command) {
        log.info("[ReservationPaymentSaga] 보상 Step 2 - 포인트 환불");

        paymentServiceClient.refundPayment(
                command.getUserId(),
                command.getScheduleId()
        );
    }

    /**
     * 보상: 예약 취소
     */
    private void compensateStep1_CancelReservation(ReservationPaymentCommand command) {
        log.info("[ReservationPaymentSaga] 보상 Step 1 - 예약 취소");

        reservationServiceClient.cancelReservation(
                command.getScheduleId(),
                command.getUserId()
        );
    }

    @Override
    public SagaStatus getStatus(String sagaId) {
        return sagaInstanceRepository.findById(sagaId)
                .map(SagaInstance::getStatus)
                .orElseThrow(() -> new IllegalArgumentException("SAGA not found: " + sagaId));
    }
}
```

---

### 6.3 Consumer 멱등성 보장

```java
package com.roovies.concertreservation.alarm.infra.adapter.in.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 멱등성이 보장된 Kafka Consumer
 * <p>
 * Redis를 사용하여 중복 처리 방지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentReservationCompletedListener {

    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationService notificationService;

    private static final String IDEMPOTENCY_KEY_PREFIX = "consumer:idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    @KafkaListener(
            topics = "reservation-completed",
            groupId = "alarm-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleReservationCompleted(
            ReservationCompletedKafkaEvent event,
            Acknowledgment ack
    ) {
        String idempotencyKey = generateIdempotencyKey(event);

        try {
            // 1. 멱등성 체크 (SETNX)
            Boolean isFirstTime = redisTemplate.opsForValue()
                    .setIfAbsent(
                            idempotencyKey,
                            "processed",
                            IDEMPOTENCY_TTL.toMillis(),
                            TimeUnit.MILLISECONDS
                    );

            if (Boolean.FALSE.equals(isFirstTime)) {
                log.warn("[IdempotentConsumer] 중복 이벤트 감지 - paymentId: {}, userId: {}",
                        event.paymentId(), event.userId());

                // 중복이지만 ack는 전송 (재처리 방지)
                ack.acknowledge();
                return;
            }

            // 2. 비즈니스 로직 실행
            notificationService.sendReservationConfirmation(event);

            // 3. 성공 시 ack
            ack.acknowledge();

            log.info("[IdempotentConsumer] 이벤트 처리 성공 - paymentId: {}, userId: {}",
                    event.paymentId(), event.userId());

        } catch (Exception e) {
            log.error("[IdempotentConsumer] 이벤트 처리 실패 - paymentId: {}, userId: {}",
                    event.paymentId(), event.userId(), e);

            // 실패 시 멱등성 키 삭제 (재처리 허용)
            redisTemplate.delete(idempotencyKey);

            // ack 하지 않음 → Kafka에서 재전송
            throw e;
        }
    }

    /**
     * 멱등성 키 생성
     * <p>
     * 형식: consumer:idempotency:{topic}:{paymentId}
     */
    private String generateIdempotencyKey(ReservationCompletedKafkaEvent event) {
        return IDEMPOTENCY_KEY_PREFIX + "reservation-completed:" + event.paymentId();
    }
}
```

---

## 7. 모니터링 및 운영

### 7.1 분산 추적 (Distributed Tracing)

#### Micrometer Tracing + Zipkin 설정

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0  # 개발: 100%, 프로덕션: 0.1 (10%)
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans

spring:
  application:
    name: payment-service

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

#### Kafka 메시지에 Trace Context 전파

```java
@Component
public class TracingKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Tracer tracer;

    public void sendWithTracing(String topic, Object message) {
        Span span = tracer.nextSpan().name("kafka-send").start();

        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // Trace Context를 Kafka 헤더에 추가
            ProducerRecord<String, Object> record = new ProducerRecord<>(topic, message);
            tracer.propagate(span, record.headers(), KafkaHeadersPropagator.INSTANCE);

            kafkaTemplate.send(record);

        } finally {
            span.end();
        }
    }
}
```

---

### 7.2 메트릭 수집

```java
@Component
public class SagaMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter sagaStartedCounter;
    private final Counter sagaCompletedCounter;
    private final Counter sagaFailedCounter;
    private final Counter sagaCompensatedCounter;
    private final Timer sagaDurationTimer;

    public SagaMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.sagaStartedCounter = Counter.builder("saga.started")
                .description("Number of SAGAs started")
                .tag("type", "reservation-payment")
                .register(meterRegistry);

        this.sagaCompletedCounter = Counter.builder("saga.completed")
                .description("Number of SAGAs completed successfully")
                .register(meterRegistry);

        this.sagaFailedCounter = Counter.builder("saga.failed")
                .description("Number of SAGAs failed")
                .register(meterRegistry);

        this.sagaCompensatedCounter = Counter.builder("saga.compensated")
                .description("Number of SAGAs compensated")
                .register(meterRegistry);

        this.sagaDurationTimer = Timer.builder("saga.duration")
                .description("SAGA execution duration")
                .register(meterRegistry);
    }

    public void recordSagaStart() {
        sagaStartedCounter.increment();
    }

    public void recordSagaComplete() {
        sagaCompletedCounter.increment();
    }

    public void recordSagaFailed() {
        sagaFailedCounter.increment();
    }

    public void recordSagaCompensated() {
        sagaCompensatedCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordDuration(Timer.Sample sample) {
        sample.stop(sagaDurationTimer);
    }
}
```

---

### 7.3 Health Check

```java
@Component
public class OutboxHealthIndicator implements HealthIndicator {

    private final OutboxEventRepository outboxEventRepository;

    @Override
    public Health health() {
        try {
            // Pending 이벤트 수 확인
            long pendingCount = outboxEventRepository.countByStatus(OutboxStatus.PENDING);

            // Failed 이벤트 수 확인
            long failedCount = outboxEventRepository.countByStatus(OutboxStatus.FAILED);

            // 임계값 체크
            if (pendingCount > 1000) {
                return Health.down()
                        .withDetail("reason", "Too many pending events")
                        .withDetail("pendingCount", pendingCount)
                        .build();
            }

            if (failedCount > 100) {
                return Health.degraded()
                        .withDetail("reason", "Too many failed events")
                        .withDetail("failedCount", failedCount)
                        .build();
            }

            return Health.up()
                    .withDetail("pendingCount", pendingCount)
                    .withDetail("failedCount", failedCount)
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

---

## 8. 마이그레이션 전략

### 8.1 Strangler Fig Pattern (교살자 패턴)

```
Phase 1: 읽기 경로 분리
┌─────────────┐
│  Monolith   │ ← 쓰기 요청
└──────┬──────┘
       │
       ↓ DB 복제
┌──────────────┐
│Concert Service│ ← 읽기 요청 (신규)
└──────────────┘

Phase 2: 쓰기 경로 이중화
┌─────────────┐      ┌──────────────┐
│  Monolith   │─────→│Concert Service│
└─────────────┘      └──────────────┘
       ↓ Both Write        ↓
     Old DB             New DB

Phase 3: 모놀리식 제거
                     ┌──────────────┐
                     │Concert Service│ ← 모든 요청
                     └──────────────┘
```

### 8.2 마이그레이션 단계

#### Step 1: 인프라 구축 (1-2주)
- [ ] Kafka 클러스터 구축 (3 brokers)
- [ ] 서비스별 DB 분리 및 복제 설정
- [ ] API Gateway 구축
- [ ] 모니터링 스택 구축 (Prometheus, Grafana, Zipkin)

#### Step 2: 읽기 전용 서비스 분리 (2-3주)
- [ ] Concert Service 구축 (조회 API만)
- [ ] Read Replica 연결
- [ ] API Gateway에서 라우팅 설정
- [ ] 트래픽 일부 이전 (Canary)

#### Step 3: Outbox Pattern 도입 (2-3주)
- [ ] 모놀리식에 Outbox 테이블 추가
- [ ] Message Relay 구현
- [ ] Kafka Producer 연동
- [ ] 이벤트 기반 아키텍처 전환

#### Step 4: 핵심 서비스 분리 (3-4주)
- [ ] Payment Service 분리
- [ ] Reservation Service 분리
- [ ] SAGA Orchestrator 구현
- [ ] 보상 트랜잭션 구현

#### Step 5: 나머지 서비스 분리 (2-3주)
- [ ] User Service 분리
- [ ] Notification Service 분리
- [ ] 트래픽 100% 이전

#### Step 6: 모놀리식 제거 (1-2주)
- [ ] 모놀리식 트래픽 0%로 전환
- [ ] 모니터링 기간 (1-2주)
- [ ] 모놀리식 종료

---

### 8.3 롤백 계획

```java
@Component
public class CircuitBreakerConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .failureRateThreshold(50)  // 50% 실패 시 Open
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(3))
                        .build())
                .build());
    }
}

@Service
public class PaymentServiceWithFallback {

    private final CircuitBreaker circuitBreaker;
    private final WebClient paymentServiceClient;
    private final PaymentMonolithClient monolithFallback;  // 롤백용

    public PaymentResult processPayment(PaymentRequest request) {
        return circuitBreaker.executeSupplier(() -> {
            // MSA 서비스 호출
            return paymentServiceClient
                    .post()
                    .uri("/api/payments")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaymentResult.class)
                    .block();
        });
    }

    // Fallback: 모놀리식으로 복귀
    public PaymentResult fallbackToMonolith(PaymentRequest request, Throwable t) {
        log.warn("[Fallback] MSA 실패, 모놀리식으로 복귀 - error: {}", t.getMessage());
        return monolithFallback.processPayment(request);
    }
}
```

---

## 9. 결론

### 9.1 기대 효과

#### 비즈니스
- ✅ **빠른 기능 출시**: 서비스별 독립 배포
- ✅ **확장성**: 트래픽이 많은 서비스만 선택적 스케일링
- ✅ **장애 격리**: 일부 서비스 장애가 전체에 영향 없음

#### 기술
- ✅ **기술 스택 다양화**: 서비스별 최적 기술 선택
- ✅ **팀 자율성**: 팀별 소유권 및 의사결정
- ✅ **코드 복잡도 감소**: 도메인별 명확한 경계

---

### 9.2 주의사항

#### 복잡도 증가
- ⚠️ **운영 복잡도**: 모니터링, 디버깅, 트러블슈팅 어려움
- ⚠️ **네트워크 비용**: 서비스 간 통신 오버헤드
- ⚠️ **데이터 일관성**: 최종 일관성 모델 이해 필요

#### 해결 방안
- 📊 **충분한 모니터링**: Tracing, Metrics, Logging
- 🛡️ **장애 대응 자동화**: Circuit Breaker, Retry, Fallback
- 📚 **문서화**: 서비스 간 API 계약, 이벤트 스키마

---

### 9.3 Best Practices 요약

| 패턴 | 사용 시점 | 복잡도 | 일관성 보장 |
|------|----------|--------|-------------|
| **Outbox Pattern** | 이벤트 발행 보장 필요 | 중간 | At-least-once |
| **SAGA Choreography** | 서비스가 적고 단순한 흐름 | 낮음 | 최종 일관성 |
| **SAGA Orchestration** | 복잡한 비즈니스 로직 | 높음 | 최종 일관성 |
| **Event Sourcing** | 감사 로그, 시간 여행 필요 | 매우 높음 | Strong |

---

### 9.4 참고 자료

- [Microservices Patterns - Chris Richardson](https://microservices.io/patterns/)
- [Building Microservices - Sam Newman](https://www.oreilly.com/library/view/building-microservices-2nd/9781492034018/)
- [Designing Data-Intensive Applications - Martin Kleppmann](https://dataintensive.net/)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Kafka: The Definitive Guide](https://www.confluent.io/resources/kafka-the-definitive-guide/)

