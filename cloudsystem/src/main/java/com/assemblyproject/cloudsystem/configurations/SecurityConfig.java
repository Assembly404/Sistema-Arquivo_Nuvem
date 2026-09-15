package com.assemblyproject.cloudsystem.configurations;

import com.assemblyproject.cloudsystem.filter.JwtAuthFilter;
import com.assemblyproject.cloudsystem.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final PasswordConfig passwordConfig;
    private final UserDetailsService userDetailsService;
    private final UserService userService;


    public SecurityConfig(JwtAuthFilter authFilter, UserDetailsService userDetailsService, PasswordConfig passwordConfig, UserService userService) {
        this.jwtAuthFilter = authFilter;
        this.userDetailsService = userDetailsService;
        this.passwordConfig = passwordConfig;
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/welcome", "/auth/addNewUser", "/auth/generateToken").permitAll()
                        .requestMatchers("/auth/user/**").hasAuthority("ROLE_USER")
                                .requestMatchers("/auth/admin/**").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationManager(authenticationManager(http))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

//    @Bean
//    public AuthenticationProvider authenticationProvider() {
//        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
//        provider.setPasswordEncoder(passwordConfig.passwordEncoder());
//        return provider;
//    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =         http.getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder
                .userDetailsService(userService)
                .passwordEncoder(passwordConfig.passwordEncoder());
        return authenticationManagerBuilder.build();
    }

}
