package com.example.api_gateway.filter;

import com.example.api_gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * AuthenticationFilter validates the JWT on every protected route.
 *
 * On success it forwards three headers downstream so services know who is calling:
 *   X-User-Id    — the userId (JWT subject)
 *   X-User-Email — the email claim
 *   X-User-Role  — the role claim (CUSTOMER | ADMIN)
 *
 * /admin/** routes additionally require role == ADMIN (403 otherwise).
 */
@Slf4j
@Component
public class AuthenticationFilter
        extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_PATH_PREFIX = "/admin";

    private final JwtUtil jwtUtil;

    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // ── 1. Extract Authorization header ──────────────────
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                log.warn("Missing or malformed Authorization header for path: {}", path);
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(BEARER_PREFIX.length());

            // ── 2. Validate token ─────────────────────────────────
            if (!jwtUtil.isTokenValid(token)) {
                log.warn("Invalid or expired JWT for path: {}", path);
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            }

            String userId = jwtUtil.extractUserId(token);
            String email  = jwtUtil.extractEmail(token);
            String role   = jwtUtil.extractRole(token);

            // ── 3. Admin route guard ──────────────────────────────
            if (path.startsWith(ADMIN_PATH_PREFIX) && !"ADMIN".equalsIgnoreCase(role)) {
                log.warn("Access denied: userId={} (role={}) tried to access {}", userId, role, path);
                return reject(exchange, HttpStatus.FORBIDDEN, "Admin access required");
            }

            // ── 4. Forward identity headers downstream ────────────
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id",    userId)
                    .header("X-User-Email", email)
                    .header("X-User-Role",  role)
                    .build();

            log.debug("Authenticated userId={} role={} → {}", userId, role, path);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    // ── helpers ───────────────────────────────────────────────────

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String body = String.format("{\"status\":%d,\"error\":\"%s\"}", status.value(), message);
        var buffer = response.bufferFactory().wrap(body.getBytes());

        return response.writeWith(Mono.just(buffer));
    }

    // Config class required by AbstractGatewayFilterFactory
    public static class Config {
        // No config properties needed — filter is stateless
    }
}
