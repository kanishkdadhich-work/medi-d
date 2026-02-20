package com.medid.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class UserDetailsConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        var manager = new InMemoryUserDetailsManager();

        manager.createUser(User.withUsername("receptionist")
                .password(passwordEncoder.encode("receptpass"))
                .roles("RECEPTIONIST").build());

        manager.createUser(User.withUsername("doctor")
                .password(passwordEncoder.encode("doctorpass"))
                .roles("DOCTOR").build());

        manager.createUser(User.withUsername("pharmacist")
                .password(passwordEncoder.encode("pharmacistpass"))
                .roles("PHARMACIST").build());

        return manager;
    }
}
