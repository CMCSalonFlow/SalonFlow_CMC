package com.example.salonflow.security.oauth;

import com.example.salonflow.dto.auth.AuthResponse;
import com.example.salonflow.services.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    private final ObjectProvider<AuthenticationService> authenticationService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2AuthenticationToken oauthToken =
                (OAuth2AuthenticationToken) authentication;

        AuthResponse authResponse =
                authenticationService.getObject()
                        .loginWithOAuth2(
                                oauthToken.getAuthorizedClientRegistrationId(),
                                oauthToken.getPrincipal()
                        );

        String redirectUrl =
                "http://localhost:5173/oauth2/success"
                        + "?userId=" + authResponse.getUserId()
                        + "&username=" + URLEncoder.encode(
                                authResponse.getUsername(),
                                StandardCharsets.UTF_8
                        )
                        + "&email=" + URLEncoder.encode(
                                authResponse.getEmail(),
                                StandardCharsets.UTF_8
                        )
                        + "&accessToken=" + URLEncoder.encode(
                                authResponse.getAccessToken(),
                                StandardCharsets.UTF_8
                        )
                        + "&refreshToken=" + URLEncoder.encode(
                                authResponse.getRefreshToken(),
                                StandardCharsets.UTF_8
                        )
                        + "&roles=" + URLEncoder.encode(
                                String.join(",", authResponse.getRoles()),
                                StandardCharsets.UTF_8
                        );

        response.sendRedirect(redirectUrl);
    }
}