package com.roovies.concertreservation.config.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);

        // ============================================================
        // 정책적 강제 설정 (정확한 오프셋 관리)
        // ============================================================
        // 오프셋 자동 커밋 비활성화
        // → 직접 MANUAL ACK 모드에서 비즈니스 로직 성공 시 커밋
        // → 재처리 가능성 있지만 안전함. 메시지 절대 안 놓치고 싶을 때 사용.
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // 새 컨슈머 그룹일 경우 가장 처음부터 읽음 (latest로 하면 시작 이후 메시지부터 읽음)
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // ============================================================
        // ErrorHandlingDeserializer 래핑
        // 역직렬화 실패(잘못된 JSON 등) 발생해도 Consumer가 죽지 않고 해당 레코드만 스킵하거나 DLQ로 흘려보내도록 보호하는 구조
        // YAML로는 이런 래핑 구조를 표현할 수 없음
        // ============================================================
        // Kafka Key 역직렬화 시 ErrorHandlingDeserializer로 한 번 감싸서, 실제 역직렬화 실패를 캐치하고 recover 처리할 수 있게 함
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        // Kafka Value 역직렬화도 동일하게 예외를 흡수하도록 래핑
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // 실제 Key 역직렬화에 사용할 실제 구현체 지정 (String 형태로 역직렬화)
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        // 실제 Value 역직렬화에 사용할 구현체 지정 (JSON → Java 객체 변환)
        // 실패하면 ErrorHandlingDeserializer가 예외를 잡아서 처리
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JsonDeserializer 설정
        // 신뢰할 패키지 지정: JSON → Java 객체 변환 시 해당 패키지의 클래스만 역직렬화 허용 (보안 목적)
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.roovies.*");
        // 타입 정보 헤더 사용 여부: 프로듀서가 보낸 타입 메타데이터(JSON 헤더)를 기반으로 실제 Java 타입 매핑
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        log.info("Kafka Consumer 초기화 - servers: {}, group: {}, autoCommit: {}",
                props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG),
                props.get(ConsumerConfig.GROUP_ID_CONFIG),
                props.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Error Handler - Kafka Listener 수준의 재시도/백오프/예외 처리 정책
     * YAML로 표현 불가한 로직(지수 백오프, 예외 필터링, DLT 전송 등)은 반드시 코드에서 정의해야 함
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {

        // 지수 백오프(Exponential Backoff) 설정: 최대 3회 재시도
        // 초기 1초 → 2초 → 4초 … 최대 10초까지 증가
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000L);    // 첫 재시도까지 1초 대기
        backOff.setMaxInterval(10_000L);       // 대기 간격 최대치는 10초
        backOff.setMultiplier(2.0);            // 재시도마다 대기 시간 2배 증가

        // DefaultErrorHandler: 재시도 중에도 계속 예외를 캐치하고, 재시도 모두 실패하면 마지막 콜백(Recoverer) 실행
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, ex) -> {
                    // 재시도 소진 시 마지막으로 기록되는 recover 로직
                    log.error("재시도 소진 - topic: {}, partition: {}, offset: {}, key: {}",
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            record.key(),
                            ex);

                    // TODO: 여기서 DLT(Kafka Dead Letter Topic) 전송 또는 Slack/Email 알림 처리
                },
                backOff
        );

        // 재시도 가치가 없는 예외 → 재시도 없이 즉시 recover 로직으로 이동
        // (비즈니스 로직 상 고쳐서 다시 처리할 의미가 없는 예외)
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class, // 잘못된 입력
                NullPointerException.class      // 프로그래밍 오류 → 재시도 필요 없음
        );

        return errorHandler;
    }

    /**
     * Kafka Listener Container Factory
     * - 모든 @KafkaListener가 사용하는 공통 컨테이너 설정을 중앙에서 관리하는 Bean
     * - YAML 설정(spring.kafka.listener.*)로는 단순한 옵션만 지정 가능하며,
     *   AckMode, ErrorHandler, Micrometer 트레이싱 등 Listener 동작 제어는 코드에서만 가능
     *
     * 구성 요소 및 역할:
     *   1) ConsumerFactory 주입
     *      - 실제 KafkaConsumer 인스턴스 생성 책임을 분리하여 재사용성과 테스트 용이성 확보
     *
     *   2) 공통 ErrorHandler 적용
     *      - 모든 Listener에 동일한 재시도·백오프·예외처리 정책 적용
     *      - Listener가 예외로 중단되지 않도록 안정성 계층 제공
     *
     *   3) AckMode = MANUAL
     *      - Listener가 메시지를 처리한 “정확한 시점”에 ack.acknowledge() 호출
     *      - 자동 커밋과 달리 비즈니스 성공 기준으로 오프셋 커밋을 통제할 수 있어
     *        중복 처리/데이터 유실 방지에 유리
     *
     *   4) Micrometer Observation 활성화
     *      - Listener 단위 Trace/Span 생성 및 전파
     *      - Kafka 소비 성능/지연 분석에 사용 (OpenTelemetry 포함)
     *
     *   5) Concurrency 설정
     *      - YAML의 spring.kafka.listener.concurrency 값을 우선 사용
     *      - Listener 실행 스레드 수를 컨트롤해 파티션 병렬 처리량 확장
     *
     * 운영 기준:
     *   - KafkaListener의 실행 방식(스레드 수, 에러 처리, 커밋 전략 등)을 표준화하는 핵심 Factory
     *   - Consumer 컨테이너의 생성·동작 방식은 모두 여기서 결정되므로 반드시 코드 기반으로 관리해야 함
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // ConsumerFactory 주입 → 실제 KafkaConsumer 생성 책임을 위임
        // bootstrap.servers, deserializer, group.id 등의 설정이 여기서 적용됨
        factory.setConsumerFactory(consumerFactory);

        // 공통 ErrorHandler 적용 → 모든 @KafkaListener에서 동일한 재시도/백오프 정책 사용
        // 개별 Listener에 다른 정책 필요 시 @KafkaListener(errorHandler = "...") 사용
        factory.setCommonErrorHandler(kafkaErrorHandler);

        // AckMode 설정: MANUAL → Listener가 직접 ack.acknowledge() 호출해야 offset commit됨
        // 자동 커밋과 달리 처리 성공 시점 제어 가능 (정확한 처리 보장에 유리)
        // RECORD: 메시지 하나 처리 후 자동 커밋 / BATCH: poll() 단위로 자동 커밋
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Micrometer 기반 Observability 활성화 → Listener 레벨에서 Trace/Span 자동 생성
        // Kafka 헤더의 traceparent를 읽어 분산 트레이싱 컨텍스트 전파
        factory.getContainerProperties().setObservationEnabled(true);

        // Listener concurrency 설정: 병렬로 동작하는 Consumer Thread 수 (YAML spring.kafka.listener.concurrency 우선 사용)
        // concurrency=3이면 내부적으로 3개의 독립된 KafkaConsumer 인스턴스 생성
        // 주의: 파티션 수보다 크면 유휴 스레드 발생 (예: 파티션 2개인데 concurrency 3이면 1개는 놀게 됨)
        int concurrency = kafkaProperties.getListener().getConcurrency() != null
                ? kafkaProperties.getListener().getConcurrency()
                : 3;
        factory.setConcurrency(concurrency);

        // 초기 설정 로그 → Listener 실행 스레드 수 및 ackMode 확인용
        log.info("Kafka Listener Factory 초기화 - concurrency: {}, ackMode: MANUAL",
                concurrency);

        return factory;
    }

}
