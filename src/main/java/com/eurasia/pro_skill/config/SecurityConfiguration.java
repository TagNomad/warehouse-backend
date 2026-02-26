package com.eurasia.pro_skill.config;

import com.eurasia.pro_skill.common.ApiError;
import com.eurasia.pro_skill.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置：无状态 JWT 鉴权，放行公开端点
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ── Filter 层错误处理（@ControllerAdvice 无法捕获）────────────────────────

    /**
     * 401 — 未携带 token 或 token 无效/过期
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            ApiError error = new ApiError(
                    HttpStatus.UNAUTHORIZED.value(),
                    "未认证，请先登录",
                    request.getRequestURI()
            );
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), error);
            response.flushBuffer(); // 提交响应，阻止 Tomcat 再转发到 /error
        };
    }

    /**
     * 403 — 已认证但无访问权限
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            ApiError error = new ApiError(
                    HttpStatus.FORBIDDEN.value(),
                    "无权访问此资源",
                    request.getRequestURI()
            );
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), error);
            response.flushBuffer(); // 提交响应，阻止 Tomcat 再转发到 /error
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（使用 JWT 无状态认证，不需要 CSRF 保护）
                .csrf(AbstractHttpConfigurer::disable)

                // 无状态 Session（不使用 HttpSession 存储认证信息）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 未认证时返回 401；已认证无权限时返回 403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))

                // 请求鉴权规则
                .authorizeHttpRequests(auth -> auth
                        // 登录接口放行
                        .requestMatchers("/auth/**").permitAll()
                        // 错误处理端点放行（避免 /error 转发时再次触发 Security）
                        .requestMatchers("/error").permitAll()
                        // Swagger / OpenAPI 放行
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        // Actuator 健康检查放行
                        .requestMatchers("/actuator/**").permitAll()
                        // 其余所有接口需要认证
                        .anyRequest().authenticated()
                )

                // 在用户名密码过滤器之前插入 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
