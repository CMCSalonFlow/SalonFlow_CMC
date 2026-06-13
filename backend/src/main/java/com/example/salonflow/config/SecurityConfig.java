package com.example.salonflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Spring Security Configuration with CSRF Protection
 * 
 * CSRF (Cross-Site Request Forgery) protection is enabled by default in Spring Security 6+
 * This configuration customizes the CSRF behavior for a stateless JWT-based API.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configure the security filter chain with CSRF protection
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        // CSRF Token Request Attribute Handler
        CsrfTokenRequestAttributeHandler csrfTokenRequestAttributeHandler = 
            new CsrfTokenRequestAttributeHandler();
        
        http
            // CSRF Protection
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfTokenRequestAttributeHandler)
                .ignoringRequestMatchers(
                    "/auth/register",
                    "/auth/login",
                    "/csrf-token"
                )
            )
            
            // CORS - được cấu hình trong CorsConfig
            .cors(cors -> cors.disable())
            
            // Authorization Rules
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/csrf-token").permitAll()
                .requestMatchers("/test/**").permitAll()
                .anyRequest().authenticated()
            )
            
            // Disable Form Login (vì sử dụng JWT)
            .formLogin(form -> form.disable())
            
            // Session Management (stateless - JWT based)
            .sessionManagement(session -> session
                .sessionConcurrency(concurrency -> concurrency.maximumSessions(1))
            )
            
            // Disable HTTP Basic
            .httpBasic(basic -> basic.disable());
        
        return http.build();
    }

    /**
     * Password Encoder Bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
