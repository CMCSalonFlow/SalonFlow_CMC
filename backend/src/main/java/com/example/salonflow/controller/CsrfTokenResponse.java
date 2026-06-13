package com.example.salonflow.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for CSRF Token Response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsrfTokenResponse {
    
    /**
     * The CSRF token value
     */
    private String token;
    
    /**
     * The HTTP header name to include the token in requests
     * Default: X-CSRF-TOKEN
     */
    private String headerName;
    
    /**
     * The form parameter name if using form submission
     * Default: _csrf
     */
    private String parameterName;
}
