package com.shopflow.product.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Header names must match exactly what api-gateway forwards.
    // Defined as constants to prevent silent typos across the codebase.
    static final String HEADER_USER_ID = "X-User-Id";
    static final String HEADER_USER_ROLE = "X-User-Role";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/api/sellers/internal/**").permitAll()
                        .requestMatchers("/api/products/internal/**").permitAll()

                        // Public read endpoints — no authentication required.
                        // Browsing products and categories is open to anyone.
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/search").permitAll()

                        // Stock operations — called by order-service internally.
                        // Authenticated as the service identity forwarded by gateway.
                        .requestMatchers(HttpMethod.POST,
                                "/api/products/*/skus/*/reserve").authenticated()
                        .requestMatchers(HttpMethod.POST,
                                "/api/products/*/skus/*/release").authenticated()

                        // Product write operations — SELLER or ADMIN only.
                        .requestMatchers(HttpMethod.POST,
                                "/api/products").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/products/**").hasAnyRole("SELLER", "ADMIN")

                        // Category management — ADMIN only.
                        .requestMatchers(HttpMethod.POST,
                                "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/categories/**").hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        gatewayHeaderFilter(),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public OncePerRequestFilter gatewayHeaderFilter() {
        return new OncePerRequestFilter() {

            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                String userId = request.getHeader(HEADER_USER_ID);
                String role   = request.getHeader(HEADER_USER_ROLE);

                // Both headers must be present for authentication to proceed.
                // A request missing either came from outside the gateway
                // or the gateway failed to enrich it — treat as unauthenticated.
                if (userId != null && role != null && !userId.isBlank() && !role.isBlank()) {

                    // Spring Security's hasRole() and hasAnyRole() automatically
                    // prepend "ROLE_" when matching. The authority must be stored
                    // with the prefix so the matchers work correctly.
                    var authority = new SimpleGrantedAuthority("ROLE_" + role);

                    // UsernamePasswordAuthenticationToken(principal, credentials, authorities)
                    // Third constructor marks the token as authenticated = true.
                    // userId stored as principal — extracted in controller via
                    // SecurityContextHolder.getContext().getAuthentication().getName()
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(authority)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

                filterChain.doFilter(request, response);
            }
        };
    }
}