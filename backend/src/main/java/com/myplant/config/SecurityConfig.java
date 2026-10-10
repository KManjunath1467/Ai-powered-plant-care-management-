package com.myplant.config;

import com.myplant.security.JwtAuthenticationFilter;
import com.myplant.security.JwtTokenProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {

        this.jwtTokenProvider = jwtTokenProvider;

    }

    // Password Encoder
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder(12);

    }

    // Authentication Manager
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {

        return config.getAuthenticationManager();

    }

    // Authentication Provider
    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    // Security Filter Chain
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http

                // Disable CSRF
                .csrf(csrf -> csrf.disable())

                // Stateless Session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Public + Protected Routes
                .authorizeHttpRequests(authz -> authz

                        // PUBLIC ROUTES
                        .requestMatchers(

                                "/",
                                "/error",

                                "/api/test",

                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/**",

                                "/api/plant-care-rules/**",

                                "/api/health",

                                "/api/ai/**",

                                "/actuator/**",

                                // IMAGE ACCESS
                                "/uploads/**",

                                // IMAGE UPLOAD API
                                "/api/plants/*/upload-image",
                                "/api/plants/*/upload",

                                // IOT SENSOR READING INGESTION
                                "/api/sensors/*/readings",
                                "/api/sensors/**/readings"

                        ).permitAll()

                        // ALL OTHER ROUTES REQUIRE LOGIN
                        .anyRequest().authenticated()
                )

                // CORS
                .cors(cors -> cors.configurationSource(request -> {

                    var corsConfig =
                            new org.springframework.web.cors.CorsConfiguration();

                    corsConfig.setAllowedOrigins(
                            java.util.List.of(
                                    "http://localhost:3000",
                                    "http://localhost:5173",
                                    "http://localhost:5174"
                            )
                    );

                    corsConfig.setAllowedMethods(
                            java.util.List.of("*")
                    );

                    corsConfig.setAllowedHeaders(
                            java.util.List.of("*")
                    );

                    corsConfig.setAllowCredentials(true);

                    return corsConfig;
                }))

                // JWT FILTER
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}