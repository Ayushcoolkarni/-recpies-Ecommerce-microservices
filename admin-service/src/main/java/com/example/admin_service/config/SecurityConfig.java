package com.example.admin_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Security for admin-service.
 *
 * The API Gateway's AuthenticationFilter already validates the JWT and
 * blocks requests where role != ADMIN before they ever reach this service.
 * This config adds a second, defence-in-depth check by reading the
 * X-User-Role header the gateway forwards downstream.
 *
 * Any direct request that bypasses the gateway and lacks X-User-Role=ADMIN
 * will receive 403 Forbidden.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AdminRoleFilter adminRoleFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .addFilterBefore(adminRoleFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Reads the X-User-Role header set by the API Gateway and populates
     * the Spring Security context so hasRole("ADMIN") checks work.
     */
    @Component
    public static class AdminRoleFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {

            String role   = request.getHeader("X-User-Role");
            String userId = request.getHeader("X-User-Id");

            if (role != null && userId != null) {
                // Spring Security expects "ROLE_" prefix when using hasRole()
                var auth = new UsernamePasswordAuthenticationToken(
                        userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            filterChain.doFilter(request, response);
        }
    }
}
