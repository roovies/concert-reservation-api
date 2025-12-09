package com.roovies.concertreservation.config.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;

    /**
     * Kafka 메시지 전용 ObjectMapper를 별도로 등록하는 이유:
     * 1. Web MVC용 ObjectMapper와 설정 충돌 방지
     *    - Web 계층의 Jackson 설정이 Kafka 메시지 직렬화 포맷까지 영향을 주면 안 됨
     *    - Kafka 메시지는 내부 시스템 간 계약(Contract) 역할을 하므로 포맷이 안정적이어야 함
     *
     * 2. 날짜/시간 직렬화 정책을 Kafka 기준으로 명확히 고정
     *    - JavaTimeModule 적용
     *    - WRITE_DATES_AS_TIMESTAMPS 비활성화 → 항상 ISO-8601 문자열로 직렬화
     *
     * 3. JsonSerializer에서 타입 정보(@class) 삽입 기능을 안전하게 사용하기 위함
     *    - Web ObjectMapper에서 타입 정보 삽입이 활성화되면 API 보안/호환성 문제 발생 가능
     *    - Kafka 전용 ObjectMapper를 사용해 타입 정보 정책을 독립적으로 관리
     *
     * 4. 성능 최적화 및 일관성 확보
     *    - ObjectMapper는 생성 비용이 크며 캐시를 내부적으로 사용하므로 싱글톤 Bean으로 관리 필요
     *    - Producer Serializer가 동일한 ObjectMapper 인스턴스를 공유하므로 Heap 부하 감소
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                // Java 8+ Date/Time API(LocalDateTime, ZonedDateTime 등) 직렬화 지원
                // 이 모듈 없으면 java.time.* 타입 직렬화 시 InvalidDefinitionException 발생
                .registerModule(new JavaTimeModule())
                // 날짜를 숫자(timestamp) 대신 ISO-8601 문자열로 직렬화 (예: "2024-12-09T15:34:35" (사람이 읽기 쉽고, 타임존 정보 보존)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(ObjectMapper kafkaObjectMapper) {
        // YAML 설정을 기반으로 Properties 구성
        // prefix=null → spring.kafka.producer.* 값을 그대로 읽어옴
        // prefix="custom" 등 지정 시 spring.kafka.custom.producer.* 사용 가능
        Map<String, Object> props = kafkaProperties.buildProducerProperties(null);

        // ============================================================
        // 정책적 강제 설정 (환경별로 절대 달라지면 안 되는 핵심 안정성 옵션)
        // ============================================================
        // acks=all → 리더 + ISR(팔로워)까지 데이터 복제 완료해야 OK 응답 줌
        // 즉, 가장 높은 내구성 보장. 메시지 안 날아가게 하는 설정임.
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        // idempotence=true → 중복 전송 방지(재시도 중 동일 메시지가 2번 저장되는 문제 해결)
        // 내부적으로 PID/Sequence 번호 기반으로 중복 체크 수행
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // compression=snappy → 전송 시 메시지를 압축해 네트워크 비용 절감
        // CPU 오버헤드는 낮고 처리량 증가 효과 큼
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // retries=3 → 브로커 리더 재선출 등 일시적 장애 발생 시 3회까지 재전송
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        // inflight request 제한 → idempotence=true 시 5 이하 유지 권장
        // 너무 많으면 순서 꼬여서 중복 감지 실패 가능
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        log.info("Kafka Producer 초기화 - servers: {}, acks: {}, idempotence: {}",
                props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG),
                props.get(ProducerConfig.ACKS_CONFIG),
                props.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));

        // ============================================================
        // Serializer 구성
        // Custom ObjectMapper 적용 → 공통 직렬화 정책, 모듈 등록, 날짜 형식 통일 등 가능
        // YAML에 직렬화 클래스 문자열 적는 방식보다 타입 안정성 및 유지보수성 높음
        // ============================================================
        JsonSerializer<Object> valueSerializer = new JsonSerializer<>(kafkaObjectMapper);
        // addTypeInfo=true → JSON 헤더에 타입 정보 포함(멀티 DTO 전송 시 유용)
        valueSerializer.setAddTypeInfo(true);

        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(), // Key는 문자열 직렬화
                valueSerializer         // Value는 JSON 직렬화
        );
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        /*
         * Micrometer Tracing 활성화
         * - Trace/Span: 요청 흐름을 추적하기 위한 ID(Trace)와 작업 단위(Span)를 자동 생성
         * - Kafka 메시지 발행 시 이 Trace 정보가 헤더로 전파되어
         *   "HTTP 요청 → 서비스 로직 → Kafka publish" 전체 호출 경로를 한 줄로 추적 가능
         * - 장애 분석, 병목 지점 파악, 분산 환경 디버깅에 필수적인 Observability 확보용
         */
        template.setObservationEnabled(true);
        return template;
    }
}
