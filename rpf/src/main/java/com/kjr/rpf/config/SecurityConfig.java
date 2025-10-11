package com.kjr.rpf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers("/resumes/upload", "/resumes/list", "/resumes/view/**").permitAll()
                .requestMatchers("/resumes/download/**", "/resumes/print/**", "/resumes/unmasked/**")
                    .authenticated()
                .anyRequest().permitAll()
            )
            .formLogin((form) -> form
                .loginPage("/login")
                .defaultSuccessUrl("/resumes/list", true)
                .permitAll()
            )
            .logout((logout) -> 
                logout.permitAll()
                    .logoutSuccessUrl("/resumes/list")
            )
            .csrf().disable();

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // In a production environment, replace this with a proper UserDetailsService
        // that loads users from a database
        UserDetails user = User.withUsername("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("USER")
            .build();
        
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
