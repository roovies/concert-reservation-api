package com.roovies.concertreservation.testcontainers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class MySQLTestContainer {

    @Bean
    @ServiceConnection // Spring Boot 3.1+ (자동으로 properties 설정)
    public MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("concertreservation")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
    }
}