package com.example.salonflow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to ensure CSRF token is included in response headers
 * 
 * This filter ensures that the CSRF token is always available in the response headers
 * for client-side requests, making it easier for frontend applications to access it.
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    /**
     * Load CSRF token from Spring Security and ensure it's available
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain)
            throws ServletException, IOException {
        
        // Get CSRF token from request attribute (set by Spring Security)
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        
        // Load the token value to initialize CSRF token cookie
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}
