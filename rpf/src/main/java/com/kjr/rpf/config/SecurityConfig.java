package com.kjr.rpf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/", "/search", "/resumes/search", "/resumes/list", "/resumes/view/**").permitAll()
                        .requestMatchers(
                                "/resumes/download/**", 
                                "/resumes/print/**", 
                                "/resumes/unmasked/**",
                                "/export/**"
                        ).hasRole("RECRUITER")
                        .requestMatchers("/resumes/upload").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin((form) -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/resumes/list", true)
                        .permitAll()
                )
                .logout((logout) ->
                        logout.permitAll()
                                .logoutSuccessUrl("/login?logout")
                                .deleteCookies("JSESSIONID")
                                .invalidateHttpSession(true)
                )
                .exceptionHandling(exception -> 
                    exception.accessDeniedPage("/access-denied")
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> 
                    session.maximumSessions(1)
                           .maxSessionsPreventsLogin(true)
                           .expiredUrl("/login?expired")
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // In a production environment, replace this with a proper UserDetailsService
        // that loads users from a database
        UserDetails recruiter = User.withUsername("recruiter")
                .password(passwordEncoder().encode("recruiter@123"))
                .roles("RECRUITER")
                .build();
                
        UserDetails admin = User.withUsername("admin")
                .password(passwordEncoder().encode("admin@123"))
                .roles("ADMIN", "RECRUITER")
                .build();

        return new InMemoryUserDetailsManager(recruiter, admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
