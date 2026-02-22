package com.medid.config;

import com.medid.repository.UserRepository;
import com.medid.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {


    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter, UserRepository userRepository) {
        this.jwtFilter = jwtFilter;
    }



    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/signup").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/pharmacy/reports/**").hasRole("ADMIN")
                        .requestMatchers("/api/pharmacy/**").hasAnyRole("PHARMACIST", "ADMIN")
                        .requestMatchers("/api/patients/**").hasAnyRole("RECEPTIONIST", "DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/appointments").hasAnyRole("RECEPTIONIST", "ADMIN")
                        .requestMatchers("/api/appointments/book", "/api/appointments/book-flex").hasAnyRole("RECEPTIONIST", "ADMIN")
                        .requestMatchers("/api/appointments/doctors").hasAnyRole("RECEPTIONIST", "ADMIN")
                        .requestMatchers("/api/appointments/doctor/**").hasRole("DOCTOR")
                        .requestMatchers("/api/appointments/*/cancel").hasAnyRole("DOCTOR", "RECEPTIONIST", "ADMIN")
                        .requestMatchers("/api/appointments/*/complete").hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers("/api/appointments/*/status").hasAnyRole("DOCTOR", "RECEPTIONIST", "ADMIN")
                        .requestMatchers("/api/prescriptions/create").hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers("/api/prescriptions/pending").hasAnyRole("PHARMACIST", "ADMIN")
                        .requestMatchers("/api/prescriptions/latest").hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/medicines/search", "/api/medicines").hasAnyRole("DOCTOR", "PHARMACIST", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/medicines").hasAnyRole("PHARMACIST", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/medicines/expired", "/api/medicines/expired/*").hasAnyRole("PHARMACIST", "ADMIN")
                        .anyRequest().authenticated()
                );

        // Add our JWT Filter before the standard Username/Password filter
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


}
