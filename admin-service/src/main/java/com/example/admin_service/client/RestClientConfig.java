package com.example.admin_service.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Creates three load-balanced RestClient beans — one per downstream service.
 *
 * BEFORE (broken): hardcoded localhost:8082, 8083, 8081
 *   → bypasses Eureka, breaks in multi-host environments
 *
 * AFTER (fixed): lb://service-name resolved via Eureka
 *   → works correctly regardless of where each service is deployed
 */
@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient orderRestClient(RestClient.Builder builder) {
        return builder.baseUrl("http://order-service").build();
    }

    @Bean
    public RestClient inventoryRestClient(RestClient.Builder builder) {
        return builder.baseUrl("http://inventory-service").build();
    }

    @Bean
    public RestClient recipeRestClient(RestClient.Builder builder) {
        return builder.baseUrl("http://recipe-service").build();
    }

    @Bean
    public RestClient userRestClient(RestClient.Builder builder) {
        return builder.baseUrl("http://user-service").build();
    }
}
