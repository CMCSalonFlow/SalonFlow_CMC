package com.example.salonflow.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for CSRF Token Management
 * 
 * Endpoints to retrieve CSRF tokens for client-side requests
 */
@Slf4j
@RestController
public class CsrfController {

    /**
     * Get CSRF token for client-side requests
     * 
     * Spring Security automatically provides the CsrfToken through the request attribute.
     * The token will be automatically added to the response headers.
     * 
     * @param token The CSRF token provided by Spring Security
     * @return CSRF token information
     */
    @GetMapping("/csrf-token")
    public CsrfTokenResponse getCsrfToken(CsrfToken token) {
        log.debug("CSRF token requested");
        
        return CsrfTokenResponse.builder()
            .token(token.getToken())
            .headerName(token.getHeaderName())
            .parameterName(token.getParameterName())
            .build();
    }

    /**
     * POST endpoint để refresh CSRF token
     * 
     * @param token The CSRF token provided by Spring Security
     * @return CSRF token information
     */
    @PostMapping("/csrf-token/refresh")
    public CsrfTokenResponse refreshCsrfToken(CsrfToken token) {
        log.debug("CSRF token refreshed");
        
        return CsrfTokenResponse.builder()
            .token(token.getToken())
            .headerName(token.getHeaderName())
            .parameterName(token.getParameterName())
            .build();
    }
}
