package com.aziz.demosec.security;

import com.aziz.demosec.security.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
@EnableMethodSecurity
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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authProvider)
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/auth/**", "/api/auth/**", "/error/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/uploads/comments/**").permitAll()
                        .requestMatchers("/api/home-care-services/**").permitAll()
                        .requestMatchers("/api/donations/**", "/api/aid-requests/**").permitAll()
                        .requestMatchers("/api/emergency-alerts/**", "/api/interventions/**").permitAll()
                        .requestMatchers("/api/ambulances/**", "/api/smart-devices/**").permitAll()
                        .requestMatchers("/api/laboratories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clinics/**", "/api/v1/doctors/**").permitAll()
                        .requestMatchers("/api/upload/**").permitAll()
                        .requestMatchers("/api/pharmacy/orders/*/invoice").permitAll()
                        .requestMatchers("/api/pharmacy/deliveries/**").permitAll()
                        .requestMatchers("/api/homecare/services", "/api/homecare/services/**").permitAll()
                        .requestMatchers("/api/homecare/providers/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/forum/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/forum/posts/**").hasAnyRole(
                                "DOCTOR", "CLINIC", "PHARMACIST", "LABORATORY_STAFF",
                                "NUTRITIONIST", "HOME_CARE_PROVIDER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/forum/posts/**").hasAnyRole(
                                "DOCTOR", "CLINIC", "PHARMACIST", "LABORATORY_STAFF",
                                "NUTRITIONIST", "HOME_CARE_PROVIDER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/forum/posts/**").hasAnyRole(
                                "DOCTOR", "CLINIC", "PHARMACIST", "LABORATORY_STAFF",
                                "NUTRITIONIST", "HOME_CARE_PROVIDER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/forum/comments/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/forum/comments/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/forum/comments/**").authenticated()
                        .requestMatchers("/api/forum/posts/*/like").authenticated()

                        .requestMatchers("/treatment/**", "/diagnosis/**", "/consultation/**", "/prescription/**")
                            .hasAnyRole("DOCTOR", "NUTRITIONIST")

                        .requestMatchers("/api/code-blue/**").authenticated()
                        .requestMatchers("/api/forum/messaging/**").authenticated()
                        .requestMatchers("/api/forum/whatsapp/**").authenticated()

                        .requestMatchers("/api/admin/**", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/doctor/**", "/doctor/**").hasRole("DOCTOR")
                        .requestMatchers("/api/clinic/**", "/clinic/**").hasRole("CLINIC")
                        .requestMatchers("/api/pharmacist/**", "/pharmacist/**").hasRole("PHARMACIST")
                        .requestMatchers("/api/laboratory/**", "/laboratory/**").hasRole("LABORATORY_STAFF")
                        .requestMatchers("/api/nutritionist/**", "/nutritionist/**").hasRole("NUTRITIONIST")
                        .requestMatchers("/api/visitor/**", "/visitor/**").hasRole("VISITOR")
                        .requestMatchers("/api/patient/**", "/patient/**").hasRole("PATIENT")
                        .requestMatchers("/api/baby-care/**").hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers("/api/home-care/**").hasRole("HOME_CARE_PROVIDER")
                        .requestMatchers("/api/homecare/provider/**").hasAnyAuthority("HOME_CARE_PROVIDER", "ROLE_HOME_CARE_PROVIDER")

                        .requestMatchers("/api/pharmacy/orders/patient/**").hasRole("PATIENT")
                        .requestMatchers("/api/pharmacy/orders/pharmacy/**").hasRole("PHARMACIST")
                        .requestMatchers("/api/pharmacy/orders/**").authenticated()

                        .requestMatchers("/laboratory/**").hasRole("LABORATORY_STAFF")
                        .requestMatchers("/api/lab-staff/**").hasRole("LABORATORY_STAFF")
                        .requestMatchers("/api/lab-narrator/**").hasRole("LABORATORY_STAFF")

                        .requestMatchers("/api/users/role/DOCTOR").hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers("/api/users/**", "/user/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()

                        .requestMatchers("/api/v1/patients/*/appointments").hasRole("PATIENT")
                        .requestMatchers("/api/v1/doctors/*/appointments").hasRole("DOCTOR")
                        .requestMatchers("/api/v1/appointments/**", "/api/v1/**").authenticated()
                        .requestMatchers("/availability/**", "/provider-calendar/**")
                            .hasAnyRole("DOCTOR", "NUTRITIONIST", "HOME_CARE_PROVIDER")

                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            // ⬇️ Remplacez par l'URL exacte de app-frontend1 depuis Azure Overview
            "https://app-frontend1.azurewebsites.net",
            "https://app-frontend-medicareai-2026-exhmfqgwewhzcjeu.swedencentral-01.azurewebsites.net"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
