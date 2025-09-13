package com.roovies.concertreservation.config.cache;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableRedisRepositories(basePackages = "com.roovies.concertreservation")
public class RedisConfig {
    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(){
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        // config.setPassword("your_password"); // 필요한 경우
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
//                .setPassword("password") 필요한 경우
                .setConnectionMinimumIdleSize(10)     // 최소 유휴 커넥션 수 (풀 내 항상 유지할 커넥션 수)
                .setConnectionPoolSize(20)            // 커넥션 풀 최대 수 (최대 동시 커넥션 수)
                .setIdleConnectionTimeout(10000)      // 유휴 커넥션이 닫히기까지 대기 시간 (ms) — 기본 10000ms (10초)
                .setConnectTimeout(10000)             // Redis 서버 연결 시도 타임아웃 (ms)
                .setTimeout(3000);                    // Redis 명령어 응답 대기 시간 (ms) — 소켓 레벨에서 timeout 처리
        return Redisson.create(config);
    }
}
