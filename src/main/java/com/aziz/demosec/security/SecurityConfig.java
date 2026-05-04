package com.aziz.demosec.security;

import com.aziz.demosec.security.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public DaoAuthenticationProvider authProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder);
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider authProvider) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authProvider)
                .authorizeHttpRequests(auth -> auth

                        // ─── PUBLIC ──────────────────────────────────────────
                        .requestMatchers("/auth/**").permitAll()

                        // ─── WEBSOCKET ───────────────────────────────────────
                        .requestMatchers("/ws", "/ws/**").permitAll()

                        // ─── AI ELIGIBILITY ────────────────────────────────
                        .requestMatchers("/api/ai/**").permitAll()

                        // ─── WS TEST (temporaire — supprimer après validation) ─
                        .requestMatchers("/api/ws-test/**").permitAll()

                        // ─── DONATION (temporairement public pour tester) ─────
                        .requestMatchers("/api/donations").permitAll()
                        .requestMatchers("/api/donations/**").permitAll()
                        .requestMatchers("/api/aid-requests").permitAll()
                        .requestMatchers("/api/aid-requests/**").permitAll()

                        // ─── EMERGENCY (temporairement public pour tester) ────
                        .requestMatchers("/api/emergency-alerts").permitAll()
                        .requestMatchers("/api/emergency-alerts/**").permitAll()
                        .requestMatchers("/api/interventions").permitAll()
                        .requestMatchers("/api/interventions/**").permitAll()
                        .requestMatchers("/api/ambulances").permitAll()
                        .requestMatchers("/api/ambulances/**").permitAll()
                        .requestMatchers("/api/smart-devices").permitAll()
                        .requestMatchers("/api/smart-devices/**").permitAll()

                        // ✅ Endpoints publics — auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // ✅ Endpoints publics — données de référence (accessibles sans token)
                        .requestMatchers("/api/v1/clinics").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/doctors").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/doctors/**").permitAll()
                        .requestMatchers("/api/home-care-services/**").permitAll()
                        .requestMatchers("/api/home-care-services").permitAll()

                        // ✅ Endpoints protégés par rôle
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/doctor/**").hasRole("DOCTOR")
                        .requestMatchers("/api/clinic/**").hasRole("CLINIC")
                        .requestMatchers("/api/pharmacist/**").hasRole("PHARMACIST")
                        .requestMatchers("/api/laboratory/**").hasRole("LABORATORYSAFF")
                        .requestMatchers("/api/nutritionist/**").hasRole("NUTRITIONIST")
                        .requestMatchers("/api/visitor/**").hasRole("VISITOR")
                        .requestMatchers("/api/patient/**").hasRole("PATIENT")
                        .requestMatchers("/api/baby-care/**").hasRole("PATIENT")
                        .requestMatchers("/api/home-care/**").hasRole("HOME_CARE_PROVIDER")


                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}