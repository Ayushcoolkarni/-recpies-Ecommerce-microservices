package com.example.admin_service.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient orderRestClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:8082")
                .build();
    }

    @Bean
    public RestClient inventoryRestClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:8083")
                .build();
    }

    @Bean
    public RestClient recipeRestClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:8081")
                .build();
    }
}