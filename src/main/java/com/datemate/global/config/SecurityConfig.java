package com.datemate.global.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 설정
 * 1. CORS 허용 — React Native 개발 환경 대응
 * 2. CSRF 비활성화 — JWT 기반 Stateless 인증
 * 3. 세션 미사용 — STATELESS 정책
 * 4. 인증 경로 설정 — /api/v1/auth/**, /api/v1/courses/shared**, /api/v1/places/photo** 허용
 *
 * TODO: 프로덕션에서는 JWT 필터를 추가하고 경로별 인증을 세분화할 것
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // 공개 API — 인증 불필요
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/courses/shared/**").permitAll()
                .requestMatchers("/api/v1/places/photo/**").permitAll()
                // Dev: 모든 API를 허용 (프로덕션에서는 인증 필수로 전환)
                .requestMatchers("/api/v1/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * CORS 설정
     * 1. 개발 환경: localhost + Expo 개발 서버 허용
     * 2. React Native는 origin이 없을 수 있으므로 allowedOriginPatterns 사용
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 개발 환경 — 모든 origin 허용
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
