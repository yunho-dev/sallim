package com.sallim.global.security;

import com.sallim.global.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable) // 기본 로그아웃 기능 비활성화
                .authorizeHttpRequests(auth -> auth
                        // 로그인/회원가입은 인증 전 상태에서 호출돼야 하는 경로라 예외적으로 permitAll
                        .requestMatchers(
                                "/", "/login", "/signup",
                                "/features", "/security", "/contact",
                                "/api/members/login",
                                "/api/members/signup",
                                "/api/members/check-id",
                                "/api/members/check-email"
                        ).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/libs/**", "/fonts/**", "/images/**").permitAll()
                        .anyRequest().authenticated() // 나머지 요청은 전부 인증 필요
                )
                // 인증이 안된 채로 들어왔을 때 호출하는 콜백
                // API 요청(/api/**)이면 401만 던지고(프론트 JS가 status 보고 처리),
                // 페이지 요청(SSR)이면 로그인 페이지로 리다이렉트
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String uri = request.getRequestURI();
                            if (uri.startsWith("/api/")) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            } else {
                                response.sendRedirect("/login"); // 로그인 폼 GET 경로에 맞춰서
                            }
                        })
                )
                // JWT 검증 필터를 UsernamePasswordAuthenticationFilter보다 먼저 실행
                // → authorizeHttpRequests가 인증 여부를 판단하기 전에 SecurityContext가 채워져 있어야 하기 때문
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}