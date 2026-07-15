package com.example.salonflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(
        basePackages = "com.example.salonflow.search.repository"
)
public class ElasticsearchConfig {
}