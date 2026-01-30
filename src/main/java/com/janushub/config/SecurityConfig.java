package com.janushub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

     @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ❌ desactivar CSRF (necessari per Postman)
            .csrf(csrf -> csrf.disable())

            // ✅ permetre TOTES les rutes
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )

            // ❌ desactivar formulari de login
            .formLogin(form -> form.disable())

            // ❌ desactivar basic auth
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
