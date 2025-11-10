package com.roovies.concertreservation.shared.infra.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Aspect // Spring AOP Aspect 선언: 횡단 관심사 처리
@Component
@RequiredArgsConstructor
@Slf4j
// Order: AOP 실행 순서 지정
// LOWEST_PRECEDENCE - 1: 가장 낮은 우선순위보다 1 높음
// 즉, 거의 마지막에 실행됨 → @Transactional보다 먼저 실행되도록 (@Transactional = LOWEST_PRECEDENCE)
// 왜? 트랜잭션 시작 전에 락을 획득해야 다른 트랜잭션과 충돌 방지
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    // SpEL 파서: 문자열 표현식을 파싱하여 실행 가능한 Expression 객체 생성
    // 예: "'order:' + #orderId" 문자열 → 평가 가능한 Expression
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        // 단일 락 vs 다중 락 판별
        boolean isMultiLock = !distributedLock.keyPrefix().isEmpty() &&
                              !distributedLock.keyList().isEmpty();

        if (isMultiLock) {
            return handleMultipleLocks(joinPoint, distributedLock);
        } else {
            return handleSingleLock(joinPoint, distributedLock);
        }
    }

    /**
     * 단일 락 처리
     */
    private Object handleSingleLock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock)
            throws Throwable {
        // 1. 동적으로 락 키 생성
        String lockKey = generateLockKey(distributedLock.key(), joinPoint);

        // 2. Redisson Lock 객체 생성
        RLock lock = redissonClient.getLock(lockKey);

        log.debug("Attempting to acquire lock: {}", lockKey);

        try {
            // 3. 락 획득 시도
            boolean acquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!acquired) {
                log.warn("Failed to acquire lock: {}", lockKey);
                throw new IllegalStateException(
                        String.format("Failed to acquire lock: %s (waited %d %s)",
                                lockKey,
                                distributedLock.waitTime(),
                                distributedLock.timeUnit())
                );
            }

            log.debug("Lock acquired: {}", lockKey);

            // 4. 비즈니스 로직 실행
            return joinPoint.proceed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Thread interrupted while waiting for lock: " + lockKey,
                    e
            );
        } finally {
            // 5. 락 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: {}", lockKey);
            }
        }
    }

    /**
     * 다중 락 처리 (좌석별 락 등)
     */
    private Object handleMultipleLocks(ProceedingJoinPoint joinPoint, DistributedLock distributedLock)
            throws Throwable {
        // 1. 다중 락 키 생성
        List<String> lockKeys = generateMultipleLockKeys(
                distributedLock.keyPrefix(),
                distributedLock.keyList(),
                joinPoint
        );

        // 2. 정렬 (데드락 방지)
        if (distributedLock.sorted()) {
            lockKeys = lockKeys.stream().sorted().toList();
        }

        log.debug("Attempting to acquire {} locks: {}", lockKeys.size(), lockKeys);

        // 3. 모든 락을 순차적으로 획득
        List<RLock> acquiredLocks = new ArrayList<>();
        try {
            for (String lockKey : lockKeys) {
                RLock lock = redissonClient.getLock(lockKey);
                boolean acquired = lock.tryLock(
                        distributedLock.waitTime(),
                        distributedLock.leaseTime(),
                        distributedLock.timeUnit()
                );

                if (!acquired) {
                    log.warn("Failed to acquire lock: {}", lockKey);
                    throw new IllegalStateException(
                            String.format("Failed to acquire lock: %s (waited %d %s)",
                                    lockKey,
                                    distributedLock.waitTime(),
                                    distributedLock.timeUnit())
                    );
                }

                acquiredLocks.add(lock);
                log.debug("Lock acquired: {}", lockKey);
            }

            log.debug("All {} locks acquired successfully", acquiredLocks.size());

            // 4. 비즈니스 로직 실행
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Thread interrupted while waiting for locks",
                    e
            );
        } finally {
            // 5. 모든 락 해제 (역순)
            for (RLock lock : acquiredLocks) {
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                } catch (Exception e) {
                    log.error("Error releasing lock", e);
                }
            }
            if (!acquiredLocks.isEmpty()) {
                log.debug("All {} locks released", acquiredLocks.size());
            }
        }
    }

    /**
     * 단일 락 키 생성
     */
    private String generateLockKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            EvaluationContext context = new StandardEvaluationContext();

            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }

            Expression expression = parser.parseExpression(keyExpression);
            Object value = expression.getValue(context);

            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.error("Failed to generate lock key from expression: {}",
                    keyExpression, e);
            throw new IllegalArgumentException(
                    "Failed to generate lock key from expression: " + keyExpression,
                    e
            );
        }
    }

    /**
     * 다중 락 키 생성
     *
     * @param keyPrefixExpression SpEL 표현식 (예: "'lock:seat:' + #command.scheduleId() + ':'")
     * @param keyListExpression SpEL 표현식 (예: "#command.seatIds()")
     * @return 생성된 락 키 리스트 (예: ["lock:seat:1:1", "lock:seat:1:2"])
     */
    private List<String> generateMultipleLockKeys(
            String keyPrefixExpression,
            String keyListExpression,
            ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            // SpEL 컨텍스트 생성
            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }

            // 1. keyPrefix 평가 (예: "lock:seat:1:")
            Expression prefixExpression = parser.parseExpression(keyPrefixExpression);
            String prefix = prefixExpression.getValue(context, String.class);

            // 2. keyList 평가 (예: [1L, 2L, 3L])
            Expression listExpression = parser.parseExpression(keyListExpression);
            Object listValue = listExpression.getValue(context);

            if (listValue == null) {
                throw new IllegalArgumentException("Lock key list is null");
            }

            // 3. List로 변환
            List<?> keyList;
            if (listValue instanceof List) {
                keyList = (List<?>) listValue;
            } else {
                throw new IllegalArgumentException(
                    "Lock key list expression must evaluate to a List, but got: " +
                    listValue.getClass().getName()
                );
            }

            // 4. prefix + 각 요소로 락 키 생성
            List<String> lockKeys = new ArrayList<>();
            for (Object item : keyList) {
                String lockKey = prefix + item.toString();
                lockKeys.add(lockKey);
            }

            if (lockKeys.isEmpty()) {
                throw new IllegalArgumentException("Lock key list is empty");
            }

            return lockKeys;

        } catch (Exception e) {
            log.error("Failed to generate multiple lock keys - prefix: {}, list: {}",
                    keyPrefixExpression, keyListExpression, e);
            throw new IllegalArgumentException(
                    "Failed to generate multiple lock keys: " + e.getMessage(),
                    e
            );
        }
    }

}
