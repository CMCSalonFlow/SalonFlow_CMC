package com.example.salonflow.config;

import com.example.salonflow.security.BranchContextFilter;
import com.example.salonflow.security.CustomUserDetailsService;
import com.example.salonflow.security.JwtAuthenticationFilter;
import com.example.salonflow.security.RateLimitFilter;
import com.example.salonflow.security.SecurityHeadersFilter;
import com.example.salonflow.security.oauth.OAuth2AuthenticationFailureHandler;
import com.example.salonflow.security.oauth.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter     jwtFilter;
    private final BranchContextFilter branchContextFilter;
    private final RateLimitFilter             rateLimitFilter;      // ← MỚI
    private final SecurityHeadersFilter       securityHeadersFilter; // ← MỚI
    private final CustomUserDetailsService    userDetailsService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;
    private final CorsConfigurationSource     corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
            // ── CORS ─────────────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // ── CSRF: tắt vì dùng JWT Bearer token (stateless) ───
            // JWT trong Authorization header tự nhiên chống CSRF
            // vì browser không tự gửi header này cross-origin
            .csrf(csrf -> csrf.disable())

            // ── Session: STATELESS vì dùng JWT ───────────────────
            // ⚠️  Sửa từ IF_REQUIRED → STATELESS cho nhất quán với JWT
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ── Phân quyền endpoint ───────────────────────────────
            .authorizeHttpRequests(auth -> auth
                    // Auth public
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    // OAuth2 public
                    .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                    // Swagger public
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    // Tất cả còn lại yêu cầu đăng nhập
                    .anyRequest().authenticated()
            )

            .authenticationProvider(authenticationProvider())

            // ── OAuth2 Login ──────────────────────────────────────
            .oauth2Login(oauth2 -> oauth2
                    .successHandler(oauth2SuccessHandler)
                    .failureHandler(oauth2FailureHandler)
            )             

            // ── Thứ tự filter (quan trọng!) ───────────────────────
            //
            //  SecurityHeadersFilter          (chạy đầu tiên, set headers)
            //       ↓
            //  RateLimitFilter                (kiểm tra IP, block nếu vượt limit)
            //       ↓
            //  JwtAuthenticationFilter        (xác thực token)
            //       ↓
            //  UsernamePasswordAuthFilter     (Spring Security default)
            //
            .addFilterBefore(
                    securityHeadersFilter,
                    UsernamePasswordAuthenticationFilter.class
            )
            .addFilterBefore(
                    rateLimitFilter,
                    UsernamePasswordAuthenticationFilter.class
            )
            .addFilterBefore(
                    jwtFilter,
                    UsernamePasswordAuthenticationFilter.class
                )
                .addFilterAfter(
                        branchContextFilter,
                        JwtAuthenticationFilter.class
            );
                

        return http.build();
    }
}
