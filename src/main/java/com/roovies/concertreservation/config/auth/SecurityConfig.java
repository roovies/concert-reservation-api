package com.roovies.concertreservation.config.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // ✅ CSRF 비활성화 (6.1 이상)
                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/v1/**").permitAll()  // /api/v1/ 하위 경로 다 허용
//                        .anyRequest().authenticated() // 나머지는 인증 필요
                        .anyRequest().permitAll()  // 모든 요청 허용 (임시)
                );


        return http.build();
    }

}
