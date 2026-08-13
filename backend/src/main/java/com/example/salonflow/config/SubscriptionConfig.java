package com.example.salonflow.config;

import com.example.salonflow.config.properties.StripeProperties;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
@RequiredArgsConstructor
@Slf4j
public class SubscriptionConfig {

    private final StripeProperties stripeProperties;

    @PostConstruct
    public void initStripe() {
        if (stripeProperties.getApiKey() != null && !stripeProperties.getApiKey().trim().isEmpty()) {
            Stripe.apiKey = stripeProperties.getApiKey().trim();
            log.info("Stripe SDK initialized successfully.");
        } else {
            log.warn("Stripe API key is not set. Stripe operations will run in mock mode.");
        }
    }
}
