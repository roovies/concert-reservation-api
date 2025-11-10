package com.roovies.concertreservation.shared.infra.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

// 메서드에만 적용 가능 (클래스나 필드에는 불가)
// 분산락은 특정 메서드의 실행을 제어하는 것이므로 METHOD가 적절
@Target(ElementType.METHOD)

// 런타임에 어노테이션 정보가 유지됨
// AOP가 실행 시점에 이 어노테이션을 읽어야 하므로 RUNTIME 필요
// (SOURCE나 CLASS면 컴파일 후 사라짐)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    /**
     * 단일 락 키 (SpEL 표현식 지원)
     *
     * 예시:
     * - "'reservation:' + #concertId" -> 파라미터 값으로 동적 키 생성
     * - "'payment:' + #request.userId" -> 객체 필드 접근
     *
     * 참고: key와 keyPrefix는 동시에 사용할 수 없음
     */
    String key() default "";

    /**
     * 다중 락 키 접두사 (SpEL 표현식 지원)
     *
     * keyList와 함께 사용하여 여러 개의 락 키 생성
     * 예: keyPrefix = "'lock:seat:' + #command.scheduleId() + ':'"
     *     keyList = "#command.seatIds()"
     *     -> ["lock:seat:1:1", "lock:seat:1:2", "lock:seat:1:3"]
     */
    String keyPrefix() default "";

    /**
     * 다중 락 키 리스트 (SpEL 표현식 지원)
     *
     * keyPrefix와 함께 사용
     * 리스트의 각 요소가 keyPrefix와 결합되어 락 키 생성
     * 예: keyList = "#command.seatIds()"
     *     -> [1L, 2L, 3L]
     */
    String keyList() default "";

    /**
     * 다중 락 키 정렬 여부 (데드락 방지)
     *
     * true: 락 키를 정렬하여 항상 동일한 순서로 획득 (데드락 방지)
     * false: 정렬하지 않음 (순서 보장 필요 시)
     */
    boolean sorted() default true;

    /**
     * 락 획득을 기다리는 최대 시간
     *
     * 0L (기본값): 락을 즉시 획득 시도, 실패하면 바로 예외
     * 5L: 5밀리초 동안 대기, 그래도 실패하면 예외
     *
     * 사용 이유:
     * - 무한 대기 방지 (데드락 상황 회피)
     * - 빠른 실패로 사용자 경험 개선
     */
    long waitTime() default 0L;

    /**
     * 락을 자동으로 해제하는 시간 (Lease Time)
     *
     * 0L (기본값): 락을 명시적으로 unlock할 때까지 유지
     * 3000L: 3초 후 자동 해제
     *
     * 사용 이유:
     * - 애플리케이션이 죽어서 unlock을 못 하는 경우 대비
     * - 예외 발생으로 finally 블록이 실행 안 되는 극단적 상황 방지
     * - 하지만 0L(수동 해제)이 기본값인 이유는 명시적 제어가 더 안전하기 때문
     */
    long leaseTime() default 0L;

    /**
     * 시간 단위
     *
     * MILLISECONDS (기본값): 밀리초 단위로 시간 지정
     * - 세밀한 제어 가능
     * - Redis 타임아웃도 보통 밀리초 단위
     *
     * 다른 옵션: SECONDS, MINUTES 등도 가능
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}
