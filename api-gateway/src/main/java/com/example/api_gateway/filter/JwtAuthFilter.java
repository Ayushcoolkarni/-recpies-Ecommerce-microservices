package com.recipeecom.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT Authentication Filter for Spring Cloud Gateway.
 *
 * Add to your application.yml route like:
 *
 *   filters:
 *     - JwtAuth
 *
 * Public routes (login, register, health) are whitelisted and bypass this filter.
 */
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /** Routes that do NOT require a JWT token */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/users/register",
            "/api/users/login",
            "/api/users/refresh",
            "/actuator/health",
            "/actuator/info"
    );

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // Skip auth for public routes
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange, "Missing or malformed Authorization header");
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = validateToken(token);

                // Forward user identity to downstream services as headers
                ServerWebExchange mutated = exchange.mutate()
                        .request(r -> r
                                .header("X-User-Id",    claims.getSubject())
                                .header("X-User-Email", claims.get("email", String.class) != null
                                        ? claims.get("email", String.class) : "")
                                .header("X-User-Role",  claims.get("role",  String.class) != null
                                        ? claims.get("role",  String.class) : "USER")
                        )
                        .build();

                return chain.filter(mutated);

            } catch (Exception e) {
                return unauthorized(exchange, "Invalid or expired JWT token");
            }
        };
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        var buffer = exchange.getResponse().bufferFactory()
                .wrap(("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public static class Config {
        // No config properties needed — add here if you want per-route customisation
    }
}
