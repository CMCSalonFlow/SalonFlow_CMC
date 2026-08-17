package com.example.salonflow.config;

import com.example.salonflow.security.BranchContextFilter;
import com.example.salonflow.security.CustomUserDetailsService;
import com.example.salonflow.security.JwtAuthenticationFilter;
import com.example.salonflow.security.RateLimitFilter;
import com.example.salonflow.security.SecurityHeadersFilter;
import com.example.salonflow.security.oauth.OAuth2AuthenticationFailureHandler;
import com.example.salonflow.security.oauth.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final BranchContextFilter branchContextFilter;
    private final RateLimitFilter rateLimitFilter;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final CustomUserDetailsService userDetailsService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

                // ================= CORS =================
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // ================= CSRF (JWT disable) =================
                .csrf(csrf -> csrf.disable())

                // ================= SESSION STATELESS =================
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ================= AUTH RULES =================
                .authorizeHttpRequests(auth -> auth

                        // public APIs
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/chatbot/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator", "/actuator/**").permitAll()
                        .requestMatchers("/api/v1/monitoring", "/api/v1/monitoring/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/recommendations", "/api/v1/recommendations/**").permitAll()
                        .requestMatchers("/api/v1/branches/search").permitAll()
                        .requestMatchers("/api/v1/salons/public", "/api/v1/salons/nearby").permitAll()
                        .requestMatchers("/api/v1/branches/public").permitAll()
                        .requestMatchers("/api/v1/branches/*/services/public").permitAll()
                        .requestMatchers("/api/v1/branches/*/bundles/public").permitAll()
                        .requestMatchers("/api/v1/branches/*/staff/public").permitAll()
                        .requestMatchers("/api/v1/branches/*/staff/*/availability").permitAll()
                        .requestMatchers("/api/v1/branches/*/guest-bookings").permitAll()
                        .requestMatchers("/api/v1/payments/vnpay-callback", "/api/v1/payments/vnpay-ipn").permitAll()
                        .requestMatchers("/api/v1/subscriptions/webhook").permitAll()

                        // media upload (tuỳ bạn có thể đổi authenticated)
                        .requestMatchers("/api/v1/media/**").authenticated()

                        // categories public nếu cần
                        .requestMatchers("/api/v1/categories/public").permitAll()

                        // others
                        .anyRequest().authenticated())

                // ================= DISABLE FORM LOGIN (FIX 302 ROOT CAUSE) =================
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // ================= EXCEPTION HANDLING (FIX 302 → 401 JSON) =================
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"UNAUTHORIZED\"}");
                        }))

                // ================= PROVIDER =================
                .authenticationProvider(authenticationProvider())

                // ================= FILTER CHAIN =================
                .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(branchContextFilter, JwtAuthenticationFilter.class)

                // ================= OAUTH2 =================
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler));

        return http.build();
    }
}
