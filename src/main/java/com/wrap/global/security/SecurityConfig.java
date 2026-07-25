package com.wrap.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.common.ApiResponse.ErrorBody;
import com.wrap.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST, "/members/signup", "/api/members/login"
                        ).permitAll()
                        .requestMatchers(
                                "/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**", "/api/test"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
                            ErrorBody errorBody = new ErrorBody(
                                    errorCode.getCode(), errorCode.getMessage(), List.of()
                            );
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(
                                    response.getWriter(), ApiResponse.fail(errorBody)
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            ErrorCode errorCode = ErrorCode.FORBIDDEN;
                            ErrorBody errorBody = new ErrorBody(
                                    errorCode.getCode(), errorCode.getMessage(), List.of()
                            );
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(
                                    response.getWriter(), ApiResponse.fail(errorBody)
                            );
                        })
                );

        return http.build();
    }
}
