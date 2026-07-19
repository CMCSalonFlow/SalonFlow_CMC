package com.example.salonflow.config;

import com.example.salonflow.config.properties.FcmProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FcmProperties.class)
public class FcmConfig {
}
