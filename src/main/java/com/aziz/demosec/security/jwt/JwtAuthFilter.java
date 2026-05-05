package com.aziz.demosec.security.jwt;

import com.aziz.demosec.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.ExpiredJwtException;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/auth/**",
            "/api/auth/**",
            "/api/home-care-services/**",
            "/uploads/**",
            "/api/donations/**",
            "/api/aid-requests/**",
            "/api/emergency-alerts/**",
            "/api/interventions/**",
            "/api/ambulances/**",
            "/api/smart-devices/**",
            "/api/laboratories/**",
            "/api/clinics/**",
            "/api/upload/**",
            "/api/pharmacy/orders/*/invoice",
            "/api/pharmacy/deliveries/**",
            "/api/homecare/services/**",
            "/api/homecare/providers/**",
            "/error/**",
            "/ws/**");

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        // Skip OPTIONS preflight requests (CORS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip public endpoints using AntPathMatcher
        AntPathMatcher matcher = new AntPathMatcher();
        for (String endpoint : PUBLIC_ENDPOINTS) {
            if (matcher.match(endpoint, path)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // Read JWT from Authorization header OR ?token= query param (SSE)
        String authHeader = request.getHeader("Authorization");
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else if (request.getParameter("token") != null) {
            jwt = request.getParameter("token");
        }

        if (jwt == null) {
            log.debug("JWT filter skip: method={}, uri={}", request.getMethod(), path);
            filterChain.doFilter(request, response);
            return;
        }

        String userEmail;

        try {
            userEmail = jwtService.extractEmail(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT valid: subject={}, authorities={}", userEmail, userDetails.getAuthorities());
                } else {
                    log.debug("JWT invalid: subject={}", userEmail);
                }
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("JWT expired");
            return;
        } catch (Exception e) {
            log.error("JWT processing failed", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid JWT");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
