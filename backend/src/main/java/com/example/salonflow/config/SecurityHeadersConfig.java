package com.example.salonflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Security Headers Configuration
 * 
 * Cấu hình các HTTP headers bảo mật bổ sung để tăng cường CSRF protection:
 * - X-Content-Type-Options: Ngăn chặn MIME-type sniffing
 * - X-Frame-Options: Ngăn chặn Clickjacking
 * - X-XSS-Protection: Kích hoạt XSS protection
 * - Strict-Transport-Security: Yêu cầu HTTPS
 * - Content-Security-Policy: Hạn chế nguồn tài nguyên
 */
@Configuration
public class SecurityHeadersConfig {

    /**
     * Cấu hình response headers
     * 
     * Lưu ý: Trong production environment, hãy kích hoạt điều này
     * bằng cách uncomment trong application.properties:
     * server.servlet.session.cookie.secure=true
     * server.servlet.session.cookie.http-only=true
     * server.servlet.session.cookie.same-site=strict
     */
    @Bean
    public WebMvcConfigurer securityHeadersConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
                registry.addInterceptor(new SecurityHeadersInterceptor());
            }
        };
    }
}
