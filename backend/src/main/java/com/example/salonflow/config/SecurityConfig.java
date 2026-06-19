package com.example.salonflow.config;

import com.example.salonflow.security.BranchContextFilter;
import com.example.salonflow.security.CustomUserDetailsService;
import com.example.salonflow.security.JwtAuthenticationFilter;
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

    private final JwtAuthenticationFilter jwtFilter;
    private final BranchContextFilter branchContextFilter;

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

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(
                passwordEncoder()
        );

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
                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource
                        )
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )
                .authorizeHttpRequests(auth ->
                        auth

                                .requestMatchers(
                                        "/api/v1/auth/**"
                                ).permitAll()

                                .requestMatchers(
                                        "/oauth2/**",
                                        "/login/oauth2/**"
                                ).permitAll()

                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**"
                                ).permitAll()

                                .anyRequest()
                                .authenticated()
                )
                .authenticationProvider(
                        authenticationProvider()
                )
                .oauth2Login(oauth2 ->
                        oauth2
                                .successHandler(oauth2SuccessHandler)
                                .failureHandler(oauth2FailureHandler)
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
