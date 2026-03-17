package com.example.api_gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Returns structured JSON for all gateway-level errors instead of
 * the default Spring Whitelabel page.
 */
@Slf4j
@Order(-1)
@Configuration
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
        } else if (ex instanceof NotFoundException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }

        log.error("Gateway error [{}]: {}", status, ex.getMessage());

        String body = String.format(
                "{\"status\":%d,\"error\":\"%s\",\"timestamp\":\"%s\"}",
                status.value(),
                ex.getMessage() != null ? ex.getMessage().replace("\"", "'") : status.getReasonPhrase(),
                LocalDateTime.now()
        );

        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
}
